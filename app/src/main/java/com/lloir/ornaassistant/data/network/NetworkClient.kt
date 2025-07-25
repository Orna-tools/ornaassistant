package com.lloir.ornaassistant.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

interface NetworkClient {
    data class Response(
        val isSuccessful: Boolean,
        val body: ByteArray? = null,
        val errorMessage: String? = null
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Response

            if (isSuccessful != other.isSuccessful) return false
            if (body != null) {
                if (other.body == null) return false
                if (!body.contentEquals(other.body)) return false
            } else if (other.body != null) return false
            if (errorMessage != other.errorMessage) return false

            return true
        }

        override fun hashCode(): Int {
            var result = isSuccessful.hashCode()
            result = 31 * result + (body?.contentHashCode() ?: 0)
            result = 31 * result + (errorMessage?.hashCode() ?: 0)
            return result
        }
    }
    
    suspend fun downloadFile(url: String): Response
}

@Singleton
class NetworkClientImpl @Inject constructor() : NetworkClient {
    override suspend fun downloadFile(url: String): NetworkClient.Response {
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val bytes = inputStream.readBytes()
                    inputStream.close()
                    connection.disconnect()
                    
                    NetworkClient.Response(isSuccessful = true, body = bytes)
                } else {
                    NetworkClient.Response(
                        isSuccessful = false,
                        errorMessage = "HTTP error: $responseCode"
                    )
                }
            } catch (e: Exception) {
                NetworkClient.Response(
                    isSuccessful = false,
                    errorMessage = e.message ?: "Unknown error"
                )
            }
        }
    }
}