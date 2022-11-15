package es.unizar.urlshortener.core

class InvalidUrlException(val url: String) : Exception("[$url] does not follow a supported schema")

class RedirectionNotFound(val key: String) : Exception("[$key] is not known")

//@ResponseStatus(HttpStatus.BAD_REQUEST)
class BadRequestException(msg: String) : Exception(msg)
//@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
class CsvImportException(msg: String) : Exception(msg)
