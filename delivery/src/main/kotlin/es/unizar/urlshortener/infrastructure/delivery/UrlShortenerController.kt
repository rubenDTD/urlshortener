package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.Click
import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.usecases.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.*
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import ru.chermenin.ua.UserAgent
import java.net.URI
import java.util.concurrent.TimeUnit
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


/**
 * The specification of the controller.
 */
interface UrlShortenerController {

    /**
     * Redirects and logs a short url identified by its [id].
     *
     * **Note**: Delivery of use cases [RedirectUseCase] and [LogClickUseCase].
     */
    fun redirectTo(id: String, request: HttpServletRequest, response: HttpServletResponse? = null, model: Model? = null): Any

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
     * **Note**: Delivery of use case [ShortUrlDataOut].
     */
    fun csv(@RequestParam("file") file: MultipartFile, request: HttpServletRequest): ResponseEntity<String>
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
    val processing: String,
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
    val createShortUrlCsvUseCase: CreateShortUrlCsvUseCase,
    val headersInfoUseCase: HeadersInfoUseCase
) : UrlShortenerController {

    @Operation(summary = "Redirect to URI")
    @GetMapping("/{id:(?!api|index).*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest, response: HttpServletResponse?,
                            model: Model?): Any {
        val redirection = redirectUseCase.redirectTo(id)
        logClickUseCase.logClick(id, ClickProperties(ip = request.remoteAddr, referrer = redirection.target))
        val h = HttpHeaders()
        CoroutineScope(Dispatchers.IO).launch() {
            headersInfoUseCase.getBrowserAndPlatform(request.getHeader("User-Agent"), id)
        }

        return if (redirectUseCase.isProcessing(id)){
            ResponseEntity<Void>(h, HttpStatus.TOO_EARLY)
        }else {
            if (blackListUseCase.isSpam(id)) {
                ResponseEntity<Void>(h, HttpStatus.FORBIDDEN)
            } else {
                if (sponsorUseCase.hasSponsor(id)) {
                    model?.addAttribute("uri", redirection.target)
                    val cacheControl = CacheControl.maxAge(120, TimeUnit.SECONDS)
                        .noTransform()
                        .mustRevalidate().headerValue
                    response?.addHeader("Cache-Control", cacheControl);
                    "banner"
                } else {
                    h.location = URI.create(redirection.target)
                    ResponseEntity<Void>(h, HttpStatus.valueOf(redirection.mode))
                }
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
                processing = true
            )
        ).let {
            CoroutineScope(Dispatchers.IO).launch() {
                blackListUseCase.checkBlackList(request.remoteAddr, data.url, it.hash)
            }
            val h = HttpHeaders()
            val url = linkTo<UrlShortenerControllerImpl> { redirectTo(it.hash, request) }.toUri()
            h.location = url
            val response = ShortUrlDataOut(
                url = url,
                sponsor = data.sponsor,
                properties = mapOf(
                    "safe" to it.properties.safe,
                    "status" to it.properties.processing
                )
            )
            ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.CREATED)
        }

    @Operation(summary = "Return clicks summary")
    @GetMapping("/api/link/{id}")
    override fun summary(@PathVariable id: String): ResponseEntity<SummaryDataOut> {
        val h = HttpHeaders()
        return if(blackListUseCase.isSpam(id)) {
            ResponseEntity<SummaryDataOut>(h, HttpStatus.FORBIDDEN)
        } else {
            val processing = if (redirectUseCase.isProcessing(id)){
                "Web redirection analysis in progress"
            }else{
                "Web redirection analyzed and available"
            }
            val response = SummaryDataOut(processing, infoSummaryUseCase.summary(id))
            ResponseEntity<SummaryDataOut>(response,HttpStatus.OK)
        }
    }

    @Operation(summary = "Process CSV file")
    @PostMapping("/api/bulk", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    override fun csv(@RequestParam("file") file: MultipartFile, request: HttpServletRequest): ResponseEntity<String> {
        val h = HttpHeaders()
        if(file.isEmpty)  {
            h.add("Warning", "Empty file")
            return ResponseEntity<String>(h, HttpStatus.OK)
        } else {
            try {
                val s = createShortUrlCsvUseCase.create(
                        file = file,
                        data = ShortUrlProperties(
                            ip = request.remoteAddr,
                            sponsor = null,
                        )
                )
                val url: URI
                return if(s.shortUrl.hash.isEmpty()) {
                    h.add("Warning", "All URL are invalid")
                    ResponseEntity<String>(h, HttpStatus.CREATED)
                }else {
                    url = linkTo<UrlShortenerControllerImpl> { redirectTo(s.shortUrl.hash, request) }.toUri()
                    h.location = url
                    h.set("Content-Type", "text/csv")
                    h.set("Content-Disposition", "attachment; filename=shortURLs.csv")
                    h.set("Content-Length", s.csv.length.toString())
                    ResponseEntity<String>(s.csv, h, HttpStatus.CREATED)
                }
            } catch(e: Exception) {
                h.add("Error", "Cannot read csv")
                h.set("Content-Type", "application/json")
                return ResponseEntity<String>(h, HttpStatus.BAD_REQUEST)
            }
        }
    }
}
