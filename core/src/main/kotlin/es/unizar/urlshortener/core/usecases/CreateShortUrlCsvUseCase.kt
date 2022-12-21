package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.amqp.rabbit.core.RabbitTemplate

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
    private val validatorService: ValidatorService,
    private val hashService: HashService,
    //private val rabbitMQService: RMQService

) : CreateShortUrlCsvUseCase {
    override fun create(file: MultipartFile, data: ShortUrlProperties): CsvResponse {
        var first = ShortUrl(
                hash = "error",
                redirection = Redirection(target = "error"),
                properties = ShortUrlProperties(
                        safe = data.safe,
                        ip = data.ip,
                        sponsor = data.sponsor
                ))
        val ret = CsvResponse(first, "")
        var found = false
        var i = 0
        file.inputStream.bufferedReader().forEachLine {
            //rabbitMQService.send(i.toString(), it)
            ret.csv += it
            if (validatorService.isValid(it)) {
                val id = hashService.hasUrl(it)
                ret.csv += ",$id"
                val su = ShortUrl(
                        hash = id,
                        redirection = Redirection(target = it),
                        properties = ShortUrlProperties(
                                safe = data.safe,
                                ip = data.ip,
                                sponsor = data.sponsor
                        )
                )
                if (!found) {
                    first = su
                    found = true
                }
                ret.csv += ",\n"
                shortUrlRepository.save(su)
            }
            else {
                ret.csv += ",,Invalid URL\n"
            }
            i++
        }
        if(!found)
            first.hash = ""
        ret.shortUrl = first
        return ret
    }
}
