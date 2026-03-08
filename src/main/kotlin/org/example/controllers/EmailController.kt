package org.example.controllers

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.example.service.EmailStatusService
import org.example.service.ExternalEmailService
import org.example.service.IpLocationService
import org.example.service.PdfGenerationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestClientException
import org.springframework.web.reactive.function.client.WebClientException
import org.springframework.web.bind.annotation.*
import kotlin.system.measureTimeMillis

@RestController
@RequestMapping("/api/email")
class EmailController(
    private val externalEmailService: ExternalEmailService,
    private val ipLocationService: IpLocationService,
    private val pdfGenerationService: PdfGenerationService,
    private val emailStatusService: EmailStatusService,
) {
    private val logger = KotlinLogging.logger {}

    @PostMapping("/send-blocking")
    fun sendEmail(
        @RequestBody request: EmailRequest,
    ): ResponseEntity<String> {
        if (request.to.isBlank()) {
            return ResponseEntity.badRequest().body("to, subject, and body are required")
        }

        return try {
            val timeMillis =
                measureTimeMillis {
                    val location = ipLocationService.getUserCity(request.ip)
                    val pdfContent =
                        pdfGenerationService.generatePdf(
                            title = "Title",
                            body = "Body",
                        )
                    externalEmailService.sendEmail(request.to, pdfContent, location)
                    emailStatusService.saveEmailStatusBlocking(request.to, "SENT")
                }
            ResponseEntity.ok("Email sent successfully. Elapsed time $timeMillis ms")
        } catch (ex: IllegalArgumentException) {
            emailStatusService.saveEmailStatusBlocking(request.to, "FAILED")
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body("Invalid email request")
        } catch (ex: IllegalStateException) {
            emailStatusService.saveEmailStatusBlocking(request.to, "FAILED")
            ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body("Failed to process email with upstream services")
        } catch (ex: RestClientException) {
            emailStatusService.saveEmailStatusBlocking(request.to, "FAILED")
            ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body("Failed to reach upstream email services")
        }
    }

    @PostMapping("/send-non-blocking")
    suspend fun sendEmailNonBlocking(
        @RequestBody request: EmailRequest,
    ): ResponseEntity<String> {
        if (request.to.isBlank()) {
            return ResponseEntity.badRequest().body("to, subject, and body are required")
        }

        return try {
            val timeMillis =
                measureTimeMillis {
                    coroutineScope {
                        logger.info { "Thread coroutine scope: ${Thread.currentThread().name}" } // stays same

                        val location =
                            async {
                                ipLocationService.getUserCityNonBlocking(request.ip)
                            }
                        val pdfContent =
                            async {
                                pdfGenerationService.generatePdfNonBlocking(
                                    title = "Title",
                                    body = "Body",
                                )
                            }
                        logger.info { "Thread coroutine scope: ${Thread.currentThread().name}" } // stays same

                        externalEmailService.sendEmailNonBlocking(request.to, pdfContent.await(), location.await())
                        logger.info { "Thread coroutine scope: ${Thread.currentThread().name}" }
                        emailStatusService.saveEmailStatusNonBlocking(request.to, "SENT")
                    }
                }
            ResponseEntity.ok("Email sent successfully. Elapsed time $timeMillis ms")
        } catch (ex: IllegalArgumentException) {
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body("Invalid email request")
        } catch (ex: IllegalStateException) {
            ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body("Failed to process email with upstream services")
        } catch (ex: WebClientException) {
            ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body("Failed to reach upstream email services")
        }
    }

    @PostMapping("/send-non-blocking-sequential")
    suspend fun sendEmailNonBlocking2(
        @RequestBody request: EmailRequest,
    ): ResponseEntity<String> {
        if (request.to.isBlank()) {
            return ResponseEntity.badRequest().body("to, subject, and body are required")
        }

        return try {
            val timeMillis =
                measureTimeMillis {
                    coroutineScope {
                        logger.info { "Thread coroutine scope: ${Thread.currentThread().name}" }

                        val location = ipLocationService.getUserCityNonBlocking(request.ip)
                        val pdfContent =
                            pdfGenerationService.generatePdfNonBlocking(
                                title = "Title",
                                body = "Body",
                            )
                        externalEmailService.sendEmailNonBlocking(request.to, pdfContent, location)
                        emailStatusService.saveEmailStatusNonBlocking(request.to, "SENT")
                    }
                }
            ResponseEntity.ok("Email sent successfully. Elapsed time $timeMillis ms")
        } catch (ex: IllegalArgumentException) {
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body("Invalid email request")
        } catch (ex: IllegalStateException) {
            ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body("Failed to process email with upstream services")
        } catch (ex: WebClientException) {
            ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body("Failed to reach upstream email services")
        }
    }
}

data class EmailRequest(
    val to: String,
    val ip: String,
)
