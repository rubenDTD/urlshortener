package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.Click
import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.usecases.*
import jdk.jfr.ContentType
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.net.URI
import javax.servlet.http.HttpServletRequest


/**
 * The specification of the controller.
 */
interface UrlShortenerController {

    /**
     * Redirects and logs a short url identified by its [id].
     *
     * **Note**: Delivery of use cases [RedirectUseCase] and [LogClickUseCase].
     */
    fun redirectTo(id: String, request: HttpServletRequest): ResponseEntity<Void>

    /**
     * Creates a short url from details provided in [data].
     *
     * **Note**: Delivery of use case [CreateShortUrlUseCase].
     */
    fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut>


    /**
     * Returns a summary with users and clicks associated with a URI identified by [id]
     *
     * **Note**: Delivery of use case [InfoSummaryUseCase].
     */
    fun summary(id: String): ResponseEntity<SummaryDataOut>

    /**
     * ... [id].
     * ... [model]
     *
     * **Note**: Delivery of use case [any].
     */
    fun banner(@PathVariable id: String, request: HttpServletRequest,model: Model): Any

    /**
     * Creates a short url from details provided in [file].
     *
     * **Note**: Delivery of use case [ShortUrlDataOut].
     */
    fun csv(@RequestParam("file") file: MultipartFile, request: HttpServletRequest): ResponseEntity<Any>
}

/**
 * Data required to create a short url.
 */
data class ShortUrlDataIn(
    val url: String,
    val sponsor: String? = null
)

/**
 * Data returned after the creation of a short url.
 */
data class ShortUrlDataOut(
    val url: URI? = null,
    val properties: Map<String, Any> = emptyMap()
)

data class SummaryDataOut(
    val clicks: List<Click>
)


/**
 * The implementation of the controller.
 *
 * **Note**: Spring Boot is able to discover this [RestController] without further configuration.
 */
@Controller
class UrlShortenerControllerImpl(
    val redirectUseCase: RedirectUseCase,
    val logClickUseCase: LogClickUseCase,
    val createShortUrlUseCase: CreateShortUrlUseCase,
    val infoSummaryUseCase: InfoSummaryUseCase,
    val sponsorUseCase: SponsorUseCase,
    val createShortUrlCsvUseCase: CreateShortUrlCsvUseCase
) : UrlShortenerController {

    @GetMapping("/api/banner/{id}")
    override fun banner(@PathVariable id: String, request: HttpServletRequest, model: Model): Any {
        val redirection = redirectUseCase.redirectTo(id)
        logClickUseCase.logClick(id, ClickProperties(ip = request.remoteAddr))
        return if(sponsorUseCase.hasSponsor(id)) {
            model.addAttribute("uri", redirection.target)
            "banner"
        } else {
            val h = HttpHeaders()
            h.location = URI.create(redirection.target)
            ResponseEntity<Void>(h, HttpStatus.valueOf(redirection.mode))
        }
    }

    @GetMapping("/{id:(?!api|index).*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<Void> =
        redirectUseCase.redirectTo(id).let {
            logClickUseCase.logClick(id, ClickProperties(ip = request.remoteAddr))
            val h = HttpHeaders()
            h.location = URI.create(it.target)
            ResponseEntity<Void>(h, HttpStatus.valueOf(it.mode))
        }

    @PostMapping("/api/link", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    override fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut> =
        createShortUrlUseCase.create(
            url = data.url,
            data = ShortUrlProperties(
                ip = request.remoteAddr,
                sponsor = data.sponsor
            )
        ).let {
            val h = HttpHeaders()
            val url = linkTo<UrlShortenerControllerImpl> { redirectTo(it.hash, request) }.toUri()
            h.location = url
            val response = ShortUrlDataOut(
                url = url,
                properties = mapOf(
                    "safe" to it.properties.safe
                )
            )
            ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.CREATED)
        }

    @GetMapping("/api/link/{id}")
    override fun summary(@PathVariable id: String): ResponseEntity<SummaryDataOut> =
        infoSummaryUseCase.summary(
            key = id
        ).let {
            val response = SummaryDataOut(
                clicks = it
            )
            ResponseEntity<SummaryDataOut>(response,HttpStatus.OK)
        }

    @PostMapping("/api/bulk", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    override fun csv(@RequestParam("file") file: MultipartFile, request: HttpServletRequest): ResponseEntity<Any> {
        File("salida.csv").delete()
        val h = HttpHeaders()
        if(file.isEmpty)  {
            h.add("Warning", "Fichero vacío")
            return ResponseEntity<Any>("", h, HttpStatus.OK)
        } else {
            try {
                val s = createShortUrlCsvUseCase.create(
                        file = file,
                        data = ShortUrlProperties(
                                ip = request.remoteAddr,
                                sponsor = null
                        )
                )
                val url: URI
                if(s.shortUrl.hash.isEmpty()) {
                    h.add("Warning", "No valid URLs found")
                    return ResponseEntity<Any>("", h, HttpStatus.CREATED)
                }else {
                    url = linkTo<UrlShortenerControllerImpl> { redirectTo(s.shortUrl.hash, request) }.toUri()
                    h.location = url
                }
                h.set("content_type", "text/csv")
                val response = ShortUrlDataOut(
                        url = url,
                        properties = mapOf(
                                "safe" to s.shortUrl.properties.safe
                        )
                )
                return ResponseEntity<Any>(response, h, HttpStatus.CREATED)
            } catch(e: Exception) {
                h.add("Error", "Problema con la lectura del csv")
                return ResponseEntity<Any>("", h, HttpStatus.BAD_REQUEST)
            }
        }
    }
}
