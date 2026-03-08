package org.example.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.entity.EmailStatus
import org.example.repository.EmailStatusRepository
import org.springframework.stereotype.Service

@Service
class EmailStatusService(
    private val repository: EmailStatusRepository,
    ) {

    fun saveEmailStatus(emailId: String, status: String) {
        val emailStatus = EmailStatus(emailId = emailId, status = status)
        repository.save(emailStatus).block()
    }

    suspend fun saveEmailStatusAsync(emailId: String, status: String) = withContext(Dispatchers.IO) {
        saveEmailStatus(emailId, status)
    }
}