package com.example.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface BloggerApiService {

    @GET("feeds/posts/default")
    suspend fun getPostsFeed(
        @Query("alt") alt: String = "json",
        @Query("start-index") startIndex: Int = 1,
        @Query("max-results") maxResults: Int = 150,
        @Header("Cache-Control") cacheControl: String? = null
    ): Response<ResponseBody>

    @GET("feeds/pages/default")
    suspend fun getPagesFeed(
        @Query("alt") alt: String = "json",
        @Query("max-results") maxResults: Int = 100,
        @Header("Cache-Control") cacheControl: String? = null
    ): Response<ResponseBody>

    @GET("feeds/comments/default")
    suspend fun getRecentCommentsFeed(
        @Query("alt") alt: String = "json",
        @Query("max-results") maxResults: Int = 40,
        @Header("Cache-Control") cacheControl: String? = null
    ): Response<ResponseBody>

    @GET("feeds/{postId}/comments/default")
    suspend fun getPostCommentsFeed(
        @Path("postId") postId: String,
        @Query("alt") alt: String = "json",
        @Query("max-results") maxResults: Int = 100,
        @Header("Cache-Control") cacheControl: String? = null
    ): Response<ResponseBody>

    @GET
    suspend fun fetchRawUrl(
        @Url url: String,
        @Header("Cache-Control") cacheControl: String? = null
    ): Response<ResponseBody>
}
