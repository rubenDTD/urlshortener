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
class CreateShortUrlCsvUseCaseImpl(private val shortUrlRepository: ShortUrlRepositoryService,
    private val validatorService: ValidatorService,
    private val hashService: HashService,
    private val csvService: CsvService
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

        file.inputStream.bufferedReader().forEachLine {
            ret.csv += it
            if (validatorService.isValid(it)) {
                val id: String = hashService.hasUrl(it)
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
            //csvService.writeCsvFile(short)
        }
        if(!found)
            first.hash = ""
        ret.shortUrl = first
        return ret
    }
}
