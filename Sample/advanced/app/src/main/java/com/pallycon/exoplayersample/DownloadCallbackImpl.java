package com.pallycon.exoplayersample;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.pallycon.widevinelibrary.NetworkConnectedException;
import com.pallycon.widevinelibrary.PallyconDownloadException;
import com.pallycon.widevinelibrary.PallyconDownloadTask;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by pallycon on 2018-04-12.
 */

public class DownloadCallbackImpl implements PallyconDownloadTask.PallyconDownloadCallback {
	private String TAG = "pallycon_callback";
	private Context context;

	DownloadCallbackImpl(Context context) {
		this.context = context;
	}

	@Override
	public boolean downloadFile(String currentUrl, int totalCount, int currentCount, String localPath) throws NetworkConnectedException {
		InputStream is = null;
		BufferedInputStream bis = null;
		FileOutputStream fos = null;
		BufferedOutputStream bos = null;

		int tmpTotalContentBytes = 0;
		long totalContentBytes = 0;
		long downloadedBytes = 0;

		try {
			URL url = new URL(currentUrl);
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

			File localFile = new File(localPath);
			if(!localFile.getParentFile().exists()) {
				if(localFile.getParentFile().mkdirs() == false) {
					throw new PallyconDownloadException("can't make dirs");
				}
			}

			downloadedBytes = (int) localFile.length();

			urlConnection.setRequestMethod("GET");
			urlConnection.setRequestProperty("Range", "bytes=" + downloadedBytes + "-");
			urlConnection.connect();

			int responseCode = urlConnection.getResponseCode();
			Log.e(TAG, "responseCode : " + responseCode + " : " + url.getFile());
			if (responseCode == 416) {
				Log.e(TAG, url.getFile() + " is already downloaded");
				if(currentCount != -1) {
					Log.e(TAG, "update your progressbar to 100%");
				}
				return true;
			}
//			} else if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
//				return false;
//			}

			tmpTotalContentBytes = urlConnection.getContentLength();
			if (tmpTotalContentBytes <= 0) {
				Log.e(TAG, url.getFile() + " is already downloaded");
				if(currentCount != -1) {
					Log.e(TAG, "update your progressbar to 100%");
				}
				return true;
			} else {
				totalContentBytes = downloadedBytes + tmpTotalContentBytes;
			}

			urlConnection.setReadTimeout(3000);
			urlConnection.setConnectTimeout(3000);

			is = urlConnection.getInputStream();
			bis = new BufferedInputStream(is);

			fos = new FileOutputStream(localFile, downloadedBytes != 0);
			bos = new BufferedOutputStream(fos);

			byte[] buffer = new byte[102400];

			int readSize = 0;
			int oldPercent = 0;

			while ((readSize = bis.read(buffer)) != -1) {
				bos.write(buffer, 0, readSize);
				downloadedBytes += readSize;

				int percent = (int) (((double) downloadedBytes / (double) totalContentBytes) * 100.0);

				if (oldPercent != percent) {
					oldPercent = percent;

					if(currentCount != -1) {
						Handler handler = new Handler(context.getMainLooper());
						final int updatePercent = oldPercent;
						handler.post(new Runnable() {
							@Override
							public void run() {
								showSimpleToast(context, updatePercent + " : % downloaded");
							}
						});
					}
				}
			}

			if(Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
				urlConnection.disconnect();
			}

			return true;

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			try {
				if(bos != null) {
					bos.flush();
					bos.close();
				}

				if(fos != null) {
					fos.close();
				}

				if(is != null) {
					is.close();
				}

				if(bis != null) {
					bis.close();
				}

			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}
	}

	public static Toast mToast = null;
	public static void showSimpleToast(Context context, String msg) {
		if( mToast == null ) {
			mToast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
		} else {
			mToast.setText( msg );
		}
		mToast.show();
	}
}
