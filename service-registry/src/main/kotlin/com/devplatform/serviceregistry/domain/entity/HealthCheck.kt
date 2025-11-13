package com.devplatform.serviceregistry.domain.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(name = "health_checks")
class HealthCheck(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instance_id", nullable = false)
    var instance: ServiceInstance? = null,

    @Column(nullable = false)
    val timestamp: LocalDateTime = LocalDateTime.now(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: HealthStatus,

    @Column(name = "response_time_ms")
    var responseTimeMs: Int? = null,

    @Column(columnDefinition = "TEXT", name = "error_message")
    var errorMessage: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var details: Map<String, Any>? = null
) {
    fun isHealthy(): Boolean = status == HealthStatus.HEALTHY

    fun hasError(): Boolean = status == HealthStatus.ERROR || status == HealthStatus.UNHEALTHY

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HealthCheck) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String =
        "HealthCheck(id=$id, status=$status, timestamp=$timestamp, responseTimeMs=$responseTimeMs)"
}

enum class HealthStatus {
    HEALTHY,
    UNHEALTHY,
    TIMEOUT,
    ERROR
}
