package com.devplatform.serviceregistry.domain.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(name = "audit_logs")
class AuditLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 50, name = "entity_type")
    val entityType: String,

    @Column(nullable = false, name = "entity_id")
    val entityId: Long,

    @Column(nullable = false, length = 50)
    val action: String,

    @Column(length = 100, name = "performed_by")
    val performedBy: String? = null,

    @Column(nullable = false)
    val timestamp: LocalDateTime = LocalDateTime.now(),

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "old_value")
    val oldValue: Map<String, Any>? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "new_value")
    val newValue: Map<String, Any>? = null,

    @Column(name = "ip_address")
    val ipAddress: String? = null,

    @Column(columnDefinition = "TEXT", name = "user_agent")
    val userAgent: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AuditLog) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String =
        "AuditLog(id=$id, entityType='$entityType', entityId=$entityId, action='$action', timestamp=$timestamp)"
}
