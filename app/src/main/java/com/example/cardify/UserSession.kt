package com.example.cardify

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

object UserSession {
    private const val PREFS_NAME = "cardify_session"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_TAG = "user_tag"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ensureUser()
        }
    }

    fun initialize(onReady: (Boolean) -> Unit = {}) {
        val initialized = prefs != null
        if (initialized) {
            ensureUser()
        }
        onReady(initialized)
    }

    private fun ensureUser() {
        val preferences = prefs ?: return
        if (!preferences.contains(KEY_USER_ID)) {
            val id = UUID.randomUUID().toString()
            val suffix = id.takeLast(4)
            val defaultName = "게스트$suffix"
            preferences.edit()
                .putString(KEY_USER_ID, id)
                .putString(KEY_USER_NAME, defaultName)
                .putString(KEY_USER_TAG, DEFAULT_TAG)
                .apply()
        } else if (!preferences.contains(KEY_USER_NAME)) {
            val id = preferences.getString(KEY_USER_ID, "").orEmpty()
            val suffix = id.takeLast(4).ifEmpty { "0000" }
            preferences.edit()
                .putString(KEY_USER_NAME, "게스트$suffix")
                .putString(KEY_USER_TAG, preferences.getString(KEY_USER_TAG, DEFAULT_TAG) ?: DEFAULT_TAG)
                .apply()
        } else if (!preferences.contains(KEY_USER_TAG)) {
            preferences.edit().putString(KEY_USER_TAG, DEFAULT_TAG).apply()
        }
    }

    val userId: String
        get() = prefs?.getString(KEY_USER_ID, "").orEmpty()

    var userName: String
        get() = prefs?.getString(KEY_USER_NAME, "").orEmpty()
        set(value) {
            prefs?.edit()?.putString(KEY_USER_NAME, value)?.apply()
        }

    var userTag: String
        get() = prefs?.getString(KEY_USER_TAG, DEFAULT_TAG).orEmpty()
        set(value) {
            prefs?.edit()?.putString(KEY_USER_TAG, value)?.apply()
        }

    private const val DEFAULT_TAG = "운동러"
}
