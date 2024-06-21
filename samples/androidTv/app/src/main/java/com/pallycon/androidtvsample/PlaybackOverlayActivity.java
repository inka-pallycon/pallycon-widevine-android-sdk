/*
 * PallyCon Team ( https://www.pallycon.com )
 *
 * This is a simple example project to show how to build a APP using the PallyCon Widevine SDK
 * The SDK is based on Exo player library
 */

package com.pallycon.androidtvsample;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.widget.Toast;

import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlaybackException;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.trackselection.TrackSelector;
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter;
import androidx.media3.ui.PlayerView;

import org.json.JSONException;

/**
 * Created by PallyconTeam
 */

public class PlaybackOverlayActivity extends Activity implements PlaybackOverlayFragment.OnPlayPauseClickedListener {
	public static final String TAG = "pallycon_sampleapp";
	public static final String PREFER_EXTENSION_DECODERS = "prefer_extension_decoders";

	private DataSource.Factory mediaDataSourceFactory;
	private DefaultBandwidthMeter bandwidthMeter;
	private TrackSelector trackSelector;
	private PlayerView playerView;
	private ExoPlayer player;
	private boolean shouldAutoPlay;
	private Handler eventHandler;
	private LeanbackPlaybackState mPlaybackState = LeanbackPlaybackState.IDLE;

	// TODO : must impleent ExoPlayer.EventListener
	Player.Listener playerEventListener = new Player.Listener() {
		@Override
		public void onPlayerError(PlaybackException error) {
			String errorString;
			if (error.errorCode == ExoPlaybackException.TYPE_RENDERER) {
				errorString = error.getLocalizedMessage();
			} else if (error.errorCode == ExoPlaybackException.TYPE_SOURCE) {
				errorString = error.getLocalizedMessage();
			} else if (error.errorCode == ExoPlaybackException.TYPE_UNEXPECTED) {
				errorString = error.getLocalizedMessage();
			} else {
				errorString = error.getLocalizedMessage();
			}

			AlertDialog.Builder builder = new AlertDialog.Builder(PlaybackOverlayActivity.this);
			builder.setTitle("Play Error");
			builder.setMessage(errorString);
			builder.setPositiveButton("OK", null);
			Dialog dialog = builder.create();
			dialog.show();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_player);

		shouldAutoPlay = true;
		playerView = findViewById(R.id.player_view);
		playerView.requestFocus();
		playerView.setUseController(false);

		eventHandler = new Handler();
		bandwidthMeter = new DefaultBandwidthMeter.Builder(this).build();

		mediaDataSourceFactory = buildDataSourceFactory();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if(!requestVisibleBehind(true)) {
			releasePlayer();
			finish();
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		releasePlayer();
		finish();
	}

	private void initializePlayer(Movie movie) throws JSONException {
		Intent intent = getIntent();
		Uri uri = Uri.parse(movie.uri);

		if (player == null) {
			trackSelector = new DefaultTrackSelector(this);

			PallyConWvSDKManager wvSDKManager = PallyConWvSDKManager.getInstance(this);
			MediaSource mediaSource = wvSDKManager.getMediaSource();

			if (mediaSource == null) {
				// failed
				return;
			}

			boolean preferExtensionDecoders = intent.getBooleanExtra(PREFER_EXTENSION_DECODERS, false);
			@DefaultRenderersFactory.ExtensionRendererMode int extensionRendererMode = DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON;

			// TODO : Set Pallycon drmSessionManager for drm controller.
			player = new ExoPlayer.Builder(this)
					.setTrackSelector(trackSelector)
					.build();
			// TODO : Set Pallycon drmSessionManager for listener.
			player.addListener(playerEventListener);
			player.setMediaSource(mediaSource);
			playerView.setPlayer(player);
			// TODO : Set Sercurity API to protect media recording by screen recorder
			SurfaceView view = (SurfaceView) playerView.getVideoSurfaceView();
			if (Build.VERSION.SDK_INT >= 17) {
				view.setSecure(true);
			}

			player.setPlayWhenReady(shouldAutoPlay);
		}

		if (Util.maybeRequestReadExternalStoragePermission(this, uri)) {
			return;
		}

		player.prepare();
	}

	private void releasePlayer() {
		if (player != null) {
			shouldAutoPlay = player.getPlayWhenReady();
			player.release();
			player = null;
			trackSelector = null;
		}
	}

	private DataSource.Factory buildDataSourceFactory() {
		HttpDataSource.Factory httpDataSourceFactory = buildHttpDataSourceFactory();
		return new DefaultDataSource.Factory(this, httpDataSourceFactory);
	}

	private HttpDataSource.Factory buildHttpDataSourceFactory() {
		return new DefaultHttpDataSource.Factory()
				.setUserAgent(Util.getUserAgent(this, "ExoPlayerSample"));
	}

	@Override
	public int getCurrentTimeMs() {
		if( player != null ) {
			try {
				return (int) player.getCurrentPosition();
			} catch (Exception e) {
				return 0;
			}
		}
		return 0;
	}

	@Override
	public void onSetRepeatMode(int repeatMode) {
		if( player != null ) {
			switch( repeatMode ) {
				case 0:
					player.setRepeatMode(Player.REPEAT_MODE_ALL);
					break;
				case 1:
					player.setRepeatMode(Player.REPEAT_MODE_ONE);
					break;
				case 2:
					player.setRepeatMode(Player.REPEAT_MODE_OFF);
					break;
				default:
					player.setRepeatMode(Player.REPEAT_MODE_OFF);
					break;
			}
		}
	}

	@Override
	public void onFragmentPlayPause(Movie movie, int position, Boolean playPause) {
		// TODO : Set the actions according to remote control input
		if (position == 0 || mPlaybackState == LeanbackPlaybackState.IDLE) {
			try {
				initializePlayer(movie);
			} catch (JSONException e) {
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
				finish();
			}
			mPlaybackState = LeanbackPlaybackState.IDLE;
		}

		if (playPause && mPlaybackState != LeanbackPlaybackState.PLAYING) {
			mPlaybackState = LeanbackPlaybackState.PLAYING;
			if (position > 0) {
				player.seekTo(position);
				player.setPlayWhenReady(true);
			}
		} else {
			mPlaybackState = LeanbackPlaybackState.PAUSED;
			player.setPlayWhenReady(false);
		}
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// TODO : Implement key actions
		PlaybackOverlayFragment playbackOverlayFragment = (PlaybackOverlayFragment) getFragmentManager().findFragmentById(R.id.playback_controls_fragment);
		switch (keyCode) {
			case KeyEvent.KEYCODE_MEDIA_PLAY:
				playbackOverlayFragment.togglePlayback(false);
				return true;
			case KeyEvent.KEYCODE_MEDIA_PAUSE:
				playbackOverlayFragment.togglePlayback(false);
				return true;
			case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
				if (mPlaybackState == LeanbackPlaybackState.PLAYING) {
					playbackOverlayFragment.togglePlayback(false);
				} else {
					playbackOverlayFragment.togglePlayback(true);
				}
				return true;
			default:
				return super.onKeyUp(keyCode, event);
		}
	}

	/*
	 * List of various states that we can be in
	 */
	public enum LeanbackPlaybackState {
		PLAYING, PAUSED, BUFFERING, IDLE
	}
}
