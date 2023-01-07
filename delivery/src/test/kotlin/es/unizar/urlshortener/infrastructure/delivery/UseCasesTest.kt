package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ContextConfiguration

@WebMvcTest
@AutoConfigureMockMvc
@ContextConfiguration(
    classes = [
        SponsorUseCaseImpl::class,
        CreateShortUrlUseCaseImpl::class,
        InfoSummaryUseCaseImpl::class,
        RedirectUseCaseImpl::class,
        LogClickUseCaseImpl::class
    ]
)
class UseCasesTest {

    @Autowired
    private lateinit var sponsorUseCase: SponsorUseCase

    @Autowired
    private lateinit var createShortUrlUseCase: CreateShortUrlUseCase

    @Autowired
    private lateinit var infoSummaryUseCase: InfoSummaryUseCase

    @Autowired
    private lateinit var redirectUseCase: RedirectUseCase

    @Autowired
    private lateinit var logClickUseCase: LogClickUseCase

    @MockBean
    private lateinit var shortUrlRepository: ShortUrlRepositoryService

    @MockBean
    private lateinit var validatorService: ValidatorService

    @MockBean
    private lateinit var clickRepository: ClickRepositoryService

    @MockBean
    private lateinit var hashService: HashService

    @Test
    fun `hasSponsor return true if key has sponsor`() {
        given(shortUrlRepository.findByKey("key"))
            .willReturn(ShortUrl("hash", Redirection("uri"),
                                      properties = ShortUrlProperties(sponsor = "true")))

        val result = sponsorUseCase.hasSponsor("key")
        assertThat(result).isEqualTo(true)
    }

    @Test
    fun `hasSponsor return false if key has not sponsor`() {
        given(shortUrlRepository.findByKey("key"))
            .willReturn(ShortUrl("hash", Redirection("uri")))

        val result = sponsorUseCase.hasSponsor("key")
        assertThat(result).isEqualTo(false)
    }

    @Test
    fun `create returns invalid if url is not valid`() {
        given(validatorService.isValid("url"))
            .willReturn(false)

        assertThrows<InvalidUrlException> { createShortUrlUseCase.create("url", ShortUrlProperties()) }
    }

    @Test
    fun `summary returns clicks`() {
        given(clickRepository.summary("key"))
            .willReturn(mutableListOf(Click("f684a3c4", ClickProperties())))

        val result = infoSummaryUseCase.summary("key")
        assertThat(result.size).isEqualTo(1)
    }

    @Test
    fun `redirectTo returns a redirection if key exists`() {
        given(shortUrlRepository.findByKey("key"))
            .willReturn(ShortUrl("key",Redirection("url")))

        val result = redirectUseCase.redirectTo("key")
        assertThat(result.target).isEqualTo("url")
    }

    @Test
    fun `redirectTo returns not found if key does not exist`() {
        given(shortUrlRepository.findByKey("key"))
            .willReturn(null)

        assertThrows<RedirectionNotFound> { redirectUseCase.redirectTo("key") }
    }

    @Test
    fun `isProcessing returns processing state if key exists`() {
        given(shortUrlRepository.findByKey("key"))
            .willReturn(ShortUrl("key",Redirection("url"),
                                      properties = ShortUrlProperties(processing = true)
            ))

        val result = redirectUseCase.isProcessing("key")
        assertThat(result).isEqualTo(true)
    }

    @Test
    fun `isProcessing returns not found if key does not exist`() {
        given(shortUrlRepository.findByKey("key"))
            .willReturn(null)

        assertThrows<RedirectionNotFound> { redirectUseCase.isProcessing("key") }
    }

    @Test
    fun `logClick saves a click with data`() {
        val cl = Click(
            hash = "hash",
            properties = ClickProperties()
        )
        given(clickRepository.save(cl))
            .willReturn(cl)

        val result = logClickUseCase.logClick("hash",ClickProperties())

        assertThat(result).isEqualTo(Unit)
    }

}
