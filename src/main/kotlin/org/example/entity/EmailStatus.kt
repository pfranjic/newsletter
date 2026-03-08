package org.example.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table(name = "email_status")
data class EmailStatus(
    @Id
    val id: Long? = null,
    @Column("email_id")
    val emailId: String = "",
    @Column("status")
    val status: String = "",
    @Column("created_at")
    val createdAt: Instant = Instant.now(),
)
