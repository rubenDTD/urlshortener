package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.ClickRepositoryService
import ru.chermenin.ua.UserAgent

interface HeadersInfoUseCase {

    fun getHeaderProperties(uaString: String, id: Long)
}

class HeadersInfoUseCaseImpl(
    private val clickRepository: ClickRepositoryService
) : HeadersInfoUseCase {
    override fun getHeaderProperties(uaString: String, id: Long) {
        val ua = UserAgent.parse(uaString)

    }

}