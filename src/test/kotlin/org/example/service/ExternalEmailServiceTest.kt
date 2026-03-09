package org.example.service

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestClient
import java.util.concurrent.TimeUnit

class ExternalEmailServiceTest {
    private lateinit var server: MockWebServer
    private lateinit var service: ExternalEmailService

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()

        val emailServiceUrl = server.url("/email").toString()
        service = ExternalEmailService(RestClient.builder(), emailServiceUrl)
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `sendEmail should translate 503 to IllegalStateException`() {
        server.enqueue(MockResponse().setResponseCode(503))

        val ex =
            assertThrows(IllegalStateException::class.java) {
                service.sendEmail("alice@example.com", byteArrayOf(1, 2, 3), "Zagreb")
            }

        assertEquals("Failed to reach email service", ex.message)
    }

    @Test
    fun `sendEmail should complete on 200 response`() {
        server.enqueue(MockResponse().setResponseCode(200))

        service.sendEmail("alice@example.com", byteArrayOf(1, 2, 3), "Zagreb")

        val recordedRequest = server.takeRequest(2, TimeUnit.SECONDS)
        assertTrue(recordedRequest != null, "Expected request was not received")
        assertEquals("POST", recordedRequest?.method)
    }

    @Test
    fun `sendEmailNonBlocking should translate 503 to IllegalStateException`() {
        server.enqueue(MockResponse().setResponseCode(503))

        val ex =
            assertThrows(IllegalStateException::class.java) {
                runBlocking {
                    service.sendEmailNonBlocking("bob@example.com", byteArrayOf(4, 5, 6), "Split")
                }
            }

        assertEquals("Failed to reach email service", ex.message)
    }

    @Test
    fun `sendEmailAsync should translate 503 to IllegalStateException`() {
        server.enqueue(MockResponse().setResponseCode(503))

        val ex =
            assertThrows(IllegalStateException::class.java) {
                runBlocking {
                    service.sendEmailAsync("carol@example.com", byteArrayOf(7, 8, 9), "Rijeka")
                }
            }

        assertEquals("Failed to reach email service", ex.message)
    }
}
