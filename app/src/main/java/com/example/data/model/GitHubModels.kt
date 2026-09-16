package com.example.data.model

import android.net.Uri
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubUser(
  @Json(name = "login") val login: String,
  @Json(name = "id") val id: Long,
  @Json(name = "avatar_url") val avatarUrl: String?,
  @Json(name = "name") val name: String?,
  @Json(name = "html_url") val htmlUrl: String,
  @Json(name = "public_repos") val publicRepos: Int = 0,
  @Json(name = "total_private_repos") val totalPrivateRepos: Int = 0,
  @Json(name = "bio") val bio: String? = null,
  @Json(name = "email") val email: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubRepo(
  @Json(name = "id") val id: Long,
  @Json(name = "name") val name: String,
  @Json(name = "full_name") val fullName: String,
  @Json(name = "private") val isPrivate: Boolean = false,
  @Json(name = "html_url") val htmlUrl: String,
  @Json(name = "description") val description: String? = null,
  @Json(name = "default_branch") val defaultBranch: String = "main",
  @Json(name = "stargazers_count") val starsCount: Int = 0,
  @Json(name = "forks_count") val forksCount: Int = 0,
  @Json(name = "updated_at") val updatedAt: String? = null,
  @Json(name = "owner") val owner: GitHubUser? = null
)

@JsonClass(generateAdapter = true)
data class GitHubBranch(
  @Json(name = "name") val name: String,
  @Json(name = "commit") val commit: BranchCommit
)

@JsonClass(generateAdapter = true)
data class BranchCommit(
  @Json(name = "sha") val sha: String,
  @Json(name = "url") val url: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubRef(
  @Json(name = "ref") val ref: String,
  @Json(name = "object") val targetObject: GitObject
)

@JsonClass(generateAdapter = true)
data class GitObject(
  @Json(name = "sha") val sha: String,
  @Json(name = "type") val type: String,
  @Json(name = "url") val url: String
)

@JsonClass(generateAdapter = true)
data class GitCommitDetail(
  @Json(name = "sha") val sha: String,
  @Json(name = "tree") val tree: GitTreeInfo
)

@JsonClass(generateAdapter = true)
data class GitTreeInfo(
  @Json(name = "sha") val sha: String,
  @Json(name = "url") val url: String
)

@JsonClass(generateAdapter = true)
data class CreateBlobRequest(
  @Json(name = "content") val content: String,
  @Json(name = "encoding") val encoding: String = "base64"
)

@JsonClass(generateAdapter = true)
data class CreateBlobResponse(
  @Json(name = "sha") val sha: String,
  @Json(name = "url") val url: String
)

@JsonClass(generateAdapter = true)
data class TreeItem(
  @Json(name = "path") val path: String,
  @Json(name = "mode") val mode: String = "100644",
  @Json(name = "type") val type: String = "blob",
  @Json(name = "sha") val sha: String
)

@JsonClass(generateAdapter = true)
data class CreateTreeRequest(
  @Json(name = "base_tree") val baseTree: String? = null,
  @Json(name = "tree") val tree: List<TreeItem>
)

@JsonClass(generateAdapter = true)
data class CreateTreeResponse(
  @Json(name = "sha") val sha: String,
  @Json(name = "url") val url: String,
  @Json(name = "truncated") val truncated: Boolean = false
)

@JsonClass(generateAdapter = true)
data class CreateCommitRequest(
  @Json(name = "message") val message: String,
  @Json(name = "tree") val tree: String,
  @Json(name = "parents") val parents: List<String>
)

@JsonClass(generateAdapter = true)
data class CreateCommitResponse(
  @Json(name = "sha") val sha: String,
  @Json(name = "html_url") val htmlUrl: String? = null,
  @Json(name = "url") val url: String
)

@JsonClass(generateAdapter = true)
data class UpdateRefRequest(
  @Json(name = "sha") val sha: String,
  @Json(name = "force") val force: Boolean = false
)

@JsonClass(generateAdapter = true)
data class GitHubContentItem(
  @Json(name = "name") val name: String,
  @Json(name = "path") val path: String,
  @Json(name = "sha") val sha: String,
  @Json(name = "size") val size: Long = 0,
  @Json(name = "type") val type: String, // "file" or "dir"
  @Json(name = "html_url") val htmlUrl: String? = null,
  @Json(name = "download_url") val downloadUrl: String? = null,
  @Json(name = "content") val content: String? = null,
  @Json(name = "encoding") val encoding: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateOrUpdateFileRequest(
  @Json(name = "message") val message: String,
  @Json(name = "content") val content: String, // Base64 encoded
  @Json(name = "sha") val sha: String? = null, // null if creating new
  @Json(name = "branch") val branch: String? = null
)

@JsonClass(generateAdapter = true)
data class DeleteFileRequest(
  @Json(name = "message") val message: String,
  @Json(name = "sha") val sha: String,
  @Json(name = "branch") val branch: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateRepoRequest(
  @Json(name = "name") val name: String,
  @Json(name = "description") val description: String? = null,
  @Json(name = "private") val isPrivate: Boolean = false,
  @Json(name = "auto_init") val autoInit: Boolean = true
)

@JsonClass(generateAdapter = true)
data class DeviceCodeResponse(
  @Json(name = "device_code") val deviceCode: String,
  @Json(name = "user_code") val userCode: String,
  @Json(name = "verification_uri") val verificationUri: String,
  @Json(name = "expires_in") val expiresIn: Int,
  @Json(name = "interval") val interval: Int
)

@JsonClass(generateAdapter = true)
data class OAuthTokenResponse(
  @Json(name = "access_token") val accessToken: String? = null,
  @Json(name = "token_type") val tokenType: String? = null,
  @Json(name = "scope") val scope: String? = null,
  @Json(name = "error") val error: String? = null,
  @Json(name = "error_description") val errorDescription: String? = null
)

// UI & Scanning helper models
data class ScannedFileItem(
  val uri: Uri,
  val relativePath: String,
  val displayName: String,
  val sizeBytes: Long,
  val mimeType: String,
  val isDirectory: Boolean = false
)

sealed interface UploadStatus {
  object Idle : UploadStatus
  data class Scanning(val progressMessage: String) : UploadStatus
  data class Ready(
    val folderName: String,
    val totalFiles: Int,
    val totalBytes: Long,
    val files: List<ScannedFileItem>
  ) : UploadStatus
  data class Uploading(
    val currentFileIndex: Int,
    val totalFiles: Int,
    val currentFileName: String,
    val progress: Float,
    val stepDescription: String
  ) : UploadStatus
  data class Success(
    val commitSha: String,
    val commitUrl: String,
    val filesUploaded: Int,
    val totalBytes: Long,
    val repoFullName: String,
    val branch: String
  ) : UploadStatus
  data class Failure(
    val errorMessage: String,
    val canRetry: Boolean = true
  ) : UploadStatus
}
