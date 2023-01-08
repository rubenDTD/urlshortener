package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import org.springframework.web.multipart.MultipartFile

/**
 * Given the name of a csv file with a URL per line generates another csv file
 * with the returns the key that is used to create a short URL.
 * When the url is created optional data may be added.
 *
 * **Note**: This is an example of functionality.
 */
interface CreateShortUrlCsvUseCase {
    fun create(file: MultipartFile, data: ShortUrlProperties): CsvResponse
}

/**
 * Implementation of [CreateShortUrlCsvUseCase].
 */
class CreateShortUrlCsvUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService,
    private val hashService: HashService,
    private val rabbitMQService: RMQService
) : CreateShortUrlCsvUseCase {
    @Throws(BadRequestException::class)
    override fun create(file: MultipartFile, data: ShortUrlProperties): CsvResponse {
        val ret = CsvResponse("", "")
        file.inputStream.bufferedReader().forEachLine {
            if(it == "throw") throw BadRequestException("Forced error for test")
            rabbitMQService.send(it, data.safe, data.ip, data.sponsor)
        }
        var found = false
        file.inputStream.bufferedReader().forEachLine {
            val hash = hashService.hasUrl(it)
            val shortUrl = shortUrlRepository.findByKey(hash)
            if(shortUrl != null) {
                if(!found) ret.hash = shortUrl.hash; found = true
                ret.csv += "$it,http://localhost:8080/${shortUrl.hash},\n"
            } else {
                ret.csv += "$it,,debe ser una URI http o https\n"
            }
        }
        return ret
    }
}
