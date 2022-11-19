package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.Click
import es.unizar.urlshortener.core.ClickRepositoryService

interface InfoSummaryUseCase {
    fun summary(key: String): List<Click>
}

class InfoSummaryUseCaseImpl(
    private val clickRepository: ClickRepositoryService
) : InfoSummaryUseCase {
    override fun summary(key: String): List<Click> {
        return clickRepository.summary(key)
    }
}
