package org.example.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull

@Service
class PdfGenerationService(
    private val restClientBuilder: RestClient.Builder,
    @Value("\${external.pdf-service.url}") private val pdfServiceUrl: String,
) {
    private val logger = KotlinLogging.logger {}

    fun generatePdf(
        title: String,
        body: String,
    ): ByteArray {
        val response =
            restClientBuilder
                .build()
                .post()
                .uri(pdfServiceUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_PDF)
                .body(ExternalPdfRequest(title = title, body = body))
                .retrieve()
                .body<ByteArray>()
        logger.info { "Thread pdf service blocking: ${Thread.currentThread().name}" }

        return response ?: throw IllegalStateException("PDF service returned an empty response")
    }

    suspend fun generatePdfNonBlocking(
        title: String,
        body: String,
    ): ByteArray {
        val webClient = WebClient.builder().build()
        val response =
            webClient
                .post()
                .uri(pdfServiceUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_PDF)
                .bodyValue(ExternalPdfRequest(title = title, body = body))
                .retrieve()
                .awaitBodyOrNull<ByteArray>()
        logger.info { "Thread pdf service non-blocking: ${Thread.currentThread().name}" }

        return response ?: throw IllegalStateException("PDF service returned an empty response")
    }

    suspend fun generatePdfAsync(
        title: String,
        body: String,
    ): ByteArray =
        withContext(Dispatchers.IO) {
            generatePdf(title, body)
        }
}

data class ExternalPdfRequest(
    val title: String,
    val body: String,
)
