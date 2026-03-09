package org.example.controllers

import kotlinx.coroutines.runBlocking
import org.example.service.EmailStatusService
import org.example.service.ExternalEmailService
import org.example.service.IpLocationService
import org.example.service.PdfGenerationService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.http.HttpStatus

class EmailControllerTest {
    private lateinit var externalEmailService: ExternalEmailService
    private lateinit var ipLocationService: IpLocationService
    private lateinit var pdfGenerationService: PdfGenerationService
    private lateinit var emailStatusService: EmailStatusService
    private lateinit var controller: EmailController

    @BeforeEach
    fun setUp() {
        externalEmailService = mock(ExternalEmailService::class.java)
        ipLocationService = mock(IpLocationService::class.java)
        pdfGenerationService = mock(PdfGenerationService::class.java)
        emailStatusService = mock(EmailStatusService::class.java)

        controller =
            EmailController(
                externalEmailService = externalEmailService,
                ipLocationService = ipLocationService,
                pdfGenerationService = pdfGenerationService,
                emailStatusService = emailStatusService,
            )
    }

    @Test
    fun `sendEmail should return bad request when to is blank`() {
        val response = controller.sendEmail(EmailRequest(to = " ", ip = "1.1.1.1"))

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("to, subject, and body are required", response.body)
        verifyNoInteractions(ipLocationService, pdfGenerationService, externalEmailService, emailStatusService)
    }

    @Test
    fun `sendEmail should return ok and mark sent on success`() {
        val request = EmailRequest(to = "alice@example.com", ip = "1.1.1.1")
        val pdf = byteArrayOf(1, 2, 3)

        `when`(ipLocationService.getUserCity(request.ip)).thenReturn("Zagreb")
        `when`(pdfGenerationService.generatePdf("Title", "Body")).thenReturn(pdf)

        val response = controller.sendEmail(request)

        assertEquals(HttpStatus.OK, response.statusCode)
        verify(externalEmailService).sendEmail(request.to, pdf, "Zagreb")
        verify(emailStatusService).saveEmailStatusBlocking(request.to, "SENT")
        verify(emailStatusService, never()).saveEmailStatusBlocking(request.to, "FAILED")
    }

    @Test
    fun `sendEmail should return bad gateway and mark failed on upstream error`() {
        val request = EmailRequest(to = "alice@example.com", ip = "1.1.1.1")

        `when`(ipLocationService.getUserCity(request.ip)).thenThrow(IllegalStateException("upstream down"))

        val response = controller.sendEmail(request)

        assertEquals(HttpStatus.BAD_GATEWAY, response.statusCode)
        assertEquals("Failed to process email with upstream services", response.body)
        verify(emailStatusService).saveEmailStatusBlocking(request.to, "FAILED")
        verify(emailStatusService, never()).saveEmailStatusBlocking(request.to, "SENT")
    }

    @Test
    fun `sendEmailNonBlocking should return bad request when to is blank`() {
        val response =
            runBlocking {
                controller.sendEmailNonBlocking(EmailRequest(to = "", ip = "1.1.1.1"))
            }

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("to, subject, and body are required", response.body)
        verifyNoInteractions(ipLocationService, pdfGenerationService, externalEmailService, emailStatusService)
    }

    @Test
    fun `sendEmailNonBlocking should return ok and mark sent on success`() {
        val request = EmailRequest(to = "bob@example.com", ip = "8.8.8.8")
        val pdf = byteArrayOf(7, 8, 9)

        runBlocking {
            `when`(ipLocationService.getUserCityNonBlocking(request.ip)).thenReturn("Split")
            `when`(pdfGenerationService.generatePdfNonBlocking("Title", "Body")).thenReturn(pdf)
        }

        val response = runBlocking { controller.sendEmailNonBlocking(request) }

        assertEquals(HttpStatus.OK, response.statusCode)
        runBlocking {
            verify(externalEmailService).sendEmailNonBlocking(request.to, pdf, "Split")
            verify(emailStatusService).saveEmailStatusNonBlocking(request.to, "SENT")
            verify(emailStatusService, never()).saveEmailStatusNonBlocking(request.to, "FAILED")
        }
    }

    @Test
    fun `sendEmailNonBlocking should return bad gateway and mark failed on upstream error`() {
        val request = EmailRequest(to = "bob@example.com", ip = "8.8.8.8")
        val pdf = byteArrayOf(1)

        runBlocking {
            `when`(ipLocationService.getUserCityNonBlocking(request.ip)).thenReturn("Split")
            `when`(pdfGenerationService.generatePdfNonBlocking("Title", "Body")).thenReturn(pdf)
            doThrow(IllegalStateException("upstream down"))
                .`when`(externalEmailService)
                .sendEmailNonBlocking(request.to, pdf, "Split")
        }

        val response = runBlocking { controller.sendEmailNonBlocking(request) }

        assertEquals(HttpStatus.BAD_GATEWAY, response.statusCode)
        assertEquals("Failed to process email with upstream services", response.body)
        runBlocking {
            verify(emailStatusService).saveEmailStatusNonBlocking(request.to, "FAILED")
            verify(emailStatusService, never()).saveEmailStatusNonBlocking(request.to, "SENT")
        }
    }

    @Test
    fun `sendEmailNonBlockingSequential should return ok and mark sent on success`() {
        val request = EmailRequest(to = "carol@example.com", ip = "9.9.9.9")
        val pdf = byteArrayOf(4, 5, 6)

        runBlocking {
            `when`(ipLocationService.getUserCityNonBlocking(request.ip)).thenReturn("Rijeka")
            `when`(pdfGenerationService.generatePdfNonBlocking("Title", "Body")).thenReturn(pdf)
        }

        val response = runBlocking { controller.sendEmailNonBlocking2(request) }

        assertEquals(HttpStatus.OK, response.statusCode)
        runBlocking {
            verify(externalEmailService).sendEmailNonBlocking(request.to, pdf, "Rijeka")
            verify(emailStatusService).saveEmailStatusNonBlocking(request.to, "SENT")
            verify(emailStatusService, never()).saveEmailStatusNonBlocking(request.to, "FAILED")
        }
    }

    @Test
    fun `sendEmailNonBlockingSequential should return bad gateway and mark failed on upstream error`() {
        val request = EmailRequest(to = "carol@example.com", ip = "9.9.9.9")

        runBlocking {
            `when`(ipLocationService.getUserCityNonBlocking(request.ip)).thenThrow(IllegalStateException("upstream down"))
        }

        val response = runBlocking { controller.sendEmailNonBlocking2(request) }

        assertEquals(HttpStatus.BAD_GATEWAY, response.statusCode)
        assertEquals("Failed to process email with upstream services", response.body)
        runBlocking {
            verify(emailStatusService).saveEmailStatusNonBlocking(request.to, "FAILED")
            verify(emailStatusService, never()).saveEmailStatusNonBlocking(request.to, "SENT")
        }
    }
}
