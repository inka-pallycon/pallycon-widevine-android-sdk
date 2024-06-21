package com.pallycon.androidtvsample;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.exoplayer.source.MediaSource;

import com.pallycon.widevine.exception.PallyConException;
import com.pallycon.widevine.exception.PallyConLicenseServerException;
import com.pallycon.widevine.model.ContentData;
import com.pallycon.widevine.model.DownloadState;
import com.pallycon.widevine.model.PallyConEventListener;
import com.pallycon.widevine.sdk.PallyConWvSDK;
import com.pallycon.widevine.track.PallyConDownloaderTracks;

import java.util.concurrent.Callable;

public class PallyConWvSDKManager {
    private Context context = null;
    private Activity activity = null;
    private PallyConWvSDK wvSDK = null;
    private PallyConEventListener drmListener = new PallyConEventListener() {
        @Override
        public void onFailed(@NonNull ContentData contentData, @Nullable PallyConLicenseServerException e) {
            e.printStackTrace();
            Utils.showSimpleDialog(activity, "onFailed", e.getMessage());
        }

        @Override
        public void onFailed(@NonNull ContentData contentData, @Nullable PallyConException e) {
            e.printStackTrace();
            Utils.showSimpleDialog(activity, "onFailed", e.getMessage());
        }

        @Override
        public void onPaused(@NonNull ContentData contentData) {

        }

        @Override
        public void onRemoved(@NonNull ContentData contentData) {

        }

        @Override
        public void onRestarting(@NonNull ContentData contentData) {

        }

        @Override
        public void onStopped(@NonNull ContentData contentData) {

        }

        @Override
        public void onProgress(@NonNull ContentData contentData, float percent, long downloadedBytes) {

        }

        @Override
        public void onCompleted(@NonNull ContentData contentData) {

        }
    };

    public static PallyConWvSDKManager getInstance(Activity activity) {
        LazyHolder.INSTANCE.activity = activity;
        LazyHolder.INSTANCE.context = activity.getApplicationContext();
        return LazyHolder.INSTANCE;
    }

    private static class LazyHolder {
        private static final PallyConWvSDKManager INSTANCE = new PallyConWvSDKManager();
    }

    public void createSDK(ContentData contentData) {
        wvSDK = PallyConWvSDK.createPallyConWvSDK(
                this.context,
                contentData);
        wvSDK.setPallyConEventListener(drmListener);
    }

    public void downloadLicense(Callable<Void> onSuccess) {
        wvSDK.downloadLicense(null, () -> {
                    try {
                        onSuccess.call();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                },
                e -> {
                    e.printStackTrace();
                    Utils.showSimpleDialog(activity, "content data is not correct", e.getMessage());
                    return null;
                });
    }

    public void download(PallyConDownloaderTracks tracks) {
        try {
            wvSDK.download(tracks);
        } catch (PallyConException.ContentDataException e) {
            e.printStackTrace();
            Utils.showSimpleDialog(activity, "content data error", e.getMessage());
        } catch (PallyConException.DownloadException e) {
            e.printStackTrace();
            Utils.showSimpleDialog(activity, "download exception", e.getMessage());
        } catch (PallyConException.DrmException e) {
            e.printStackTrace();
            Utils.showSimpleDialog(activity, "drm exception", e.getMessage());
        }
    }

    public MediaSource getMediaSource() {
        MediaSource mediaSource = null;
        try {
            mediaSource = wvSDK.getMediaSource();
        } catch (PallyConException.DetectedDeviceTimeModifiedException e) {
            e.printStackTrace();
            Utils.showSimpleDialog(activity, "modified device time", e.getMessage());
        } catch (PallyConException.ContentDataException e) {
            e.printStackTrace();
            Utils.showSimpleDialog(activity, "content data error", e.getMessage());
        } finally {
            return mediaSource;
        }
    }

    public DownloadState getDownloadState() {
        return wvSDK.getDownloadState();
    }

    public void defaultDownload() {
        wvSDK.getContentTrackInfo((tracks) -> {
            download(tracks);
            return null;
        }, e -> {
            e.printStackTrace();
            Utils.showSimpleDialog(activity, "PallyConException", e.getMessage());
            return null;
        });
    }

    public void removeLicense() {
        try {
            wvSDK.removeLicense();
        } catch (PallyConException.DrmException e) {
            e.printStackTrace();
            Utils.showSimpleDialog(activity, "DrmException", e.getMessage());
        }
    }

    public void remove() {
        try {
            wvSDK.remove();
        } catch (PallyConException.ContentDataException e) {
            e.printStackTrace();
            Utils.showSimpleDialog(activity, "content data error", e.getMessage());
        } catch (PallyConException.DownloadException e) {
            e.printStackTrace();
            Utils.showSimpleDialog(activity, "DownloadException", e.getMessage());
        }
    }

    public void release() {
        wvSDK.release();
    }
}
