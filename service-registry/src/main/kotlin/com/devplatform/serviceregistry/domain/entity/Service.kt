package com.devplatform.serviceregistry.domain.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "services")
class Service(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true, length = 100)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(nullable = false, length = 255, name = "base_url")
    var baseUrl: String,

    @Column(length = 50)
    var version: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ServiceStatus = ServiceStatus.UNKNOWN,

    @Column(nullable = false, name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false, name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(length = 100, name = "created_by")
    var createdBy: String? = null,

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null,

    @OneToMany(mappedBy = "service", cascade = [CascadeType.ALL], orphanRemoval = true)
    val instances: MutableList<ServiceInstance> = mutableListOf()
) {
    fun addInstance(instance: ServiceInstance) {
        instances.add(instance)
        instance.service = this
    }

    fun removeInstance(instance: ServiceInstance) {
        instances.remove(instance)
        instance.service = null
    }

    fun isDeleted(): Boolean = deletedAt != null

    fun softDelete() {
        deletedAt = LocalDateTime.now()
        status = ServiceStatus.DOWN
    }

    fun restore() {
        deletedAt = null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Service) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String =
        "Service(id=$id, name='$name', status=$status, baseUrl='$baseUrl')"
}

enum class ServiceStatus {
    UP,
    DOWN,
    DEGRADED,
    UNKNOWN
}
