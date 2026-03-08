package org.example.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Service
class PdfGenerationService(
    private val restClientBuilder: RestClient.Builder,
    @Value("\${external.pdf-service.url}") private val pdfServiceUrl: String
) {

    fun generatePdf(title: String, body: String): ByteArray {
        val response = restClientBuilder
            .build()
            .post()
            .uri(pdfServiceUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_PDF)
            .body(ExternalPdfRequest(title = title, body = body))
            .retrieve()
            .body<ByteArray>()

        return response ?: throw IllegalStateException("PDF service returned an empty response")
    }

    suspend fun generatePdfAsync(title: String, body: String): ByteArray = withContext(Dispatchers.IO) {
        generatePdf(title, body)
    }
}

data class ExternalPdfRequest(
    val title: String,
    val body: String
)
