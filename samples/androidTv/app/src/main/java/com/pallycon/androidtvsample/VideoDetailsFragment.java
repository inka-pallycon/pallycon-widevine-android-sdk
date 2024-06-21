/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.pallycon.androidtvsample;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.NetworkOnMainThreadException;
import android.os.StrictMode;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.DetailsFragment;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.DetailsOverviewRow;
import androidx.leanback.widget.DetailsOverviewRowPresenter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnActionClickedListener;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.core.app.ActivityOptionsCompat;
import android.util.DisplayMetrics;
import android.util.JsonReader;
import android.util.Log;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.pallycon.widevine.exception.PallyConException;
import com.pallycon.widevine.exception.PallyConLicenseServerException;
import com.pallycon.widevine.model.ContentData;
import com.pallycon.widevine.model.DownloadState;
import com.pallycon.widevine.model.PallyConDrmConfigration;
import com.pallycon.widevine.model.PallyConEventListener;
import com.pallycon.widevine.sdk.PallyConWvSDK;
import com.pallycon.widevine.track.PallyConDownloaderTracks;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/*
 * LeanbackDetailsFragment extends DetailsFragment, a Wrapper fragment for leanback details screens.
 * It shows a detailed view of video and its meta plus related videos.
 */
public class VideoDetailsFragment extends DetailsFragment {
	private static final String TAG = "VideoDetailsFragment";

	private static final int ACTION_PLAY = 1;
	private static final int ACTION_REMOVE_LICENSE = 2;
	private static final int ACTION_REMOVE_CONTENT = 3;

	private static final int DETAIL_THUMB_WIDTH = 274;
	private static final int DETAIL_THUMB_HEIGHT = 274;

	private Movie mSelectedMovie;
	private Site site;

	private ArrayObjectAdapter mAdapter;
	private ClassPresenterSelector mPresenterSelector;

	private BackgroundManager mBackgroundManager;
	private Drawable mDefaultBackground;
	private DisplayMetrics mMetrics;

	// TODO : Prepare variable available for PallyconWVMSDK
	private final int MY_STORAGE_PERMISSION = 1;
	private Handler eventHandler = new Handler();
	private PallyConWvSDKManager wvSdkManager = null;

	protected class Site {
		public String siteId;
		public String siteKey;
	}

	private Site createSite() throws IOException {
		InputStream is = getActivity().getAssets().open("site.json");
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

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate DetailsFragment");
		super.onCreate(savedInstanceState);

		// TODO : get PallyconWVMSDK object and initialize for download and playback
		StrictMode.ThreadPolicy pol = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
		StrictMode.setThreadPolicy(pol);

		try {
			site = createSite();
		} catch (IOException e) {
			e.printStackTrace();
		}

		prepareBackgroundManager();

		mSelectedMovie = (Movie) getActivity().getIntent()
				.getSerializableExtra(DetailsActivity.MOVIE);
		if (mSelectedMovie != null) {
			setupAdapter();
			setupDetailsOverviewRow();
			setupDetailsOverviewRowPresenter();
			setupMovieListRow();
			setupMovieListRowPresenter();
			updateBackground(mSelectedMovie.getBackgroundImageUrl());
			setOnItemViewClickedListener(new ItemViewClickedListener());

			// TODO : 1.set content information
			PallyConDrmConfigration config = new PallyConDrmConfigration(
					site.siteId,
					mSelectedMovie.token
			);
			ContentData content = new ContentData(
					mSelectedMovie.uri,
					config
			);

			// TODO: 2. initialize PallyconWVM SDK
			wvSdkManager = PallyConWvSDKManager.getInstance(getActivity());
			wvSdkManager.createSDK(content);
		} else {
			Intent intent = new Intent(getActivity(), MainActivity.class);
			startActivity(intent);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onStop() {
		super.onStop();
		wvSdkManager.release();
	}

	private void prepareBackgroundManager() {
		mBackgroundManager = BackgroundManager.getInstance(getActivity());
		mBackgroundManager.attach(getActivity().getWindow());
		mDefaultBackground = getResources().getDrawable(R.drawable.default_background);
		mMetrics = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
	}

	protected void updateBackground(String uri) {
		Glide.with(getActivity())
				.load(uri)
				.centerCrop()
				.error(mDefaultBackground)
				.into(new CustomTarget<Drawable>(mMetrics.widthPixels, mMetrics.heightPixels) {
					@Override
					public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
						mBackgroundManager.setDrawable(resource);
					}

					@Override
					public void onLoadCleared(@Nullable Drawable placeholder) {

					}
				});
	}

	private void setupAdapter() {
		mPresenterSelector = new ClassPresenterSelector();
		mAdapter = new ArrayObjectAdapter(mPresenterSelector);
		setAdapter(mAdapter);
	}

	private void setupDetailsOverviewRow() {
		Log.d(TAG, "doInBackground: " + mSelectedMovie.toString());
		final DetailsOverviewRow row = new DetailsOverviewRow(mSelectedMovie);
		row.setImageDrawable(getResources().getDrawable(R.drawable.default_background));
		int width = Utils.convertDpToPixel(getActivity()
				.getApplicationContext(), DETAIL_THUMB_WIDTH);
		int height = Utils.convertDpToPixel(getActivity()
				.getApplicationContext(), DETAIL_THUMB_HEIGHT);
		Glide.with(getActivity())
				.load(mSelectedMovie.getCardImageUrl())
				.centerCrop()
				.error(R.drawable.default_background)
				.into(new CustomTarget<Drawable>(width, height) {
					@Override
					public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
						Log.d(TAG, "details overview card image url ready: " + resource);
						row.setImageDrawable(resource);
						mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size());
					}

					@Override
					public void onLoadCleared(@Nullable Drawable placeholder) {

					}
				});

		row.addAction(new Action(ACTION_PLAY, getResources().getString(R.string.action_play_1)));
		row.addAction(new Action(ACTION_REMOVE_LICENSE, getResources().getString(R.string.action_remove_license_1)));
		row.addAction(new Action(ACTION_REMOVE_CONTENT, getResources().getString(R.string.action_remove_content_1)));

		mAdapter.add(row);
	}

	private void setupDetailsOverviewRowPresenter() {
		// Set detail background and style.
		DetailsOverviewRowPresenter detailsPresenter =
				new DetailsOverviewRowPresenter(new DetailsDescriptionPresenter());
		detailsPresenter.setBackgroundColor(getResources().getColor(R.color.selected_background));
		detailsPresenter.setStyleLarge(true);

		// Hook up transition element.
		detailsPresenter.setSharedElementEnterTransition(getActivity(),
				DetailsActivity.SHARED_ELEMENT_NAME);

		detailsPresenter.setOnActionClickedListener(new OnActionClickedListener() {
			@Override
			public void onActionClicked(Action action) {
				if (action.getId() == ACTION_PLAY) {
					Intent intent = new Intent(getActivity(), PlaybackOverlayActivity.class);
					intent.putExtra(DetailsActivity.MOVIE, mSelectedMovie);

					// TODO: Create downloadLicense task with content information. content.name is used as a name of downloadLicense folder.
					try {
						if (mSelectedMovie.category.equals("Streaming")) {
							MovieList.setupStreamingMovies();
							startActivity(intent);

						} else if (mSelectedMovie.category.equals("Download")) {
							MovieList.setupDownloadMovies();

							try {
								// TODO: Check that the content has already been downloaded.
								DownloadState state = wvSdkManager.getDownloadState();
								if (state == DownloadState.COMPLETED) {
									Movie intentMovie = (Movie)mSelectedMovie.clone();
									intent.putExtra(DetailsActivity.MOVIE, intentMovie);
									startActivity(intent);
								} else {
									AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
									builder.setTitle("DownLoad");
									builder.setMessage("No downloaded content. Do you want to start downloading?");
									builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialogInterface, int i) {
											// TODO: Start downloading.
											wvSdkManager.defaultDownload();
										}
									});
									builder.setNegativeButton("Cancel", null);
									Dialog dialog = builder.create();
									dialog.show();
								}
							} catch (NetworkOnMainThreadException e) {
								e.printStackTrace();
								Utils.showSimpleDialog(getActivity(), "Code Error", "you have got main thread network permission!");
							} catch (CloneNotSupportedException e) {
								e.printStackTrace();
								Utils.showSimpleDialog(getActivity(), "Network Error", e.getMessage());
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}

				} else if (action.getId() == ACTION_REMOVE_LICENSE ){
					// TODO: Delete license.
					wvSdkManager.removeLicense();
					Toast.makeText(getActivity(), "License has been removed.", Toast.LENGTH_LONG).show();

				} else if (action.getId() == ACTION_REMOVE_CONTENT) {
					// TODO: Remove content file (mpd, video, audio).
					wvSdkManager.remove();
					Toast.makeText(getActivity(), "Content has been removed.", Toast.LENGTH_LONG).show();
				}
			}
		});
		mPresenterSelector.addClassPresenter(DetailsOverviewRow.class, detailsPresenter);
	}

	private void setupMovieListRow() {
		String subcategories[] = {getString(R.string.related_movies)};
		List<Movie> list = null;
		try {
			list = MovieList.setupStreamingMovies();
		} catch (IOException e) {
			e.printStackTrace();
		}
		CardPresenter cardPresenter = new CardPresenter();

		ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(cardPresenter);
		for (int j = 0; j < list.size(); j++) {
			listRowAdapter.add(list.get(j));
		}
		HeaderItem header = new HeaderItem(0, MovieList.MOVIE_CATEGORY[0]);
		mAdapter.add(new ListRow(header, listRowAdapter));
	}

	private void setupMovieListRowPresenter() {
		mPresenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());
	}

	private final class ItemViewClickedListener implements OnItemViewClickedListener {
		@Override
		public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
								  RowPresenter.ViewHolder rowViewHolder, Row row) {

			if (item instanceof Movie) {
				Movie movie = (Movie) item;
				Log.d(TAG, "Item: " + item.toString());
				Intent intent = new Intent(getActivity(), DetailsActivity.class);
				intent.putExtra(getResources().getString(R.string.movie), mSelectedMovie);
				intent.putExtra(getResources().getString(R.string.should_start), true);
				startActivity(intent);


				Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
						getActivity(),
						((ImageCardView) itemViewHolder.view).getMainImageView(),
						DetailsActivity.SHARED_ELEMENT_NAME).toBundle();
				getActivity().startActivity(intent, bundle);
			}
		}
	}
}
