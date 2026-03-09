package org.demo.repository

import org.demo.entity.EmailStatus
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface EmailStatusRepositoryNonBlocking : CoroutineCrudRepository<EmailStatus, Long> 