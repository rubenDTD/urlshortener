package es.unizar.urlshortener.infrastructure.repositories

import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import es.unizar.urlshortener.core.*
import org.springframework.web.multipart.MultipartFile
import java.io.*
import java.nio.charset.StandardCharsets

/**
 * Implementation of the port [ClickRepositoryService].
 */
class ClickRepositoryServiceImpl(
    private val clickEntityRepository: ClickEntityRepository
) : ClickRepositoryService {
    override fun save(cl: Click): Click = clickEntityRepository.save(cl.toEntity()).toDomain()

    override fun summary(key: String): List<Click> {
        val clicks = clickEntityRepository.findByHash(key)
        val clicksDom = mutableListOf<Click>()
        for (click in clicks) {
            clicksDom.add(click.toDomain())
        }
        return clicksDom
    }
}

/**
 * Implementation of the port [ShortUrlRepositoryService].
 */
class ShortUrlRepositoryServiceImpl(
    private val shortUrlEntityRepository: ShortUrlEntityRepository
) : ShortUrlRepositoryService {
    override fun findByKey(id: String): ShortUrl? = shortUrlEntityRepository.findByHash(id)?.toDomain()

    override fun save(su: ShortUrl): ShortUrl = shortUrlEntityRepository.save(su.toEntity()).toDomain()
}

class CsvServiceImpl : CsvService {
    override fun writeCsvFile(url: String) {
        FileOutputStream("salida.csv",true).use { fos ->
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { osw ->
                osw.write(url)
            }
        }
    }
}

