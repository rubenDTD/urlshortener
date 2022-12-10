package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.ShortUrlRepositoryService
import java.io.File


/**
 * Check if IP or URI are in the black list.
 *
 * **Note**: This is an example of functionality.
 */
interface BlackListUseCase {
    fun checkBlackList(key: String) : Boolean
    fun checkSpam(key: String) : Boolean
}

/**
 * Implementation of [BlackListUseCase].
 */
class BlackListUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService
) : BlackListUseCase {
    override fun checkBlackList(key: String) : Boolean {
        val blackList = readList("./app/blackList.txt")
        return blackList.contains(key)
    }

    override fun checkSpam(key: String): Boolean =
        shortUrlRepository.findByKey(key)?.properties?.spam ?: false
    private fun readList(filename: String) : List<String>
    = File(filename).useLines{ it.toList() }
}
