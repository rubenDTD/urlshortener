package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.ShortUrlRepositoryService
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import java.io.File
import java.io.InputStream
import java.nio.file.Paths


/**
 * Check if IP or URI are in the black list.
 *
 * **Note**: This is an example of functionality.
 */
interface BlackListUseCase {
    fun checkBlackList(key: String) : Boolean
    fun isSpam(key: String) : Boolean
}

/**
 * Implementation of [BlackListUseCase].
 */
class BlackListUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService
) : BlackListUseCase {
    override fun checkBlackList(key: String) : Boolean {
        val file = ClassPathResource("blackList.txt").inputStream
        val blackList = readList(file)
        return blackList.contains(key)
    }

    override fun isSpam(key: String): Boolean =
        shortUrlRepository.findByKey(key)?.properties?.spam ?: false
    private fun readList(file: InputStream) : List<String>
    = file.bufferedReader().readLines()
}
