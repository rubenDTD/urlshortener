package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.Click
import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.usecases.*
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
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
    //fun redirectTo(id: String, request: HttpServletRequest): ResponseEntity<Void>
    fun redirectTo(id: String, request: HttpServletRequest, model: Model? = null): Any

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
     * Creates a short url from details provided in [file].
     *
     * **Note**: Delivery of use case [User].
     */
    fun csv(@RequestParam("file") file: MultipartFile, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut>
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

    @GetMapping("/{id:(?!api|index).*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest, model: Model?): Any {
        val redirection = redirectUseCase.redirectTo(id)
        logClickUseCase.logClick(id, ClickProperties(ip = request.remoteAddr))
        return if(sponsorUseCase.hasSponsor(id)) {
            model?.addAttribute("uri", redirection.target)
            "banner"
        } else {
            val h = HttpHeaders()
            h.location = URI.create(redirection.target)
            ResponseEntity<Void>(h, HttpStatus.valueOf(redirection.mode))
        }
    }

    /*@GetMapping("/{id:(?!api|index).*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<Void> =
        redirectUseCase.redirectTo(id).let {
            logClickUseCase.logClick(id, ClickProperties(ip = request.remoteAddr))
            val h = HttpHeaders()
            h.location = URI.create(it.target)
            ResponseEntity<Void>(h, HttpStatus.valueOf(it.mode))
        }*/

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
    override fun csv(@RequestParam("file") file: MultipartFile, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut> {
        val s = createShortUrlCsvUseCase.create(
            file = file,
            data = ShortUrlProperties(
                ip = request.remoteAddr,
                sponsor = null
            )
        )
        val h = HttpHeaders()
        val url = linkTo<UrlShortenerControllerImpl> { redirectTo(s.hash, request) }.toUri()
        h.location = url
        val response = ShortUrlDataOut(
            url = url,
            properties = mapOf(
                "safe" to s.properties.safe
            )
        )
        return ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.CREATED)
    }
}
