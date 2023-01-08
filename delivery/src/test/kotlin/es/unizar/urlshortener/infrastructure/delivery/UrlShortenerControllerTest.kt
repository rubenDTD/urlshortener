package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.*
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.never
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpHeaders.LOCATION
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest
@ContextConfiguration(
    classes = [
        UrlShortenerControllerImpl::class,
        RestResponseEntityExceptionHandler::class]
)
class UrlShortenerControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var redirectUseCase: RedirectUseCase

    @MockBean
    private lateinit var logClickUseCase: LogClickUseCase

    @MockBean
    private lateinit var createShortUrlUseCase: CreateShortUrlUseCase

    @MockBean
    private lateinit var infoSummaryUseCase: InfoSummaryUseCase

    @MockBean
    private lateinit var blackListUseCase: BlackListUseCase

    @MockBean
    private lateinit var sponsorUseCase: SponsorUseCase

    @MockBean
    private lateinit var createShortUrlCsvUseCase: CreateShortUrlCsvUseCase

    @MockBean
    private lateinit var headersInfoUseCase: HeadersInfoUseCase

    @Test
    fun `redirectTo returns a redirect when the key exists`() {
        given(redirectUseCase.redirectTo("key")).willReturn(Redirection("http://example.com/"))

        mockMvc.perform(get("/{id}", "key").header("User-Agent",
                               "Mozilla/5.0 (X11; Linux x86_64; rv:10.0) Gecko/20100101 Firefox/10.0"))
            .andExpect(status().isTemporaryRedirect)
            .andExpect(redirectedUrl("http://example.com/"))

        verify(logClickUseCase).logClick("key", ClickProperties(ip = "127.0.0.1", referrer = "http://example.com/"))
    }

    @Test
    fun `redirectTo returns a not found when the key does not exist`() {
        given(redirectUseCase.redirectTo("key"))
            .willAnswer { throw RedirectionNotFound("key") }

        mockMvc.perform(get("/{id}", "key"))
            .andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.statusCode").value(404))

        verify(logClickUseCase, never()).logClick("key", ClickProperties(ip = "127.0.0.1"))
    }

    @Test
    fun `redirectTo logs click stats when the key is spam`() {
        given(redirectUseCase.redirectTo("key")).willReturn(Redirection("http://example.com/"))
        given(blackListUseCase.isSpam("key")).willReturn(true)

        mockMvc.perform(get("/{id}", "key").header("User-Agent",
                               "Mozilla/5.0 (X11; Linux x86_64; rv:10.0) Gecko/20100101 Firefox/10.0"))
            .andDo(print())
            .andExpect(status().isForbidden)

        verify(logClickUseCase).logClick("key", ClickProperties(ip = "127.0.0.1", referrer = "http://example.com/"))
    }

    @Test
    fun `redirectTo returns ok and banner when the key has sponsor`() {
        given(redirectUseCase.redirectTo("key")).willReturn(Redirection("http://example.com/"))
        given(sponsorUseCase.hasSponsor("key")).willReturn(true)

        mockMvc.perform(get("/{id}", "key").header("User-Agent",
            "Mozilla/5.0 (X11; Linux x86_64; rv:10.0) Gecko/20100101 Firefox/10.0"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("Redirecting in 10 seconds...")))

    }

    @Test
    fun `redirectTo returns too early when key is processing`() {
        given(redirectUseCase.redirectTo("key")).willReturn(Redirection("http://example.com/"))
        given(redirectUseCase.isProcessing("key")).willReturn(true)

        mockMvc.perform(get("/{id}", "key").header("User-Agent",
            "Mozilla/5.0 (X11; Linux x86_64; rv:10.0) Gecko/20100101 Firefox/10.0"))
            .andExpect(status().isTooEarly)

    }

    @Test
    fun `creates returns a basic redirect if it can compute a hash`() {
        given(
            createShortUrlUseCase.create(
                url = "http://example.com/",
                data = ShortUrlProperties(ip = "127.0.0.1", processing = true)
            )
        ).willReturn(ShortUrl("f684a3c4", Redirection("http://example.com/")))

        mockMvc.perform(
            post("/api/link")
                .param("url", "http://example.com/")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andDo(print())
            .andExpect(status().isCreated)
            .andExpect(redirectedUrl("http://localhost/f684a3c4"))
            .andExpect(jsonPath("$.url").value("http://localhost/f684a3c4"))
    }

    @Test
    fun `creates returns bad request if it cant compute a hash`() {
        given(
            createShortUrlUseCase.create(
                url = "ftp://example.com/",
                data = ShortUrlProperties(ip = "127.0.0.1",
                        processing = true)
            )
        ).willAnswer { throw InvalidUrlException("ftp://example.com/") }

        mockMvc.perform(
            post("/api/link")
                .param("url", "ftp://example.com/")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.statusCode").value(400))
    }

    @Test
    fun `summary returns ok if key exists`() {
        val clicks = mutableListOf(Click("f684a3c4", ClickProperties()))
        given(
            infoSummaryUseCase.summary(
                key = "f684a3c4"
            )
        ).willReturn(clicks)

        mockMvc.perform(get("/api/link/f684a3c4"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.clicks[0].hash").value(clicks[0].hash))
            .andExpect(jsonPath("$.clicks.size()").value(1))
    }

    @Test
    fun `summary returns not found if key does not exist`() {
        given(
            infoSummaryUseCase.summary(
                key = "f684a3c4"
            )
        ).willAnswer { throw RedirectionNotFound("f684a3c4") }

        mockMvc.perform(get("/api/link/f684a3c4"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.statusCode").value(404))
    }

    @Test
    fun `summary returns forbidden if key is spam`() {
        given(
            blackListUseCase.isSpam(
                key = "f684a3c4"
            )
        ).willReturn(true)

        mockMvc.perform(get("/api/link/f684a3c4"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `summary returns in progress if key is still processing`() {
        given(
            redirectUseCase.isProcessing(
                key = "f684a3c4"
            )
        ).willReturn(true)

        mockMvc.perform(get("/api/link/f684a3c4"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.processing").value("Web redirection analysis in progress"))
    }

    @Test
    fun `summary returns available if key is processed`() {
        given(
            redirectUseCase.isProcessing(
                key = "f684a3c4"
            )
        ).willReturn(false)

        mockMvc.perform(get("/api/link/f684a3c4"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.processing").value("Web redirection analyzed and available"))
    }

    @Test
    fun `creates correct return for not empty csv`() {
        val mockMultipartFile = MockMultipartFile(
                "file",
                "htp://www.unizar.es/\nhttp://www.example.com/".toByteArray())
        given(createShortUrlCsvUseCase.create(file = mockMultipartFile, data = ShortUrlProperties(ip = "127.0.0.1")))
                .willReturn(CsvResponse(
                "64faa857",
                "htp://www.unizar.es/,,debe ser una URI http o https\n" +
                    "http://www.example.com/,http://localhost:8080/64faa857,")
                )

        mockMvc.perform(multipart("/api/bulk")
                .file(mockMultipartFile))
                .andDo(print())
                .andExpect(status().isCreated)
                .andExpect(header().string(CONTENT_TYPE, "text/csv"))
                .andExpect(header().string(LOCATION, "http://localhost/64faa857"))
    }

    @Test
    fun `creates correct return for empty csv`() {
        val mockMultipartFile = MockMultipartFile(
                "file",
                "".toByteArray())
        given(createShortUrlCsvUseCase.create(file = mockMultipartFile, data = ShortUrlProperties(ip = "127.0.0.1")))
                .willReturn(CsvResponse("ERROR", ""))

        mockMvc.perform(multipart("/api/bulk")
                .file(mockMultipartFile))
                .andDo(print())
                .andExpect(status().isOk)
    }

    @Test
    @Disabled
    fun `creates correct return for bad request`() {
        val mockMultipartFile = MockMultipartFile(
                "file",
                "throw".toByteArray())
        given(createShortUrlCsvUseCase.create(file = mockMultipartFile, data = ShortUrlProperties(ip = "127.0.0.1")))
                .willReturn(CsvResponse("ERROR", ""))

        mockMvc.perform(multipart("/api/bulk")
                .file(mockMultipartFile))
                .andDo(print())
                .andExpect(status().isBadRequest)
    }
}
