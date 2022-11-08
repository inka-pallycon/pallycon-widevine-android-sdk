/*
 * PallyCon Team ( http://www.pallycon.com )
 *
 * This is a simple example project to show how to build a APP using the PallyCon Widevine SDK
 * The SDK is based on Exo player library
 */

package com.pallycon.exoplayersample;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.NetworkOnMainThreadException;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.JsonReader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ParserException;
import com.google.android.exoplayer2.util.MimeTypes;
//import com.google.android.gms.cast.framework.CastButtonFactory;
//import com.google.android.gms.cast.framework.CastContext;
import com.google.common.base.Ascii;
import com.pallycon.widevinelibrary.MpdTrackSelector;
import com.pallycon.widevinelibrary.NetworkConnectedException;
import com.pallycon.widevinelibrary.PallyConDetectRooting;
import com.pallycon.widevinelibrary.PallyConMpegDashInfo;
import com.pallycon.widevinelibrary.PallyconDownloadException;
import com.pallycon.widevinelibrary.PallyconDownloadTask;
import com.pallycon.widevinelibrary.PallyconDrmException;
import com.pallycon.widevinelibrary.PallyconLoggingListener;
import com.pallycon.widevinelibrary.PallyconServerResponseException;
import com.pallycon.widevinelibrary.PallyconWVMSDK;
import com.pallycon.widevinelibrary.PallyconWVMSDKFactory;
import com.pallycon.widevinelibrary.UnAuthorizedDeviceException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// TODO : must implement PallyconDownloadTask.PallyconDownloadEventListener
public class MainActivity extends AppCompatActivity implements PallyconDownloadTask.PallyconDownloadEventListener {
	private final int MY_STORAGE_PERMISSION = 1;
	private static final String TAG = "pallycon_manager";
	private List<ContentGroup> arrContentGroup = new ArrayList<>();
	private ExpandableListView dataListView;
	private Content content;
	private Site site;
	private View downloadLayout;
	private PallyconDownloadTask downloadTask;
	private PallyconDownloadTask.PallyconDownloadFinishCallback pallyconDownloadFinishCallback;
	private PallyconWVMSDK.DownloadLicenseResultCallback downloadLicenseResultCallback;
	private Dialog downloadDialog;
	private Handler eventHandler = new Handler();
	PallyconWVMSDK WVMAgent = null;

	@Override
	public void onPreExecute() {
		// TODO: Configure the UI to be displayed on the screen before starting the download.
		if (downloadDialog == null) {
			LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
			downloadLayout = inflater.inflate(R.layout.layout_download, null);
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Download");
			builder.setView(downloadLayout);
			builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialogInterface, int i) {
					downloadTask.terminate();
				}
			});

			downloadDialog = builder.create();
			downloadDialog.setCancelable(false);
			downloadDialog.setCanceledOnTouchOutside(false);
			downloadDialog.show();
		}
	}

	@Override
	public void onPostExecute() {
		// TODO: Release the UI after the download is complete.
		if (downloadDialog != null) {
			downloadDialog.dismiss();
			downloadDialog = null;
		}
	}

	@Override
	public void onProgressUpdate(String fileName, long downloadedSize, long totalSize, int percent, int totalCount, int currentCount) {
		// TODO: Use the download progress data to update the download UI in real time.
		if (downloadLayout == null) {
			LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
			downloadLayout = inflater.inflate(R.layout.layout_download, null);
		}

		ProgressBar progressDownloadPercent = null;
		TextView tvRight = null;
		TextView tvLeft = null;

		TextView tvContentName = (TextView) downloadLayout.findViewById(R.id.download_content_name);
		progressDownloadPercent = (ProgressBar) downloadLayout.findViewById(R.id.download_progress);
		tvRight = (TextView) downloadLayout.findViewById(R.id.download_right);
		tvLeft = (TextView) downloadLayout.findViewById(R.id.download_left);

		if (tvContentName != null) {
			tvContentName.setText(fileName);
		}

		if (progressDownloadPercent != null) {
			progressDownloadPercent.setMax(100);
			progressDownloadPercent.setProgress(percent);
		}

		if (tvRight != null) {
			tvRight.setText(String.valueOf(currentCount) + "/" + String.valueOf(totalCount));
		}

		if (tvLeft != null) {
			tvLeft.setText(String.valueOf(downloadedSize) + "/" + String.valueOf(totalSize));
		}
	}

	@Override
	public void onProgressUpdate(String fileName, int totalCount, int currentCount, int percent) {
		// TODO: Use the download progress data to update the download UI in real time.
		if (downloadLayout == null) {
			LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
			downloadLayout = inflater.inflate(R.layout.layout_download, null);
		}

		ProgressBar progressDownloadPercent = null;
		TextView tvRight = null;
		TextView tvLeft = null;

		TextView tvContentName = (TextView) downloadLayout.findViewById(R.id.download_content_name);
		progressDownloadPercent = (ProgressBar) downloadLayout.findViewById(R.id.download_progress);
		tvRight = (TextView) downloadLayout.findViewById(R.id.download_right);
		tvLeft = (TextView) downloadLayout.findViewById(R.id.download_left);

		if (tvContentName != null) {
			tvContentName.setText(fileName);
		}

		if (progressDownloadPercent != null) {
			progressDownloadPercent.setMax(100);
			progressDownloadPercent.setProgress(percent);
		}

		if (tvRight != null) {
			tvRight.setText(String.valueOf(currentCount) + "/" + String.valueOf(totalCount));
		}

//		if (tvLeft != null) {
//			tvLeft.setText(String.valueOf(currentCount) + "/" + String.valueOf(totalCount));
//		}
	}

	@Override
	public void onCancelled() {
		// TODO: Release the UI when the download is canceled.
		if (downloadDialog != null) {
			downloadDialog.dismiss();
			downloadDialog = null;
		}
	}

	@Override
	public void onNetworkError(NetworkConnectedException e) {
		// TODO: Check the network connection status.
		e.printStackTrace();
		showSimpleDialog("Network Error", e.getMessage());
	}

	@Override
	public void onPallyconDownloadError(PallyconDownloadException e) {
		// TODO: Check for download failures such as storage I / O, MPD file format, URL address, and so on.
		e.printStackTrace();
		showSimpleDialog("PallyconDownload Error", e.getMessage());
	}

	protected class Site {
		public String siteId;
		public String siteKey;
	}

	protected class ContentGroup {
		public String title;
		public List<Content> arrContent;

		ContentGroup(String title) {
			this.title = title;
			this.arrContent = new ArrayList<>();
		}
	}

	protected class Content {
		public Uri uri;
		public String name = "";
		public UUID drmSchemeUuid;
		public String drmLicenseUrl = "";
		public String userId = "";
		public String cid = "";
		public String oid = "";
		public String token = "";
		public String thumbUrl = "";
		public String customData = "";
		public String cookie = "";
		public boolean multiSession;

		public Content() {};
	}

//	//// Chromecast
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		super.onCreateOptionsMenu(menu);
//		getMenuInflater().inflate(R.menu.browse, menu);
//		CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu, R.id.media_route_menu_item);
//		return true;
//	}
//	//// CC

	private PallyconLoggingListener loggingListener = new PallyconLoggingListener() {
		@Override
		public void log(String message, String stackTrace){
			Log.d(TAG, stackTrace);
			Log.d(TAG, message);
		}
	};

	@Override
	protected void onDestroy() {
		super.onDestroy();
		WVMAgent.release();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		try {
			site = createSite();
			WVMAgent = PallyconWVMSDKFactory.getInstance(this);
			WVMAgent.setPallyconLoggingListener(loggingListener);
			WVMAgent.init(this, eventHandler, site.siteId, site.siteKey);

			if (PallyConDetectRooting.detectRootingDevice())
				Log.d(TAG, "Rooting");
			else
				Log.d(TAG, "Not Rooting");

		} catch (PallyconDrmException e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			finish();
			return;
		} catch (IOException e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			finish();
			return;
		} catch (UnAuthorizedDeviceException e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		try {
			boolean isL1 = WVMAgent.isL1WidevineAvailable(MimeTypes.VIDEO_H264);
			if(isL1) {
				Log.d(TAG, "L1 WideVine");
			} else {
				Log.d(TAG, "L3 WideVine");
			}

		} catch (PallyconDrmException e) {
			e.printStackTrace();
		}

		pallyconDownloadFinishCallback = new PallyconDownloadTask.PallyconDownloadFinishCallback() {
			@Override
			public void onDownloadFinish() {
				try {
					WVMAgent.setCookie(content.cookie);
					Log.d(TAG, "download license after download finish...");
					if (content.token != null && content.token.equals("") == false) {
						WVMAgent.downloadLicenseByToken(content.drmSchemeUuid, content.drmLicenseUrl, content.uri, content.token);
					} else if (content.customData != null && content.customData.equals("") == false) {
						WVMAgent.downloadLicenseByCustomData(content.drmSchemeUuid, content.drmLicenseUrl, content.uri, content.customData);
					} else {
						WVMAgent.downloadLicense(content.drmSchemeUuid, content.drmLicenseUrl, content.uri, content.userId, content.cid, content.oid);
					}

					// for proxy
					//WVMAgent.downloadLicenseByProxy(content.drmSchemeUuid, content.drmLicenseUrl, content.uri, content.cid);

				} catch (PallyconServerResponseException e) {
					showSimpleDialog("Server Response Error", "errorCode : " + e.getErrorCode() + "\n" + "message : " + e.getMessage());
				} catch (PallyconDrmException e) {
					showSimpleDialog("PallyconDrm Error", e.getMessage());
				}
			}
		};

		try {
			dataListView = (ExpandableListView) findViewById(R.id.data_list);
			dataListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
					int itemType = ExpandableListView.getPackedPositionType(l);
					if (itemType == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
						int childPosition = ExpandableListView.getPackedPositionChild(l);
						int groupPosition = ExpandableListView.getPackedPositionGroup(l);
						content = arrContentGroup.get(groupPosition).arrContent.get(childPosition);

						String title = arrContentGroup.get(groupPosition).title;
						if (title.equals("Download")) {
							AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
							builder.setTitle("Delete Menu");
							builder.setItems(new String[]{"remove file", "remove license", "remove All license", "exist license", "Re-Provisioning"}, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialogInterface, int i) {
									if (i == 0) {
										// TODO: Remove content file (mpd, video, audio).
										try {
											downloadTask = new PallyconDownloadTask(MainActivity.this, content.uri, content.name, MainActivity.this, eventHandler, null, pallyconDownloadFinishCallback);
											downloadTask.removeDownloadContent();
										} catch (PallyconDownloadException e) {
											Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
											return;
										}

									} else if (i == 1) {
										// TODO: Delete license.
										try {
											if(content.customData != null && content.customData.equals("") == false) {
												WVMAgent.removeLicenseByCustomData(content.drmSchemeUuid, content.drmLicenseUrl, content.uri, content.customData);
											} else {
												WVMAgent.removeLicense(content.drmSchemeUuid, content.drmLicenseUrl, content.uri, content.userId, content.cid, content.oid);
											}

											// for proxy
											//WVMAgent.removeLicenseByProxy(content.drmSchemeUuid, content.drmLicenseUrl, content.uri, content.cid);

										} catch (PallyconDrmException e) {
											Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
										} catch (NetworkConnectedException e) {
											Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
										}
									}
									else if (i == 2) {
										try {
											// remove all license in the database
											WVMAgent.removeAllLicenseInDataBase();

										} catch (PallyconDrmException e) {
											Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
										}
									}
									else if (i == 3) {
										try {
											if (WVMAgent.getOfflineLicenseExpireDate(content.cid) != null )
												Toast.makeText(MainActivity.this, "Exist License : " + WVMAgent.getOfflineLicenseExpireDate(content.cid), Toast.LENGTH_LONG).show();
											else
												Toast.makeText(MainActivity.this, "Not Exist License : " + content.cid, Toast.LENGTH_LONG).show();
										} catch (PallyconDrmException e) {
											Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
										}
									}
									else if (i == 4) {
										boolean isOK = WVMAgent.provideForceProvisionRequest();
										if (isOK) {
											// Existing licenses cannot be used when provisioning changes.
											Toast.makeText(MainActivity.this, "Re-Provisioning. All stored offline licenses are removed.", Toast.LENGTH_LONG).show();
										} else {
											Toast.makeText(MainActivity.this, "Provision Request Fail. Please check your network or reinstall the app and try again.", Toast.LENGTH_LONG).show();
										}
									}
								}
							});
							builder.setNegativeButton("Cancel", null);
							Dialog dialog = builder.create();
							dialog.show();
						}
						return true;

					} else {
						return false;
					}
				}
			});
			dataListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
				@Override
				public boolean onChildClick(ExpandableListView expandableListView, View view, int groupPosition, int childPosition, long id) {
					content = arrContentGroup.get(groupPosition).arrContent.get(childPosition);
					Intent intent = new Intent(MainActivity.this, PlayerActivity.class);

					String title = arrContentGroup.get(groupPosition).title;
					if (title.equals("Streaming")) {
						intent.setData(content.uri);
						intent.putExtra(PlayerActivity.CONTENTS_TITLE, content.name);
						intent.putExtra(PlayerActivity.THUMB_URL, content.thumbUrl);
						if (content.drmSchemeUuid != null) {
							intent.putExtra(PlayerActivity.DRM_SCHEME_UUID_EXTRA, content.drmSchemeUuid.toString());
							intent.putExtra(PlayerActivity.DRM_LICENSE_URL, content.drmLicenseUrl);
							intent.putExtra(PlayerActivity.DRM_MULTI_SESSION, content.multiSession);
							intent.putExtra(PlayerActivity.DRM_USERID, content.userId);
							intent.putExtra(PlayerActivity.DRM_CID, content.cid);
							intent.putExtra(PlayerActivity.DRM_OID, content.oid);
							intent.putExtra(PlayerActivity.DRM_CUSTOM_DATA, content.customData);
							intent.putExtra(PlayerActivity.DRM_TOKEN, content.token);
							intent.putExtra(PlayerActivity.DRM_COOKIE, content.cookie);
						}

						startActivity(intent);

					} else if (title.equals("Download")) {
						// TODO: Create downloadLicense task with content information. content.name is used as a name of downloadLicense folder.
						try {
							// TODO: If you don't want to create downloadcallback implementation, input null into callback parameter
							DownloadCallbackImpl downloadCallback = new DownloadCallbackImpl(getApplicationContext());
							downloadTask = new PallyconDownloadTask(MainActivity.this, content.uri, content.name, MainActivity.this, eventHandler, null, pallyconDownloadFinishCallback);
							downloadTask.setCookie(content.cookie);
						} catch (PallyconDownloadException e) {
							Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
							return false;
						}

						try {
							// TODO: Check that the content has already been downloaded.
							boolean result = downloadTask.isDownloadCompleted();
							if (result) {
								Uri localUri = downloadTask.getLocalUri(content.uri, content.name);
								intent.setData(localUri);
								intent.putExtra(PlayerActivity.CONTENTS_TITLE, content.name);
								//intent.putExtra(PlayerActivity.THUMB_URL, content.thumbUrl);
								if (content.drmSchemeUuid != null) {
									// TODO: Gets the local content path for local playback.
									intent.putExtra(PlayerActivity.DRM_SCHEME_UUID_EXTRA, content.drmSchemeUuid.toString());
									intent.putExtra(PlayerActivity.DRM_LICENSE_URL, content.drmLicenseUrl);
									intent.putExtra(PlayerActivity.DRM_MULTI_SESSION, content.multiSession);
									intent.putExtra(PlayerActivity.DRM_USERID, content.userId);
									intent.putExtra(PlayerActivity.DRM_CID, content.cid);
									intent.putExtra(PlayerActivity.DRM_OID, content.oid);
									intent.putExtra(PlayerActivity.DRM_CUSTOM_DATA, content.customData);
									intent.putExtra(PlayerActivity.DRM_TOKEN, content.token);
									intent.putExtra(PlayerActivity.DRM_COOKIE, content.cookie);
								}
								startActivity(intent);
							} else {
								MpdTrackSelector mpdTrackSelector = new MpdTrackSelector(MainActivity.this);
								mpdTrackSelector.setCookie(content.cookie);
								if( mpdTrackSelector.parseMpd(content.uri) ) {
									Log.d(TAG, "MPD parsed");
									Log.d(TAG, "parsed mpd info.");
									mpdTrackSelector.selectAll(false);
									mpdTrackSelector.logInfo();

//									Log.d(TAG, "select quality");
//									mpdTrackSelector.selectVideoRepresentation(0, 0, 0, true);
//									mpdTrackSelector.selectAudioRepresentation(0, 1, 0, true);

									Log.d(TAG, "download top bitrate only");
									mpdTrackSelector.selectBestQuality();

									mpdTrackSelector.logInfo();
									String newMpdFilename = mpdTrackSelector.writeModifiedMpd();
									Log.d(TAG, "modified MPD: " + newMpdFilename);

									downloadTask.setModifiedMpdPath(newMpdFilename);
								} else {
									Log.d(TAG, "MPD parsing fail");
								}

								AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
								builder.setTitle("DownLoad");
								builder.setMessage("No downloaded content. Do you want to start downloading?");
								builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialogInterface, int i) {
										// TODO: Start downloading.
										downloadTask.execute();
									}
								});
								builder.setNegativeButton("Cancel", null);
								builder.setNeutralButton("Size", new DialogInterface.OnClickListener(){
									@Override
									public void onClick(DialogInterface dialog, int i)
									{
										PallyConMpegDashInfo mpdSize = new PallyConMpegDashInfo(MainActivity.this, content.uri);
										try {
											mpdSize.extractPallyConMPDSizeInfo();
										} catch (PallyconDrmException e) {
											e.printStackTrace();
										}
										AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
										builder.setTitle("MPD Size" + "(" + content.name + ")");
										builder.setMessage(mpdSize.getVideoResolutionSize()
												+ "------------------ \n"
												+ mpdSize.getVideoBitrateSize());
										builder.setPositiveButton("OK", null);
										Dialog dialog2 = builder.create();
										dialog2.show();
									}
								});
								Dialog dialog = builder.create();
								dialog.show();
							}

						} catch (NetworkOnMainThreadException e) {
							e.printStackTrace();
							showSimpleDialog("Code Error", "you have got main thread network permission!");

						} catch (PallyconDownloadException e) {
							e.printStackTrace();
							showSimpleDialog("Download Error", e.getMessage());

						} catch (NetworkConnectedException e) {
							e.printStackTrace();
							showSimpleDialog("Network Error", e.getMessage());

						}
					}

					return true;
				}
			});
			updateList();

		} catch (IOException e) {
			Log.e(TAG, "updateList failed.." + e);
			e.printStackTrace();
		}

		//// Chromecast
		//CastContext.getSharedInstance(this);
		//// CC
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		switch (requestCode) {
			case MY_STORAGE_PERMISSION:
				for (int grant : grantResults) {
					if (grant != PackageManager.PERMISSION_GRANTED) {
						break;
					}
					Toast.makeText(this, "WRITE_EXTERNAL_STORAGE permission is allowed!!", Toast.LENGTH_LONG).show();
				}
				break;
			default:
		}

		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	private Site createSite() throws IOException {
		InputStream is = getAssets().open("site.json");
		InputStreamReader isr = new InputStreamReader(is, "UTF-8");
		JsonReader reader = new JsonReader(isr);

		Site site = new Site();

		reader.beginObject();
		while(reader.hasNext()) {
			String name = reader.nextName();
			switch(name) {
				case "siteId":
					site.siteId = reader.nextString();
					break;
				case "siteKey":
					site.siteKey = reader.nextString();
					break;
			}
		}

		return site;
	}

	private void updateList() throws IOException {
		// TODO : make streaming list with pallycon quick start guide.
		ContentGroup streamingGroup = new ContentGroup("Streaming");
		ContentGroup downloadGroup = new ContentGroup("Download");

		InputStream is = getAssets().open("media.exolist.json");
		InputStreamReader isr = new InputStreamReader(is, "UTF-8");
		JsonReader reader = new JsonReader(isr);

		reader.beginArray();
		while(reader.hasNext()) {
			reader.beginObject();
			Content content = new Content();
			String type = "";

			while(reader.hasNext()) {
				String name = reader.nextName();
				switch(name) {
					case "type":
						type = reader.nextString();
						break;
					case "uri":
						content.uri = Uri.parse(reader.nextString());
						break;
					case "name":
						content.name = reader.nextString();
						break;
					case "drmSchemeUuid":
						content.drmSchemeUuid = getDrmUuid(reader.nextString());
						break;
					case "drmLicenseUrl":
						content.drmLicenseUrl = reader.nextString();
						break;
					case "userId":
						content.userId = reader.nextString();
						break;
					case "cid":
						content.cid = reader.nextString();
						break;
					case "oid":
						content.oid = reader.nextString();
						break;
					case "token":
						content.token = reader.nextString();
						break;
					case "customData":
						content.customData = reader.nextString();
						break;
					case "thumbnail":
						content.thumbUrl = reader.nextString();
						break;
					case "multiSession":
						content.multiSession = reader.nextBoolean();
						break;
					case "cookie":
						content.cookie = reader.nextString();
						break;

					default:
						reader.skipValue();
				}
			}
			reader.endObject();

			if(type.equals("streaming")) {
				streamingGroup.arrContent.add(content);
			} else if(type.equals("download")) {
				downloadGroup.arrContent.add(content);
			}

		}
		reader.endArray();

		arrContentGroup.add(streamingGroup);
		arrContentGroup.add(downloadGroup);

		dataListView.setAdapter(new SampleAdapter(this, arrContentGroup));
	}

	private @Nullable
	UUID getDrmUuid(String drmScheme) {
		switch (Ascii.toLowerCase(drmScheme)) {
			case "widevine":
				return C.WIDEVINE_UUID;
			case "playready":
				return C.PLAYREADY_UUID;
			default:
				try {
					return UUID.fromString(drmScheme);
				} catch (RuntimeException e) {
					return null;
				}
		}
	}

	private class SampleAdapter extends BaseExpandableListAdapter {
		private final Context context;
		private final List<ContentGroup> sampleGroups;

		private SampleAdapter(Context context, List<ContentGroup> sampleGroups) {
			this.context = context;
			this.sampleGroups = sampleGroups;
		}

		@Override
		public int getGroupCount() {
			return sampleGroups.size();
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return getGroup(groupPosition).arrContent.size();
		}

		@Override
		public ContentGroup getGroup(int groupPosition) {
			return sampleGroups.get(groupPosition);
		}

		@Override
		public Content getChild(int groupPosition, int childPosition) {
			return sampleGroups.get(groupPosition).arrContent.get(childPosition);
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				view = LayoutInflater.from(context).inflate(android.R.layout.simple_expandable_list_item_1, parent, false);
			}

			((TextView) view).setText(getGroup(groupPosition).title);
			return view;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				view = LayoutInflater.from(context).inflate(R.layout.listcell_content, parent, false);
			}

			TextView contentName = (TextView) view.findViewById(R.id.contentName);
			contentName.setText(getChild(groupPosition, childPosition).name);

			TextView userId = (TextView) view.findViewById(R.id.userId);
			userId.setText("USERID : " + getChild(groupPosition, childPosition).userId);

			TextView cid = (TextView) view.findViewById(R.id.cid);
			cid.setText("CID : " + getChild(groupPosition, childPosition).cid);

			TextView oid = (TextView) view.findViewById(R.id.oid);
			oid.setText("OID : " + getChild(groupPosition, childPosition).oid);

			return view;
		}

		@Override
		public boolean isChildSelectable(int i, int i1) {
			return true;
		}
	}

	private void showSimpleDialog(String title, String message) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				showSimpleDialog2(title, message);
			}
		});
	}

	private void showSimpleDialog2(String title, String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(title);
		builder.setMessage(message);
		builder.setPositiveButton("OK", null);
		Dialog dialog = builder.create();
		dialog.show();
	}
}
