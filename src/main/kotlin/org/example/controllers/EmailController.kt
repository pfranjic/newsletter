package org.example.controllers

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.example.service.EmailStatusService
import org.example.service.ExternalEmailService

@RestController
@RequestMapping("/api/email")
class EmailController(
    private val externalEmailService: ExternalEmailService,
    private val ipLocationService: IpLocationService,
    private val pdfGenerationService: PdfGenerationService,
    private val emailStatusService: EmailStatusService
) {

    @PostMapping("/send")
    fun sendEmail(@RequestBody request: EmailRequest): ResponseEntity<String> {
        if (request.to.isBlank() || request.subject.isBlank() || request.body.isBlank()) {
            return ResponseEntity.badRequest().body("to, subject, and body are required")
        }

        return try {
            val location = ipLocationService.getUserLocation(request.to)
            val pdfContent = pdfGenerationService.generatePdf(
                title = "Title",
                body = "Body"
            )
            externalEmailService.sendEmail(request.to, pdfContent, location)
            emailStatusService.saveEmailStatus(request.to, "SENT")
            ResponseEntity.ok("Email sent successfully")
        } catch (ex: Exception) {
            emailStatusService.saveEmailStatus(request.to, "FAILED")
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to send email: ${ex.message}")
        }
    }
}

data class EmailRequest(
    val to: String,

)