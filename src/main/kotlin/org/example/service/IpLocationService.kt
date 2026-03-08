package org.example.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull
import org.springframework.http.MediaType

@Service
class IpLocationService(
    private val restClientBuilder: RestClient.Builder,
    @Value("\${external.ip-geo-service.url}") private val ipGeoServiceUrl: String
) {
    private val logger = KotlinLogging.logger{}

    fun getUserCity(ipAddress: String): String {
        val targetIp = ipAddress.trim()
        require(targetIp.isNotBlank()) { "ipAddress is required" }

        val response = restClientBuilder
            .build()
            .post()
            .uri("$ipGeoServiceUrl")
            .body(mapOf("ip" to targetIp))
            .retrieve()
            .body<IpApiResponse>()
            ?: throw IllegalStateException("Geolocation service returned an empty response")

        if (response.status != "success") {
            throw IllegalStateException(response.message ?: "Unable to resolve location for IP")
        }
        logger.info { "Thread IP service blocking: ${Thread.currentThread().name}" }

        return response.city ?: throw IllegalStateException("City information is missing in geolocation response")
    }

    suspend fun getUserCityNonBlocking(ipAddress: String): String {
        val webClient = WebClient.builder().build()
        val targetIp = ipAddress.trim()
        require(targetIp.isNotBlank()) { "ipAddress is required" }

        val response = webClient.post()
            .uri(ipGeoServiceUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mapOf("ip" to targetIp))
            .retrieve()
            .awaitBodyOrNull<IpApiResponse>() 
            ?: throw IllegalStateException("Geolocation service returned an empty response")

        if (response.status != "success") {
            throw IllegalStateException(response.message ?: "Unable to resolve location for IP")
        }
        logger.info { "Thread IP service non-blocking: ${Thread.currentThread().name}" }

        return response.city ?: throw IllegalStateException("City information is missing in geolocation response")
    }

    suspend fun getUserCityAsync(ipAddress: String): String = withContext(Dispatchers.IO) {
        getUserCity(ipAddress)
    }
}

data class IpLocationResult(
    val query: String,
    val country: String,
    val region: String,
    val city: String,
    val zip: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val isp: String
)

data class IpApiResponse(
    val status: String? = null,
    val message: String? = null,
    val query: String? = null,
    val country: String? = null,
    val regionName: String? = null,
    val city: String? = null,
    val zip: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val timezone: String? = null,
    val isp: String? = null
)
