package com.pallycon.jetcompose

import android.content.Context
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.pallycon.widevine.exception.PallyConException
import com.pallycon.widevine.exception.PallyConLicenseServerException
import com.pallycon.widevine.model.PallyConEventListener
import com.pallycon.widevine.sdk.PallyConWvSDK
import com.pallycon.widevine.model.ContentData as PallyConData

@Composable
fun PlayerScreen(pallyConData: PallyConData, navController: NavController) {
    val context = LocalContext.current
    val mediaSource: MediaSource? = remember {
        getMediaSourceUsingSDK(context, pallyConData)
    }

    Surface(
        content = {
            mediaSource?.let {
                VideoPlayer(mediaSource = it)
            }
        }
    )
}

@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
fun VideoPlayer(mediaSource: MediaSource) {
    val context = LocalContext.current
    val playbackPosition = rememberSaveable {
        mutableStateOf(0L)
    }

    val exoPlayer = remember(context, mediaSource) {
        ExoPlayer.Builder(context).build().apply {
            addMediaSource(mediaSource)
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    super.onPlayerError(error)
                    var message = error.message
                    if (error.errorCode == PlaybackException.ERROR_CODE_DRM_LICENSE_EXPIRED) {
                        message = "License Expired"
                    }

                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)
                }

                override fun onEvents(player: Player, events: Player.Events) {
                    super.onEvents(player, events)
                    if (events.size() > 0 &&
                        events[0] == Player.EVENT_SURFACE_SIZE_CHANGED) {
                        playbackPosition.value = currentPosition
                    }
                }
            })

            seekTo(playbackPosition.value)

            prepare()
            play()
        }
    }

    DisposableEffect(exoPlayer) { onDispose { exoPlayer.release() } }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            PlayerView(context).apply {
                player = exoPlayer
                useController = true
            }
        }
    )
}

fun getMediaSourceUsingSDK(context: Context, pallyConData: PallyConData): MediaSource? {
    val wvmAgent = PallyConWvSDK.createPallyConWvSDK(context, pallyConData)

    val pallyConEventListener: PallyConEventListener = object : PallyConEventListener {
        override fun onCompleted(currentUrl: String?) {

        }

        override fun onProgress(currentUrl: String?, percent: Float, downloadedBytes: Long) {

        }

        override fun onStopped(currentUrl: String?) {

        }

        override fun onRestarting(currentUrl: String?) {

        }

        override fun onRemoved(currentUrl: String?) {

        }

        override fun onPaused(currentUrl: String?) {

        }

        override fun onFailed(currentUrl: String?, e: PallyConException?) {
            if (e is PallyConException.PallyConLicenseCipherException) {
                // Ignore the error except when using the LicenseCipher function.
            } else {
                Toast.makeText(context, e?.msg ?: "PallyConException", Toast.LENGTH_LONG).show()
            }
        }

        override fun onFailed(currentUrl: String?, e: PallyConLicenseServerException?) {
            val message = "${e?.errorCode()}, ${e?.body()}"
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
    wvmAgent.setPallyConEventListener(pallyConEventListener)
    return try {
        wvmAgent.getMediaSource()
    } catch (e: PallyConException.ContentDataException) {
        e.printStackTrace()
        Toast.makeText(context, e?.message, Toast.LENGTH_LONG).show()
        null
    } catch (e: PallyConException.DetectedDeviceTimeModifiedException) {
        e.printStackTrace()
        Toast.makeText(context, e?.message, Toast.LENGTH_LONG).show()
        null
    }
}