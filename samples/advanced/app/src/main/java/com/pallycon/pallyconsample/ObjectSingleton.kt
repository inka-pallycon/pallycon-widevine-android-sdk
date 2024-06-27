package com.pallycon.pallyconsample

import android.content.Context
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.RetryPolicy
import com.bitmovin.analytics.media3.exoplayer.api.IMedia3ExoPlayerCollector
import com.pallycon.widevine.model.DownloadState
import com.pallycon.widevine.model.PallyConCallback
import com.pallycon.widevine.model.PallyConDrmConfigration
import com.pallycon.widevine.model.PallyConEventListener
import com.pallycon.widevine.sdk.PallyConWvSDK
import java.io.File

class ObjectSingleton {
    val contents = mutableListOf<ContentData>()
    val downloadChannel = "download_channel"
    var context: Context? = null

//    private val analyticsConfig = AnalyticsConfig("e18f4a0f-e96b-4051-9468-730ac683a603")
//    private val analyticsConfig = AnalyticsConfig("302d9067-3462-4253-a867-b92e6b2ed237")
    private val analyticsConfig = AnalyticsConfig(licenseKey = "302d9067-3462-4253-a867-b92e6b2ed237", retryPolicy = RetryPolicy.LONG_TERM)
    var analyticsCollector: IMedia3ExoPlayerCollector? = null

    companion object {
        private var instance: ObjectSingleton? = null

        fun getInstance(): ObjectSingleton {
            return instance ?: synchronized(this) {
                instance ?: ObjectSingleton().also {
                    instance = it
                }
            }
        }

        fun release() {
            instance = null
        }
    }

    fun updateContentData(index: Int, subTitle: String, status: DownloadState) {
        contents[index].subTitle = subTitle
        contents[index].status = status
    }

    fun createContents(context: Context, pallyConEventListener: PallyConEventListener?, pallyConCallback: PallyConCallback?) {
        this.context = context

        val fi = context.getExternalFilesDir(null) ?: context.filesDir
        val localPath = File(fi, "downloads").toString()
        PallyConWvSDK.setDownloadDirectory(context, localPath)
        val config = PallyConDrmConfigration(
            "DEMO",
            "eyJrZXlfcm90YXRpb24iOmZhbHNlLCJyZXNwb25zZV9mb3JtYXQiOiJvcmlnaW5hbCIsInVzZXJfaWQiOiJ0ZXN0LXVzZXIiLCJkcm1fdHlwZSI6IndpZGV2aW5lIiwic2l0ZV9pZCI6IkRFTU8iLCJoYXNoIjoicEMxWlB0QlF4ZHpoRldESnQrNkhrQTZsV2dzUEhCMFhERWlUYjVBWGRFaz0iLCJjaWQiOiJwYWxtdWx0aSIsInBvbGljeSI6IjlXcUlXa2RocHhWR0s4UFNJWWNuSnNjdnVBOXN4Z3ViTHNkK2FqdVwvYm9rQzlTMit4QVBZUmJtZno4dG9FQjM4UGRyM0JqeHZId0J1WlA2WUttY042dHlFSW16a05zQ1I2ZllkUlVvMG9ielVkdVhNdTAydjlxemZEMXdzUWlkdEJtdUZUNjNKc01KTzdDZ2xyWWMyWkRaeWt0dHRQNW1FenR5Mm9aOWdFaVVkYXFCTFE5Sm5sNlZQMko3THlZQVdJNmI1a0g3aUdBMXE4WDE2MVFcL2V5OFo1UHpqZDRhc2cwUVRJaXUwb3VuMzdGc0ZPbzFyYkZcL3FBc0IzYUlEUDdQc2FjTE5Wc2l4bGVCSEV6WjZUWEVTelVFdWtNV0l3R29TdjdCK1d1YW5vTnVIK0RVRitZalByTU5Gbjk1TlwvQ3hJeUY5enQ5UlplT0Q2VHA4UDhHaEZIRlRTSXFSWEVPSkdKMTRUYlZ6TEI3UmptM2tWaUZIRGduTjY1Nks0S3RxMGxuNktXbWRacThobGtMNUVDU21CVUNzZDh5a3lGXC92ckFnS2VnWVpranhmZ0VmT096QTZXYXFJaXl6VTh1YWxxV1RGT0hKTVArY3hLSkNcL2gyd2Y3MVVHQU9vTFpyUzRDOEcxYm51c1lqUklJVEpnZXpzeStcL0kzWXdRWmZiamhQRUZaSGRyOHNtSERydmE0cXNzVk9cL0JUckhsb1JDWTRRQnRWZzZia2g4MURleHlRVjNNZExyWVN5RXoycDlqcU9oY2tpNCtEMHRcL0VhUTNUODBkZHdJdEZCeWtKaGI5MmFFNEZJc09wU3VuNVgrNUlVRGgycTRvanI3cTFNKzNGaU1ocFZ0TWpGSm56UE5JV0NqSytkN1RHMnRKZHRhTnBoTHc3bk1cL1NJZlRUNHd4ZVUxQTk5d3E1ZE1iVWNmTGd6dWZuTkJKSFA1RnY2Z1IxbmNYWDFnVWgyQUhlVHpPUU5DYkl4SllmOCswTksrRVVJUWZVcDQ2TWFIckI4TXFSTkFsTUQrR0gzRHptMXpOUlM3bFNHSkRCS1YwZVNRY1V0eEFZV0dESXVYbDNBanRqdW81YU1wWlV3c3hGSm9CVGNFZHNUTWphY2k4MmhMOW5Jbks0RnBOMWxxQlFcL0FkVmpsRTNzdTROR1dtcms4WW1SVmtvellKaXZOYXBmSXBCVUMyb0dqZGN2SVwvVnVjR0xydlhLTThNbThrMXprZ0FFVWR0RGVLR3dpdk1vV1NDZHh4NUhRZW83N3lLMjNOUk9zZ3RjRjN3YjNJbDVsTCtwXC9ENGhiSHh4XC8xaG5PZ0lBNEJtUHE2Z1NTMD0iLCJ0aW1lc3RhbXAiOiIyMDI0LTA1LTE0VDAyOjQ0OjM4WiJ9"
        )
        val data = com.pallycon.widevine.model.ContentData(
            "demo-bbb-simple",
            "https://contents.pallycon.com/DEMO/app/big_buck_bunny/dash/stream.mpd",
            config
        )
        val wvSDK = PallyConWvSDK.createPallyConWvSDK(
            context,
            data
        )

        wvSDK.setPallyConEventListener(pallyConEventListener)
//        wvSDK.setPallyConCallback(pallyConCallback)
        wvSDK.setDownloadService(DemoDownloadService::class.java)
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
        val data2 = com.pallycon.widevine.model.ContentData(
            contentId = "TestRunner_User",
            url = "https://contents.pallycon.com/TEST/PACKAGED_CONTENT/TEST_SIMPLE/dash/stream.mpd",
            drmConfig = config2
        )
        val wvSDK2 = PallyConWvSDK.createPallyConWvSDK(
            context,
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
                "TestRunner_User1",
                "TestRunner_User1"
            )
        )

        val config3 = PallyConDrmConfigration(
            "DEMO",
            "eyJrZXlfcm90YXRpb24iOmZhbHNlLCJyZXNwb25zZV9mb3JtYXQiOiJvcmlnaW5hbCIsInVzZXJfaWQiOiJUZXN0UnVubmVyIiwiZHJtX3R5cGUiOiJ3aWRldmluZSIsInNpdGVfaWQiOiJERU1PIiwiaGFzaCI6IjdqcjNOb2w4N1l1U29hNlk2RXJCMFVoMkNIM1pWR2VBUVNtMTh5YkZheFU9IiwiY2lkIjoiVGVzdFJ1bm5lciIsInBvbGljeSI6IjlXcUlXa2RocHhWR0s4UFNJWWNuSnNjdnVBOXN4Z3ViTHNkK2FqdVwvYm9tUVpQYnFJK3hhZVlmUW9jY2t2dUVmQWFxZFc1aFhnSk5nY1NTM2ZTN284TnNqd3N6ak11dnQrMFF6TGtaVlZObXgwa2VmT2Uyd0NzMlRJVGdkVTRCdk45YWJoZDByUWtNSXJtb0llb0pIcUllSGNSdlZmNlQxNFJtVEFERXBDWTdQSGZQZlwvVkZZXC9WYlh1eFhcL1dUdFZTRkpTSDlzeHB3UUlRWHI1QjZSK0FhYWZTZlZYU0trNG1WRmxlMlBcL3Byamg1OCtiT2hidFU0NDRseDlvcmVHNSIsInRpbWVzdGFtcCI6IjIwMjMtMDUtMTlUMDI6MTM6NTlaIn0="
        )
        val data3 = com.pallycon.widevine.model.ContentData(
            contentId = "TestRunner_HLS",
            url = "https://contents.pallycon.com/TEST/PACKAGED_CONTENT/TEST_SIMPLE/cmaf/master.m3u8",
            drmConfig = config3
        )
        val wvSDK3 = PallyConWvSDK.createPallyConWvSDK(
            context,
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

        val config4 = PallyConDrmConfigration(
            siteId = "4LYI",
            token = "eyJrZXlfcm90YXRpb24iOmZhbHNlLCJyZXNwb25zZV9mb3JtYXQiOiJvcmlnaW5hbCIsInVzZXJfaWQiOiJ0ZXN0LXVzZXIiLCJkcm1fdHlwZSI6IndpZGV2aW5lIiwic2l0ZV9pZCI6IjRMWUkiLCJoYXNoIjoiXC9FemdsSVA2UEVKbG5DWldWOXNGeGNneFl3eTlpSTZVV2R5RytoRDBLcUU9IiwiY2lkIjoiQmlnQnVja0J1bm55X3NrXzIzMTEyMiIsInBvbGljeSI6ImR1bEVac0NxaG9qdExNTWJOT3oxQzBUMysyZUZiRzByWDNDZWJRdjFJT1ZIZXZwaVRhYStIR3FtXC9ydVMrWnhNQ2grNCs1OGJLc1NZUllxVVhlRlBObGZIejU0d29VdkYrRVg5VVU4YU5NZUFjQllXYnlXNTVmTmhPNExcL3lHUU4wUlFUeGFZSW8wQzBRNFEybCsyTytFWW1WUUZzRzRMU2k4ZkQ0RjhmMk93SG9qenhXNDV3Sm5EUk1Mb2s5NFpxeWU0YmU3bksyRVwvQ1BxWjZFUUtBZGF2cDgwd09oMzl2c3hQOE1iaFA5am9JQ2lzVEYyN0RSMlExY2twVmFLU3pRSVVSQzMzSlNNNHh0N0JrdmowQUpwZHhqVk82dkh5UnlVa1ZVTzVXNEpVS2szOEc4NGtNYWhhc2hFXC90N2V5YW04MWRkSnNqRHg1K2hNcGFGbmxkREZjdDczWThTV3JuVGNnak9rRjJPM1FtVk1Vc21JR0xYeTZON0VyT0NIV0ZBdzBjNnR4K2xqOGFPalNvY2I1OHV5SkJHR2kxSStzR3NXSElhVUZZa0Q2N1N1OVNsaHVHK3I2bVdyVXV5ZVwvMDZ0VkFmaFpcL3FvNlFFWjZaVTl1VE5ZQkhFYWFsWThsUmdsMEZmZ0JKc01EdStcL080ZmlwSnlvYzFyZXF1U3I1dWJxekhacGpuRlwvUUpOVjZES1lBVTRyZkZpNTY4Y1ZTVTl2N3EyVnFGNFdUZDNselozWWFzamphcFhteXVST3N2YzRmeW1kWFZJb2xJM1dUQjFIQ29sZzFjaEFpYTRlXC85aXhHMDdcLzBtTVRhcEdQMmZBR1wvYnZ3d2VCZzVLXC9LZjBaZUNCT0YwRGdndVU3R2c4VFpOWVJTb01FTFhlTHJKNWR1Zzc3cSs2ekYzUXJ1eWJzc2dCYVQwaUxlZnJUUjFcL1JSd25BRURoV1FqY2JBeGtJYnlmUjYwVjA3UEVCeUJqSzk5VlFGUW1DWTQyNGRwUW5UNXFTdEhcL1wvd01ucU4rSWpkWXd5QlwvaHIwN01KTGJveHNRNVBBRmF5elJBb2xkZDYyQWVLNTNpWENwUVJydSt4SVI5ZzNPVExPZXdTbXRBVGZxXC9QQ1dLUVFtSWxMdEcyRHNNNzZjdHI0TGFvK2FyaWYzV1NOdVRjdkU4NEFMTU9wNDBTd2l5enkwb1BzeWpYNUk2ZEMrMWxod0taeWx5eTl5ZmR5SDlBRVVPeWgzTGpUamp1UDFNS0U2WmtTK1RvUlhCMTArdXI5VnZZMXVNUFZ4SDArY3RkS3V3eWkraEZGTjQ0bWVHdkZCeHZHdUpnWjlVRWY5RnFOWT0iLCJ0aW1lc3RhbXAiOiIyMDI0LTA2LTI1VDA1OjM4OjI0WiJ9",
            licenseCipherPath = "plc-kt-4LYI.bin"
        )
        val data4 = com.pallycon.widevine.model.ContentData(
            contentId = "TATATA",
            url = "https://contents.pallycon.com/TS_PackTest/tommy/pack_file/global_QA/BigBuckBunny_sk_231122/dash/stream.mpd",
            drmConfig = config4
        )
        val wvSDK4 = PallyConWvSDK.createPallyConWvSDK(
            context,
            data4
        )
        val state4 = wvSDK4.getDownloadState()
        contents.add(
            ContentData(
                "TATA",
                state4,
                state4.toString(),
                data4,
                wvSDK4,
                null,
                "TATAT",
                "TATAT"
            )
        )
    }

    fun getDownloadNotificationHelper(): DownloadNotificationHelper {
        return DownloadNotificationHelper(this.context!!, downloadChannel)
    }

    fun getDownloadManager(): DownloadManager? {
        return if (contents.size > 0) {
            return contents[0].wvSDK?.getDownloadManager(this.context!!)
        } else {
            null
        }
    }

    fun setAnalytics(context: Context) {
        analyticsCollector = IMedia3ExoPlayerCollector.Factory.create(context, analyticsConfig)
    }
}