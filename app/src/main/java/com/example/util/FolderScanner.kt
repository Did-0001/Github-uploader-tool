package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.documentfile.provider.DocumentFile
import com.example.data.model.ScannedFileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.DecimalFormat

object FolderScanner {

  suspend fun scanTree(
    context: Context,
    treeUri: Uri,
    filterGit: Boolean = true,
    filterHidden: Boolean = true,
    onProgress: (scannedCount: Int, currentPath: String) -> Unit = { _, _ -> }
  ): Pair<String, List<ScannedFileItem>> = withContext(Dispatchers.IO) {
    val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
      ?: throw IllegalArgumentException("Could not open selected folder")

    val folderName = rootDoc.name ?: "selected_folder"
    val resultList = mutableListOf<ScannedFileItem>()

    fun traverse(dir: DocumentFile, currentPathPrefix: String) {
      val children = dir.listFiles()
      for (child in children) {
        val name = child.name ?: continue

        if (filterHidden && name.startsWith(".") && name != ".gitkeep" && name != ".gitignore" && name != ".env.example") {
          continue
        }

        if (child.isDirectory) {
          if (filterGit && (name.equals(".git", ignoreCase = true) || name.equals(".gradle", ignoreCase = true))) {
            continue
          }
          traverse(child, "$currentPathPrefix$name/")
        } else if (child.isFile) {
          val relativePath = "$currentPathPrefix$name"
          resultList.add(
            ScannedFileItem(
              uri = child.uri,
              relativePath = relativePath,
              displayName = name,
              sizeBytes = child.length(),
              mimeType = child.type ?: "application/octet-stream",
              isDirectory = false
            )
          )
          onProgress(resultList.size, relativePath)
        }
      }
    }

    traverse(rootDoc, "")
    Pair(folderName, resultList)
  }

  suspend fun readFileAsBase64(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    val inputStream: InputStream = context.contentResolver.openInputStream(uri)
      ?: throw IllegalStateException("Unable to open input stream for $uri")

    inputStream.use { stream ->
      val byteStream = ByteArrayOutputStream()
      val buffer = ByteArray(8192)
      var bytesRead: Int
      while (stream.read(buffer).also { bytesRead = it } != -1) {
        byteStream.write(buffer, 0, bytesRead)
      }
      Base64.encodeToString(byteStream.toByteArray(), Base64.NO_WRAP)
    }
  }

  fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val boundedGroup = digitGroups.coerceIn(0, units.size - 1)
    val formatter = DecimalFormat("#,##0.#")
    return "${formatter.format(bytes / Math.pow(1024.0, boundedGroup.toDouble()))} ${units[boundedGroup]}"
  }
}
