package com.pallycon.jetcompose

import android.os.Bundle
import androidx.navigation.NavType
import com.google.gson.Gson
import com.pallycon.widevine.model.ContentData

class AssetParamType : NavType<ContentData>(isNullableAllowed = false) {
    override fun get(bundle: Bundle, key: String): ContentData? {
        return bundle.getParcelable(key)
    }
    override fun parseValue(value: String): ContentData {
        return Gson().fromJson(value, ContentData::class.java)
    }
    override fun put(bundle: Bundle, key: String, value: ContentData) {
        bundle.putParcelable(key, value)
    }
}