package com.devplatform.serviceregistry.repository

import com.devplatform.serviceregistry.domain.entity.Service
import com.devplatform.serviceregistry.domain.entity.ServiceStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ServiceRepository : JpaRepository<Service, Long> {

    fun findByName(name: String): Service?

    fun findByDeletedAtIsNull(): List<Service>

    fun findByDeletedAtIsNullAndStatus(status: ServiceStatus): List<Service>

    fun findByDeletedAtIsNullOrderByNameAsc(): List<Service>

    fun existsByNameAndDeletedAtIsNull(name: String): Boolean

    @Query("SELECT s FROM Service s WHERE s.deletedAt IS NULL AND s.name LIKE %:searchTerm%")
    fun searchByName(@Param("searchTerm") searchTerm: String): List<Service>

    @Query("""
        SELECT COUNT(s) FROM Service s
        WHERE s.deletedAt IS NULL AND s.status = :status
    """)
    fun countByStatus(@Param("status") status: ServiceStatus): Long

    @Query("""
        SELECT COUNT(s) FROM Service s
        WHERE s.deletedAt IS NULL
    """)
    fun countActive(): Long

    @Query("""
        SELECT s FROM Service s
        LEFT JOIN FETCH s.instances i
        WHERE s.deletedAt IS NULL AND s.id = :id
    """)
    fun findByIdWithInstances(@Param("id") id: Long): Service?

    @Query("""
        SELECT s FROM Service s
        WHERE s.deletedAt IS NULL
        AND s.updatedAt < :threshold
    """)
    fun findStaleServices(@Param("threshold") threshold: LocalDateTime): List<Service>
}
