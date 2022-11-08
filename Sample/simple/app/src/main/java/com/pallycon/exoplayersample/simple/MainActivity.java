package com.pallycon.exoplayersample.simple;

import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Toast;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.drm.DrmSessionManager;
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
import com.google.android.exoplayer2.util.Util;
import com.pallycon.widevinelibrary.PallyconDrmException;
import com.pallycon.widevinelibrary.PallyconEventListener;
import com.pallycon.widevinelibrary.PallyconKeyRequest;
import com.pallycon.widevinelibrary.PallyconWVMSDK;
import com.pallycon.widevinelibrary.PallyconWVMSDKFactory;
import com.pallycon.widevinelibrary.UnAuthorizedDeviceException;

import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    PallyconWVMSDK WVMAgent = null;

    private ExoPlayer player;
    private StyledPlayerView playerView;
    private DataSource.Factory mediaDataSourceFactory;
    private DefaultTrackSelector trackSelector;
    private MediaSource mediaSource;
    private String userAgent;

    // PallyconKeyRequest
    private PallyconKeyRequest keyRequestCallback = new PallyconKeyRequest() {
        @Override
        public DataSourceInputStream contentKeyRequest(byte[] keyData, Map<String, String> requestData) {
            HttpDataSource.Factory httpDataSourceFactory =
                    new DefaultHttpDataSource.Factory()
                            .setUserAgent(Util.getUserAgent(MainActivity.this, "PallyConSimple"));
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

    // TODO : must implement ExoPlayer.EventListener
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

            Toast.makeText(MainActivity.this, errorString, Toast.LENGTH_LONG).show();
        }
    };

    // TODO : must implement PallyconEventListener
    private PallyconEventListener pallyconEventListener = new PallyconEventListener() {
        @Override
        public void onDrmKeysLoaded(Map<String, String> licenseInfo) {
        }

        @Override
        public void onDrmSessionManagerError(Exception e) {
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
        }

        @Override
        public void onDrmKeysRestored() {
        }

        @Override
        public void onDrmKeysRemoved() {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy pol = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
        StrictMode.setThreadPolicy(pol);

        playerView = findViewById(R.id.player_view);
        playerView.requestFocus();

        userAgent = Util.getUserAgent(this, "ExoPlayerDemo");
        mediaDataSourceFactory = buildDataSourceFactory();
        trackSelector = new DefaultTrackSelector(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WVMAgent.release();
    }


    private void initializePlayer() {
        // TODO: 1. initialize PallyconWVM SDK
        String siteId = "your site id";
        String siteKey = "your site key";
        try {
            WVMAgent = PallyconWVMSDKFactory.getInstance(this);
            WVMAgent.init(this, null, siteId, siteKey);
            WVMAgent.setPallyconEventListener(pallyconEventListener);
            //WVMAgent.setPallyconKeyRequestCallback(keyRequestCallback);
        } catch (PallyconDrmException e) {
            e.printStackTrace();
        } catch (UnAuthorizedDeviceException e) {
            e.printStackTrace();
        }

        // TODO : 2.set content information
        UUID drmSchemeUuid = UUID.fromString((C.WIDEVINE_UUID).toString());
        Uri uri = Uri.parse("https://contents.pallycon.com/TEST/PACKAGED_CONTENT/TEST_SIMPLE/dash/stream.mpd");
        String drmLicenseUrl = "https://license.pallycon.com/ri/licenseManager.do";
        String token = "Token String";

        // TODO : 3.set drm session manager
        DrmSessionManager drmSessionManager = null;
        try {
            drmSessionManager = WVMAgent.createDrmSessionManagerByToken(
                    drmSchemeUuid,
                    drmLicenseUrl,
                    uri,
                    token);
        } catch (PallyconDrmException e) {
            e.printStackTrace();
        }

        mediaSource = buildMediaSource(uri, drmSessionManager);

        // player setting
        player = new ExoPlayer.Builder(/* context= */ this)
                .setTrackSelector(trackSelector)
                .build();
        player.setMediaSource(mediaSource);
        player.addListener(playerEventListener);
        playerView.setPlayer(player);
        player.setPlayWhenReady(true);

        player.prepare();
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            initializePlayer();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Util.SDK_INT <= 23 || player == null) {
            initializePlayer();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Util.SDK_INT <= 23) {
            releasePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
    }

    @SuppressWarnings("unchecked")
    private MediaSource buildMediaSource(Uri uri, DrmSessionManager drmSessionManager) {
        int type = Util.inferContentType(uri.getLastPathSegment());
        DataSource.Factory dataSourceFactory =
                new DefaultDataSource.Factory(this);
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

        return new DefaultDataSource.Factory(this, httpDataSourceFactory);
    }

    private HttpDataSource.Factory buildHttpDataSourceFactory() {
        return new DefaultHttpDataSource.Factory()
                .setUserAgent(Util.getUserAgent(this, "ExoPlayerSample"));
    }
}
