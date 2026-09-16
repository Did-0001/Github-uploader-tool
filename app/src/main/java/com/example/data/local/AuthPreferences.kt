package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class AuthPreferences(context: Context) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences("git_uploader_auth_prefs", Context.MODE_PRIVATE)

  companion object {
    private const val KEY_TOKEN = "github_token"
    private const val KEY_AUTH_TYPE = "auth_type" // "pat" or "oauth"
    private const val KEY_USERNAME = "github_username"
    private const val KEY_USER_NAME = "github_user_display_name"
    private const val KEY_AVATAR_URL = "github_avatar_url"
    private const val KEY_LAST_REPO = "last_selected_repo"
    private const val KEY_LAST_BRANCH = "last_selected_branch"
    private const val KEY_FILTER_GIT = "filter_git_folder"
    private const val KEY_FILTER_HIDDEN = "filter_hidden_files"
  }

  fun saveToken(token: String, authType: String = "pat") {
    prefs.edit()
      .putString(KEY_TOKEN, token.trim())
      .putString(KEY_AUTH_TYPE, authType)
      .apply()
  }

  fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

  fun getAuthType(): String = prefs.getString(KEY_AUTH_TYPE, "pat") ?: "pat"

  fun saveUserProfile(username: String, name: String?, avatarUrl: String?) {
    prefs.edit()
      .putString(KEY_USERNAME, username)
      .putString(KEY_USER_NAME, name)
      .putString(KEY_AVATAR_URL, avatarUrl)
      .apply()
  }

  fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)
  fun getUserDisplayName(): String? = prefs.getString(KEY_USER_NAME, null)
  fun getAvatarUrl(): String? = prefs.getString(KEY_AVATAR_URL, null)

  fun saveLastSelectedRepo(repoFullName: String, branch: String) {
    prefs.edit()
      .putString(KEY_LAST_REPO, repoFullName)
      .putString(KEY_LAST_BRANCH, branch)
      .apply()
  }

  fun getLastSelectedRepo(): String? = prefs.getString(KEY_LAST_REPO, null)
  fun getLastSelectedBranch(): String? = prefs.getString(KEY_LAST_BRANCH, null)

  var filterGitFolder: Boolean
    get() = prefs.getBoolean(KEY_FILTER_GIT, true)
    set(value) = prefs.edit().putBoolean(KEY_FILTER_GIT, value).apply()

  var filterHiddenFiles: Boolean
    get() = prefs.getBoolean(KEY_FILTER_HIDDEN, true)
    set(value) = prefs.edit().putBoolean(KEY_FILTER_HIDDEN, value).apply()

  fun clear() {
    prefs.edit().clear().apply()
  }

  val isLoggedIn: Boolean
    get() = !getToken().isNullOrBlank()
}
