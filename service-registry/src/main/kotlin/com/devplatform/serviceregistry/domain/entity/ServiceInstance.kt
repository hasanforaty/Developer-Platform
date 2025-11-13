package com.devplatform.serviceregistry.domain.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(name = "service_instances")
class ServiceInstance(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    var service: Service? = null,

    @Column(nullable = false, unique = true, length = 100, name = "instance_id")
    var instanceId: String,

    @Column(nullable = false, length = 255)
    var host: String,

    @Column(nullable = false)
    var port: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: InstanceStatus = InstanceStatus.STARTING,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var metadata: Map<String, Any>? = null,

    @Column(name = "last_heartbeat")
    var lastHeartbeat: LocalDateTime? = null,

    @Column(nullable = false, name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false, name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "instance", cascade = [CascadeType.ALL], orphanRemoval = true)
    val healthChecks: MutableList<HealthCheck> = mutableListOf()
) {
    fun addHealthCheck(healthCheck: HealthCheck) {
        healthChecks.add(healthCheck)
        healthCheck.instance = this
    }

    fun updateHeartbeat() {
        lastHeartbeat = LocalDateTime.now()
    }

    fun isHealthy(): Boolean = status == InstanceStatus.UP

    fun markAsDown() {
        status = InstanceStatus.DOWN
    }

    fun markAsUp() {
        status = InstanceStatus.UP
    }

    fun getHealthCheckHistory(limit: Int = 10): List<HealthCheck> =
        healthChecks.sortedByDescending { it.timestamp }.take(limit)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ServiceInstance) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String =
        "ServiceInstance(id=$id, instanceId='$instanceId', host='$host', port=$port, status=$status)"
}

enum class InstanceStatus {
    STARTING,
    UP,
    DOWN,
    UNKNOWN
}
