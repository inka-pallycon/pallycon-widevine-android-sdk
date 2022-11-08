/*
 * PallyCon Team ( https://www.pallycon.com )
 *
 * This is a simple example project to show how to build a APP using the PallyCon Widevine SDK
 * The SDK is based on Exo player library
 */

package com.pallycon.exoplayersample;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.TracksInfo;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.KeysExpiredException;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.source.BehindLiveWindowException;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSourceInputStream;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.StatsDataSource;
import com.google.android.exoplayer2.util.ErrorMessageProvider;
import com.google.android.exoplayer2.util.Util;
import com.pallycon.widevinelibrary.DatabaseDecryptException;
import com.pallycon.widevinelibrary.DetectedDeviceTimeModifiedException;
import com.pallycon.widevinelibrary.NetworkConnectedException;
import com.pallycon.widevinelibrary.PallyconDrmException;
import com.pallycon.widevinelibrary.PallyconEncrypterException;
import com.pallycon.widevinelibrary.PallyconEventListener;
import com.pallycon.widevinelibrary.PallyconKeyRequest;
import com.pallycon.widevinelibrary.PallyconServerResponseException;
import com.pallycon.widevinelibrary.PallyconWVMSDK;
import com.pallycon.widevinelibrary.PallyconWVMSDKFactory;
import com.pallycon.widevinelibrary.UnAuthorizedDeviceException;

import org.json.JSONException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Created by PallyconTeam
 */

public class PlayerActivity extends AppCompatActivity
        implements View.OnClickListener {

    public static final String TAG = "pallycon_sampleapp";
    public static final String CONTENTS_TITLE = "contents_title";
    public static final String DRM_SCHEME_UUID_EXTRA = "drm_scheme_uuid";
    public static final String DRM_LICENSE_URL = "drm_license_url";
    public static final String DRM_USERID = "drm_userid";
    public static final String DRM_OID = "drm_oid";
    public static final String DRM_CID = "drm_cid";
    public static final String DRM_TOKEN = "drm_token";
    public static final String DRM_CUSTOM_DATA = "drm_custom_data";
    public static final String DRM_MULTI_SESSION = "drm_multi_session";
    public static final String DRM_COOKIE = "drm_cookie";
    public static final String THUMB_URL = "thumb_url";
    public static final String PREFER_EXTENSION_DECODERS = "prefer_extension_decoders";

    private static final String KEY_TRACK_SELECTOR_PARAMETERS = "track_selector_parameters";
    private static final String KEY_WINDOW = "window";
    private static final String KEY_POSITION = "position";
    private static final String KEY_AUTO_PLAY = "auto_play";

    private DataSource.Factory mediaDataSourceFactory;
    private DefaultBandwidthMeter bandwidthMeter;
    private StyledPlayerView playerView;
    private ExoPlayer player;
    private boolean shouldAutoPlay;
    private Handler eventHandler;
    private PallyconWVMSDK WVMAgent;
    private LinearLayout debugRootView;

    private DefaultTrackSelector trackSelector;
    private DefaultTrackSelector.Parameters trackSelectorParameters;
    private String cookie;

    private Button selectTracksButton;
    private boolean isShowingTrackSelectionDialog;
    private TracksInfo lastSeenTracksInfo;

    private boolean startAutoPlay;
    private int startWindow;
    private long startPosition;

    private void updateButtonVisibility() {
        selectTracksButton.setEnabled(
                player != null && TrackSelectionDialog.willHaveContent(trackSelector));
    }

    private void updateTrackSelectorParameters() {
        if (trackSelector != null) {
            trackSelectorParameters = trackSelector.getParameters();
        }
    }

    private void updateStartPosition() {
        if (player != null) {
            startAutoPlay = player.getPlayWhenReady();
            startWindow = player.getCurrentWindowIndex();
            startPosition = Math.max(0, player.getContentPosition());
        }
    }

    protected void clearStartPosition() {
        startAutoPlay = true;
        startWindow = C.INDEX_UNSET;
        startPosition = C.TIME_UNSET;
    }

    private void showControls() {
        debugRootView.setVisibility(View.VISIBLE);
    }

    private static boolean isBehindLiveWindow(ExoPlaybackException e) {
        if (e.type != ExoPlaybackException.TYPE_SOURCE) {
            return false;
        }
        Throwable cause = e.getSourceException();
        while (cause != null) {
            if (cause instanceof BehindLiveWindowException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    // TODO : must implement ExoPlayer.EventListener
    private class PlayerEventListener implements Player.Listener {

        @Override
        public void onPlaybackStateChanged(@Player.State int playbackState) {
            if (playbackState == Player.STATE_ENDED) {
                showControls();
            }
            updateButtonVisibility();
        }

        @Override
        public void onPlayerError(PlaybackException error) {
            if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                player.seekToDefaultPosition();
                player.prepare();
            } else {
                updateButtonVisibility();
                showControls();
            }
        }

        @Override
        @SuppressWarnings("ReferenceEquality")
        public void onTracksInfoChanged(TracksInfo tracksInfo) {
            updateButtonVisibility();
            if (tracksInfo == lastSeenTracksInfo) {
                return;
            }
            if (!tracksInfo.isTypeSupportedOrEmpty(C.TRACK_TYPE_VIDEO)) {
                showToast(R.string.error_unsupported_video);
            }
            if (!tracksInfo.isTypeSupportedOrEmpty(C.TRACK_TYPE_AUDIO)) {
                showToast(R.string.error_unsupported_audio);
            }
            lastSeenTracksInfo = tracksInfo;
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        updateTrackSelectorParameters();
        updateStartPosition();
        outState.putParcelable(KEY_TRACK_SELECTOR_PARAMETERS, trackSelectorParameters.toBundle());
        outState.putBoolean(KEY_AUTO_PLAY, startAutoPlay);
        outState.putInt(KEY_WINDOW, startWindow);
        outState.putLong(KEY_POSITION, startPosition);
    }

    // PallyconKeyRequest
    private PallyconKeyRequest keyRequestCallback = new PallyconKeyRequest() {
        @Override
        public DataSourceInputStream contentKeyRequest(byte[] keyData, Map<String, String> requestData) {
            HttpDataSource.Factory httpDataSourceFactory =
                    new DefaultHttpDataSource.Factory()
                            .setUserAgent(Util.getUserAgent(PlayerActivity.this, "PallyConSample"));
            StatsDataSource dataSource = new StatsDataSource(httpDataSourceFactory.createDataSource());
            DataSpec dataSpec =
                    new DataSpec.Builder()
                            .setUri("https://license.pallycon.com/ri/licenseManager.do")
                            .setHttpRequestHeaders(requestData)
                            .setHttpMethod(DataSpec.HTTP_METHOD_POST)
                            .setHttpBody(keyData)
                            .setFlags(DataSpec.FLAG_ALLOW_GZIP)
                            .build();
            DataSourceInputStream inputStream = new DataSourceInputStream(dataSource, dataSpec);
            return inputStream;
        }
    };

    // TODO : must implement PallyconEventListener
    private PallyconEventListener pallyconEventListener = new PallyconEventListener() {

        @Override
        public void onDrmKeysLoaded(Map<String, String> licenseInfo) {
            // TODO: Use the loaded license information.
            StringBuilder stringBuilder = new StringBuilder();

            Iterator<String> keys = licenseInfo.keySet().iterator();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = licenseInfo.get(key);
                try {
                    if (Long.parseLong(value) == 0x7fffffffffffffffL) {
                        value = "Unlimited";
                    }
                } catch (Exception e) {
                    // e.printStackTrace();
                }
                stringBuilder.append(key).append(" : ").append(value);
                if (keys.hasNext()) {
                    stringBuilder.append("\n");
                }
            }

            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(PlayerActivity.this);
            alertBuilder.setTitle("License Info");
            alertBuilder.setMessage(stringBuilder.toString());
            alertBuilder.setPositiveButton("OK", null);
            Dialog dialog = alertBuilder.create();
            dialog.show();
        }

        @Override
        public void onDrmSessionManagerError(Exception e) {
            // TODO: Handle exceptions in error situations. Please refer to the API guide document for details of exception.
            AlertDialog.Builder builder = new AlertDialog.Builder(PlayerActivity.this);
            builder.setTitle("DrmManager Error");

            if (e instanceof NetworkConnectedException) {
                builder.setMessage(e.getMessage());

            } else if (e instanceof PallyconServerResponseException) {
                PallyconServerResponseException e1 = (PallyconServerResponseException) e;
                builder.setMessage("errorCode : " + e1.getErrorCode() + "\n" + "message : " + e1.getMessage());

            } else if (e instanceof KeysExpiredException) {
                builder.setMessage("license has been expired. please remove the license first and try again.");
                builder.setPositiveButton("OK", null);
                Dialog dialog = builder.create();
                dialog.show();
                return;

            } else if(e instanceof DatabaseDecryptException) {
                builder.setMessage("errorMsg : " + e.getMessage());
                builder.setPositiveButton("OK", null);
                Dialog dialog = builder.create();
                dialog.show();
                return;

            } else if (e instanceof DetectedDeviceTimeModifiedException) {
                // TODO: content playback should be prohibited to prevent illegal use of content.
                builder.setMessage("Device time has been changed. go to [Settings] > [Date & time] and use [Automatic date & time] and Connect Internet");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                });
                Dialog dialog = builder.create();
                dialog.setCancelable(false);
                dialog.setCanceledOnTouchOutside(false);
                dialog.show();
                return;

            } else {
                builder.setMessage(e.getMessage());
            }

            builder.setPositiveButton("OK", null);
            Dialog dialog = builder.create();
            dialog.show();
        }

        @Override
        public void onDrmKeysRestored() {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(PlayerActivity.this);
            alertBuilder.setTitle("License Info");
            alertBuilder.setMessage("Drm key Restored !!!!!");
            alertBuilder.setPositiveButton("OK", null);
            Dialog dialog = alertBuilder.create();
            dialog.show();
        }

        @Override
        public void onDrmKeysRemoved() {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(PlayerActivity.this);
            alertBuilder.setTitle("License Info");
            alertBuilder.setMessage("Drm key Removed !!!!!");
            alertBuilder.setPositiveButton("OK", null);
            Dialog dialog = alertBuilder.create();
            dialog.show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        debugRootView = findViewById(R.id.controls_root);
        selectTracksButton = findViewById(R.id.select_tracks_button);
        selectTracksButton.setOnClickListener(this);

        Log.d(TAG, "onCreate");

        shouldAutoPlay = true;
        playerView = findViewById(R.id.player_view);
        playerView.setErrorMessageProvider(new PlayerErrorMessageProvider());
        playerView.requestFocus();

//		//// Chromecast
//		mCastContext = CastContext.getSharedInstance(this);
//		// mCastContext is always not null. No need to check null
//		createMessageReceivedCallback();
//		createSessionManagerListener();
//		createRemoteMediaClientListener();
//		createRemoteMediaClientProgressListener();
//		mSessionManager = mCastContext.getSessionManager();
//		mCastSession = mSessionManager.getCurrentCastSession();
//		if( mCastSession != null ) {
//			Log.d(TAG, "[CAST] CastSession exists");
//			try {
//				mCastSession.setMessageReceivedCallbacks(CAST_MSG_NAMESPACE, mMessageReceivedCallback);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		} else {
//			Log.d(TAG, "mCastSession is null");
//		}
//
//		bNowOnChromecast = false;
//		bCreated = true;
//		//// CC

        eventHandler = new Handler();

        Intent intent = getIntent();
        cookie = intent.getStringExtra(DRM_COOKIE);
        mediaDataSourceFactory = buildDataSourceFactory();

        trackSelectorParameters = new DefaultTrackSelector.ParametersBuilder(this).build();

        if (savedInstanceState != null) {
            trackSelectorParameters = savedInstanceState.getParcelable(KEY_TRACK_SELECTOR_PARAMETERS);
            startAutoPlay = savedInstanceState.getBoolean(KEY_AUTO_PLAY);
            startWindow = savedInstanceState.getInt(KEY_WINDOW);
            startPosition = savedInstanceState.getLong(KEY_POSITION);
        } else {
            DefaultTrackSelector.ParametersBuilder builder =
                    new DefaultTrackSelector.ParametersBuilder(/* context= */ this);
            trackSelectorParameters = builder.build();
            clearStartPosition();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();

//		// Check chromecast first
//		//// Chromecast
//		mSessionManager.addSessionManagerListener(mSessionManagerListener, CastSession.class);
//		if (mCastSession != null && (mCastSession.isConnected() || mCastSession.isConnecting())) {
//			if( bCreated ) {
//				// If PlayerActivity is created, it means that the user select a content at MainActivity.
//				releaseCast();
//				bCreated = false;
//				bNowOnChromecast = false;
//			}
//
//			if( bNowOnChromecast ) {
//				// If the content is playing at Chromecast already, do nothing.
//				Log.d(TAG, "[CAST] continue playing on Chromecast");
//				return;
//			}
//
//			// If the content is not playing at Chromecast, load the content.
//			Uri contentUri = getIntent().getData();
//			if( contentUri != null && contentUri.toString().startsWith("/") ) {
//				// The contents is in internal storage. It is not supported by Cast.
//				mPlayStatus.mScreen = PlayStatus.SCREEN_LOCAL;
//				Log.d(TAG, "[CAST] Chromecast is connected but the content type is not streaming");
//			} else {
//				// The contents is in remote storage. Cast will be work.
//				mPlayStatus.mScreen = PlayStatus.SCREEN_CAST;
//				Log.d(TAG, "[CAST] Chromecast is connected");
//
//				Log.d(TAG, "[CAST] start playing on Chromecast");
//				if( !loadRemoteMedia(0, true) )
//					Log.d(TAG, "[CAST] failure on loadRemoteMedia()");
//
//				return;
//			}
//		} else {
//			mPlayStatus.mScreen = PlayStatus.SCREEN_LOCAL;
//			Log.d(TAG, "Chromecast is not connected");
//		}
//
//		long position = mPlayStatus.mPosition;
//		releaseCast();
//		mPlayStatus.mPosition = position;
//		//// CC
//
//		// If the CastControllerActivity is destroyed when it is full screen activity, onSessionEnded() of Chromecast will not be called.
//		// PlayActivity will come to the foreground, and onResume() will be called.
//		if( mPlayStatus.mCurrentState == PlayStatus.STATE_PLAYING )
//			shouldAutoPlay = true;

        if (Util.SDK_INT <= 23 || player == null) {
            try {
                initializePlayer();
            } catch (PallyconDrmException e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            } catch (PallyconEncrypterException e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            } catch (JSONException e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        } else {
            playerView.setUseController(true);
            player.setPlayWhenReady(shouldAutoPlay);
        }
    }

    @Override
    protected void onPause() {
//		//// Chromecast
//		if( mCastContext != null && mSessionManagerListener != null && mRemoteClient != null ) {
//			mSessionManager.removeSessionManagerListener(mSessionManagerListener, CastSession.class);
//		}
//		//// CC

        if (Util.SDK_INT <= 23) {
            releasePlayer();
        }

        super.onPause();
    }

    @Override
    protected void onStop() {
        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
//		//// Chromecast
//		mPlayStatus.clear();
//		//// CC

        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length == 0) {
            // Empty results are triggered if a permission is requested while another request was already
            // pending and can be safely ignored in this case.
            return;
        }
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            try {
                initializePlayer();
            } catch (PallyconDrmException e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            } catch (PallyconEncrypterException e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            } catch (JSONException e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        } else {
            showToast(R.string.storage_permission_denied);
            finish();
        }
    }

    private void initializePlayer() throws PallyconDrmException, PallyconEncrypterException, JSONException {
        UUID drmSchemeUuid = null;
        Intent intent = getIntent();
        Uri uri = intent.getData();

        if (uri == null || uri.toString().length() < 1)
            throw new PallyconDrmException("The content url is missing");

        if (player == null) {
            trackSelector = new DefaultTrackSelector(this);
            trackSelector.setParameters(trackSelectorParameters);
            lastSeenTracksInfo = TracksInfo.EMPTY;

            if (intent.hasExtra(DRM_SCHEME_UUID_EXTRA)) {
                drmSchemeUuid = UUID.fromString(intent.getStringExtra(DRM_SCHEME_UUID_EXTRA));
            }

            DrmSessionManager drmSessionManager = null;
            if (drmSchemeUuid != null) {
                String drmLicenseUrl = intent.getStringExtra(DRM_LICENSE_URL);
                boolean multiSession = intent.getBooleanExtra(DRM_MULTI_SESSION, false);
                try {
                    // TODO : Acquire Pallycon Widevine module.
                    WVMAgent = PallyconWVMSDKFactory.getInstance(this);
                    WVMAgent.setPallyconEventListener(pallyconEventListener);
                    //WVMAgent.setPallyconKeyRequestCallback(keyRequestCallback);
                } catch (PallyconDrmException e) {
                    e.printStackTrace();
                } catch (UnAuthorizedDeviceException e) {
                    e.printStackTrace();
                }

                try {
                    String userId = intent.getStringExtra(DRM_USERID);
                    String cid = intent.getStringExtra(DRM_CID);
                    String oid = intent.getStringExtra(DRM_OID);
                    String token = intent.getStringExtra(DRM_TOKEN);
                    String customData = intent.getStringExtra(DRM_CUSTOM_DATA);
                    // TODO : Create Pallycon drmSessionManager to get into ExoPlayerFactory

                    if (token.equals("") == false) {
                        drmSessionManager = WVMAgent.createDrmSessionManagerByToken(drmSchemeUuid, drmLicenseUrl, uri, token);
                        //drmSessionManager = WVMAgent.createDrmSessionManagerByToken(drmSchemeUuid, drmLicenseUrl, uri, userId, cid, token, multiSession);
                    } else if (customData.equals("") == false) {
                        drmSessionManager = WVMAgent.createDrmSessionManagerByCustomData(drmSchemeUuid, drmLicenseUrl, uri, customData, multiSession);
                    } else if (userId == null || userId.length() < 1) {
                        drmSessionManager = WVMAgent.createDrmSessionManagerByProxy(drmSchemeUuid, drmLicenseUrl, uri, cid, multiSession);
                    } else {
                        drmSessionManager = WVMAgent.createDrmSessionManager(drmSchemeUuid, drmLicenseUrl, uri, userId, cid, oid, multiSession);
                    }

                } catch (PallyconDrmException e) {
                    e.printStackTrace();
                    throw e;
                }
            }

            // TODO : Set Pallycon drmSessionManager for drm controller.
            MediaSource mediaSource = buildMediaSource(uri, drmSessionManager);

            player = new ExoPlayer.Builder(/* context= */ this)
                    .setTrackSelector(trackSelector)
                    .build();
            // TODO : Set Pallycon drmSessionManager for listener.
            player.addListener(new PlayerEventListener());
            player.setMediaSource(mediaSource);

            // TODO : Set Sercurity API to protect media recording by screen recorder
            SurfaceView view = (SurfaceView) playerView.getVideoSurfaceView();
            if (Build.VERSION.SDK_INT >= 17) {
                view.setSecure(true);
            }

            player.prepare();
            playerView.setPlayer(player);

//			//// Chromecast
//			if( mPlayStatus.mPosition >  0 )
//				player.seekTo(mPlayStatus.mPosition);
//			//// CC
            player.setPlayWhenReady(shouldAutoPlay);
        }

        if (Util.maybeRequestReadExternalStoragePermission(this, uri)) {
            return;
        }

        updateButtonVisibility();
    }

    private void releasePlayer() {
        if (player != null) {
            updateTrackSelectorParameters();
            updateStartPosition();
            player.release();
            player = null;
            trackSelector = null;
        }
    }

    private MediaSource buildMediaSource(Uri uri, DrmSessionManager drmSessionManager) {
        int type = Util.inferContentType(uri.getLastPathSegment());
        DataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(this);
        switch (type) {
            case C.TYPE_DASH:
                return new DashMediaSource.Factory(dataSourceFactory)
                        .setDrmSessionManagerProvider(unusedMediaItem -> drmSessionManager)
                        .createMediaSource(MediaItem.fromUri(uri));
            case C.TYPE_OTHER:
                return new ProgressiveMediaSource.Factory(dataSourceFactory)
                        .setDrmSessionManagerProvider(unusedMediaItem -> drmSessionManager)
                        .createMediaSource(MediaItem.fromUri(uri));
            default:
                throw new IllegalStateException("Unsupported type: " + type);
        }
    }

    private DataSource.Factory buildDataSourceFactory() {
        HttpDataSource.Factory httpDataSourceFactory = buildHttpDataSourceFactory();

        HashMap<String, String> cookieRequestProperties = new HashMap<>();
        cookieRequestProperties.put("Cookie", cookie);
        httpDataSourceFactory.setDefaultRequestProperties(cookieRequestProperties);
        return new DefaultDataSource.Factory(this, httpDataSourceFactory);
    }

    private HttpDataSource.Factory buildHttpDataSourceFactory() {
        return new DefaultHttpDataSource.Factory()
                .setUserAgent(Util.getUserAgent(this, "ExoPlayerSample"));
    }

    // Activity input

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // See whether the player view wants to handle media or DPAD keys events.
        return playerView.dispatchKeyEvent(event) || super.dispatchKeyEvent(event);
    }

    // OnClickListener methods

    @Override
    public void onClick(View view) {
        if (view == selectTracksButton
                && !isShowingTrackSelectionDialog
                && TrackSelectionDialog.willHaveContent(trackSelector)) {
            isShowingTrackSelectionDialog = true;
            TrackSelectionDialog trackSelectionDialog =
                    TrackSelectionDialog.createForTrackSelector(
                            trackSelector,
                            /* onDismissListener= */ dismissedDialog -> isShowingTrackSelectionDialog = false);
            trackSelectionDialog.show(getSupportFragmentManager(), /* tag= */ null);
        }
    }

    private void showToast(int messageId) {
        showToast(getString(messageId));
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    private class PlayerErrorMessageProvider implements ErrorMessageProvider<PlaybackException> {

        @Override
        public Pair<Integer, String> getErrorMessage(PlaybackException e) {
            String errorString = getString(R.string.error_generic);
            Throwable cause = e.getCause();
            if (cause instanceof MediaCodecRenderer.DecoderInitializationException) {
                // Special case for decoder initialization failures.
                MediaCodecRenderer.DecoderInitializationException decoderInitializationException =
                        (MediaCodecRenderer.DecoderInitializationException) cause;
                if (decoderInitializationException.codecInfo == null) {
                    if (decoderInitializationException.getCause() instanceof MediaCodecUtil.DecoderQueryException) {
                        errorString = getString(R.string.error_querying_decoders);
                    } else if (decoderInitializationException.secureDecoderRequired) {
                        errorString =
                                getString(
                                        R.string.error_no_secure_decoder, decoderInitializationException.mimeType);
                    } else {
                        errorString =
                                getString(R.string.error_no_decoder, decoderInitializationException.mimeType);
                    }
                } else {
                    errorString =
                            getString(
                                    R.string.error_instantiating_decoder,
                                    decoderInitializationException.codecInfo.name);
                }
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(PlayerActivity.this);
            builder.setTitle("Play Error");
            builder.setMessage(errorString);
            builder.setPositiveButton("OK", null);
            Dialog dialog = builder.create();
            dialog.show();

            return Pair.create(0, errorString);
        }
    }

//	//// Chromecast
//	private CastContext mCastContext = null;
//	private CastSession mCastSession = null;
//	private SessionManager mSessionManager = null;
//	private SessionManagerListener<CastSession> mSessionManagerListener = null;
//	private RemoteMediaClient mRemoteClient = null;
//	private RemoteMediaClient.Listener			mRemoteClientListener = null;
//	private RemoteMediaClient.ProgressListener	mRemoteClientProgressListener = null;
//	private Cast.MessageReceivedCallback		mMessageReceivedCallback = null;
//	private PlayStatus mPlayStatus = PlayStatus.getObject();
//	private boolean bNowOnChromecast = false;
//	private boolean bCreated = false;
//	private static boolean bCastReceiverRegistered = false;
//	private final String CAST_MSG_NAMESPACE = "urn:x-cast:com.pallycon.cast";
//
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		super.onCreateOptionsMenu(menu);
//		getMenuInflater().inflate(R.menu.browse, menu);
//		CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu, R.id.media_route_menu_item);
//		return true;
//	}
//
//	private void releaseCast() {
//		Log.d(TAG, "[CAST] releaseCast()");
//
//		bNowOnChromecast = false;
//		mPlayStatus.clear();
//
//		if( mRemoteClient != null ) {
//			Log.d(TAG, "[CAST] remote client is not null. releasing...");
//			if (mRemoteClient.getCurrentItem() != null) {
//				Log.d(TAG, "[CAST] there is a loaded content. stopping it...");
//				mRemoteClient.stop();
//			}
//
//			removeCastReceivers();
//			mRemoteClient = null;
//		} else {
//			bCastReceiverRegistered = false;
//		}
//	}
//
//	private void addCastReceivers() {
//		if( !bCastReceiverRegistered ) {
//			mRemoteClient.addListener(mRemoteClientListener);
//			mRemoteClient.addProgressListener(mRemoteClientProgressListener, 0);
//			bCastReceiverRegistered = true;
//		}
//	}
//	private void removeCastReceivers() {
//		//if( bCastReceiverRegistered ) {
//			mRemoteClient.removeListener(mRemoteClientListener);
//			mRemoteClient.removeProgressListener(mRemoteClientProgressListener);
//			bCastReceiverRegistered = false;
//		//}
//	}
//
//	private void createSessionManagerListener() {
//		mSessionManagerListener = new SessionManagerListener<CastSession>() {
//			@Override
//			public void onSessionEnded(CastSession session, int error) {
//				Log.d(TAG, "[CAST] onSessionEnded()");
//				onApplicationDisconnected();
//			}
//
//			@Override
//			public void onSessionResumed(CastSession session, boolean wasSuspended) {
//				Log.d(TAG, "[CAST] onSessionResumed()");
//				onApplicationConnected(session);
//			}
//
//			@Override
//			public void onSessionResumeFailed(CastSession session, int error) {
//				Log.d(TAG, "[CAST] onSessionResumeFailed()");
//				onApplicationDisconnected();
//			}
//
//			@Override
//			public void onSessionStarted(CastSession session, String sessionId) {
//				Log.d(TAG, "[CAST] onSessionStarted()");
//				onApplicationConnected(session);
//			}
//
//			@Override
//			public void onSessionStartFailed(CastSession session, int error) {
//				Log.d(TAG, "[CAST] onSessionStartFailed()");
//				onApplicationDisconnected();
//			}
//
//			@Override
//			public void onSessionStarting(CastSession session) {
//				Log.d(TAG, "[CAST] onSessionStarting()");
//			}
//
//			@Override
//			public void onSessionEnding(CastSession session) {
//				Log.d(TAG, "[CAST] onSessionEnding()");
//			}
//
//			@Override
//			public void onSessionResuming(CastSession session, String sessionId) {
//				Log.d(TAG, "[CAST] onSessionResuming()");
//			}
//
//			@Override
//			public void onSessionSuspended(CastSession session, int reason) {
//				Log.d(TAG, "[CAST] onSessionSuspended()");
//			}
//
//			private void onApplicationConnected(CastSession castSession) {
//				mCastSession = castSession;
//				getRemoteMediaClient();
//
//				try {
//					mCastSession.setMessageReceivedCallbacks(CAST_MSG_NAMESPACE, mMessageReceivedCallback);
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//
//				mPlayStatus.mPosition = 0;
//				boolean playWhenReady = true;
//
//				mPlayStatus.mScreen = PlayStatus.SCREEN_CAST;
//				if( player != null ) {
//					mPlayStatus.mPosition = player.getCurrentPosition();
//					playWhenReady = player.getPlayWhenReady();
//
//					// pause local player
//					player.setPlayWhenReady(false);
//
//					// disable ui of local player
//					simpleExoPlayerView.setUseController(false);
//				}
//
//				loadRemoteMedia(mPlayStatus.mPosition, playWhenReady);
//			}
//
//			private void onApplicationDisconnected() {
//				// backup latest position before release
//				long latestPosition = mPlayStatus.mPosition;
//				releaseCast();
//
//				mPlayStatus.mScreen = PlayStatus.SCREEN_LOCAL;
//
//				// enable ui of local player
//				simpleExoPlayerView.setUseController(true);
//
//				if( player == null ) {
//					Log.d(TAG, "[CAST] no local player. initializing...");
//					try {
//						initializePlayer();
//					} catch (PallyconDrmException e) {
//						e.printStackTrace();
//					} catch (PallyconEncrypterException e) {
//						e.printStackTrace();
//					} catch (JSONException e) {
//						e.printStackTrace();
//					}
//				}
//
//				// seek position of local player to position of remote player
//				if( player != null ) {
//					player.seekTo(latestPosition);
//					player.setPlayWhenReady(shouldAutoPlay);
//				}
//			}
//		};
//	}
//
//	private String getIdleReasonString(int idleReason) {
//		switch(idleReason) {
//		case MediaStatus.IDLE_REASON_FINISHED:	// 1
//			return "Finish";
//		case MediaStatus.IDLE_REASON_CANCELED:	// 2
//			return "Canceled";
//		case MediaStatus.IDLE_REASON_INTERRUPTED:	// 3
//			return "Interrupted";
//		case MediaStatus.IDLE_REASON_ERROR:	// 4
//			return "Error";
//		case MediaStatus.IDLE_REASON_NONE:	// 0
//		default:
//			return "None";
//		}
//	}
//
//	private void createRemoteMediaClientListener() {
//		mRemoteClientListener = new RemoteMediaClient.Listener() {
//			@Override
//			public void onStatusUpdated() {
//				Log.d(TAG, "[CAST] onStatusUpdated()");
//
//				if( mRemoteClient == null )
//					return;
//
//				switch( mRemoteClient.getPlayerState() ) {
//					case MediaStatus.PLAYER_STATE_IDLE:
//						int idleReason = mRemoteClient.getIdleReason();
//						Log.d(TAG, "[CAST] RemoteMediaClient.getPlayerState(): PLAYER_STATE_IDLE (" + getIdleReasonString(idleReason) +")");
//						mPlayStatus.mCurrentState = PlayStatus.STATE_IDLE;
//
//						if( idleReason == MediaStatus.IDLE_REASON_FINISHED ) {
//							// starting cast after finish, idel reason is 'FINISH' yet.
//							// app must handel 'FINISH' at start
//							if( mPlayStatus.mPosition > 0 ) {
//								releaseCast();
//								finish();
//							}
//						}
//						break;
//					case MediaStatus.PLAYER_STATE_BUFFERING:
//						Log.d(TAG, "[CAST] RemoteMediaClient.getPlayerState(): PLAYER_STATE_BUFFERING");
//						mPlayStatus.mCurrentState = PlayStatus.STATE_BUFFERING;
//						break;
//					case MediaStatus.PLAYER_STATE_PLAYING:
//						Log.d(TAG, "[CAST] RemoteMediaClient.getPlayerState(): PLAYER_STATE_PLAYING");
//						mPlayStatus.mCurrentState = PlayStatus.STATE_PLAYING;
//						shouldAutoPlay = true;
//						break;
//					case MediaStatus.PLAYER_STATE_PAUSED:
//						Log.d(TAG, "[CAST] RemoteMediaClient.getPlayerState(): PLAYER_STATE_PAUSED");
//						mPlayStatus.mCurrentState = PlayStatus.STATE_PAUSED;
//						shouldAutoPlay = false;
//						break;
//					case MediaStatus.PLAYER_STATE_UNKNOWN:
//					default:
//						Log.d(TAG, "[CAST] RemoteMediaClient.getPlayerState(): PLAYER_STATE_UNKNOWN");
//						break;
//				}
//			}
//
//			@Override
//			public void onMetadataUpdated() {
////				Log.d(TAG, "[CAST] onMetadataUpdated()");
//			}
//
//			@Override
//			public void onQueueStatusUpdated() {
////				Log.d(TAG, "[CAST] onQueueStatusUpdated()");
//			}
//
//			@Override
//			public void onPreloadStatusUpdated() {
////				Log.d(TAG, "[CAST] onPreloadStatusUpdated()");
//			}
//
//			@Override
//			public void onSendingRemoteMediaRequest() {
//				Log.d(TAG, "[CAST] onSendingRemoteMediaRequest()");
//				Intent intent = new Intent(PlayerActivity.this, CastControllerActivity.class);
//				startActivity(intent);
//			}
//
//			@Override
//			public void onAdBreakStatusUpdated() {
//				Log.d(TAG, "[CAST] onAdBreakStatusUpdated()");
//			}
//		};
//	}
//
//	private void createRemoteMediaClientProgressListener() {
//		mRemoteClientProgressListener = new RemoteMediaClient.ProgressListener() {
//			@Override
//			public void onProgressUpdated(long l, long l1) {
//				mPlayStatus.mPosition = l;
//			}
//		};
//	}
//
//	private void createMessageReceivedCallback() {
//		mMessageReceivedCallback = new Cast.MessageReceivedCallback() {
//			@Override
//			public void onMessageReceived(CastDevice castDevice, String namespace, String message) {
//				Log.d(TAG, "[CAST] message from receiver: " + message);
//				switch (message) {
//					case "PLAYBACK":
//						Toast.makeText(getApplicationContext(), "The receiver can not play the contents. Is it supported contents format?", Toast.LENGTH_LONG).show();
//						break;
//					case "MEDIAKEYS":
//						Toast.makeText(getApplicationContext(), "The receiver can not decrypt the contents. Please check license status", Toast.LENGTH_LONG).show();
//						break;
//					case "NETWORK":
//						Toast.makeText(getApplicationContext(), "The receiver can not find the contents. Please check network status", Toast.LENGTH_LONG).show();
//						break;
//					case "MANIFEST":
//						Toast.makeText(getApplicationContext(), "The receiver can not read the contents' manifest. Please contact contents provider", Toast.LENGTH_LONG).show();
//						break;
//					case "UNKNOWN":
//					default:
//						Toast.makeText(getApplicationContext(), "The receiver reports unknown error. Please try again", Toast.LENGTH_LONG).show();
//						break;
//				}
//
//				releaseCast();
//				finish();
//			}
//		};
//	}
//	private boolean getRemoteMediaClient(CastSession session) {
//		if( session != null )
//			mCastSession = session;
//		return 	getRemoteMediaClient();
//	}
//
//	private boolean getRemoteMediaClient() {
//		if( mRemoteClient != null ) {
//			removeCastReceivers();
//			mRemoteClient = null;
//		}
//
//		mRemoteClient = mCastSession.getRemoteMediaClient();
//		if (mRemoteClient == null) {
//			Log.d(TAG, "[CAST] remote media client is null");
//			return false;
//		}
//
//		addCastReceivers();
//
//		return true;
//	}
//
//	private MediaInfo buildMediaInfo(String source, String title, String subtitle, String thumbImageUrl) {
//		MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
//
//		if( title == null )
//			title = "Content from sender app";
//		if( thumbImageUrl == null )
//			thumbImageUrl = "https://demo.netsync.co.kr/Mob/Cont/images/no_thumb.png";
//
//		movieMetadata.putString(MediaMetadata.KEY_TITLE, title);
//		movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, subtitle);
//		movieMetadata.addImage(new WebImage(Uri.parse(thumbImageUrl)));
//
//		JSONObject castCustomData = new JSONObject();
//		try {
//			Intent intent = getIntent();
//			if( intent.hasExtra(DRM_LICENSE_URL)) {
//				// get license custom data
//				String userid = intent.getStringExtra(DRM_USERID);
//				String cid = intent.getStringExtra(DRM_CID);
//				String oid = intent.getStringExtra(DRM_OID);
//
//				String licenseUrl = intent.getStringExtra(DRM_LICENSE_URL);
//				String customData = PallyconWVMSDKFactory.getInstance(this).getCustomData(userid, cid, oid);
//
//				// input license data for receiver
//				castCustomData.put("ContentProtection", "widevine"); // 'widevine' must be lower case
//
//				// IT DOES NOT WORK
////				castCustomData.put("licenseUrl", licenseUrl);
////				castCustomData.put("licenseCustomData", customData);
//
//				// IT DOES NOT WORK TOO
//				// create json object with 'pallycon-customdata-v2' and license custom data
////				JSONObject licenseCustomData = new JSONObject();
////				licenseCustomData.put("pallycon-customdata-v2", customData);
////				castCustomData.put("licenseUrl", licenseUrl);
////				castCustomData.put("licenseCustomData", licenseCustomData);
//
//				// IT WORKS !!
//				castCustomData.put("licenseUrl", licenseUrl + "?pallycon-customdata-v2=" + customData);
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		return new MediaInfo.Builder(source)
//				.setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
//				.setContentType("application/dash+xml")
//				.setMetadata(movieMetadata)
//				.setCustomData(castCustomData)
//				.build();
//	}
//
//	private boolean loadRemoteMedia(long position, boolean autoPlay) {
//		Log.d(TAG, "[CAST] loadRemoteMedia()");
//		if (mCastSession == null) {
//			Log.d(TAG, "[CAST] cast session is null");
//			return false;
//		}
//
//		Intent intent = getIntent();
//		Uri uri = intent.getData();
//
//		if( uri == null || uri.toString().length() < 1 ) {
//			Log.d(TAG, "[CAST] uri to cast is invalid");
//			return false;
//		}
//
//		try {
//			MediaLoadOptions options = new MediaLoadOptions.Builder()
//					.setAutoplay(autoPlay)
//					.setPlayPosition(position)
//					.build();
//			if( !getRemoteMediaClient() )
//				return false;
//
//			bNowOnChromecast = true;
//			mRemoteClient.load(buildMediaInfo(uri.toString(), intent.getStringExtra(CONTENTS_TITLE), null, intent.getStringExtra(THUMB_URL)), options);
//			return true;
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		return false;
//	}
//
//	//// end of Chromecast
}
