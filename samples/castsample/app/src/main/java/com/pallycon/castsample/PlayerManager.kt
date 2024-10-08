package com.pallycon.castsample

import android.content.Context
import android.view.KeyEvent
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.SessionAvailabilityListener
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.Listener
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.PlayerView
import com.google.android.gms.cast.Cast
import com.google.android.gms.cast.framework.CastContext
import com.pallycon.widevine.exception.PallyConException
import com.pallycon.widevine.exception.PallyConLicenseServerException
import com.pallycon.widevine.model.DownloadState
import com.pallycon.widevine.sdk.PallyConWvSDK
import com.pallycon.widevine.model.ContentData
import com.pallycon.widevine.model.PallyConEventListener

/*
* Copyright (C) 2019 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
/**
 * Manages players and an internal media queue for the demo app.
 *
 */
@UnstableApi @OptIn(UnstableApi::class)
internal class PlayerManager(
    context: Context,
    listener: Listener,
    playerView: PlayerView,
) :
    Listener, SessionAvailabilityListener {
    /** Listener for events.  */
    internal interface Listener {
        /** Called when the currently played item of the media queue changes.  */
        fun onQueuePositionChanged(previousIndex: Int, newIndex: Int)

        /**
         * Called when a track of type `trackType` is not supported by the player.
         *
         * @param trackType One of the [C]`.TRACK_TYPE_*` constants.
         */
        fun onUnsupportedTrack(trackType: Int)
    }

    private val context: Context
    private val playerView: PlayerView
    private var localPlayer: Player? = null
    private var castPlayer: CastPlayer? = null
    private val mediaQueue: ArrayList<MediaItem>
    private val listener: Listener
    private var lastSeenTracks: Tracks? = null
    private var currentItemIndex: Int
    private var currentPlayer: Player? = null

    private var wvSDK: PallyConWvSDK? = null

    /**
     * Creates a new manager for [ExoPlayer] and [CastPlayer].
     *
     * @param context A [Context].
     * @param listener A [Listener] for queue position changes.
     * @param playerView The [StyledPlayerView] for playback.
     * @param castContext The [CastContext].
     */
    init {
        this.context = context
        this.listener = listener
        this.playerView = playerView
        mediaQueue = ArrayList()
        currentItemIndex = C.INDEX_UNSET
    }

    fun createPlayers(castContext: CastContext?, wvSDK: PallyConWvSDK) {
        localPlayer = ExoPlayer.Builder(context)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(context)
                    .setDataSourceFactory(
                        wvSDK.getDataSourceFactory()
                    )
            )
            .build()
        localPlayer?.addListener(this)
        castPlayer = CastPlayer(castContext!!)
        castPlayer?.addListener(this)
        castPlayer?.setSessionAvailabilityListener(this)
        setCurrentPlayer(if (castPlayer!!.isCastSessionAvailable) castPlayer!! else localPlayer!!)
    }

    // Queue manipulation methods.
    /**
     * Plays a specified queue item in the current player.
     *
     * @param itemIndex The index of the item to play.
     */
    fun selectQueueItem(itemIndex: Int) {
        setCurrentItem(itemIndex)
    }

    /** Returns the index of the currently played item.  */
    fun getCurrentItemIndex(): Int {
        return currentItemIndex
    }

    /**
     * Appends `item` to the media queue.
     *
     * @param item The [MediaItem] to append.
     */
    fun addItem(item: MediaItem) {
        if (!mediaQueue.contains(item)) {
            mediaQueue.add(item)
            currentPlayer!!.addMediaItem(item)
        }
    }

    fun addItem(content: ContentData) {
        if (content != null && content.url != null) {
            wvSDK = PallyConWvSDK.createPallyConWvSDK(
                context,
                content
            )
        }

        try {
            val drmInfo = wvSDK?.getDrmInformation()
            drmInfo?.let {
                if ((it.licenseDuration <= 0 || it.playbackDuration <= 0) &&
                    wvSDK?.getDownloadState() == DownloadState.COMPLETED) {
                    Toast.makeText(context, "Expired license", Toast.LENGTH_LONG)
                        .show()
                }
                // mediaItem = wvSDK?.getMediaItem()
                wvSDK?.getMediaItem()?.let {
                    addItem(it)
                }
            }
        } catch (e: PallyConException.DrmException) {
            print(e)
            Toast.makeText(context, "DrmException", Toast.LENGTH_LONG)
                .show()
        } catch (e: PallyConException.DetectedDeviceTimeModifiedException) {
            print(e)
            Toast.makeText(context, "DeviceTimeModified", Toast.LENGTH_LONG)
                .show()
        } catch (e: Exception) {
            print(e)
            Toast.makeText(context, "Exception", Toast.LENGTH_LONG)
                .show()
        }
    }

    /** Returns the size of the media queue.  */
    fun getMediaQueueSize(): Int {
        return mediaQueue.size
    }

    /**
     * Returns the item at the given index in the media queue.
     *
     * @param position The index of the item.
     * @return The item at the given index in the media queue.
     */
    fun getItem(position: Int): MediaItem {
        return mediaQueue[position]
    }

    /**
     * Removes the item at the given index from the media queue.
     *
     * @param item The item to remove.
     * @return Whether the removal was successful.
     */
    fun removeItem(item: MediaItem): Boolean {
        val itemIndex = mediaQueue.indexOf(item)
        if (itemIndex == -1) {
            return false
        }
        currentPlayer!!.removeMediaItem(itemIndex)
        mediaQueue.removeAt(itemIndex)
        if (itemIndex == currentItemIndex && itemIndex == mediaQueue.size) {
            maybeSetCurrentItemAndNotify(C.INDEX_UNSET)
        } else if (itemIndex < currentItemIndex) {
            maybeSetCurrentItemAndNotify(currentItemIndex - 1)
        }
        return true
    }

    /**
     * Moves an item within the queue.
     *
     * @param item The item to move.
     * @param newIndex The target index of the item in the queue.
     * @return Whether the item move was successful.
     */
    fun moveItem(item: MediaItem, newIndex: Int): Boolean {
        val fromIndex = mediaQueue.indexOf(item)
        if (fromIndex == -1) {
            return false
        }

        // Player update.
        currentPlayer!!.moveMediaItem(fromIndex, newIndex)
        mediaQueue.add(newIndex, mediaQueue.removeAt(fromIndex))

        // Index update.
        if (fromIndex == currentItemIndex) {
            maybeSetCurrentItemAndNotify(newIndex)
        } else if (fromIndex < currentItemIndex && newIndex >= currentItemIndex) {
            maybeSetCurrentItemAndNotify(currentItemIndex - 1)
        } else if (fromIndex > currentItemIndex && newIndex <= currentItemIndex) {
            maybeSetCurrentItemAndNotify(currentItemIndex + 1)
        }
        return true
    }

    /**
     * Dispatches a given [KeyEvent] to the corresponding view of the current player.
     *
     * @param event The [KeyEvent].
     * @return Whether the event was handled by the target view.
     */
    fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        return playerView.dispatchKeyEvent(event!!)
    }

    /** Releases the manager and the players that it holds.  */
    fun release() {
        currentItemIndex = C.INDEX_UNSET
        mediaQueue.clear()
        castPlayer?.setSessionAvailabilityListener(null)
        castPlayer?.release()
        playerView.player = null
        localPlayer?.release()
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
    }

    // Player.Listener implementation.
    override fun onPlaybackStateChanged(playbackState: @Player.State Int) {
        updateCurrentItemIndex()
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        updateCurrentItemIndex()
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        updateCurrentItemIndex()
    }

    override fun onTracksChanged(tracks: Tracks) {
        if (currentPlayer !== localPlayer || tracks === lastSeenTracks) {
            return
        }
        if (tracks.containsType(C.TRACK_TYPE_VIDEO)
            && !tracks.isTypeSupported(C.TRACK_TYPE_VIDEO, true)
        ) {
            listener.onUnsupportedTrack(C.TRACK_TYPE_VIDEO)
        }
        if (tracks.containsType(C.TRACK_TYPE_AUDIO)
            && !tracks.isTypeSupported(C.TRACK_TYPE_AUDIO,true)
        ) {
            listener.onUnsupportedTrack(C.TRACK_TYPE_AUDIO)
        }
        lastSeenTracks = tracks
    }

    // CastPlayer.SessionAvailabilityListener implementation.
    override fun onCastSessionAvailable() {
        setCurrentPlayer(castPlayer!!)
    }

    override fun onCastSessionUnavailable() {
        setCurrentPlayer(localPlayer!!)
    }

    // Internal methods.
    private fun updateCurrentItemIndex() {
        val playbackState = currentPlayer!!.playbackState
        maybeSetCurrentItemAndNotify(
            if (playbackState != STATE_IDLE && playbackState != STATE_ENDED) currentPlayer!!.currentMediaItemIndex else C.INDEX_UNSET
        )
    }

    private fun setCurrentPlayer(currentPlayer: Player) {
        if (this.currentPlayer === currentPlayer) {
            return
        }
        playerView.player = currentPlayer
        playerView.controllerHideOnTouch = currentPlayer === localPlayer
        if (currentPlayer === castPlayer) {
            playerView.controllerShowTimeoutMs = 0
            playerView.showController()
            playerView.defaultArtwork = ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.baseline_cast_connected_400,
                null
            )
        } else { // currentPlayer == localPlayer
            playerView.controllerShowTimeoutMs = PlayerControlView.DEFAULT_SHOW_TIMEOUT_MS
            playerView.defaultArtwork = null
        }

        // Player state management.
        var playbackPositionMs = C.TIME_UNSET
        var currentItemIndex = C.INDEX_UNSET
        var playWhenReady = true
        val previousPlayer = this.currentPlayer
        if (previousPlayer != null) {
            // Save state from the previous player.
            val playbackState = previousPlayer.playbackState
            if (playbackState != STATE_ENDED) {
                playbackPositionMs = previousPlayer.currentPosition
                playWhenReady = previousPlayer.playWhenReady
                currentItemIndex = previousPlayer.currentMediaItemIndex
                if (currentItemIndex != this.currentItemIndex) {
                    playbackPositionMs = C.TIME_UNSET
                    currentItemIndex = this.currentItemIndex
                }
            }
            previousPlayer.stop()
            previousPlayer.clearMediaItems()
        }
        this.currentPlayer = currentPlayer

        // Media queue management.
        currentPlayer.setMediaItems(mediaQueue, currentItemIndex, playbackPositionMs)
        currentPlayer.playWhenReady = playWhenReady
        currentPlayer.prepare()
    }

    /**
     * Starts playback of the item at the given index.
     *
     * @param itemIndex The index of the item to play.
     */
    private fun setCurrentItem(itemIndex: Int) {
        maybeSetCurrentItemAndNotify(itemIndex)
        if (currentPlayer!!.currentTimeline.windowCount != mediaQueue.size) {
            // This only happens with the cast player. The receiver app in the cast device clears the
            // timeline when the last item of the timeline has been played to end.
            currentPlayer!!.setMediaItems(mediaQueue, itemIndex, C.TIME_UNSET)
        } else {
            currentPlayer!!.seekTo(itemIndex, C.TIME_UNSET)
        }
        currentPlayer!!.playWhenReady = true
    }

    private fun maybeSetCurrentItemAndNotify(currentItemIndex: Int) {
        if (this.currentItemIndex != currentItemIndex) {
            val oldIndex = this.currentItemIndex
            this.currentItemIndex = currentItemIndex
            listener.onQueuePositionChanged(oldIndex, currentItemIndex)
        }
    }
}