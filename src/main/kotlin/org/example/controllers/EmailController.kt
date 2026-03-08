package org.example.controllers

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.example.service.EmailStatusService
import org.example.service.ExternalEmailService
import org.example.service.IpLocationService
import org.example.service.PdfGenerationService
import kotlin.system.measureTimeMillis

@RestController
@RequestMapping("/api/email")
class EmailController(
    private val externalEmailService: ExternalEmailService,
    private val ipLocationService: IpLocationService,
    private val pdfGenerationService: PdfGenerationService,
    private val emailStatusService: EmailStatusService
) {
    private val logger = KotlinLogging.logger{}
    @PostMapping("/send")
    fun sendEmail(@RequestBody request: EmailRequest): ResponseEntity<String> {
        if (request.to.isBlank()) {
            return ResponseEntity.badRequest().body("to, subject, and body are required")
        }

        return try {
            val timeMillis = measureTimeMillis {
                val location = ipLocationService.getUserCity(request.ip)
                val pdfContent = pdfGenerationService.generatePdf(
                    title = "Title",
                    body = "Body"
                )
                externalEmailService.sendEmail(request.to, pdfContent, location)
                //emailStatusService.saveEmailStatus(request.to, "SENT")
            }
            ResponseEntity.ok("Email sent successfully. Elapsed time $timeMillis ms")
        } catch (ex: Exception) {
            emailStatusService.saveEmailStatus(request.to, "FAILED")
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to send email: ${ex.message}")
        }
    }
    @PostMapping("/send-non-blocking")
    suspend fun sendEmailNonBlocking(@RequestBody request: EmailRequest): ResponseEntity<String> {
        if (request.to.isBlank()) {
            return ResponseEntity.badRequest().body("to, subject, and body are required")
        }

        return try {
            val timeMillis = measureTimeMillis {
                coroutineScope {
                    logger.info { "Thread coroutine scope: ${Thread.currentThread().name}" } // stays same

                    val location = async {
                        ipLocationService.getUserCityNonBlocking(request.ip)

                    }
                    val pdfContent = async{ pdfGenerationService.generatePdfNonBlocking(
                        title = "Title",
                        body = "Body"
                    )
                    }
                    logger.info { "Thread coroutine scope: ${Thread.currentThread().name}" } // stays same

                    externalEmailService.sendEmailNonBlocking(request.to, pdfContent.await(), location.await())
                    logger.info { "Thread coroutine scope: ${Thread.currentThread().name}" }

                }
            }
            ResponseEntity.ok("Email sent successfully. Elapsed time $timeMillis ms")
        } catch (ex: Exception) {
//            emailStatusService.saveEmailStatus(request.to, "FAILED")
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to send email: ${ex.message}")
        }
    }

    @PostMapping("/send-non-blocking2")
    suspend fun sendEmailNonBlocking2(@RequestBody request: EmailRequest): ResponseEntity<String> {
        if (request.to.isBlank()) {
            return ResponseEntity.badRequest().body("to, subject, and body are required")
        }

        return try {
            val timeMillis = measureTimeMillis {
                coroutineScope {
                    logger.info { "Thread coroutine scope: ${Thread.currentThread().name}" }

                    val location =  ipLocationService.getUserCityNonBlocking(request.ip)
                    val pdfContent =  pdfGenerationService.generatePdfNonBlocking(
                        title = "Title",
                        body = "Body"
                    )
                    externalEmailService.sendEmailNonBlocking(request.to, pdfContent, location)

                }
            }
            ResponseEntity.ok("Email sent successfully. Elapsed time $timeMillis ms")
        } catch (ex: Exception) {
//            emailStatusService.saveEmailStatus(request.to, "FAILED")
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to send email: ${ex.message}")
        }
    }
}

data class EmailRequest(
    val to: String,
    val ip: String
)