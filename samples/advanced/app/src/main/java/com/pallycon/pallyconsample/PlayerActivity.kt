package com.pallycon.pallyconsample

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.SurfaceView
import android.widget.Toast
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.util.Util
import com.pallycon.pallyconsample.databinding.ActivityPlayerBinding
import com.pallycon.widevine.exception.PallyConException
import com.pallycon.widevine.model.ContentData
import com.pallycon.widevine.model.DownloadState
import com.pallycon.widevine.sdk.PallyConWvSDK

class PlayerActivity : Activity() {

    private lateinit var binding: ActivityPlayerBinding
    private var exoPlayer: ExoPlayer? = null
    private var wvSDK: PallyConWvSDK? = null

    companion object {
        const val CONTENT = "CONTENT_ITEM"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // TODO : Set Sercurity API to protect media recording by screen recorder
        val view = binding.exoplayerView.videoSurfaceView as SurfaceView
//        val view = binding.surfaceView
        if (Build.VERSION.SDK_INT >= 17) {
            view.setSecure(true)
        }
    }

    fun initializePlayer() {
        if (intent.hasExtra(CONTENT) && wvSDK == null) {
            val content: ContentData? = intent.getParcelableExtra(CONTENT)
            if (content != null && content!!.url != null) {
                wvSDK = PallyConWvSDK.createPallyConWvSDK(
                    this,
                    content!!
                )
            }
        }

        var mediaSource: MediaSource? = null
        try {
            val drmInfo = wvSDK?.getDrmInformation()
            drmInfo?.let {
                if ((it.licenseDuration <= 0 || it.playbackDuration <= 0) &&
                    wvSDK?.getDownloadState() == DownloadState.COMPLETED) {
                    Toast.makeText(applicationContext, "Expired license", Toast.LENGTH_LONG)
                        .show()
                }
                // mediaItem = wvSDK?.getMediaItem()
                wvSDK?.getMediaSource()?.let { media ->
                    mediaSource = media
                }
            }
        } catch (e: PallyConException.DrmException) {
            print(e)
            Toast.makeText(applicationContext, "DrmException", Toast.LENGTH_LONG)
                .show()
        } catch (e: PallyConException.DetectedDeviceTimeModifiedException) {
            print(e)
            Toast.makeText(applicationContext, "DeviceTimeModified", Toast.LENGTH_LONG)
                .show()
        } catch (e: Exception) {
            print(e)
            Toast.makeText(applicationContext, "Exception", Toast.LENGTH_LONG)
                .show()
        }

        // using mediaItem
        /*
         ExoPlayer.Builder(this).setMediaSourceFactory(
                DefaultMediaSourceFactory(this)
                setDataSourceFactory(wvSDK!!.getDataSourceFactory())).build()
        */

        if (mediaSource == null) {
            return
        }

        ExoPlayer.Builder(this).build()
            .also { player ->
                exoPlayer = player
                binding.exoplayerView.player = player
//                exoPlayer?.setVideoSurfaceView(binding.surfaceView)
//                exoPlayer?.setVideoSurface(binding.surfaceView.holder.surface)
                exoPlayer?.setMediaSource(mediaSource!!)
                exoPlayer?.prepare()
                exoPlayer?.playWhenReady = true
                exoPlayer?.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        super.onPlayerError(error)
                        if (error.errorCode ==
                            PlaybackException.ERROR_CODE_DRM_LICENSE_EXPIRED) {
                            Toast.makeText(applicationContext, "License Expired", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(applicationContext, error.message, Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        super.onIsPlayingChanged(isPlaying)
                        if (isPlaying && exoPlayer != null) {
//                            viewModel.setDuration(exoPlayer!!.duration)
                        }
                    }
                })
            }
    }

    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 24 || exoPlayer != null) {
            exoPlayer?.playWhenReady = true
        }
    }

    override fun onResume() {
        super.onResume()
        exoPlayer?.playWhenReady = true
        initializePlayer()
    }

    override fun onStop() {
        super.onStop()
        exoPlayer?.playWhenReady = false
        if (isFinishing) {
            releasePlayer()
        }
    }

    private fun releasePlayer() {
        exoPlayer?.release()
    }
}