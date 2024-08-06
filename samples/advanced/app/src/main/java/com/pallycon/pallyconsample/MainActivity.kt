package com.pallycon.pallyconsample

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.upstream.CmcdConfiguration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.common.collect.ImmutableMap
import com.pallycon.pallyconsample.databinding.ActivityMainBinding
import com.pallycon.pallyconsample.dialog.TrackSelectDialog
import com.pallycon.widevine.exception.PallyConException
import com.pallycon.widevine.exception.PallyConLicenseServerException
import com.pallycon.widevine.model.DownloadState
import com.pallycon.widevine.model.PallyConCallback
import com.pallycon.widevine.model.PallyConDrmInformation
import com.pallycon.widevine.model.PallyConEventListener
import com.pallycon.widevine.sdk.PallyConWvSDK
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var adapter: RecyclerViewAdapter? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private var notificationPermissionToastShown = false
    private var downloadContentDataNotifictionPermission: ContentData? = null

    private val pallyConEventListener: PallyConEventListener = object : PallyConEventListener {
        override fun onCompleted(contentData: com.pallycon.widevine.model.ContentData) {
            ObjectSingleton.getInstance().contents.withIndex()
                .find { it.value.content == contentData }?.let { (index, data) ->
                    prepareForIndex(index)
                }
        }

        override fun onProgress(
            contentData: com.pallycon.widevine.model.ContentData,
            percent: Float,
            downloadedBytes: Long,
        ) {
            ObjectSingleton.getInstance().contents.withIndex()
                .find { it.value.content == contentData }?.let { (index, data) ->
                    if (data.status != DownloadState.COMPLETED) {
                        ObjectSingleton.getInstance().updateContentData(
                            index,
                            "Downloading.. %" + String.format("%.0f", percent),
                            DownloadState.DOWNLOADING
                        )
                        adapter?.updateItem(index, ObjectSingleton.getInstance().contents[index])
                    }
                }
        }

        override fun onStopped(contentData: com.pallycon.widevine.model.ContentData) {
            ObjectSingleton.getInstance().contents.withIndex()
                .find { it.value.content == contentData }?.let { (index, data) ->
                    ObjectSingleton.getInstance().updateContentData(
                        index, "Stopped",
                        DownloadState.STOPPED
                    )
                    adapter?.updateItem(index, ObjectSingleton.getInstance().contents[index])
                }
        }

        override fun onRestarting(contentData: com.pallycon.widevine.model.ContentData) {
            ObjectSingleton.getInstance().contents.withIndex()
                .find { it.value.content == contentData }?.let { (index, data) ->
                    ObjectSingleton.getInstance().updateContentData(
                        index, "Restart",
                        DownloadState.RESTARTING
                    )
                    adapter?.updateItem(index, ObjectSingleton.getInstance().contents[index])
                }
        }

        override fun onRemoved(contentData: com.pallycon.widevine.model.ContentData) {
            ObjectSingleton.getInstance().contents.withIndex()
                .find { it.value.content == contentData }?.let { (index, data) ->
                    prepareForIndex(index)
            }
        }

        override fun onPaused(contentData: com.pallycon.widevine.model.ContentData) {
            val contents = ObjectSingleton.getInstance()
            contents.contents.forEachIndexed { index, contentData ->
                var state = contents.contents[index].wvSDK.getDownloadState()
                if (state == DownloadState.DOWNLOADING) {
                    ObjectSingleton.getInstance()
                        .updateContentData(index, "Paused", DownloadState.PAUSED)
                    adapter?.updateItem(index, ObjectSingleton.getInstance().contents[index])
                }
            }
        }

        override fun onFailed(
            contentData: com.pallycon.widevine.model.ContentData,
            e: PallyConException?,
        ) {

            ObjectSingleton.getInstance().contents.withIndex()
                .find { it.value.content == contentData }?.let { (index, data) ->
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

                        else -> {
                            subTitle = "Failed"
                        }
                    }

                    ObjectSingleton.getInstance()
                        .updateContentData(index, subTitle, DownloadState.FAILED)
                    adapter?.updateItem(index, ObjectSingleton.getInstance().contents[index])
                }
            e?.let {
                scope.launch(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "${it.msg}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onFailed(
            contentData: com.pallycon.widevine.model.ContentData,
            e: PallyConLicenseServerException?,
        ) {
            ObjectSingleton.getInstance().contents.withIndex()
                .find { it.value.content == contentData }?.let { (index, data) ->
                    ObjectSingleton.getInstance()
                        .updateContentData(index, "Failed", DownloadState.FAILED)
                    adapter?.updateItem(index, ObjectSingleton.getInstance().contents[index])
                }

            if (e != null && e.errorCode() != 7127) {
                Toast.makeText(
                    this@MainActivity,
                    "Server Error - ${e!!.errorCode()}, ${e!!.message()}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private val pallyConCallback: PallyConCallback = object : PallyConCallback {
        override fun executeKeyRequest(
            contentData: com.pallycon.widevine.model.ContentData,
            keyData: ByteArray,
            requestData: Map<String, String>
        ): ByteArray {
            val urlObject = URL(contentData.drmConfig!!.drmLicenseUrl)

            val conn = urlObject.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"

            for (data in requestData) {
                conn.addRequestProperty(data.key, data.value)
            }

            conn.outputStream.use {
                it.write(keyData, 0, keyData.size)
            }

            return Util.toByteArray(conn.inputStream)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initialize()
    }

    override fun onDestroy() {
        super.onDestroy()
        ObjectSingleton.release()
        val contents = ObjectSingleton.getInstance()
        for (content in contents.contents) {
            content.wvSDK.release()
        }

        contents.contents.clear()
    }

    private fun initialize() {
        ObjectSingleton.getInstance().createContents(this)
        PallyConWvSDK.addPallyConEventListener(pallyConEventListener)

        val cmcdFactory = CmcdConfiguration.Factory { playerId ->
            val sessionId = UUID.randomUUID().toString()
            val contentId = UUID.randomUUID().toString()

            val cmcdRequestConfig = object : CmcdConfiguration.RequestConfig {
                override fun isKeyAllowed(key: String): Boolean {
                    return true
                }

                override fun getCustomData(): ImmutableMap<String, String> {
                    return ImmutableMap.of(
                        CmcdConfiguration.KEY_CMCD_OBJECT, "test=abcd"
                    )
                }

                override fun getRequestedMaximumThroughputKbps(throughputKbps: Int): Int {
                    return 5 * throughputKbps
                }
            }

            return@Factory CmcdConfiguration(sessionId, contentId, cmcdRequestConfig)
        }
        PallyConWvSDK.setCmcdConfigurationFactory(cmcdFactory)

        adapter = RecyclerViewAdapter() { contentData, selectType ->
            when (selectType) {
                SelectType.Download -> {
                    if (!notificationPermissionToastShown &&
                        Build.VERSION.SDK_INT >= 33 &&
                        checkSelfPermission(Api33.postNotificationPermissionString)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        downloadContentDataNotifictionPermission = contentData
                        requestPermissions(
                            arrayOf<String>(Api33.postNotificationPermissionString),
                            POST_NOTIFICATION_PERMISSION_REQUEST_CODE
                        )
                    } else {
                        downloadContent(contentData)
                    }
                }

                SelectType.Remove -> removeContent(contentData)
                SelectType.Stop -> stopContent(contentData)
                SelectType.Play -> playContent(contentData)
                SelectType.Menu -> menuContent(contentData)
            }
        }
        adapter?.dataList = ObjectSingleton.getInstance().contents
        binding.recyclerView.adapter = adapter!!
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        val animator: RecyclerView.ItemAnimator? = binding.recyclerView.getItemAnimator()
        if (animator is SimpleItemAnimator) {
            (animator as SimpleItemAnimator).setSupportsChangeAnimations(false)
        }

        prepare()
    }

    override fun onResume() {
        super.onResume()
        PallyConWvSDK.setPallyConCallback(pallyConCallback)
    }

    fun prepare() {
        for (i in 0 until ObjectSingleton.getInstance().contents.size) {
            prepareForIndex(i)
        }
    }

    private fun prepareForIndex(index: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            // migration is required in advance.
            val contents = ObjectSingleton.getInstance()
            try {
                val sdk = contents.contents[index].wvSDK
                if (sdk.needsMigrateDownloadedContent()
                ) {
                    val isSuccess = sdk.migrateDownloadedContent(
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

            val state = contents.contents[index].wvSDK.getDownloadState()
            if (state != DownloadState.COMPLETED) {
                contents.contents[index].wvSDK.getContentTrackInfo({ tracks ->
                    contents.contents[index].downloadTracks = tracks
                    contents.contents[index].status = state
                    contents.contents[index].subTitle = state.toString()
                    adapter?.notifyItemChanged(index)
                }, { e ->
                    e.printStackTrace()
                })
            } else {
                contents.contents[index].status = state
                contents.contents[index].subTitle = state.toString()
                adapter?.notifyItemChanged(index)
            }
        }
    }

    private fun downloadContent(contentData: ContentData) {
        if (contentData.downloadTracks == null) {
            val index = ObjectSingleton.getInstance().contents.indexOf(contentData)
            prepareForIndex(index)
        } else {
            TrackSelectDialog(contentData.downloadTracks!!) { track ->
                try {
                    contentData.wvSDK.download(track)
                } catch (e: PallyConException) {
                    Toast.makeText(this@MainActivity, "${e.message}", Toast.LENGTH_SHORT).show()
                }
            }.show(supportFragmentManager, TrackSelectDialog.TAG)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == POST_NOTIFICATION_PERMISSION_REQUEST_CODE) {
            handlePostNotificationPermissionGrantResults(grantResults)
        }
    }

    private fun handlePostNotificationPermissionGrantResults(grantResults: IntArray) {
        if (!notificationPermissionToastShown
            && (grantResults.size == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED)
        ) {
            Toast.makeText(
                applicationContext,
                "Notifications suppressed. Grant permission to see download notifications.",
                Toast.LENGTH_LONG
            ).show()
            notificationPermissionToastShown = true
        }
        if (downloadContentDataNotifictionPermission != null) {
            downloadContent(downloadContentDataNotifictionPermission!!)
            downloadContentDataNotifictionPermission = null
        }
    }

    private fun removeContent(contentData: ContentData) {
        try {
            contentData.wvSDK.remove()
        } catch (e: PallyConException.DownloadException) {
            print(e.message)
        }
    }

    private fun stopContent(contentData: ContentData) {
        contentData.wvSDK.stop()
    }

    private fun playContent(contentData: ContentData) {
        val intent = Intent(this, PlayerActivity::class.java)

        intent.apply {
            this.putExtra(PlayerActivity.CONTENT, contentData.content)
        }
        startActivity(intent)
    }

    private fun menuContent(contentData: ContentData) {
        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("Delete Menu")
        builder.setItems(
            arrayOf(
                "download pause all",
                "download resume all",
                "download license",
                "renew license",
                "remove license",
                "remove all",
                "license info",
                "downloaded file info",
                "KeySetId",
                "re-provisioning"
            )
        ) { _, i ->
            val wvSDK = contentData.wvSDK
            try {
                val wvSDK = contentData.wvSDK
                when (i) {
                    0 -> wvSDK.pauseAll()
                    1 -> wvSDK.resumeAll()
                    2 -> {
                        scope.launch {
                            wvSDK.downloadLicense(null, onSuccess = {
                                Toast.makeText(
                                    this@MainActivity,
                                    "success download license",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }, onFailed = { e ->
                                Toast.makeText(this@MainActivity, "${e.message()}", Toast.LENGTH_SHORT)
                                    .show()
                                print(e.msg)
                            })
                        }
                    }
                    3 -> wvSDK.renewLicense()
                    4 -> wvSDK.removeLicense()
                    5 -> {
                        wvSDK.removeAll()
                        prepare()
                    }
                    6 -> {
                        val info = wvSDK.getDrmInformation()
                        val alertBuilder = AlertDialog.Builder(this)
                        alertBuilder.setTitle("drm license info")
                            .setMessage(
                                "licenseDuration : ${info.licenseDuration} \n" +
                                        "playbackDuration : ${info.playbackDuration}"
                            )
                        alertBuilder.setNegativeButton("Cancel", null)
                        alertBuilder.show()
                    }
                    7 -> {
                        val info = wvSDK.getDownloadFileInformation()
                        val alertBuilder = AlertDialog.Builder(this)
                        alertBuilder.setTitle("drm license info")
                            .setMessage(
                                "downloaded size : ${info.downloadedFileSize} \n"
                            )
                        alertBuilder.setNegativeButton("Cancel", null)
                        alertBuilder.show()
                    }
                    8 -> {
                        var keySetId = wvSDK.getKeySetId()
                        val alertBuilder = AlertDialog.Builder(this)
                        alertBuilder.setTitle("KetSetId")
                            .setMessage(
                                "KeySetId : ${keySetId}"
                            )
                        alertBuilder.setNegativeButton("Cancel", null)
                        alertBuilder.show()
                    }
                    9 -> {
                        wvSDK.reProvisionRequest({}, { e ->
                            print(e.message())
                        })
                    }
                }
            } catch (e: PallyConException.DrmException) {
                Toast.makeText(this@MainActivity, "${e.message()}", Toast.LENGTH_SHORT).show()
            } catch (e: PallyConLicenseServerException) {
                Toast.makeText(this@MainActivity, "${e.message()}", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel", null)
        val dialog: Dialog = builder.create()
        dialog.show()
    }

    companion object {
        private const val POST_NOTIFICATION_PERMISSION_REQUEST_CODE = 100

        @RequiresApi(33)
        private object Api33 {
            @get:DoNotInline
            val postNotificationPermissionString: String
                get() = Manifest.permission.POST_NOTIFICATIONS
        }
    }
}