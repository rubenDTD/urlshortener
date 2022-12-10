package es.unizar.urlshortener.core

import org.springframework.web.multipart.MultipartFile
import java.io.BufferedReader

/**
 * [ClickRepositoryService] is the port to the repository that provides persistence to [Clicks][Click].
 */
interface ClickRepositoryService {
    fun save(cl: Click): Click
    fun summary(key: String): List<Click>
}

/**
 * [ShortUrlRepositoryService] is the port to the repository that provides management to [ShortUrl][ShortUrl].
 */
interface ShortUrlRepositoryService {
    fun findByKey(id: String): ShortUrl?
    fun save(su: ShortUrl): ShortUrl
}

interface CsvService {
    fun throwIfFileEmpty(file: MultipartFile)
    fun createCSVToBean(fileReader: BufferedReader?): List<User>
    fun closeFileReader(fileReader: BufferedReader?)
    fun uploadCsvFile(file: MultipartFile): List<User>
    fun writeCsvFile(urls: Array<Array<String>>)
}

/**
 * [ValidatorService] is the port to the service that validates if an url can be shortened.
 *
 * **Note**: It is a design decision to create this port. It could be part of the core .
 */
interface ValidatorService {
    fun isValid(url: String): Boolean
}

/**
 * [HashService] is the port to the service that creates a hash from a URL.
 *
 * **Note**: It is a design decision to create this port. It could be part of the core .
 */
interface HashService {
    fun hasUrl(url: String): String
}