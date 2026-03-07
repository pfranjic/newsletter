package org.example.userregistrationdemo.controllers

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.web.bind.annotation.*
import org.example.service.EmailStatusService

@RestController
@RequestMapping("/api/email")
class EmailController(
    private val mailSender: JavaMailSender,
    private val emailStatusService: EmailStatusService
) {

    @PostMapping("/send")
    fun sendEmail(@RequestBody request: EmailRequest): ResponseEntity<String> {
        if (request.to.isBlank() || request.subject.isBlank() || request.body.isBlank()) {
            return ResponseEntity.badRequest().body("to, subject, and body are required")
        }

        return try {
            val message = SimpleMailMessage().apply {
                setTo(request.to)
                subject = request.subject
                text = request.body
            }
            mailSender.send(message)
            ResponseEntity.ok("Email sent successfully")
        // Use EmailStatusService to save status after sending
        emailStatusService.saveEmailStatus(request.to, "SENT")
        ResponseEntity.ok("Email sent successfully")
    } catch (ex: Exception) {
        // Save failed status
        emailStatusService.saveEmailStatus(request.to, "FAILED")
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Failed to send email: ${ex.message}")
    }
    }
}

data class EmailRequest(
    val to: String,
    val subject: String,
    val body: String
)