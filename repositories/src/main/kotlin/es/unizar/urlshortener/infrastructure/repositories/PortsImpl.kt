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
    override fun throwIfFileEmpty(file: MultipartFile) {
        if (file.isEmpty)
            throw BadRequestException("Empty file")
    }

    override fun createCSVToBean(fileReader: BufferedReader?): MutableList<User> {
        var csvReader: CSVReader? = null
        var aux: List<Array<String>>?
        val urls: MutableList<User> = mutableListOf()
        fileReader . use {
            try {
                csvReader = CSVReader(it)
                aux = csvReader!!.readAll()
                ((aux as MutableList<Array<String>>).forEach { iit ->
                    urls += User(iit[0])
                })
            } catch (e: Exception) {
                return urls
            } finally {
                csvReader!!.close()
            }
        }

        return urls
    }


    override fun closeFileReader(fileReader: BufferedReader?) {
        try {
            fileReader!!.close()
        } catch (ex: IOException) {
            throw CsvImportException("Error during csv import")
        }
    }

    override fun uploadCsvFile(file: MultipartFile): List<User> {
        throwIfFileEmpty(file)
        var fileReader: BufferedReader? = null
        try {
            fileReader = BufferedReader(InputStreamReader(file.inputStream))
            return createCSVToBean(fileReader)
        } catch (ex: Exception) {
            throw CsvImportException("Error during csv import")
        } finally {
            closeFileReader(fileReader)
        }
    }

    override fun writeCsvFile(urls: Array<Array<String>>) {
        FileOutputStream("salida.csv").use { fos ->
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { osw ->
                CSVWriter(osw).use { writer ->
                    urls.forEach {
                        writer.writeNext(
                            it
                        )
                    }
                }
            }
        }
    }
}

