package org.example.repository

import org.example.entity.EmailStatus
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface EmailStatusRepositoryNonBlocking : CoroutineCrudRepository<EmailStatus, Long> {
    suspend fun findByEmailId(emailId: String): EmailStatus?
    suspend fun findByStatus(status: String): List<EmailStatus>
}
