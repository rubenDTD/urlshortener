package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.ClickRepositoryService
import ru.chermenin.ua.UserAgent

interface HeadersInfoUseCase {

    suspend fun getBrowserAndPlatform(uaString: String, id: String)
}

class HeadersInfoUseCaseImpl(
    private val clickRepository: ClickRepositoryService
) : HeadersInfoUseCase {
    override suspend fun getBrowserAndPlatform(uaString: String, hash: String) {
        val ua = UserAgent.parse(uaString)
        clickRepository.updateBrowser(hash, ua.browser.toString())
        clickRepository.updatePlatform(hash, ua.os.toString())
    }
}