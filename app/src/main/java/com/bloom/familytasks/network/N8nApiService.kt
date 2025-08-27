package com.bloom.familytasks.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface N8nApiService {
    @POST("{webhookId}")
    suspend fun sendChatMessage(
        @Path("webhookId") webhookId: String,
        @Body request: ChatRequest
    ): Response<ResponseBody>

    @Multipart
    @POST("{webhookId}")
    suspend fun sendTaskWithImages(
        @Path("webhookId") webhookId: String,
        @Part("chatInput") chatInput: RequestBody,
        @Part images: List<MultipartBody.Part>
    ): Response<ValidationResponse>
}