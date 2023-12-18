package com.pallycon.exoplayersample.simple;

import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.exoplayer.ExoPlaybackException;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.dash.DashMediaSource;
import androidx.media3.exoplayer.drm.DrmSessionManager;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.ui.PlayerView;

import com.pallycon.widevine.exception.PallyConException;
import com.pallycon.widevine.exception.PallyConLicenseServerException;
import com.pallycon.widevine.model.ContentData;
import com.pallycon.widevine.model.PallyConDrmConfigration;
import com.pallycon.widevine.model.PallyConEventListener;
import com.pallycon.widevine.sdk.PallyConWvSDK;

public class MainActivity extends AppCompatActivity {

    PallyConWvSDK WVMAgent = null;

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
                "eyJrZXlfcm90YXRpb24iOmZhbHNlLCJyZXNwb25zZV9mb3JtYXQiOiJvcmlnaW5hbCIsInVzZXJfaWQiOiJ0ZXN0VXNlciIsImRybV90eXBlIjoid2lkZXZpbmUiLCJzaXRlX2lkIjoiREVNTyIsImhhc2giOiJpSGlpQmM3U1QrWTR1T0h1VnVPQVNmNU1nTDVibDJMb1FuNzNHREtcLzltbz0iLCJjaWQiOiJtdWx0aXRyYWNrcyIsInBvbGljeSI6IjlXcUlXa2RocHhWR0s4UFNJWWNuSnNjdnVBOXN4Z3ViTHNkK2FqdVwvYm9tUVpQYnFJK3hhZVlmUW9jY2t2dUVmQWFxZFc1aFhnSk5nY1NTM2ZTN284TnNqd3N6ak11dnQrMFF6TGtaVlZObXgwa2VmT2Uyd0NzMlRJVGdkVTRCdk45YWJoZDByUWtNSXJtb0llb0pIcUllSGNSdlZmNlQxNFJtVEFERXBDWTQ2NHdxamNzWjA0Uk82Zm90Nm5yZjhXSGZ3QVNjek9kV1d6QStFRlRadDhRTWw5SFRueWVYK1g3YXp1Y2VmQjJBd2V0XC9hQm0rZXpmUERodFZuaUhsSiIsInRpbWVzdGFtcCI6IjIwMjItMDgtMDVUMDY6MDM6MjJaIn0="
        );
        ContentData content = new ContentData(
                "https://contents.pallycon.com/DEV/sglee/multitracks/dash/stream.mpd",
                "",
                config
        );

        // TODO: 2. initialize PallyconWVM SDK
        WVMAgent = PallyConWvSDK.createPallyConWvSDK(this, content);
        WVMAgent.setPallyConEventListener(drmListener);
        MediaSource mediaSource = null;
        try {
            mediaSource = WVMAgent.getMediaSource();
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
