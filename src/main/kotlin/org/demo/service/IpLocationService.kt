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
class IpLocationService(
    private val restClientBuilder: RestClient.Builder,
    @Value("\${external.ip-geo-service.url}") private val ipGeoServiceUrl: String,
) {
    private val logger = KotlinLogging.logger {}

    @Throws(IllegalStateException::class)
    fun getUserCity(ipAddress: String): String {
        val targetIp = ipAddress.trim()
        require(targetIp.isNotBlank()) { "ipAddress is required" }

        val response =
            try {
                restClientBuilder
                    .build()
                    .post()
                    .uri(ipGeoServiceUrl)
                    .body(mapOf("ip" to targetIp))
                    .retrieve()
                    .body<IpApiResponse>()
                    ?: throw IllegalStateException("Geolocation service returned an empty response")
            } catch (ex: RestClientException) {
                throw IllegalStateException("Failed to reach geolocation service", ex)
            }

        if (response.status != "success") {
            throw IllegalStateException(response.message ?: "Unable to resolve location for IP")
        }
        logger.info { "Thread IP service blocking: ${Thread.currentThread().name}" }

        return response.city ?: throw IllegalStateException("City information is missing in geolocation response")
    }

    @Throws(IllegalStateException::class)
    suspend fun getUserCityNonBlocking(ipAddress: String): String {
        val targetIp = ipAddress.trim()
        require(targetIp.isNotBlank()) { "ipAddress is required" }

        val response =
            try {
                WebClient.builder()
                    .build()
                    .post()
                    .uri(ipGeoServiceUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(mapOf("ip" to targetIp))
                    .retrieve()
                    .awaitBodyOrNull<IpApiResponse>()
                    ?: throw IllegalStateException("Geolocation service returned an empty response")
            } catch (ex: WebClientException) {
                throw IllegalStateException("Failed to reach geolocation service", ex)
            }

        if (response.status != "success") {
            throw IllegalStateException(response.message ?: "Unable to resolve location for IP")
        }
        logger.info { "Thread IP service non-blocking: ${Thread.currentThread().name}" }

        return response.city ?: throw IllegalStateException("City information is missing in geolocation response")
    }
}

data class IpApiResponse(
    val status: String? = null,
    val city: String? = null,
    val message: String? = null
)
