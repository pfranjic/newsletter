package org.demo.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.demo.entity.EmailStatus
import org.demo.repository.EmailStatusRepositoryNonBlocking
import org.springframework.stereotype.Service

@Service
class EmailStatusService(
    private val repository: EmailStatusRepositoryNonBlocking,
) {
    fun saveEmailStatusBlocking(
        emailId: String,
        status: String,
    ) {
        val emailStatus = EmailStatus(emailId = emailId, status = status)
        runBlocking { repository.save(emailStatus) }
    }

    suspend fun saveEmailStatusAsync(
        emailId: String,
        status: String,
    ) = withContext(Dispatchers.IO) {
        saveEmailStatusBlocking(emailId, status)
    }

    suspend fun saveEmailStatusNonBlocking(
        emailId: String,
        status: String,
    ) {
        val emailStatus = EmailStatus(emailId = emailId, status = status)
        repository.save(emailStatus)
    }
}
