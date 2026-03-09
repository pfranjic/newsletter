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

class IpLocationServiceTest {
    private lateinit var server: MockWebServer
    private lateinit var service: IpLocationService

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()

        val ipServiceUrl = server.url("/geo").toString()
        service = IpLocationService(RestClient.builder(), ipServiceUrl)
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `getUserCity should translate 503 to IllegalStateException`() {
        server.enqueue(MockResponse().setResponseCode(503))

        val ex =
            assertThrows(IllegalStateException::class.java) {
                service.getUserCity("8.8.8.8")
            }

        assertEquals("Failed to reach geolocation service", ex.message)
    }

    @Test
    fun `getUserCity should return city on success`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("""{"status":"success","city":"Zagreb"}"""),
        )

        val city = service.getUserCity("8.8.8.8")
        val recordedRequest = server.takeRequest(2, TimeUnit.SECONDS)

        assertEquals("Zagreb", city)
        assertTrue(recordedRequest != null, "Expected request was not received")
        assertEquals("POST", recordedRequest?.method)
    }

    @Test
    fun `getUserCityNonBlocking should translate 503 to IllegalStateException`() {
        server.enqueue(MockResponse().setResponseCode(503))

        val ex =
            assertThrows(IllegalStateException::class.java) {
                runBlocking {
                    service.getUserCityNonBlocking("1.1.1.1")
                }
            }

        assertEquals("Failed to reach geolocation service", ex.message)
    }

    @Test
    fun `getUserCityAsync should translate 503 to IllegalStateException`() {
        server.enqueue(MockResponse().setResponseCode(503))

        val ex =
            assertThrows(IllegalStateException::class.java) {
                runBlocking {
                    service.getUserCityAsync("9.9.9.9")
                }
            }

        assertEquals("Failed to reach geolocation service", ex.message)
    }
}
