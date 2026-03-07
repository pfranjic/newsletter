package org.example.service

import org.example.repository.EmailStatusRepository
import org.example.model.EmailStatus

class EmailStatusService(private val repository: EmailStatusRepository) {

    fun saveEmailStatus(emailId: String, status: String) {
        val emailStatus = EmailStatus(emailId = emailId, status = status)
        repository.save(emailStatus)
    }

    fun getEmailStatus(emailId: String): EmailStatus? {
        return repository.findByEmailId(emailId)
    }
}