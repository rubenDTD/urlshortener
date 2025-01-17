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
    private val validatorService: ValidatorService,
    private val rabbitMQService: RMQService
) : CreateShortUrlCsvUseCase {
    override fun create(file: MultipartFile, data: ShortUrlProperties): CsvResponse {
        val ret = CsvResponse("", "")
        var found = false
        file.inputStream.bufferedReader().forEachLine {
            if(validatorService.isValid(it)) {
                val hash = hashService.hasUrl(it)
                rabbitMQService.send(it, hash, data.safe, data.ip, data.sponsor)
                if(!found) { ret.hash = hash; found = true; }
                ret.csv += "$it,http://localhost:8080/$hash,\n"
            } else {
                ret.csv += "$it,,debe ser una URI http o https\n"
            }
        }
        return ret
    }
}
