package es.unizar.urlshortener.infrastructure.repositories

import es.unizar.urlshortener.core.Click
import es.unizar.urlshortener.core.ClickRepositoryService
import es.unizar.urlshortener.core.ShortUrl
import es.unizar.urlshortener.core.ShortUrlRepositoryService

/**
 * Implementation of the port [ClickRepositoryService].
 */
class ClickRepositoryServiceImpl(
    private val clickEntityRepository: ClickEntityRepository
) : ClickRepositoryService {
    override fun save(cl: Click): Click = clickEntityRepository.save(cl.toEntity()).toDomain()

    override fun summary(key: String): List<Click> {
        val clicks = clickEntityRepository.findByHash(key)
        val clicksDom = mutableListOf<Click>()
        for (click in clicks) {
            clicksDom.add(click.toDomain())
        }
        return clicksDom
    }
}

/**
 * Implementation of the port [ShortUrlRepositoryService].
 */
class ShortUrlRepositoryServiceImpl(
    private val shortUrlEntityRepository: ShortUrlEntityRepository
) : ShortUrlRepositoryService {
    override fun findByKey(id: String): ShortUrl? = shortUrlEntityRepository.findByHash(id)?.toDomain()

    override fun save(su: ShortUrl): ShortUrl = shortUrlEntityRepository.save(su.toEntity()).toDomain()
}
