package org.demo.service

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestClient
import java.util.concurrent.TimeUnit

class PdfGenerationServiceTest {
    private lateinit var server: MockWebServer
    private lateinit var service: PdfGenerationService

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()

        val pdfServiceUrl = server.url("/pdf").toString()
        service = PdfGenerationService(RestClient.builder(), pdfServiceUrl)
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `generatePdf should translate 503 to IllegalStateException`() {
        server.enqueue(MockResponse().setResponseCode(503))

        val ex =
            assertThrows(IllegalStateException::class.java) {
                service.generatePdf("Weekly", "Body")
            }

        assertEquals("Failed to reach PDF service", ex.message)
    }

    @Test
    fun `generatePdf should return bytes on success`() {
        val expectedPdf = byteArrayOf(0x25, 0x50, 0x44, 0x46)
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/pdf")
                .setBody(expectedPdf.toString(Charsets.ISO_8859_1)),
        )

        val actual = service.generatePdf("Weekly", "Body")
        val recordedRequest = server.takeRequest(2, TimeUnit.SECONDS)

        assertArrayEquals(expectedPdf, actual)
        assertTrue(recordedRequest != null, "Expected request was not received")
        assertEquals("POST", recordedRequest?.method)
    }

    @Test
    fun `generatePdfNonBlocking should translate 503 to IllegalStateException`() {
        server.enqueue(MockResponse().setResponseCode(503))

        val ex =
            assertThrows(IllegalStateException::class.java) {
                runBlocking {
                    service.generatePdfNonBlocking("Weekly", "Body")
                }
            }

        assertEquals("Failed to reach PDF service", ex.message)
    }
}
