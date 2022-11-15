package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import org.springframework.web.multipart.MultipartFile

/**
 * Given an url returns the key that is used to create a short URL.
 * When the url is created optional data may be added.
 *
 * **Note**: This is an example of functionality.
 */
interface CreateShortUrlCsvUseCase {
    fun create(file: MultipartFile, data: ShortUrlProperties): ShortUrl
}

/**
 * Implementation of [CreateShortUrlCsvUseCase].
 */
class CreateShortUrlCsvUseCaseImpl(private val shortUrlRepository: ShortUrlRepositoryService,
    private val validatorService: ValidatorService,
    private val hashService: HashService,
    private val csvService: CsvService
) : CreateShortUrlCsvUseCase {
    override fun create(file: MultipartFile, data: ShortUrlProperties): ShortUrl {
        val urls = csvService.uploadCsvFile(file)
        var out: Array<Array<String>> = arrayOf()
        var first = ShortUrl(
                hash = "error",
                redirection = Redirection(target = "error"),
                properties = ShortUrlProperties(
                        safe = data.safe,
                        ip = data.ip,
                        sponsor = data.sponsor
                ))
        urls.forEach {
            var short: Array<String> = arrayOf()
            short += it.url
            if (validatorService.isValid(it.url)) {
                val id: String = hashService.hasUrl(it.url)
                short += id
                val su = ShortUrl(
                        hash = id,
                        redirection = Redirection(target = it.url),
                        properties = ShortUrlProperties(
                                safe = data.safe,
                                ip = data.ip,
                                sponsor = data.sponsor
                        )
                )
                if (it.url.equals(urls[0])) first = su
                short += ""
                shortUrlRepository.save(su)
            }
            else {
                short += ""
                short += "Invalid URL"
                //throw InvalidUrlException(it.url)
            }
            out += short
        }
        csvService.writeCsvFile(out)
        return first
    }
}
