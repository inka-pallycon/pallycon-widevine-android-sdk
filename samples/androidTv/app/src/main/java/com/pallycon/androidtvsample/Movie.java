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

import android.util.Log;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

/*
 * Movie class represents video entity with title, description, image thumbs and video url.
 *
 */
public class Movie implements Serializable, Cloneable {
	static final long serialVersionUID = 727566175075960653L;
	private static long count = 0;

	public String uri = "";
	public String name = "";
	public UUID drmSchemeUuid;
	public String drmLicenseUrl = "";
	public String cid = "";
	public String token = "";
	public String customData = "";

	public String category = "";
	public String bgImageUrl = "";
	public String cardImageUrl = "";
	public boolean multiSession;

	public Movie() { }

	public static long getCount() {
		return count;
	}

	public static void incCount() {
		count++;
	}

	public String getBackgroundImageUrl() {
		return bgImageUrl;
	}

	public String getCardImageUrl() {
		return cardImageUrl;
	}

	public URI getBackgroundImageURI() {
		try {
			Log.d("BACK MOVIE: ", bgImageUrl);
			return new URI(getBackgroundImageUrl());
		} catch (URISyntaxException e) {
			Log.d("URI exception: ", bgImageUrl);
			return null;
		}
	}
//
	public URI getCardImageURI() {
		try {
			return new URI(getCardImageUrl());
		} catch (URISyntaxException e) {
			return null;
		}
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
}
