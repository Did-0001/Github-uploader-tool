package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "upload_history")
data class UploadHistoryEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  @ColumnInfo(name = "repo_name") val repoName: String,
  @ColumnInfo(name = "branch") val branch: String,
  @ColumnInfo(name = "folder_name") val folderName: String,
  @ColumnInfo(name = "commit_sha") val commitSha: String,
  @ColumnInfo(name = "commit_url") val commitUrl: String,
  @ColumnInfo(name = "files_count") val filesCount: Int,
  @ColumnInfo(name = "total_bytes") val totalBytes: Long,
  @ColumnInfo(name = "commit_message") val commitMessage: String,
  @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)
