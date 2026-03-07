package org.example.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Service
class IpLocationService(
    private val restClientBuilder: RestClient.Builder,
    @Value("\${external.ip-geo-service.url}") private val ipGeoServiceUrl: String
) {

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

        return response.city ?: throw IllegalStateException("City information is missing in geolocation response")
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
