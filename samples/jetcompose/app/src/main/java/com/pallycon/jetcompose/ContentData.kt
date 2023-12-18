package com.pallycon.jetcompose

import com.pallycon.widevine.model.DownloadState
import com.pallycon.widevine.sdk.PallyConWvSDK
import com.pallycon.widevine.track.PallyConDownloaderTracks
import com.pallycon.widevine.model.ContentData as PallyConData

data class ContentData(
    val title : String,
    var status : DownloadState,
    var subTitle: String,
    val content : PallyConData,
    val wvSDK: PallyConWvSDK,
    var downloadTracks: PallyConDownloaderTracks?,
    val cid: String, // for migration
    val name: String // for migration
)
