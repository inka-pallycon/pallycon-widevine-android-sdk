package com.pallycon.androidtvsample;

import android.content.Context;
import android.util.JsonReader;

import androidx.media3.common.C;
import androidx.media3.common.ParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class MovieList {
	public static final String MOVIE_CATEGORY[] = {
			"Streaming",
			"Download",
	};

	public static List<MovieGroup> arrMovieGroup = new ArrayList<>();

	public static List<Movie> setupStreamingMovies() throws IOException {
		return arrMovieGroup.get(0).arrMovie;
	}

	public static List<Movie> setupDownloadMovies() throws IOException {
		return arrMovieGroup.get(1).arrMovie;
	}

	private static UUID getDrmUuid(String typeString) throws ParserException {
		switch (typeString.toLowerCase()) {
			case "widevine":
				return C.WIDEVINE_UUID;
			case "playready":
				return C.PLAYREADY_UUID;
			default:
				try {
					return UUID.fromString(typeString);
				} catch (RuntimeException e) {
					throw ParserException.createForUnsupportedContainerFeature("Unsupported drm type: " + typeString);
				}
		}
	}

	public static void updateList(Context context) throws IOException {
		// TODO : make streaming list with pallycon quick start guide.
		arrMovieGroup.clear();

		MovieGroup streamingGroup = new MovieGroup("Streaming");
		MovieGroup downloadGroup = new MovieGroup("Download");

		InputStream is = context.getAssets().open("media.exolist.json");
		InputStreamReader isr = new InputStreamReader(is, "UTF-8");
		JsonReader reader = new JsonReader(isr);

		reader.beginArray();
		while(reader.hasNext()) {
			reader.beginObject();
			Movie movie = new Movie();
			String type = "";

			while(reader.hasNext()) {
				String name = reader.nextName();
				switch(name) {
					case "type":
						type = reader.nextString();
						break;
					case "uri":
						movie.uri = reader.nextString();
						break;
					case "name":
						movie.name = reader.nextString();
						break;
					case "drmSchemeUuid":
						movie.drmSchemeUuid = getDrmUuid(reader.nextString());
						break;
					case "drmLicenseUrl":
						movie.drmLicenseUrl = reader.nextString();
						break;
					case "cid":
						movie.cid = reader.nextString();
						break;
					case "token":
						movie.token = reader.nextString();
						break;
					case "customData":
						movie.customData = reader.nextString();
						break;
					case "category":
						movie.category = reader.nextString();
						break;
					case "bgImageUrl":
						movie.bgImageUrl = reader.nextString();
						break;
					case "cardImageUrl":
						movie.cardImageUrl = reader.nextString();
						break;
					case "multiSession":
						movie.multiSession = reader.nextBoolean();
						break;

					default:
						reader.skipValue();
				}
			}
			reader.endObject();

			if(type.equals("streaming") == true) {
				streamingGroup.arrMovie.add(movie);
			} else if(type.equals("download") == true) {
				downloadGroup.arrMovie.add(movie);
			}
		}
		reader.endArray();

		arrMovieGroup.add(streamingGroup);
		arrMovieGroup.add(downloadGroup);

		return;
	}
}
