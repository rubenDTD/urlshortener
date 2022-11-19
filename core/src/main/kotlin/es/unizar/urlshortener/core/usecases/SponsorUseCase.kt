package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.ShortUrlRepositoryService

interface SponsorUseCase {
    fun hasSponsor(key: String): Boolean
}

class SponsorUseCaseImpl (
    private val shortUrlRepository: ShortUrlRepositoryService
    ) : SponsorUseCase {
    override fun hasSponsor(key: String): Boolean = shortUrlRepository
        .findByKey(key)
        ?.properties
        ?.sponsor != null
}
