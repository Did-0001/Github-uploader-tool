package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AuthPreferences
import com.example.data.local.UploadHistoryEntity
import com.example.data.model.DeviceCodeResponse
import com.example.data.model.GitHubBranch
import com.example.data.model.GitHubContentItem
import com.example.data.model.GitHubRepo
import com.example.data.model.GitHubUser
import com.example.data.model.ScannedFileItem
import com.example.data.model.UploadStatus
import com.example.data.repository.GitHubRepository
import com.example.util.FolderScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AuthUiState(
  val isLoggedIn: Boolean = false,
  val isVerifying: Boolean = false,
  val user: GitHubUser? = null,
  val errorMessage: String? = null,
  val deviceCodeInfo: DeviceCodeResponse? = null,
  val isPollingOAuth: Boolean = false,
  val isAuthorizedSuccessfully: Boolean = false
)

data class ExplorerUiState(
  val currentPath: String = "",
  val items: List<GitHubContentItem> = emptyList(),
  val isLoading: Boolean = false,
  val errorMessage: String? = null,
  val viewingFile: GitHubContentItem? = null,
  val viewingContent: String? = null,
  val isSavingFile: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
  val authPreferences = AuthPreferences(application)
  val repository = GitHubRepository(application, authPreferences)

  // Auth state
  private val _authUiState = MutableStateFlow(
    AuthUiState(
      isLoggedIn = authPreferences.isLoggedIn,
      user = if (authPreferences.isLoggedIn && authPreferences.getUsername() != null) {
        GitHubUser(
          login = authPreferences.getUsername()!!,
          id = 0,
          avatarUrl = authPreferences.getAvatarUrl(),
          name = authPreferences.getUserDisplayName(),
          htmlUrl = "https://github.com/${authPreferences.getUsername()}"
        )
      } else null
    )
  )
  val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

  // Repos state
  private val _repos = MutableStateFlow<List<GitHubRepo>>(emptyList())
  val repos: StateFlow<List<GitHubRepo>> = _repos.asStateFlow()

  private val _isLoadingRepos = MutableStateFlow(false)
  val isLoadingRepos: StateFlow<Boolean> = _isLoadingRepos.asStateFlow()

  private val _selectedRepo = MutableStateFlow<GitHubRepo?>(null)
  val selectedRepo: StateFlow<GitHubRepo?> = _selectedRepo.asStateFlow()

  private val _branches = MutableStateFlow<List<GitHubBranch>>(emptyList())
  val branches: StateFlow<List<GitHubBranch>> = _branches.asStateFlow()

  private val _selectedBranch = MutableStateFlow("main")
  val selectedBranch: StateFlow<String> = _selectedBranch.asStateFlow()

  private val _targetDirectory = MutableStateFlow("")
  val targetDirectory: StateFlow<String> = _targetDirectory.asStateFlow()

  private val _commitMessage = MutableStateFlow("")
  val commitMessage: StateFlow<String> = _commitMessage.asStateFlow()

  // Folder scanning & Upload state
  private val _uploadStatus = MutableStateFlow<UploadStatus>(UploadStatus.Idle)
  val uploadStatus: StateFlow<UploadStatus> = _uploadStatus.asStateFlow()

  private val _selectedFolderName = MutableStateFlow("")
  val selectedFolderName: StateFlow<String> = _selectedFolderName.asStateFlow()

  private val _scannedFiles = MutableStateFlow<List<ScannedFileItem>>(emptyList())
  val scannedFiles: StateFlow<List<ScannedFileItem>> = _scannedFiles.asStateFlow()

  private val _filterGit = MutableStateFlow(authPreferences.filterGitFolder)
  val filterGit: StateFlow<Boolean> = _filterGit.asStateFlow()

  private val _filterHidden = MutableStateFlow(authPreferences.filterHiddenFiles)
  val filterHidden: StateFlow<Boolean> = _filterHidden.asStateFlow()

  // Explorer state
  private val _explorerState = MutableStateFlow(ExplorerUiState())
  val explorerState: StateFlow<ExplorerUiState> = _explorerState.asStateFlow()

  // Upload history flow from Room
  val uploadHistory: StateFlow<List<UploadHistoryEntity>> = repository.uploadHistoryFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private var oauthPollingJob: Job? = null

  init {
    if (authPreferences.isLoggedIn) {
      loadUserProfileAndRepos()
    }
  }

  fun setFilterGit(enabled: Boolean) {
    _filterGit.value = enabled
    authPreferences.filterGitFolder = enabled
  }

  fun setFilterHidden(enabled: Boolean) {
    _filterHidden.value = enabled
    authPreferences.filterHiddenFiles = enabled
  }

  fun setTargetDirectory(dir: String) {
    _targetDirectory.value = dir
  }

  fun setCommitMessage(msg: String) {
    _commitMessage.value = msg
  }

  fun setSelectedBranch(branch: String) {
    _selectedBranch.value = branch
    _selectedRepo.value?.let { repo ->
      authPreferences.saveLastSelectedRepo(repo.fullName, branch)
    }
  }

  fun selectRepo(repo: GitHubRepo) {
    _selectedRepo.value = repo
    _selectedBranch.value = repo.defaultBranch
    authPreferences.saveLastSelectedRepo(repo.fullName, repo.defaultBranch)
    loadBranches(repo)
    loadExplorerPath("")
  }

  fun verifyAndLogin(token: String) {
    if (token.isBlank()) {
      _authUiState.value = _authUiState.value.copy(errorMessage = "Please enter your GitHub token.")
      return
    }

    viewModelScope.launch {
      _authUiState.value = _authUiState.value.copy(isVerifying = true, errorMessage = null)
      val result = repository.verifyToken(token)
      result.onSuccess { user ->
        _authUiState.value = _authUiState.value.copy(
          isLoggedIn = true,
          isVerifying = false,
          user = user,
          errorMessage = null
        )
        loadUserRepos()
      }.onFailure { error ->
        _authUiState.value = _authUiState.value.copy(
          isVerifying = false,
          errorMessage = error.message ?: "Authentication failed."
        )
      }
    }
  }

  fun startDeviceFlow(clientId: String = "", onCodeReady: ((DeviceCodeResponse) -> Unit)? = null) {
    val effectiveClientId = if (clientId.isBlank()) GitHubRepository.DEFAULT_CLIENT_ID else clientId.trim()
    viewModelScope.launch {
      _authUiState.value = _authUiState.value.copy(
        isVerifying = true,
        errorMessage = null,
        deviceCodeInfo = null,
        isAuthorizedSuccessfully = false
      )
      val result = repository.requestDeviceCode(effectiveClientId)
      result.onSuccess { info ->
        _authUiState.value = _authUiState.value.copy(
          isVerifying = false,
          deviceCodeInfo = info,
          isPollingOAuth = true,
          errorMessage = null
        )
        onCodeReady?.invoke(info)
        pollDeviceOAuth(effectiveClientId, info.deviceCode, info.interval.coerceAtLeast(5))
      }.onFailure { error ->
        _authUiState.value = _authUiState.value.copy(
          isVerifying = false,
          errorMessage = error.message ?: "Failed to initiate GitHub authentication."
        )
      }
    }
  }

  private fun pollDeviceOAuth(clientId: String, deviceCode: String, intervalSeconds: Int) {
    oauthPollingJob?.cancel()
    oauthPollingJob = viewModelScope.launch {
      var done = false
      var counter = 0
      while (!done && counter < 60) { // Poll for up to 5 minutes
        delay(intervalSeconds * 1000L)
        counter++
        val pollResult = repository.pollDeviceToken(clientId, deviceCode)
        pollResult.onSuccess { tokenResp ->
          if (!tokenResp.accessToken.isNullOrBlank()) {
            done = true
            _authUiState.value = _authUiState.value.copy(
              isPollingOAuth = false,
              isAuthorizedSuccessfully = true
            )
            verifyAndLogin(tokenResp.accessToken)
          } else if (tokenResp.error == "authorization_pending") {
            // Still waiting for user confirmation in browser
          } else if (tokenResp.error == "slow_down") {
            delay(5000)
          } else if (tokenResp.error != null) {
            done = true
            _authUiState.value = _authUiState.value.copy(
              isPollingOAuth = false,
              deviceCodeInfo = null,
              errorMessage = tokenResp.errorDescription ?: tokenResp.error
            )
          }
        }.onFailure {
          // ignore transient poll error
        }
      }
      _authUiState.value = _authUiState.value.copy(isPollingOAuth = false)
    }
  }

  fun cancelDeviceFlow() {
    oauthPollingJob?.cancel()
    _authUiState.value = _authUiState.value.copy(isPollingOAuth = false, deviceCodeInfo = null)
  }

  fun logout() {
    oauthPollingJob?.cancel()
    authPreferences.clear()
    _authUiState.value = AuthUiState()
    _repos.value = emptyList()
    _selectedRepo.value = null
    _branches.value = emptyList()
    _scannedFiles.value = emptyList()
    _uploadStatus.value = UploadStatus.Idle
  }

  private fun loadUserProfileAndRepos() {
    viewModelScope.launch {
      val token = authPreferences.getToken() ?: return@launch
      val userResult = repository.verifyToken(token)
      userResult.onSuccess { user ->
        _authUiState.value = _authUiState.value.copy(user = user, isLoggedIn = true)
      }
      loadUserRepos()
    }
  }

  fun loadUserRepos() {
    viewModelScope.launch {
      _isLoadingRepos.value = true
      val result = repository.getUserRepos()
      result.onSuccess { repoList ->
        _repos.value = repoList
        _isLoadingRepos.value = false

        // Try restoring last selected repo
        val lastRepoName = authPreferences.getLastSelectedRepo()
        val match = repoList.find { it.fullName == lastRepoName } ?: repoList.firstOrNull()
        if (match != null && _selectedRepo.value == null) {
          selectRepo(match)
        }
      }.onFailure {
        _isLoadingRepos.value = false
      }
    }
  }

  private fun loadBranches(repo: GitHubRepo) {
    viewModelScope.launch {
      val (owner, name) = getOwnerAndRepo(repo.fullName)
      val result = repository.getBranches(owner, name)
      result.onSuccess { branchList ->
        _branches.value = branchList
        val lastBranch = authPreferences.getLastSelectedBranch()
        if (lastBranch != null && branchList.any { it.name == lastBranch }) {
          _selectedBranch.value = lastBranch
        } else if (branchList.any { it.name == repo.defaultBranch }) {
          _selectedBranch.value = repo.defaultBranch
        } else if (branchList.isNotEmpty()) {
          _selectedBranch.value = branchList.first().name
        }
      }
    }
  }

  fun createRepository(name: String, description: String?, isPrivate: Boolean, onDone: (Boolean) -> Unit) {
    viewModelScope.launch {
      val result = repository.createRepository(name.trim(), description, isPrivate)
      result.onSuccess { newRepo ->
        _repos.value = listOf(newRepo) + _repos.value
        selectRepo(newRepo)
        onDone(true)
      }.onFailure {
        onDone(false)
      }
    }
  }

  fun scanFolder(uri: Uri) {
    viewModelScope.launch {
      _uploadStatus.value = UploadStatus.Scanning("Scanning folder and computing file tree...")
      try {
        val (folderName, files) = FolderScanner.scanTree(
          context = getApplication(),
          treeUri = uri,
          filterGit = _filterGit.value,
          filterHidden = _filterHidden.value
        ) { count, path ->
          _uploadStatus.value = UploadStatus.Scanning("Discovered $count files: $path")
        }

        _selectedFolderName.value = folderName
        _scannedFiles.value = files
        val totalBytes = files.sumOf { it.sizeBytes }

        if (files.isEmpty()) {
          _uploadStatus.value = UploadStatus.Failure("No valid files found in selected folder.")
        } else {
          _uploadStatus.value = UploadStatus.Ready(
            folderName = folderName,
            totalFiles = files.size,
            totalBytes = totalBytes,
            files = files
          )
          if (_commitMessage.value.isBlank()) {
            _commitMessage.value = "Upload $folderName (${files.size} files)"
          }
        }
      } catch (e: Exception) {
        _uploadStatus.value = UploadStatus.Failure(e.localizedMessage ?: "Failed to scan folder.")
      }
    }
  }

  fun startUpload() {
    val repo = _selectedRepo.value ?: return
    val files = _scannedFiles.value
    if (files.isEmpty()) return

    val (owner, repoName) = getOwnerAndRepo(repo.fullName)
    val branch = _selectedBranch.value
    val targetDir = _targetDirectory.value
    val folderName = _selectedFolderName.value
    val commitMsg = _commitMessage.value

    viewModelScope.launch {
      _uploadStatus.value = UploadStatus.Uploading(
        currentFileIndex = 0,
        totalFiles = files.size,
        currentFileName = "",
        progress = 0.05f,
        stepDescription = "Initializing upload..."
      )

      val result = repository.uploadFolder(
        owner = owner,
        repo = repoName,
        branch = branch,
        targetDirectory = targetDir,
        folderName = folderName,
        files = files,
        commitMessage = commitMsg
      ) { current, total, fileName, percent, step ->
        _uploadStatus.value = UploadStatus.Uploading(
          currentFileIndex = current,
          totalFiles = total,
          currentFileName = fileName,
          progress = percent,
          stepDescription = step
        )
      }

      result.onSuccess { history ->
        _uploadStatus.value = UploadStatus.Success(
          commitSha = history.commitSha,
          commitUrl = history.commitUrl,
          filesUploaded = history.filesCount,
          totalBytes = history.totalBytes,
          repoFullName = repo.fullName,
          branch = branch
        )
      }.onFailure { error ->
        _uploadStatus.value = UploadStatus.Failure(
          errorMessage = error.message ?: "Upload failed.",
          canRetry = true
        )
      }
    }
  }

  fun resetUpload() {
    _scannedFiles.value = emptyList()
    _selectedFolderName.value = ""
    _uploadStatus.value = UploadStatus.Idle
  }

  // Repo Explorer actions
  fun loadExplorerPath(path: String) {
    val repo = _selectedRepo.value ?: return
    val (owner, repoName) = getOwnerAndRepo(repo.fullName)
    viewModelScope.launch {
      _explorerState.value = _explorerState.value.copy(
        currentPath = path,
        isLoading = true,
        errorMessage = null,
        viewingFile = null,
        viewingContent = null
      )
      val result = repository.getDirectoryContents(owner, repoName, path, _selectedBranch.value)
      result.onSuccess { items ->
        _explorerState.value = _explorerState.value.copy(
          items = items,
          isLoading = false
        )
      }.onFailure { error ->
        _explorerState.value = _explorerState.value.copy(
          isLoading = false,
          errorMessage = error.message ?: "Failed to load directory."
        )
      }
    }
  }

  fun viewFile(item: GitHubContentItem) {
    val repo = _selectedRepo.value ?: return
    val (owner, repoName) = getOwnerAndRepo(repo.fullName)
    viewModelScope.launch {
      _explorerState.value = _explorerState.value.copy(isLoading = true)
      val result = repository.getFileContent(owner, repoName, item.path, _selectedBranch.value)
      result.onSuccess { (fileItem, content) ->
        _explorerState.value = _explorerState.value.copy(
          isLoading = false,
          viewingFile = fileItem,
          viewingContent = content
        )
      }.onFailure { error ->
        _explorerState.value = _explorerState.value.copy(
          isLoading = false,
          errorMessage = error.message ?: "Failed to read file."
        )
      }
    }
  }

  fun closeFileViewer() {
    _explorerState.value = _explorerState.value.copy(viewingFile = null, viewingContent = null)
  }

  fun saveEditedFile(item: GitHubContentItem, newContent: String, commitMessage: String, onDone: (Boolean) -> Unit) {
    val repo = _selectedRepo.value ?: return
    val (owner, repoName) = getOwnerAndRepo(repo.fullName)
    viewModelScope.launch {
      _explorerState.value = _explorerState.value.copy(isSavingFile = true)
      val result = repository.createOrUpdateFile(
        owner = owner,
        repo = repoName,
        path = item.path,
        textContent = newContent,
        commitMessage = commitMessage.ifBlank { "Update ${item.name}" },
        sha = item.sha,
        branch = _selectedBranch.value
      )
      _explorerState.value = _explorerState.value.copy(isSavingFile = false)
      result.onSuccess {
        onDone(true)
        closeFileViewer()
        loadExplorerPath(_explorerState.value.currentPath)
      }.onFailure { error ->
        _explorerState.value = _explorerState.value.copy(errorMessage = error.message)
        onDone(false)
      }
    }
  }

  fun createNewFileInExplorer(filename: String, content: String, commitMessage: String, onDone: (Boolean) -> Unit) {
    val repo = _selectedRepo.value ?: return
    val (owner, repoName) = getOwnerAndRepo(repo.fullName)
    val curPath = _explorerState.value.currentPath.trim('/')
    val fullPath = if (curPath.isBlank()) filename.trim('/') else "$curPath/${filename.trim('/')}"

    viewModelScope.launch {
      _explorerState.value = _explorerState.value.copy(isSavingFile = true)
      val result = repository.createOrUpdateFile(
        owner = owner,
        repo = repoName,
        path = fullPath,
        textContent = content,
        commitMessage = commitMessage.ifBlank { "Create $filename" },
        sha = null,
        branch = _selectedBranch.value
      )
      _explorerState.value = _explorerState.value.copy(isSavingFile = false)
      result.onSuccess {
        onDone(true)
        loadExplorerPath(_explorerState.value.currentPath)
      }.onFailure { error ->
        _explorerState.value = _explorerState.value.copy(errorMessage = error.message)
        onDone(false)
      }
    }
  }

  fun deleteExplorerFile(item: GitHubContentItem, commitMessage: String, onDone: (Boolean) -> Unit) {
    val repo = _selectedRepo.value ?: return
    val (owner, repoName) = getOwnerAndRepo(repo.fullName)
    viewModelScope.launch {
      _explorerState.value = _explorerState.value.copy(isSavingFile = true)
      val result = repository.deleteFile(
        owner = owner,
        repo = repoName,
        path = item.path,
        sha = item.sha,
        commitMessage = commitMessage.ifBlank { "Delete ${item.name}" },
        branch = _selectedBranch.value
      )
      _explorerState.value = _explorerState.value.copy(isSavingFile = false)
      result.onSuccess {
        onDone(true)
        closeFileViewer()
        loadExplorerPath(_explorerState.value.currentPath)
      }.onFailure { error ->
        _explorerState.value = _explorerState.value.copy(errorMessage = error.message)
        onDone(false)
      }
    }
  }

  fun deleteHistory(id: Long) {
    viewModelScope.launch {
      repository.deleteHistoryItem(id)
    }
  }

  fun clearHistory() {
    viewModelScope.launch {
      repository.clearAllHistory()
    }
  }

  private fun getOwnerAndRepo(fullName: String): Pair<String, String> {
    val parts = fullName.split("/")
    return if (parts.size >= 2) {
      Pair(parts[0], parts[1])
    } else {
      Pair(authPreferences.getUsername() ?: "", fullName)
    }
  }
}
