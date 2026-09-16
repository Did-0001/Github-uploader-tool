package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun formatFileSize_isAccurate() {
    assertEquals("0 B", com.example.util.FolderScanner.formatFileSize(0L))
    assertEquals("500 B", com.example.util.FolderScanner.formatFileSize(500L))
    assertEquals("1 KB", com.example.util.FolderScanner.formatFileSize(1024L))
    assertEquals("1.5 MB", com.example.util.FolderScanner.formatFileSize((1.5 * 1024 * 1024).toLong()))
  }

  @Test
  fun defaultClientId_isNotEmpty() {
    assertTrue(com.example.data.repository.GitHubRepository.DEFAULT_CLIENT_ID.isNotBlank())
  }
}
