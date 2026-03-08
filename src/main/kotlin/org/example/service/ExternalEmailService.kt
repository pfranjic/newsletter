package org.example.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class ExternalEmailService(
    private val restClientBuilder: RestClient.Builder,
    @Value("\${external.email-service.url}") private val emailServiceUrl: String
) {

    fun sendEmail(to: String, pdf: ByteArray, city: String) {
        val requestBuilder = restClientBuilder
            .build()
            .post()
            .uri(emailServiceUrl)
            .contentType(MediaType.APPLICATION_JSON)

        requestBuilder
            .body(ExternalEmailRequest(to, pdf, city))
            .retrieve()
            .toBodilessEntity()
    }

    suspend fun sendEmailAsync(to: String, pdf: ByteArray, city: String) = withContext(Dispatchers.IO) {
        sendEmail(to, pdf, city)
    }
}

data class ExternalEmailRequest(
    val to: String,
    val pdf: ByteArray,
    val city: String
)
