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

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.BrowseFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.core.app.ActivityOptionsCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainFragment extends BrowseFragment {
	private static final String TAG = "MainFragment";

	private static final int BACKGROUND_UPDATE_DELAY = 300;
	private static final int GRID_ITEM_WIDTH = 200;
	private static final int GRID_ITEM_HEIGHT = 200;

	private final Handler mHandler = new Handler();
	private ArrayObjectAdapter mRowsAdapter;
	private Drawable mDefaultBackground;
	private DisplayMetrics mMetrics;
	private Timer mBackgroundTimer;
	private URI mBackgroundURI;
	private BackgroundManager mBackgroundManager;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate");
		super.onActivityCreated(savedInstanceState);

		prepareBackgroundManager();

		setupUIElements();

		loadRows();

		setupEventListeners();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (null != mBackgroundTimer) {
			Log.d(TAG, "onDestroy: " + mBackgroundTimer.toString());
			mBackgroundTimer.cancel();
		}
	}

	private void setupStreamingRows() {
		// TODO : set up streaming movie list
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
		mRowsAdapter.add(new ListRow(header, listRowAdapter));
	}

	private void setupDownloadRows() {
		// TODO : set up download movie list
		List<Movie> list = null;
		try {
			list = MovieList.setupDownloadMovies();
		} catch (IOException e) {
			e.printStackTrace();
		}

		CardPresenter cardPresenter = new CardPresenter();

		ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(cardPresenter);
		for (int j = 0; j < list.size(); j++) {
			listRowAdapter.add(list.get(j));
		}
		HeaderItem header = new HeaderItem(1, MovieList.MOVIE_CATEGORY[1]);
		mRowsAdapter.add(new ListRow(header, listRowAdapter));
	}

	private void loadRows() {
		mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());

		try {
			MovieList.updateList(getActivity());
		} catch (IOException e) {
			e.printStackTrace();
		}
		setupStreamingRows();
//		setupDownloadRows();

		setAdapter(mRowsAdapter);
	}

	private void prepareBackgroundManager() {

		mBackgroundManager = BackgroundManager.getInstance(getActivity());
		mBackgroundManager.attach(getActivity().getWindow());
		mDefaultBackground = getResources().getDrawable(R.drawable.default_background);
		mMetrics = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
	}

	private void setupUIElements() {
		// setBadgeDrawable(getActivity().getResources().getDrawable(
		// R.drawable.videos_by_google_banner));
		setTitle(getString(R.string.browse_title)); // Badge, when set, takes precedent
		// over title
		setHeadersState(HEADERS_ENABLED);
		setHeadersTransitionOnBackEnabled(true);

		// set fastLane (or headers) background color
		setBrandColor(getResources().getColor(R.color.fastlane_background));
		// set search icon color
		setSearchAffordanceColor(getResources().getColor(R.color.search_opaque));
	}

	private void setupEventListeners() {
		setOnSearchClickedListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				Toast.makeText(getActivity(), "Implement your own in-app search", Toast.LENGTH_LONG)
						.show();
			}
		});

		setOnItemViewClickedListener(new ItemViewClickedListener());
		setOnItemViewSelectedListener(new ItemViewSelectedListener());
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
		mBackgroundTimer.cancel();
	}

	private void startBackgroundTimer() {
		if (null != mBackgroundTimer) {
			mBackgroundTimer.cancel();
		}
		mBackgroundTimer = new Timer();
		mBackgroundTimer.schedule(new UpdateBackgroundTask(), BACKGROUND_UPDATE_DELAY);
	}

	private final class ItemViewClickedListener implements OnItemViewClickedListener {
		@Override
		public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
								  RowPresenter.ViewHolder rowViewHolder, Row row) {

			if (item instanceof Movie) {
				Movie movie = (Movie) item;
				Log.d(TAG, "Item: " + item.toString());
				Intent intent = new Intent(getActivity(), DetailsActivity.class);
				intent.putExtra(DetailsActivity.MOVIE, movie);

				Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
						getActivity(),
						((ImageCardView) itemViewHolder.view).getMainImageView(),
						DetailsActivity.SHARED_ELEMENT_NAME).toBundle();
				getActivity().startActivity(intent, bundle);
			} else if (item instanceof String) {
				if (((String) item).indexOf(getString(R.string.error_fragment)) >= 0) {
					Intent intent = new Intent(getActivity(), BrowseErrorActivity.class);
					startActivity(intent);
				} else {
					Toast.makeText(getActivity(), ((String) item), Toast.LENGTH_SHORT)
							.show();
				}
			}
		}
	}

	private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
		@Override
		public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
								   RowPresenter.ViewHolder rowViewHolder, Row row) {
			if (item instanceof Movie) {
				mBackgroundURI = ((Movie) item).getBackgroundImageURI();
				startBackgroundTimer();
			}

		}
	}

	private class UpdateBackgroundTask extends TimerTask {

		@Override
		public void run() {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					if (mBackgroundURI != null) {
						updateBackground(mBackgroundURI.toString());
					}
				}
			});

		}
	}

	private class GridItemPresenter extends Presenter {
		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent) {
			TextView view = new TextView(parent.getContext());
			view.setLayoutParams(new ViewGroup.LayoutParams(GRID_ITEM_WIDTH, GRID_ITEM_HEIGHT));
			view.setFocusable(true);
			view.setFocusableInTouchMode(true);
			view.setBackgroundColor(getResources().getColor(R.color.default_background));
			view.setTextColor(Color.WHITE);
			view.setGravity(Gravity.CENTER);
			return new ViewHolder(view);
		}

		@Override
		public void onBindViewHolder(ViewHolder viewHolder, Object item) {
			((TextView) viewHolder.view).setText((String) item);
		}

		@Override
		public void onUnbindViewHolder(ViewHolder viewHolder) {
		}
	}

}
