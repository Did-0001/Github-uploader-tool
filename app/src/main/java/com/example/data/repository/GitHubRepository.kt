package com.example.data.repository

import android.content.Context
import android.util.Base64
import com.example.data.local.AppDatabase
import com.example.data.local.AuthPreferences
import com.example.data.local.UploadHistoryEntity
import com.example.data.model.CreateBlobRequest
import com.example.data.model.CreateCommitRequest
import com.example.data.model.CreateOrUpdateFileRequest
import com.example.data.model.CreateRepoRequest
import com.example.data.model.CreateTreeRequest
import com.example.data.model.DeleteFileRequest
import com.example.data.model.DeviceCodeResponse
import com.example.data.model.GitHubBranch
import com.example.data.model.GitHubContentItem
import com.example.data.model.GitHubRepo
import com.example.data.model.GitHubUser
import com.example.data.model.OAuthTokenResponse
import com.example.data.model.ScannedFileItem
import com.example.data.model.TreeItem
import com.example.data.model.UpdateRefRequest
import com.example.data.remote.ApiClient
import com.example.data.remote.GitHubApiService
import com.example.data.remote.GitHubOAuthService
import com.example.util.FolderScanner
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

class GitHubRepository(
  private val context: Context,
  private val authPreferences: AuthPreferences,
  private val database: AppDatabase = AppDatabase.getDatabase(context)
) {
  private val apiService: GitHubApiService by lazy {
    ApiClient.createGitHubService(authPreferences)
  }

  private val oauthService: GitHubOAuthService by lazy {
    ApiClient.createOAuthService()
  }

  companion object {
    // Official GitHub client ID used by CLI and developer tooling for device authorization
    const val DEFAULT_CLIENT_ID = "178c6fc778cc68e3d964"
  }

  val uploadHistoryFlow: Flow<List<UploadHistoryEntity>> =
    database.uploadHistoryDao().getAllHistory()

  suspend fun verifyToken(token: String): Result<GitHubUser> = withContext(Dispatchers.IO) {
    try {
      // Temporarily set token to test
      val originalToken = authPreferences.getToken()
      val originalType = authPreferences.getAuthType()
      authPreferences.saveToken(token.trim(), "oauth")

      val user = try {
        apiService.getAuthenticatedUser()
      } catch (e: Exception) {
        if (originalToken != null) {
          authPreferences.saveToken(originalToken, originalType)
        } else {
          authPreferences.clear()
        }
        throw e
      }

      authPreferences.saveUserProfile(user.login, user.name, user.avatarUrl)
      Result.success(user)
    } catch (e: Exception) {
      Result.failure(Exception(parseErrorMessage(e)))
    }
  }

  suspend fun requestDeviceCode(clientId: String = DEFAULT_CLIENT_ID): Result<DeviceCodeResponse> = withContext(Dispatchers.IO) {
    try {
      val effectiveClientId = if (clientId.isBlank()) DEFAULT_CLIENT_ID else clientId.trim()
      val response = oauthService.requestDeviceCode(effectiveClientId)
      Result.success(response)
    } catch (e: Exception) {
      Result.failure(Exception(parseErrorMessage(e)))
    }
  }

  suspend fun pollDeviceToken(clientId: String = DEFAULT_CLIENT_ID, deviceCode: String): Result<OAuthTokenResponse> = withContext(Dispatchers.IO) {
    try {
      val effectiveClientId = if (clientId.isBlank()) DEFAULT_CLIENT_ID else clientId.trim()
      val response = oauthService.pollDeviceToken(effectiveClientId, deviceCode)
      Result.success(response)
    } catch (e: Exception) {
      Result.failure(Exception(parseErrorMessage(e)))
    }
  }

  suspend fun getUserRepos(): Result<List<GitHubRepo>> = withContext(Dispatchers.IO) {
    try {
      val repos = apiService.getUserRepos()
      Result.success(repos)
    } catch (e: Exception) {
      Result.failure(Exception(parseErrorMessage(e)))
    }
  }

  suspend fun createRepository(
    name: String,
    description: String?,
    isPrivate: Boolean
  ): Result<GitHubRepo> = withContext(Dispatchers.IO) {
    try {
      val request = CreateRepoRequest(
        name = name,
        description = description,
        isPrivate = isPrivate,
        autoInit = true
      )
      val created = apiService.createRepo(request)
      Result.success(created)
    } catch (e: Exception) {
      Result.failure(Exception(parseErrorMessage(e)))
    }
  }

  suspend fun getBranches(owner: String, repo: String): Result<List<GitHubBranch>> = withContext(Dispatchers.IO) {
    try {
      val branches = apiService.getBranches(owner, repo)
      Result.success(branches)
    } catch (e: Exception) {
      Result.failure(Exception(parseErrorMessage(e)))
    }
  }

  /**
   * Upload whole folder to GitHub using Git Database API (Blobs, Trees, Commits, Refs)
   * This handles auto-creating all nested directories without intermediate commit pollution.
   */
  suspend fun uploadFolder(
    owner: String,
    repo: String,
    branch: String,
    targetDirectory: String,
    folderName: String,
    files: List<ScannedFileItem>,
    commitMessage: String,
    onProgress: (current: Int, total: Int, fileName: String, percent: Float, step: String) -> Unit
  ): Result<UploadHistoryEntity> = withContext(Dispatchers.IO) {
    try {
      if (files.isEmpty()) {
        return@withContext Result.failure(Exception("No files to upload."))
      }

      onProgress(0, files.size, "", 0.05f, "Connecting to repository...")

      // Step 1: Ensure branch reference exists
      var refResponse = apiService.getBranchRef(owner, repo, branch)
      if (!refResponse.isSuccessful || refResponse.body() == null) {
        // If repo is completely empty, initialize it with a README.md
        onProgress(0, files.size, "README.md", 0.08f, "Initializing repository branch...")
        val initContent = Base64.encodeToString(
          "# $repo\n\nUploaded via Git Uploader for Android.\n".toByteArray(),
          Base64.NO_WRAP
        )
        apiService.createOrUpdateFile(
          owner = owner,
          repo = repo,
          path = "README.md",
          body = CreateOrUpdateFileRequest(
            message = "Initial commit via Git Uploader",
            content = initContent,
            branch = branch
          )
        )
        delay(1000)
        refResponse = apiService.getBranchRef(owner, repo, branch)
        if (!refResponse.isSuccessful || refResponse.body() == null) {
          throw IllegalStateException("Failed to retrieve or create branch '$branch' reference.")
        }
      }

      val headCommitSha = refResponse.body()!!.targetObject.sha
      onProgress(0, files.size, "", 0.12f, "Fetching base Git tree...")

      // Step 2: Get base tree SHA from head commit
      val commitDetail = apiService.getCommitDetail(owner, repo, headCommitSha)
      val baseTreeSha = commitDetail.tree.sha

      // Step 3: Upload blobs with concurrency & automatic retries
      val treeItems = mutableListOf<TreeItem>()
      val semaphore = Semaphore(3) // Concurrency limit to prevent hitting secondary rate limits
      var uploadedCount = 0
      var totalBytesUploaded = 0L

      val sanitizedPrefix = targetDirectory.trim().trim('/')
      val prefixPath = if (sanitizedPrefix.isBlank()) "" else "$sanitizedPrefix/"

      for ((index, file) in files.withIndex()) {
        val remoteFilePath = "$prefixPath${file.relativePath}".replace("//", "/")
        onProgress(
          index + 1,
          files.size,
          file.displayName,
          0.15f + (0.70f * (index.toFloat() / files.size.toFloat())),
          "Uploading blob (${index + 1}/${files.size}): ${file.displayName}"
        )

        // Read base64
        val base64Content = FolderScanner.readFileAsBase64(context, file.uri)

        // Upload blob with retry
        var blobSha: String? = null
        var attempts = 0
        var lastError: Exception? = null

        semaphore.withPermit {
          while (blobSha == null && attempts < 3) {
            attempts++
            try {
              val blobResponse = apiService.createBlob(
                owner = owner,
                repo = repo,
                body = CreateBlobRequest(content = base64Content, encoding = "base64")
              )
              blobSha = blobResponse.sha
            } catch (e: Exception) {
              lastError = e
              if (attempts < 3) delay(800L * attempts)
            }
          }
        }

        if (blobSha == null) {
          throw IOException("Failed to upload ${file.displayName} after 3 attempts: ${lastError?.message}")
        }

        treeItems.add(
          TreeItem(
            path = remoteFilePath,
            mode = "100644",
            type = "blob",
            sha = blobSha!!
          )
        )
        uploadedCount++
        totalBytesUploaded += file.sizeBytes
      }

      // Step 4: Create Tree
      onProgress(files.size, files.size, "", 0.88f, "Building Git directory tree...")
      val treeResponse = apiService.createTree(
        owner = owner,
        repo = repo,
        body = CreateTreeRequest(baseTree = baseTreeSha, tree = treeItems)
      )

      // Step 5: Create Commit
      val actualCommitMsg = if (commitMessage.isBlank()) {
        "Upload $folderName (${files.size} files) via Git Uploader"
      } else {
        commitMessage.trim()
      }

      onProgress(files.size, files.size, "", 0.94f, "Creating Git commit...")
      val commitResponse = apiService.createCommit(
        owner = owner,
        repo = repo,
        body = CreateCommitRequest(
          message = actualCommitMsg,
          tree = treeResponse.sha,
          parents = listOf(headCommitSha)
        )
      )

      // Step 6: Update branch ref to point to new commit
      onProgress(files.size, files.size, "", 0.98f, "Updating branch $branch reference...")
      apiService.updateRef(
        owner = owner,
        repo = repo,
        branch = branch,
        body = UpdateRefRequest(sha = commitResponse.sha, force = false)
      )

      // Step 7: Record upload in local history
      val commitUrl = commitResponse.htmlUrl ?: "https://github.com/$owner/$repo/commit/${commitResponse.sha}"
      val historyEntity = UploadHistoryEntity(
        repoName = "$owner/$repo",
        branch = branch,
        folderName = folderName,
        commitSha = commitResponse.sha,
        commitUrl = commitUrl,
        filesCount = files.size,
        totalBytes = totalBytesUploaded,
        commitMessage = actualCommitMsg
      )
      val savedId = database.uploadHistoryDao().insert(historyEntity)
      val finalHistory = historyEntity.copy(id = savedId)

      onProgress(files.size, files.size, "", 1.0f, "Completed successfully!")
      Result.success(finalHistory)
    } catch (e: Exception) {
      Result.failure(Exception(parseErrorMessage(e)))
    }
  }

  /**
   * Repository contents & file management (view, update, create, delete)
   */
  suspend fun getDirectoryContents(
    owner: String,
    repo: String,
    path: String,
    branch: String? = null
  ): Result<List<GitHubContentItem>> = withContext(Dispatchers.IO) {
    try {
      val response = if (path.isBlank() || path == "/") {
        apiService.getRootContentsRaw(owner, repo, branch)
      } else {
        apiService.getContentsRaw(owner, repo, path.trim('/'), branch)
      }

      if (!response.isSuccessful || response.body() == null) {
        throw HttpException(response)
      }

      val jsonString = response.body()!!.string()
      val moshi = ApiClient.moshi

      // Could be a JSON array (directory) or single JSON object (file)
      if (jsonString.trim().startsWith("[")) {
        val listType = Types.newParameterizedType(List::class.java, GitHubContentItem::class.java)
        val adapter = moshi.adapter<List<GitHubContentItem>>(listType)
        val items = adapter.fromJson(jsonString) ?: emptyList()
        Result.success(items.sortedWith(compareBy<GitHubContentItem> { it.type != "dir" }.thenBy { it.name.lowercase() }))
      } else {
        val adapter = moshi.adapter(GitHubContentItem::class.java)
        val item = adapter.fromJson(jsonString)
        if (item != null) {
          Result.success(listOf(item))
        } else {
          Result.success(emptyList())
        }
      }
    } catch (e: Exception) {
      Result.failure(Exception(parseErrorMessage(e)))
    }
  }

  suspend fun getFileContent(
    owner: String,
    repo: String,
    path: String,
    branch: String? = null
  ): Result<Pair<GitHubContentItem, String>> = withContext(Dispatchers.IO) {
    try {
      val response = apiService.getContentsRaw(owner, repo, path.trim('/'), branch)
      if (!response.isSuccessful || response.body() == null) {
        throw HttpException(response)
      }
      val jsonString = response.body()!!.string()
      val adapter = ApiClient.moshi.adapter(GitHubContentItem::class.java)
      val item = adapter.fromJson(jsonString)
        ?: throw IllegalStateException("Failed to parse file item")

      val rawContent = item.content?.replace("\n", "") ?: ""
      val decodedText = if (item.encoding == "base64" && rawContent.isNotEmpty()) {
        try {
          String(Base64.decode(rawContent, Base64.DEFAULT), Charsets.UTF_8)
        } catch (_: Exception) {
          "[Binary file cannot be previewed as text]"
        }
      } else {
        item.content ?: ""
      }

      Result.success(Pair(item, decodedText))
    } catch (e: Exception) {
      Result.failure(Exception(parseErrorMessage(e)))
    }
  }

  suspend fun createOrUpdateFile(
    owner: String,
    repo: String,
    path: String,
    textContent: String,
    commitMessage: String,
    sha: String?,
    branch: String?
  ): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val base64 = Base64.encodeToString(textContent.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
      val response = apiService.createOrUpdateFile(
        owner = owner,
        repo = repo,
        path = path.trim('/'),
        body = CreateOrUpdateFileRequest(
          message = commitMessage,
          content = base64,
          sha = sha,
          branch = branch
        )
      )
      if (response.isSuccessful) {
        Result.success(Unit)
      } else {
        throw HttpException(response)
      }
    } catch (e: Exception) {
      Result.failure(Exception(parseErrorMessage(e)))
    }
  }

  suspend fun deleteFile(
    owner: String,
    repo: String,
    path: String,
    sha: String,
    commitMessage: String,
    branch: String?
  ): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val response = apiService.deleteFile(
        owner = owner,
        repo = repo,
        path = path.trim('/'),
        body = DeleteFileRequest(
          message = commitMessage,
          sha = sha,
          branch = branch
        )
      )
      if (response.isSuccessful) {
        Result.success(Unit)
      } else {
        throw HttpException(response)
      }
    } catch (e: Exception) {
      Result.failure(Exception(parseErrorMessage(e)))
    }
  }

  suspend fun deleteHistoryItem(id: Long) {
    withContext(Dispatchers.IO) {
      database.uploadHistoryDao().deleteById(id)
    }
  }

  suspend fun clearAllHistory() {
    withContext(Dispatchers.IO) {
      database.uploadHistoryDao().clearAll()
    }
  }

  private fun parseErrorMessage(throwable: Throwable): String {
    if (throwable is HttpException) {
      try {
        val errorBody = throwable.response()?.errorBody()?.string()
        if (!errorBody.isNullOrBlank()) {
          val json = JSONObject(errorBody)
          if (json.has("message")) {
            val msg = json.getString("message")
            if (throwable.code() == 401) {
              return "Authentication error: $msg. Please verify your Personal Access Token."
            }
            if (throwable.code() == 404) {
              return "Resource not found (404). Please ensure the repository exists and your token has 'repo' access."
            }
            return msg
          }
        }
      } catch (_: Exception) {
        // Fallback to standard message
      }
      return when (throwable.code()) {
        401 -> "Unauthorized. Invalid or expired GitHub token."
        403 -> "Forbidden. API rate limit exceeded or insufficient permissions."
        404 -> "Not found. The repository or resource could not be found."
        422 -> "Validation failed. Please verify branch, file contents or commit parameters."
        else -> "GitHub API error: HTTP ${throwable.code()} ${throwable.message()}"
      }
    }
    return throwable.localizedMessage ?: throwable.message ?: "An unexpected error occurred."
  }
}
