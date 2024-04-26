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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.pallycon.pallyconsample.databinding.ActivityMainBinding
import com.pallycon.pallyconsample.dialog.TrackSelectDialog
import com.pallycon.widevine.exception.PallyConException
import com.pallycon.widevine.exception.PallyConLicenseServerException
import com.pallycon.widevine.model.DownloadState
import com.pallycon.widevine.model.PallyConCallback
import com.pallycon.widevine.model.PallyConDrmInformation
import com.pallycon.widevine.model.PallyConEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var adapter: RecyclerViewAdapter? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    val contents = ObjectSingleton.getInstance()

    var notificationPermissionToastShown = false
    var downloadContentDataNotifictionPermission: ContentData? = null

    private val pallyConEventListener: PallyConEventListener = object : PallyConEventListener {
        override fun onCompleted(currentUrl: String?) {
            val data = contents.contents.find { it.content.url == currentUrl }
            data?.let {
                val index = contents.contents.indexOf(it)
                contents.contents[index].subTitle = "COMPLETED"
                contents.contents[index].status = DownloadState.COMPLETED
                adapter?.notifyItemChanged(index)
            }
        }

        override fun onProgress(currentUrl: String?, percent: Float, downloadedBytes: Long) {
            val data = contents.contents.find { it.content.url == currentUrl }
            data?.let {
                val index = contents.contents.indexOf(it)
                contents.contents[index].subTitle =
                    "Downloading.. %" + String.format("%.0f", percent)
                if (contents.contents[index].status != DownloadState.COMPLETED) {
                    contents.contents[index].status = DownloadState.DOWNLOADING
                    adapter?.notifyItemChanged(index)
                }
            }
        }

        override fun onStopped(currentUrl: String?) {
            val data = contents.contents.find { it.content.url == currentUrl }
            data?.let {
                val index = contents.contents.indexOf(it)
                contents.contents[index].subTitle = "Stoped"
                contents.contents[index].status = DownloadState.STOPPED
                adapter?.notifyItemChanged(index)
            }
        }

        override fun onRestarting(currentUrl: String?) {
            val data = contents.contents.find { it.content.url == currentUrl }
            data?.let {
                val index = contents.contents.indexOf(it)
                contents.contents[index].subTitle = "Restart"
                contents.contents[index].status = DownloadState.RESTARTING
                adapter?.notifyItemChanged(index)
            }
        }

        override fun onRemoved(currentUrl: String?) {
            val data = contents.contents.find { it.content.url == currentUrl }
            data?.let {
                val index = contents.contents.indexOf(it)
                prepareForIndex(index)
                contents.contents[index].subTitle = "Not"
                contents.contents[index].status = DownloadState.NOT
                adapter?.notifyItemChanged(index)
            }
        }

        override fun onPaused(currentUrl: String?) {
            contents.contents.forEachIndexed { index, contentData ->
                var state = contents.contents[index].wvSDK.getDownloadState()
                if (state == DownloadState.DOWNLOADING) {
                    contents.contents[index].subTitle = "Paused"
                    contents.contents[index].status = DownloadState.PAUSED
                    adapter?.notifyItemChanged(index)
                }
            }
        }

        override fun onFailed(currentUrl: String?, e: PallyConException?) {
            val data = contents.contents.find { it.content.url == currentUrl }
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
            data?.let {
                val index = contents.contents.indexOf(it)
                contents.contents[index].subTitle = subTitle
                contents.contents[index].status = DownloadState.FAILED
                adapter?.notifyItemChanged(index)
            }

            e?.let {
                scope.launch(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "${it.msg}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onFailed(currentUrl: String?, e: PallyConLicenseServerException?) {
            val data = contents.contents.find { it.content.url == currentUrl }
            data?.let {
                val index = contents.contents.indexOf(it)
                contents.contents[index].subTitle = "Failed"
                contents.contents[index].status = DownloadState.FAILED
                adapter?.notifyItemChanged(index)
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
            url: String,
            keyData: ByteArray,
            requestData: Map<String, String>,
        ): ByteArray {
            val urlObject = URL(url)

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
        for (content in contents.contents) {
            content.wvSDK.release()
        }

        contents.contents.clear()
    }

    private fun initialize() {
        ObjectSingleton.getInstance().createContents(this, pallyConEventListener, pallyConCallback)

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
                SelectType.Pause -> pauseContentAll(contentData)
                SelectType.Resume -> resumeContent(contentData)
                SelectType.Play -> playContent(contentData)
                SelectType.Menu -> menuContent(contentData)
            }
        }
        adapter?.datalist = contents.contents
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
        for (i in 0 until contents.contents.size) {
            contents.contents[i].wvSDK.setPallyConEventListener(pallyConEventListener)
        }
    }

    fun prepare() {
        for (i in 0 until contents.contents.size) {
            prepareForIndex(i)
        }
    }

    private fun prepareForIndex(index: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            // migration is required in advance.
            try {
                val sdk = contents.contents[index].wvSDK
                if (sdk.needsMigrateDownloadedContent()
                ) {
                    val isSuccess = contents.contents[index].wvSDK.migrateDownloadedContent(
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
            val index = contents.contents.indexOf(contentData)
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

    private fun resumeContent(contentData: ContentData) {
        contentData.wvSDK.resumeAll()
    }

    private fun removeContent(contentData: ContentData) {
        try {
            contentData.wvSDK.remove()
            prepare()
        } catch (e: PallyConException.DownloadException) {
            print(e.message)
        }
    }

    private fun pauseContentAll(contentData: ContentData) {
        contentData.wvSDK.pauseAll()
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
            when (i) {
                0 -> {
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

                1 -> wvSDK.renewLicense()
                2 -> wvSDK.removeLicense()
                3 -> {
                    wvSDK.removeAll()
                    prepare()
                }

                4 -> {
                    var info = PallyConDrmInformation(0, 0)
                    try {
                        info = wvSDK.getDrmInformation()
                    } catch (e: PallyConException.DrmException) {
                        Toast.makeText(this@MainActivity, "${e.message()}", Toast.LENGTH_SHORT)
                            .show()
                    }

                    val alertBuilder = AlertDialog.Builder(this)
                    alertBuilder.setTitle("drm license info")
                        .setMessage(
                            "licenseDuration : ${info.licenseDuration} \n" +
                                    "playbackDuration : ${info.playbackDuration}"
                        )
                    alertBuilder.setNegativeButton("Cancel", null)
                    alertBuilder.show()
                }

                5 -> {
                    val info = wvSDK.getDownloadFileInformation()
                    val alertBuilder = AlertDialog.Builder(this)
                    alertBuilder.setTitle("drm license info")
                        .setMessage(
                            "downloaded size : ${info.downloadedFileSize} \n"
                        )
                    alertBuilder.setNegativeButton("Cancel", null)
                    alertBuilder.show()
                }

                6 -> {
                    var keySetId = wvSDK.getKeySetId()
                    val alertBuilder = AlertDialog.Builder(this)
                    alertBuilder.setTitle("KetSetId")
                        .setMessage(
                            "KeySetId : ${keySetId}"
                        )
                    alertBuilder.setNegativeButton("Cancel", null)
                    alertBuilder.show()
                }

                7 -> {
                    wvSDK.reProvisionRequest({}, { e ->
                        print(e.message())
                    })
                }
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