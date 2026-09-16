package com.example.data.remote

import com.example.data.model.CreateBlobRequest
import com.example.data.model.CreateBlobResponse
import com.example.data.model.CreateCommitRequest
import com.example.data.model.CreateCommitResponse
import com.example.data.model.CreateOrUpdateFileRequest
import com.example.data.model.CreateRepoRequest
import com.example.data.model.CreateTreeRequest
import com.example.data.model.CreateTreeResponse
import com.example.data.model.DeleteFileRequest
import com.example.data.model.DeviceCodeResponse
import com.example.data.model.GitCommitDetail
import com.example.data.model.GitHubBranch
import com.example.data.model.GitHubRef
import com.example.data.model.GitHubRepo
import com.example.data.model.GitHubUser
import com.example.data.model.OAuthTokenResponse
import com.example.data.model.UpdateRefRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface GitHubApiService {

  @GET("user")
  suspend fun getAuthenticatedUser(): GitHubUser

  @GET("user/repos")
  suspend fun getUserRepos(
    @Query("sort") sort: String = "updated",
    @Query("per_page") perPage: Int = 100,
    @Query("type") type: String = "all",
    @Query("direction") direction: String = "desc"
  ): List<GitHubRepo>

  @POST("user/repos")
  suspend fun createRepo(
    @Body request: CreateRepoRequest
  ): GitHubRepo

  @GET("repos/{owner}/{repo}/branches")
  suspend fun getBranches(
    @Path("owner") owner: String,
    @Path("repo") repo: String
  ): List<GitHubBranch>

  @GET("repos/{owner}/{repo}/git/ref/heads/{branch}")
  suspend fun getBranchRef(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Path("branch") branch: String
  ): Response<GitHubRef>

  @GET("repos/{owner}/{repo}/git/commits/{commit_sha}")
  suspend fun getCommitDetail(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Path("commit_sha") commitSha: String
  ): GitCommitDetail

  @POST("repos/{owner}/{repo}/git/blobs")
  suspend fun createBlob(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Body body: CreateBlobRequest
  ): CreateBlobResponse

  @POST("repos/{owner}/{repo}/git/trees")
  suspend fun createTree(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Body body: CreateTreeRequest
  ): CreateTreeResponse

  @POST("repos/{owner}/{repo}/git/commits")
  suspend fun createCommit(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Body body: CreateCommitRequest
  ): CreateCommitResponse

  @PATCH("repos/{owner}/{repo}/git/refs/heads/{branch}")
  suspend fun updateRef(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Path("branch") branch: String,
    @Body body: UpdateRefRequest
  ): GitHubRef

  @POST("repos/{owner}/{repo}/git/refs")
  suspend fun createRef(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Body body: Map<String, String>
  ): GitHubRef

  @GET("repos/{owner}/{repo}/contents/{path}")
  suspend fun getContentsRaw(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Path(value = "path", encoded = true) path: String,
    @Query("ref") ref: String? = null
  ): Response<ResponseBody>

  @GET("repos/{owner}/{repo}/contents")
  suspend fun getRootContentsRaw(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Query("ref") ref: String? = null
  ): Response<ResponseBody>

  @PUT("repos/{owner}/{repo}/contents/{path}")
  suspend fun createOrUpdateFile(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Path(value = "path", encoded = true) path: String,
    @Body body: CreateOrUpdateFileRequest
  ): Response<ResponseBody>

  @HTTP(method = "DELETE", path = "repos/{owner}/{repo}/contents/{path}", hasBody = true)
  suspend fun deleteFile(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Path(value = "path", encoded = true) path: String,
    @Body body: DeleteFileRequest
  ): Response<ResponseBody>
}

interface GitHubOAuthService {
  @Headers("Accept: application/json")
  @POST("login/device/code")
  @FormUrlEncoded
  suspend fun requestDeviceCode(
    @Field("client_id") clientId: String,
    @Field("scope") scope: String = "repo,read:user,user:email"
  ): DeviceCodeResponse

  @Headers("Accept: application/json")
  @POST("login/oauth/access_token")
  @FormUrlEncoded
  suspend fun pollDeviceToken(
    @Field("client_id") clientId: String,
    @Field("device_code") deviceCode: String,
    @Field("grant_type") grantType: String = "urn:ietf:params:oauth:grant-type:device_code"
  ): OAuthTokenResponse

  @Headers("Accept: application/json")
  @POST("login/oauth/access_token")
  @FormUrlEncoded
  suspend fun exchangeWebCode(
    @Field("client_id") clientId: String,
    @Field("client_secret") clientSecret: String,
    @Field("code") code: String,
    @Field("redirect_uri") redirectUri: String
  ): OAuthTokenResponse
}
