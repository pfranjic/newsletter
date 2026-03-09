package org.demo.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.body
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientException
import org.springframework.web.reactive.function.client.awaitBodyOrNull

@Service
class PdfGenerationService(
    private val restClientBuilder: RestClient.Builder,
    @Value("\${external.pdf-service.url}") private val pdfServiceUrl: String,
) {
    private val logger = KotlinLogging.logger {}

    @Throws(IllegalStateException::class)
    fun generatePdf(
        title: String,
        body: String,
    ): ByteArray {
        val response =
            try {
                restClientBuilder
                    .build()
                    .post()
                    .uri(pdfServiceUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_PDF)
                    .body(ExternalPdfRequest(title = title, body = body))
                    .retrieve()
                    .body<ByteArray>()
            } catch (ex: RestClientException) {
                throw IllegalStateException("Failed to reach PDF service", ex)
            }
        logger.info { "Thread pdf service blocking: ${Thread.currentThread().name}" }

        return response ?: throw IllegalStateException("PDF service returned an empty response")
    }

    @Throws(IllegalStateException::class)
    suspend fun generatePdfNonBlocking(
        title: String,
        body: String,
    ): ByteArray {
        val response =
            try {
                WebClient.builder()
                    .build()
                    .post()
                    .uri(pdfServiceUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_PDF)
                    .bodyValue(ExternalPdfRequest(title = title, body = body))
                    .retrieve()
                    .awaitBodyOrNull<ByteArray>()
            } catch (ex: WebClientException) {
                throw IllegalStateException("Failed to reach PDF service", ex)
            }
        logger.info { "Thread pdf service non-blocking: ${Thread.currentThread().name}" }

        return response ?: throw IllegalStateException("PDF service returned an empty response")
    }
}

data class ExternalPdfRequest(
    val title: String,
    val body: String,
)
