package org.example.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
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
        } catch (ex: HttpServerErrorException.ServiceUnavailable) {
            throw IllegalStateException("Email service is temporarily unavailable", ex)
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
        } catch (ex: WebClientResponseException.ServiceUnavailable) {
            throw IllegalStateException("Email service is temporarily unavailable", ex)
        }
    }

    @Throws(IllegalStateException::class)
    suspend fun sendEmailAsync(
        to: String,
        pdf: ByteArray,
        city: String,
    ) = withContext(Dispatchers.IO) {
        sendEmail(to, pdf, city)
    }
}

data class ExternalEmailRequest(
    val to: String,
    val pdf: ByteArray,
    val city: String,
)
