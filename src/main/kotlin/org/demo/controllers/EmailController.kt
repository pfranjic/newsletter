package org.demo.controllers

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.demo.service.EmailStatusService
import org.demo.service.ExternalEmailService
import org.demo.service.IpLocationService
import org.demo.service.PdfGenerationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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

    companion object {
        private const val DEFAULT_PDF_TITLE = "Title"
        private const val DEFAULT_PDF_BODY = "Body"
        private const val STATUS_SENT = "SENT"
        private const val STATUS_FAILED = "FAILED"
        private const val REQUIRED_FIELDS_MESSAGE = "to, subject, and body are required"
        private const val SUCCESS_MESSAGE_TEMPLATE = "Email sent successfully. Elapsed time %d ms"
        private const val INVALID_REQUEST_MESSAGE = "Invalid email request"
        private const val UPSTREAM_FAILURE_MESSAGE = "Failed to process email with upstream services"
    }

    @PostMapping("/send-blocking")
    fun sendEmail(
        @RequestBody request: EmailRequest,
    ): ResponseEntity<String> {
        validateRequest(request)?.let { return it }

        return executeBlockingEmailFlow(
            onSuccess = { emailStatusService.saveEmailStatusBlocking(request.to, STATUS_SENT) },
            onFailure = { emailStatusService.saveEmailStatusBlocking(request.to, STATUS_FAILED) },
        ) {
            val location = ipLocationService.getUserCity(request.ip)
            val pdfContent =
                pdfGenerationService.generatePdf(
                    title = DEFAULT_PDF_TITLE,
                    body = DEFAULT_PDF_BODY,
                )
            externalEmailService.sendEmail(request.to, pdfContent, location)
        }
    }

    @PostMapping("/send-non-blocking")
    suspend fun sendEmailNonBlocking(
        @RequestBody request: EmailRequest,
    ): ResponseEntity<String> {
        validateRequest(request)?.let { return it }

        return executeSuspendEmailFlow(
            onSuccess = { emailStatusService.saveEmailStatusNonBlocking(request.to, STATUS_SENT) },
            onFailure = { emailStatusService.saveEmailStatusNonBlocking(request.to, STATUS_FAILED) },
        ) {
            sendEmailNonBlocking(request, concurrent = true)
        }
    }

    @PostMapping("/send-non-blocking-sequential")
    suspend fun sendEmailNonBlockingSequential(
        @RequestBody request: EmailRequest,
    ): ResponseEntity<String> {
        validateRequest(request)?.let { return it }

        return executeSuspendEmailFlow(
            onSuccess = { emailStatusService.saveEmailStatusNonBlocking(request.to, STATUS_SENT) },
            onFailure = { emailStatusService.saveEmailStatusNonBlocking(request.to, STATUS_FAILED) },
        ) {
            sendEmailNonBlocking(request, concurrent = false)
        }
    }
    

    private inline fun executeBlockingEmailFlow(
        onSuccess: () -> Unit,
        onFailure: () -> Unit,
        action: () -> Unit,
    ): ResponseEntity<String> =
        try {
            val timeMillis = measureTimeMillis { action() }
            onSuccess()
            successResponse(timeMillis)
        } catch (ex: Exception) {
            onFailure()
            mapExceptionToResponse(ex)
        }

    private suspend inline fun executeSuspendEmailFlow(
        onSuccess: suspend () -> Unit,
        onFailure: suspend () -> Unit,
        action: suspend () -> Unit,
    ): ResponseEntity<String> =
        try {
            val timeMillis = measureTimeMillis { action() }
            onSuccess()
            successResponse(timeMillis)
        } catch (ex: Exception) {
            onFailure()
            mapExceptionToResponse(ex)
        }

    private suspend fun sendEmailNonBlocking(
        request: EmailRequest,
        concurrent: Boolean,
    ) {
        val mode = if (concurrent) "async" else "sequential"

        coroutineScope {
            logger.info { "Thread coroutine scope $mode: ${Thread.currentThread().name}" }

            val (pdfContent, location) =
                if (concurrent) {
                    val locationDeferred = async(Dispatchers.IO) { ipLocationService.getUserCityNonBlocking(request.ip) }
                    val pdfDeferred =
                        async(Dispatchers.IO) {
                            pdfGenerationService.generatePdfNonBlocking(
                                title = DEFAULT_PDF_TITLE,
                                body = DEFAULT_PDF_BODY,
                            )
                        }
                    Pair(pdfDeferred.await(), locationDeferred.await())
                } else {
                    val location = withContext(Dispatchers.IO) { ipLocationService.getUserCityNonBlocking(request.ip) }
                    val pdf =
                        withContext(Dispatchers.IO) {
                            pdfGenerationService.generatePdfNonBlocking(
                                title = DEFAULT_PDF_TITLE,
                                body = DEFAULT_PDF_BODY,
                            )
                        }
                    Pair(pdf, location)
                }

            logger.info { "Thread coroutine scope $mode: ${Thread.currentThread().name}" }
            withContext(Dispatchers.IO) {
                externalEmailService.sendEmailNonBlocking(request.to, pdfContent, location)
            }
        }
    }

    private fun successResponse(timeMillis: Long): ResponseEntity<String> =
        ResponseEntity.ok(SUCCESS_MESSAGE_TEMPLATE.format(timeMillis))

    private fun validateRequest(request: EmailRequest): ResponseEntity<String>? {
        if (request.to.isBlank()) {
            return ResponseEntity.badRequest().body(REQUIRED_FIELDS_MESSAGE)
        }

        return null
    }

    private fun mapExceptionToResponse(ex: Exception): ResponseEntity<String> =
        when (ex) {
            is IllegalArgumentException ->
                ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(INVALID_REQUEST_MESSAGE)

            is IllegalStateException ->
                ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .body(UPSTREAM_FAILURE_MESSAGE)

            else -> throw ex
        }
}

data class EmailRequest(
    val to: String,
    val ip: String,
)
