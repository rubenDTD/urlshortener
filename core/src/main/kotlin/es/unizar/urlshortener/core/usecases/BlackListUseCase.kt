package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.ShortUrl
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
    suspend fun checkBlackList(ip: String, url: String, hash: String)
    fun isSpam(key: String) : Boolean
}

/**
 * Implementation of [BlackListUseCase].
 */
class BlackListUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService
) : BlackListUseCase {
    override suspend fun checkBlackList(ip: String, url: String, hash: String) {
        if (shouldBlock(ip) || shouldBlock(url)){
            shortUrlRepository.updateSpam(hash, true)
        }
        shortUrlRepository.updateProcessing(hash, false)
    }

    override fun isSpam(key: String): Boolean =
        shortUrlRepository.findByKey(key)?.properties?.spam ?: false
    private fun readList(file: InputStream) : List<String>
    = file.bufferedReader().readLines()

    private fun shouldBlock(key: String) : Boolean {
        val file = ClassPathResource("blackList.txt").inputStream
        val blackList = readList(file)
        return blackList.contains(key)
    }
}
