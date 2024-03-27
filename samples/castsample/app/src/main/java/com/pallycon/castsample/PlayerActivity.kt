package com.pallycon.castsample

import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.SurfaceView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ext.cast.CastPlayer
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.pallycon.castsample.databinding.ActivityPlayerBinding
import com.pallycon.widevine.exception.PallyConException
import com.pallycon.widevine.exception.PallyConLicenseServerException
import com.pallycon.widevine.model.ContentData
import com.pallycon.widevine.model.DownloadState
import com.pallycon.widevine.model.PallyConEventListener
import com.pallycon.widevine.sdk.PallyConWvSDK


class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var playerView: StyledPlayerView? = null
    private var castContext: CastContext? = null
    private var castPlayer: CastPlayer? = null
    private var wvSDK: PallyConWvSDK? = null
    private var playerManager: PlayerManager? = null

    companion object {
        const val CONTENT = "CONTENT_ITEM"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        castContext = CastContext.getSharedInstance(this)

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // TODO : Set Sercurity API to protect media recording by screen recorder
        val view = binding.exoplayerView.videoSurfaceView as SurfaceView
        playerView = binding.exoplayerView
        if (Build.VERSION.SDK_INT >= 17) {
            view.setSecure(true)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu, menu)
        CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.media_route_menu_item)
        return true
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        return super.dispatchKeyEvent(event) || playerManager?.dispatchKeyEvent(event) == true
    }

    private fun buildSample() {
        if (intent.hasExtra(CONTENT) && wvSDK == null) {
            var content: ContentData? = intent.getParcelableExtra(CONTENT)
            if (content != null && content!!.url != null) {
                wvSDK = PallyConWvSDK.createPallyConWvSDK(
                    this,
                    content!!
                )
            }
        }

        try {
            playerManager?.createPlayers(castContext, wvSDK!!)
            val drmInfo = wvSDK?.getDrmInformation()
            drmInfo?.let {
                if ((it.licenseDuration <= 0 || it.playbackDuration <= 0) &&
                    wvSDK?.getDownloadState() == DownloadState.COMPLETED) {
                    Toast.makeText(applicationContext, "Expired license", Toast.LENGTH_LONG)
                        .show()
                }
                // mediaItem = wvSDK?.getMediaItem()
                wvSDK?.getMediaItem()?.let { media ->
                    playerManager?.addItem(media)
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
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        playerManager = PlayerManager(
            this,
            object: PlayerManager.Listener {
                override fun onQueuePositionChanged(previousIndex: Int, newIndex: Int) {
                    print("onQueuePositionChanged")
                }

                override fun onUnsupportedTrack(trackType: Int) {
                    print("onUnsupportedTrack")
                }

            },
            playerView!!
        )

        buildSample()
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing) {
            releasePlayer()
        }
    }

    private fun releasePlayer() {
        playerManager?.release();
    }
}