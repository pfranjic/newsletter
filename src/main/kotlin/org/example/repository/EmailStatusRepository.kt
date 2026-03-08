package org.example.repository

import org.example.entity.EmailStatus
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface EmailStatusRepository : R2dbcRepository<EmailStatus, Long> {
    fun findByEmailId(emailId: String): Mono<EmailStatus>
    fun findByStatus(status: String): Flux<EmailStatus>
}
