package com.pallycon.exoplayersample.simple;

import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.pallycon.widevine.exception.PallyConException;
import com.pallycon.widevine.exception.PallyConLicenseServerException;
import com.pallycon.widevine.model.ContentData;
import com.pallycon.widevine.model.PallyConDrmConfigration;
import com.pallycon.widevine.model.PallyConEventListener;
import com.pallycon.widevine.sdk.PallyConWvSDK;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    PallyConWvSDK WVMAgent = null;
    PallyConWvSDK WVMAgent2 = null;

    private ExoPlayer player;
    private PlayerView playerView;
    private DataSource.Factory mediaDataSourceFactory;
    private DefaultTrackSelector trackSelector;
    private MediaSource mediaSource;
    private String userAgent;

    private PallyConEventListener drmListener = new PallyConEventListener(

    ) {
        @Override
        public void onFailed(@Nullable String currentUrl, @Nullable PallyConException e) {
            Toast.makeText(getApplicationContext(), e.getMsg(), Toast.LENGTH_LONG).show();
        }

        @Override
        public void onFailed(@Nullable String currentUrl, @Nullable PallyConLicenseServerException e) {
            String message = String.format("%d, %s", e.errorCode(), e.body());
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onPaused(@Nullable String currentUrl) {

        }

        @Override
        public void onRemoved(@Nullable String currentUrl) {

        }

        @Override
        public void onRestarting(@Nullable String currentUrl) {

        }

        @Override
        public void onStopped(@Nullable String currentUrl) {

        }

        @Override
        public void onProgress(@Nullable String currentUrl, float percent, long downloadedBytes) {

        }

        @Override
        public void onCompleted(@Nullable String currentUrl) {

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
        // TODO : 1.set content information
        PallyConDrmConfigration config = new PallyConDrmConfigration(
            "DEMO",
            "eyJrZXlfcm90YXRpb24iOmZhbHNlLCJyZXNwb25zZV9mb3JtYXQiOiJvcmlnaW5hbCIsInVzZXJfaWQiOiJwYWxseWNvbiIsImRybV90eXBlIjoid2lkZXZpbmUiLCJzaXRlX2lkIjoiREVNTyIsImhhc2giOiJkNTBDSVVUS1RwRDl6T3dGaU9DSysrXC83Q3pLOStZN3NkcHFhUUppdDJWQT0iLCJjaWQiOiJUZXN0UnVubmVyIiwicG9saWN5IjoiOVdxSVdrZGhweFZHSzhQU0lZY25Kc2N2dUE5c3hndWJMc2QrYWp1XC9ib21RWlBicUkreGFlWWZRb2Nja3Z1RWZBYXFkVzVoWGdKTmdjU1MzZlM3bzhNczB3QXNuN05UbmJIUmtwWDFDeTEyTkhwMlZPN1pMeFJvZDhVdkUwZnBFbUpYOUpuRDh6ZktkdE9RWk9UYXljK280RzNCT0xmU29OaFpWbkIwUGxEbW1rVk5jbXpndko2YloxdXBudjFcLzJFM2lXZXd3eklTNFVOQlhTS21zVUFCZnBRQjg4Q2VJYlZSM0hKZWJvcEpwZG1DTFFvRmtCT09DQU9qWElBOUVHIiwidGltZXN0YW1wIjoiMjAyMi0xMC0xMVQwNzowMToxN1oifQ=="
        );
        ContentData content = new ContentData(
                "https://contents.pallycon.com/TEST/PACKAGED_CONTENT/TEST_SIMPLE/dash/stream.mpd",
                "",
                config
        );

        PallyConDrmConfigration config2 = new PallyConDrmConfigration(
                "DEMO",
                "eyJrZXlfcm90YXRpb24iOmZhbHNlLCJyZXNwb25zZV9mb3JtYXQiOiJvcmlnaW5hbCIsInVzZXJfaWQiOiJwYWxseWNvbiIsImRybV90eXBlIjoid2lkZXZpbmUiLCJzaXRlX2lkIjoiREVNTyIsImhhc2giOiJkNTBDSVVUS1RwRDl6T3dGaU9DSysrXC83Q3pLOStZN3NkcHFhUUppdDJWQT0iLCJjaWQiOiJUZXN0UnVubmVyIiwicG9saWN5IjoiOVdxSVdrZGhweFZHSzhQU0lZY25Kc2N2dUE5c3hndWJMc2QrYWp1XC9ib21RWlBicUkreGFlWWZRb2Nja3Z1RWZBYXFkVzVoWGdKTmdjU1MzZlM3bzhNczB3QXNuN05UbmJIUmtwWDFDeTEyTkhwMlZPN1pMeFJvZDhVdkUwZnBFbUpYOUpuRDh6ZktkdE9RWk9UYXljK280RzNCT0xmU29OaFpWbkIwUGxEbW1rVk5jbXpndko2YloxdXBudjFcLzJFM2lXZXd3eklTNFVOQlhTS21zVUFCZnBRQjg4Q2VJYlZSM0hKZWJvcEpwZG1DTFFvRmtCT09DQU9qWElBOUVHIiwidGltZXN0YW1wIjoiMjAyMi0xMC0xMVQwNzowMToxN1oifQ=="
        );

        ContentData content2 = new ContentData(
                "https://contents.pallycon.com/TEST/PACKAGED_CONTENT/TEST_MULTITRACK/dash/stream.mpd",
                "",
                config2
        );

        // TODO: 2. initialize PallyconWVM SDK
        WVMAgent = PallyConWvSDK.createPallyConWvSDK(this, content);
        WVMAgent.setPallyConEventListener(drmListener);

        DrmSessionManager manager = WVMAgent.getDrmSessionManager();

        WVMAgent2 = PallyConWvSDK.createPallyConWvSDK(this, content2);
        MediaSource mediaSource = null;
        MediaSource mediaSource2 = null;
        try {
            mediaSource = WVMAgent.getMediaSource(manager);
            mediaSource2 = WVMAgent2.getMediaSource(manager);
        } catch (PallyConException.ContentDataException e) {
            e.printStackTrace();
            return;
        } catch (PallyConException.DetectedDeviceTimeModifiedException e) {
            e.printStackTrace();
            return;
        }

        // player setting
        player = new ExoPlayer.Builder(/* context= */ this)
                .setTrackSelector(trackSelector)
                .build();

//        player.setMediaSource(mediaSource);
        ArrayList<MediaSource> mediaSources = new ArrayList<>();
        mediaSources.add(mediaSource);
        mediaSources.add(mediaSource2);

        player.setMediaSources(mediaSources);

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
