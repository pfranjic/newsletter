package org.demo.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientException
import org.springframework.web.reactive.function.client.awaitBodilessEntity

@Service
class ExternalEmailService(
    private val restClientBuilder: RestClient.Builder,
    @Value("\${external.email-service.url}") private val emailServiceUrl: String,
) {
    @Throws(IllegalStateException::class)
    fun sendEmail(
        to: String,
        pdf: ByteArray,
        city: String,
    ) {
        try {
            val requestBuilder =
                restClientBuilder
                    .build()
                    .post()
                    .uri(emailServiceUrl)
                    .contentType(MediaType.APPLICATION_JSON)

            requestBuilder
                .body(ExternalEmailRequest(to, pdf, city))
                .retrieve()
                .toBodilessEntity()
        } catch (ex: RestClientException) {
            throw IllegalStateException("Failed to reach email service", ex)
        }
    }

    @Throws(IllegalStateException::class)
    suspend fun sendEmailNonBlocking(
        to: String,
        pdf: ByteArray,
        city: String,
    ) {
        try {
            val webClient = WebClient.builder().build()
            webClient
                .post()
                .uri(emailServiceUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(ExternalEmailRequest(to, pdf, city))
                .retrieve()
                .awaitBodilessEntity()
        } catch (ex: WebClientException) {
            throw IllegalStateException("Failed to reach email service", ex)
        }
    }
}

data class ExternalEmailRequest(
    val to: String,
    val pdf: ByteArray,
    val city: String,
)
