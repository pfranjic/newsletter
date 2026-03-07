package org.example.repository

import org.example.entity.EmailStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EmailStatusRepository : JpaRepository<EmailStatus, Long> {
    fun findByEmailId(emailId: String): EmailStatus?
    fun findByStatus(status: String): List<EmailStatus>
}
