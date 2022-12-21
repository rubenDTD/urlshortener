package es.unizar.urlshortener.infrastructure.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import javax.transaction.Transactional

/**
 * Specification of the repository of [ShortUrlEntity].
 *
 * **Note**: Spring Boot is able to discover this [JpaRepository] without further configuration.
 */
interface ShortUrlEntityRepository : JpaRepository<ShortUrlEntity, String> {
    fun findByHash(hash: String): ShortUrlEntity?

    @Transactional
    @Modifying
    @Query(value = "update ShortUrlEntity s set s.spam = ?2 where s.hash = ?1")
    fun updateSpam(hash: String, data: Boolean)

    @Transactional
    @Modifying
    @Query(value = "update ShortUrlEntity s set s.processing = ?2 where s.hash = ?1")
    fun updateProcessing(hash: String, data: Boolean)
}

/**
 * Specification of the repository of [ClickEntity].
 *
 * **Note**: Spring Boot is able to discover this [JpaRepository] without further configuration.
 */
interface ClickEntityRepository : JpaRepository<ClickEntity, Long> {
    fun findByHash(hash: String): List<ClickEntity>

    @Transactional
    @Modifying
    @Query(value = "update ClickEntity c set c.platform = ?3, c.browser = ?2 where c.hash = ?1")
    fun update(hash: String, browser: String, platform: String)
}