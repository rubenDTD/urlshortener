package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.Click
import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.usecases.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import ru.chermenin.ua.UserAgent
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
    val sponsor: String? = null,
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
@Tag(name = "UrlShortener Controller")
@Controller
class UrlShortenerControllerImpl(
    val redirectUseCase: RedirectUseCase,
    val logClickUseCase: LogClickUseCase,
    val createShortUrlUseCase: CreateShortUrlUseCase,
    val infoSummaryUseCase: InfoSummaryUseCase,
    val blackListUseCase: BlackListUseCase,
    val sponsorUseCase: SponsorUseCase,
    val createShortUrlCsvUseCase: CreateShortUrlCsvUseCase
) : UrlShortenerController {

    @Operation(summary = "Redirect to URI")
    @GetMapping("/{id:(?!api|index).*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest, model: Model?): Any {
        val redirection = redirectUseCase.redirectTo(id)
        val uaString = request.getHeader("User-Agent")
        val ua = UserAgent.parse(uaString)
        logClickUseCase.logClick(id, ClickProperties(ip = request.remoteAddr, referrer = redirection.target,
            ua.browser.toString(), platform = ua.os.toString()))
        val h = HttpHeaders()

      return if(sponsorUseCase.hasSponsor(id)) {
            model?.addAttribute("uri", redirection.target)
            "banner"
        } else {
            h.location = URI.create(redirection.target)
          if (blackListUseCase.isSpam(id)){
              ResponseEntity<Void>(h, HttpStatus.FORBIDDEN)
          }else{
              ResponseEntity<Void>(h, HttpStatus.valueOf(redirection.mode))
          }
        }
    }

    @Operation(summary = "Short an URI")
    @PostMapping("/api/link", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    override fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut> =
        createShortUrlUseCase.create(
            url = data.url,
            data = ShortUrlProperties(
                ip = request.remoteAddr,
                sponsor = data.sponsor,
                spam = blackListUseCase.checkBlackList(request.remoteAddr) || blackListUseCase.checkBlackList(data.url)
            )
        ).let {
            val h = HttpHeaders()
            val url = linkTo<UrlShortenerControllerImpl> { redirectTo(it.hash, request) }.toUri()
            h.location = url
            val response = ShortUrlDataOut(
                url = url,
                sponsor = data.sponsor,
                properties = mapOf(
                    "safe" to it.properties.safe,
                    "spam" to it.properties.spam
                )
            )
            if (it.properties.spam) {
                ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.FORBIDDEN)
            }else{
                ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.CREATED)
            }
        }

    @Operation(summary = "Return clicks summary")
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

    @Operation(summary = "Process CSV file")
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
