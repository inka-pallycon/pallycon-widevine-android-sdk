package com.pallycon.castsample

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.util.Util
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.pallycon.castsample.databinding.ActivityMainBinding
import com.pallycon.castsample.dialog.TrackSelectDialog
import com.pallycon.widevine.exception.PallyConException
import com.pallycon.widevine.exception.PallyConLicenseServerException
import com.pallycon.widevine.model.DownloadState
import com.pallycon.widevine.model.PallyConCallback
import com.pallycon.widevine.model.PallyConDrmConfigration
import com.pallycon.widevine.model.PallyConEventListener
import com.pallycon.widevine.sdk.PallyConWvSDK
import kotlinx.coroutines.*
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import com.pallycon.widevine.model.ContentData as PallyConData


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var adapter: RecyclerViewAdapter? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    val contents = mutableListOf<ContentData>()

    private val pallyConEventListener: PallyConEventListener = object : PallyConEventListener {
        override fun onCompleted(currentUrl: String?) {
            val data = contents.find { it.content.url == currentUrl }
            data?.let {
                val index = contents.indexOf(it)
                contents[index].subTitle = "COMPLETED"
                contents[index].status = DownloadState.COMPLETED
                adapter?.notifyItemChanged(index)
            }
        }

        override fun onProgress(currentUrl: String?, percent: Float, downloadedBytes: Long) {
            val data = contents.find { it.content.url == currentUrl }
            data?.let {
                val index = contents.indexOf(it)
                contents[index].subTitle = "Downloading.. %" + String.format("%.0f", percent)
                if (contents[index].status != DownloadState.COMPLETED) {
                    contents[index].status = DownloadState.DOWNLOADING
                    adapter?.notifyItemChanged(index)
                }
            }
        }

        override fun onStopped(currentUrl: String?) {
            val data = contents.find { it.content.url == currentUrl }
            data?.let {
                val index = contents.indexOf(it)
                contents[index].subTitle = "Stoped"
                contents[index].status = DownloadState.STOPPED
                adapter?.notifyItemChanged(index)
            }
        }

        override fun onRestarting(currentUrl: String?) {
            val data = contents.find { it.content.url == currentUrl }
            data?.let {
                val index = contents.indexOf(it)
                contents[index].subTitle = "Restart"
                contents[index].status = DownloadState.RESTARTING
                adapter?.notifyItemChanged(index)
            }
        }

        override fun onRemoved(currentUrl: String?) {
            val data = contents.find { it.content.url == currentUrl }
            data?.let {
                val index = contents.indexOf(it)
                prepareForIndex(index)
                contents[index].subTitle = "Not"
                contents[index].status = DownloadState.NOT
                adapter?.notifyItemChanged(index)
            }
        }

        override fun onPaused(currentUrl: String?) {
            contents.forEachIndexed { index, contentData ->
                var state = contents[index].wvSDK.getDownloadState()
                if (state == DownloadState.DOWNLOADING) {
                    contents[index].subTitle = "Paused"
                    contents[index].status = DownloadState.PAUSED
                    adapter?.notifyItemChanged(index)
                }
            }
        }

        override fun onFailed(currentUrl: String?, e: PallyConException?) {
            val data = contents.find { it.content.url == currentUrl }
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
                val index = contents.indexOf(it)
                contents[index].subTitle = subTitle
                contents[index].status = DownloadState.FAILED
                adapter?.notifyItemChanged(index)
            }

            e?.let {
                scope.launch(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "${it.msg}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onFailed(currentUrl: String?, e: PallyConLicenseServerException?) {
            val data = contents.find { it.content.url == currentUrl }
            data?.let {
                val index = contents.indexOf(it)
                contents[index].subTitle = "Failed"
                contents[index].status = DownloadState.FAILED
                adapter?.notifyItemChanged(index)
            }

            if (e != null && e.errorCode() != 7127) {
                Toast.makeText(this@MainActivity, "Server Error - ${e!!.errorCode()}, ${e!!.message()}", Toast.LENGTH_SHORT).show()
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
        for (content in contents) {
            content.wvSDK.release()
        }

        contents.clear()
    }

    private fun initialize() {
        initializeList()

        adapter = RecyclerViewAdapter() { contentData, selectType ->
            when (selectType) {
                SelectType.Download -> downloadContent(contentData)
                SelectType.Remove -> removeContent(contentData)
                SelectType.Pause -> pauseContentAll(contentData)
                SelectType.Resume -> resumeContent(contentData)
                SelectType.Play -> playContent(contentData)
                SelectType.Menu -> menuContent(contentData)
            }
        }
        adapter?.datalist = contents
        binding.recyclerView.adapter = adapter!!
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        val animator: RecyclerView.ItemAnimator? = binding.recyclerView.getItemAnimator()
        if (animator is SimpleItemAnimator) {
            (animator as SimpleItemAnimator).setSupportsChangeAnimations(false)
        }

        prepare()
    }

    private fun initializeList() {
        val fi = this.getExternalFilesDir(null) ?: this.filesDir
        val localPath = File(fi, "downloads").toString()

        val config = PallyConDrmConfigration(
            "DEMO",
            "eyJkcm1fdHlwZSI6IldpZGV2aW5lIiwic2l0ZV9pZCI6IkRFTU8iLCJ1c2VyX2lkIjoidGVzdFVzZXIiLCJjaWQiOiJkZW1vLWJiYi1zaW1wbGUiLCJwb2xpY3kiOiI5V3FJV2tkaHB4VkdLOFBTSVljbkp1dUNXTmlOK240S1ZqaTNpcEhIcDlFcTdITk9uYlh6QS9pdTdSa0Vwbk85c0YrSjR6R000ZkdCMzVnTGVORGNHYWdPY1Q4Ykh5c3k0ZHhSY2hYV2tUcDVLdXFlT0ljVFFzM2E3VXBnVVdTUCIsInJlc3BvbnNlX2Zvcm1hdCI6Im9yaWdpbmFsIiwia2V5X3JvdGF0aW9uIjpmYWxzZSwidGltZXN0YW1wIjoiMjAyMi0wOS0xOVQwNzo0Mjo0MFoiLCJoYXNoIjoiNDBDb1RuNEpFTnpZUHZrT1lTMHkvK2VIN1dHK0ZidUIvcThtR3VoaHVNRT0ifQ=="
        )

        val data = PallyConData(
            contentId = "demo-bbb-simple",
            url = "https://contents.pallycon.com/DEMO/app/big_buck_bunny/dash/stream.mpd",
            localPath = localPath,
            drmConfig = config,
        )
        val wvSDK = PallyConWvSDK.createPallyConWvSDK(
            this,
            data)

        wvSDK.setPallyConEventListener(pallyConEventListener)
//        wvSDK.setPallyConCallback(pallyConCallback)

        val state = wvSDK.getDownloadState()
        contents.add(
            ContentData(
                "basic content",
                state,
                state.toString(),
                data,
                wvSDK,
                null,
                "demo-bbb-simple",
                "demo-bbb-simple"
            )
        )

        val config2 = PallyConDrmConfigration(
            "DEMO",
            "eyJrZXlfcm90YXRpb24iOmZhbHNlLCJyZXNwb25zZV9mb3JtYXQiOiJvcmlnaW5hbCIsInVzZXJfaWQiOiJwYWxseWNvbiIsImRybV90eXBlIjoid2lkZXZpbmUiLCJzaXRlX2lkIjoiREVNTyIsImhhc2giOiJkNTBDSVVUS1RwRDl6T3dGaU9DSysrXC83Q3pLOStZN3NkcHFhUUppdDJWQT0iLCJjaWQiOiJUZXN0UnVubmVyIiwicG9saWN5IjoiOVdxSVdrZGhweFZHSzhQU0lZY25Kc2N2dUE5c3hndWJMc2QrYWp1XC9ib21RWlBicUkreGFlWWZRb2Nja3Z1RWZBYXFkVzVoWGdKTmdjU1MzZlM3bzhNczB3QXNuN05UbmJIUmtwWDFDeTEyTkhwMlZPN1pMeFJvZDhVdkUwZnBFbUpYOUpuRDh6ZktkdE9RWk9UYXljK280RzNCT0xmU29OaFpWbkIwUGxEbW1rVk5jbXpndko2YloxdXBudjFcLzJFM2lXZXd3eklTNFVOQlhTS21zVUFCZnBRQjg4Q2VJYlZSM0hKZWJvcEpwZG1DTFFvRmtCT09DQU9qWElBOUVHIiwidGltZXN0YW1wIjoiMjAyMi0xMC0xMVQwNzowMToxN1oifQ=="
        )
        val data2 = PallyConData(
            contentId = "TestRunner_DASH",
            url = "https://contents.pallycon.com/TEST/PACKAGED_CONTENT/TEST_SIMPLE/dash/stream.mpd",
            localPath = localPath,
            drmConfig = config2
        )
        val wvSDK2 = PallyConWvSDK.createPallyConWvSDK(
            this,
            data2
        )
        val state2 = wvSDK2.getDownloadState()
        contents.add(
            ContentData(
                "short duration content",
                state2,
                state2.toString(),
                data2,
                wvSDK2,
                null,
                "TestRunner",
                "TestRunner"
            )
        )

        val config3 = PallyConDrmConfigration(
            "DEMO",
            "eyJrZXlfcm90YXRpb24iOmZhbHNlLCJyZXNwb25zZV9mb3JtYXQiOiJvcmlnaW5hbCIsInVzZXJfaWQiOiJUZXN0UnVubmVyIiwiZHJtX3R5cGUiOiJ3aWRldmluZSIsInNpdGVfaWQiOiJERU1PIiwiaGFzaCI6IjdqcjNOb2w4N1l1U29hNlk2RXJCMFVoMkNIM1pWR2VBUVNtMTh5YkZheFU9IiwiY2lkIjoiVGVzdFJ1bm5lciIsInBvbGljeSI6IjlXcUlXa2RocHhWR0s4UFNJWWNuSnNjdnVBOXN4Z3ViTHNkK2FqdVwvYm9tUVpQYnFJK3hhZVlmUW9jY2t2dUVmQWFxZFc1aFhnSk5nY1NTM2ZTN284TnNqd3N6ak11dnQrMFF6TGtaVlZObXgwa2VmT2Uyd0NzMlRJVGdkVTRCdk45YWJoZDByUWtNSXJtb0llb0pIcUllSGNSdlZmNlQxNFJtVEFERXBDWTdQSGZQZlwvVkZZXC9WYlh1eFhcL1dUdFZTRkpTSDlzeHB3UUlRWHI1QjZSK0FhYWZTZlZYU0trNG1WRmxlMlBcL3Byamg1OCtiT2hidFU0NDRseDlvcmVHNSIsInRpbWVzdGFtcCI6IjIwMjMtMDUtMTlUMDI6MTM6NTlaIn0="
        )
        val data3 = PallyConData(
            contentId = "TestRunner_HLS",
            url = "https://contents.pallycon.com/TEST/PACKAGED_CONTENT/TEST_SIMPLE/cmaf/master.m3u8",
            localPath = localPath,
            drmConfig = config3,
        )
        val wvSDK3 = PallyConWvSDK.createPallyConWvSDK(
            this,
            data3
        )
        val state3 = wvSDK3.getDownloadState()
        contents.add(
            ContentData(
                "hls",
                state3,
                state3.toString(),
                data3,
                wvSDK3,
                null,
                "TestRunner",
                "TestRunner"
            )
        )
    }

    override fun onResume() {
        super.onResume()
        for (i in 0 until contents.size) {
            contents[i].wvSDK.setPallyConEventListener(pallyConEventListener)
        }
    }

    fun prepare() {
        for (i in 0 until contents.size) {
            prepareForIndex(i)
        }
    }

    private fun prepareForIndex(index: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            // If you were using the existing 2.x.x version of the widevine sdk,
            // migration is required in advance.
            try {
                val sdk = contents[index].wvSDK
                if (sdk.needsMigrateDownloadedContent()
                ) {
                    val isSuccess = contents[index].wvSDK.migrateDownloadedContent(
                        contentName = contents[index].name, // content's name which will be used for the name of downloaded content's folder
                        downloadedFolderName = null // content download folder name
                    )

                    if (!isSuccess) {
                        print("failed migrate downloaded content")
                    }
                }
            } catch (e: PallyConException.MigrationException) {
                print(e)
            } catch (e: PallyConException.MigrationLocalPathException) {
                // you have to change localPath
                // ex) val localPath = File(fi, "downloads_v2").toString()
                print(e)
            }

            val state = contents[index].wvSDK.getDownloadState()
            if (state != DownloadState.COMPLETED) {
                contents[index].wvSDK.getContentTrackInfo({ tracks ->
                    contents[index].downloadTracks = tracks
                    contents[index].status = state
                    contents[index].subTitle = state.toString()
                    adapter?.notifyItemChanged(index)
                }, { e ->
                    e.printStackTrace()
                })
            } else {
                contents[index].status = state
                contents[index].subTitle = state.toString()
                adapter?.notifyItemChanged(index)
            }
        }
    }

    private fun downloadContent(contentData: ContentData) {
        if (contentData.downloadTracks == null) {
            val index = contents.indexOf(contentData)
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
            try {
                val wvSDK = contentData.wvSDK
                when (i) {
                    0 -> {
                        scope.launch {
                            wvSDK.downloadLicense(null,
                                onSuccess = {
                                Toast.makeText(this@MainActivity, "success download license", Toast.LENGTH_SHORT).show()
                            }, onFailed = { e ->
                                Toast.makeText(this@MainActivity, "${e.message()}", Toast.LENGTH_SHORT).show()
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
}