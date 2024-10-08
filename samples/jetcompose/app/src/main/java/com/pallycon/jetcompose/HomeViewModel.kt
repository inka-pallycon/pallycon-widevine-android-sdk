package com.pallycon.jetcompose

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import com.pallycon.widevine.exception.PallyConException
import com.pallycon.widevine.exception.PallyConLicenseServerException
import com.pallycon.widevine.model.DownloadState
import com.pallycon.widevine.model.PallyConDrmInformation
import com.pallycon.widevine.model.PallyConEventListener
import com.pallycon.widevine.sdk.PallyConWvSDK
import com.pallycon.widevine.track.PallyConDownloaderTracks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel : ViewModel() {
    var context: Context? = null
    val scope = CoroutineScope(Dispatchers.Main)

    private val _contents = MutableStateFlow(ObjectSingleton.getInstance().getContentDatas())
    val contents: StateFlow<List<ContentData>> = _contents.asStateFlow()
    val notificationPermissionToastShown = MutableStateFlow(false)

    private val pallyConEventListener: PallyConEventListener = object : PallyConEventListener {
        override fun onCompleted(contentData: com.pallycon.widevine.model.ContentData) {
            updateContentDataFromListners(contentData.url, "COMPLETED", DownloadState.COMPLETED)
        }

        override fun onProgress(
            contentData: com.pallycon.widevine.model.ContentData,
            percent: Float,
            downloadedBytes: Long
        ) {
            val copyList = contents.value.toMutableList()
            val index = copyList.indexOfFirst { it.content == contentData }
            if (index != -1) {
                var status = copyList[index].status
                if (copyList[index].status != DownloadState.COMPLETED) {
                    status = DownloadState.DOWNLOADING
                }
                copyList[index] = copyList[index].copy(
                    subTitle = "Downloading.. %" + String.format("%.0f", percent),
                    status = status
                )
                modifyContentDataList(copyList)
            }
        }

        override fun onStopped(contentData: com.pallycon.widevine.model.ContentData) {
            updateContentDataFromListners(contentData.url, "Stoped", DownloadState.STOPPED)
        }

        override fun onRestarting(contentData: com.pallycon.widevine.model.ContentData) {
            updateContentDataFromListners(contentData.url, "Restart", DownloadState.RESTARTING)
        }

        override fun onRemoved(contentData: com.pallycon.widevine.model.ContentData) {
            updateContentDataFromListners(contentData.url, "Not", DownloadState.NOT)
        }

        override fun onPaused(contentData: com.pallycon.widevine.model.ContentData) {
            val copyList = contents.value.toMutableList()
            copyList.forEachIndexed { index, contentData ->
                var state = copyList[index].wvSDK.getDownloadState()
                if (state == DownloadState.DOWNLOADING) {
                    copyList[index] = copyList[index].copy(
                        subTitle = "Paused",
                        status = DownloadState.PAUSED
                    )
                }
            }
            modifyContentDataList(copyList)
        }

        override fun onFailed(
            contentData: com.pallycon.widevine.model.ContentData,
            e: PallyConException?
        ) {
            var subTitle: String
            when (e) {
                is PallyConException.DrmException -> {
                    subTitle = "Drm Error"
                }
                is PallyConException.DownloadException -> {
                    subTitle = "Download Error"
                }
                is PallyConException.DetectedDeviceTimeModifiedException -> {
                    subTitle = "Device time modified Error"
                }
                is PallyConException.NetworkConnectedException -> {
                    subTitle = "Network Error"
                }
                is PallyConException.PallyConLicenseCipherException -> {
                    // Ignore the error except when using the LicenseCipher function.
                    return
                }
                else -> {
                    subTitle = "Failed"
                }
            }
            updateContentDataFromListners(contentData.url, subTitle, DownloadState.FAILED)

            e?.let { e ->
                scope.launch(Dispatchers.Main) {
                    context?.let { context ->
                        Toast.makeText(context, "${e.msg}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        override fun onFailed(
            contentData: com.pallycon.widevine.model.ContentData,
            e: PallyConLicenseServerException?
        ) {
            updateContentDataFromListners(contentData.url, "Failed", DownloadState.FAILED)

            if (e != null && e.errorCode() != 7127) {
                scope.launch(Dispatchers.Main) {
                    context?.let { context ->
                        Toast.makeText(context, "Server Error - ${e!!.errorCode()}, ${e!!.message()}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun initialize(context: Context) {
        this.context = context
        if (ObjectSingleton.getInstance().contents.isEmpty()) {
            PallyConWvSDK.addPallyConEventListener(pallyConEventListener)
            ObjectSingleton.getInstance().createContents(context)
        }
        modifyContentDataList(ObjectSingleton.getInstance().getContentDatas())
    }

    fun prepare() {
        for (i in 0 until (contents.value.size)) {
            prepareForIndex(i)
        }
    }

    private fun migrateForContentData(contentData: ContentData) {
        try {
            val sdk = contentData.wvSDK
            if (sdk.needsMigrateDownloadedContent()
            ) {
                val isSuccess = contentData.wvSDK.migrateDownloadedContent(
                    contentName = "", // content's name which will be used for the name of downloaded content's folder
                    downloadedFolderName = null // content download folder name
                )

                if (!isSuccess) {
                    print("failed migrate downloaded content")
                }
            }
        } catch (e: PallyConException.ContentDataException) {
            print(e)
        } catch (e: PallyConException.MigrationException) {
            print(e)
        } catch (e: PallyConException.MigrationLocalPathException) {
            // you have to change localPath
            // ex) val localPath = File(fi, "downloads_v2").toString()
            print(e)
        }
    }

    private fun prepareForIndex(index: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            // migration is required in advance.
            val copyList = contents.value.toMutableList()
            migrateForContentData(copyList[index])

            val state = copyList[index].wvSDK.getDownloadState()
            if (state != DownloadState.COMPLETED) {
                copyList[index].wvSDK.getContentTrackInfo({ tracks ->
                    updateContentData(index, copyList[index].copy(
                        downloadTracks = tracks,
                        status = state,
                        subTitle =  state.toString()
                    ))
                }, { e ->
                    e.printStackTrace()
                })
            } else {
                copyList[index] = copyList[index].copy(
                    status = state,
                    subTitle =  state.toString()
                )
                updateContentData(index, copyList[index].copy(
                    status = state,
                    subTitle =  state.toString()
                ))
            }
        }
    }

    fun remove(contentData: ContentData) {
        contentData.wvSDK.remove()
        val index = contents.value.indexOf(contentData)
        prepareForIndex(index)
    }

    fun getDrmInformation(contentData: ContentData): PallyConDrmInformation {
        return try {
            contentData.wvSDK.getDrmInformation()
        } catch (e: PallyConException.DrmException) {
            PallyConDrmInformation(0, 0)
        }
    }

    fun pauseAll() {
        contents.value.first().wvSDK.pauseAll()
    }

    fun resumeAll() {
        contents.value.first().wvSDK.resumeAll()
    }

    fun download(contentData: ContentData, track: PallyConDownloaderTracks) {
        try {
            contentData.wvSDK.download(track)
        } catch (e: PallyConException.ContentDataException) {
            print(e)
        } catch (e: PallyConException.DownloadException) {
            print(e)
        }
    }

    fun modifyContentDataList(newData: List<ContentData>) {
        _contents.value = newData
    }

    fun updateContentData(index: Int, contentData: ContentData) {
        val copyList = _contents.value.toMutableList()
        copyList[index] = contentData
        modifyContentDataList(copyList)
    }

    fun updateContentDataFromListners(url: String?, subTitle: String, status: DownloadState) {
        val copyList = contents.value.toMutableList()
        val index = copyList.indexOfFirst { it.content.url == url }
        if (index != -1) {
            copyList[index] = copyList[index].copy(
                subTitle = subTitle,
                status = status
            )
        }
        modifyContentDataList(copyList)
    }

    companion object {
        private const val POST_NOTIFICATION_PERMISSION_REQUEST_CODE = 100

        @RequiresApi(33)
        object Api33 {
            @get:DoNotInline
            val postNotificationPermissionString: String
                get() = Manifest.permission.POST_NOTIFICATIONS
        }
    }
}