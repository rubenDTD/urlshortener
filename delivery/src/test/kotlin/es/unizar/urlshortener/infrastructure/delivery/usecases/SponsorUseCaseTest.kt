package es.unizar.urlshortener.infrastructure.delivery.usecases

import es.unizar.urlshortener.core.Redirection
import es.unizar.urlshortener.core.ShortUrl
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import es.unizar.urlshortener.core.usecases.SponsorUseCase
import es.unizar.urlshortener.core.usecases.SponsorUseCaseImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ContextConfiguration

@WebMvcTest
@AutoConfigureMockMvc
@ContextConfiguration(
    classes = [
        SponsorUseCaseImpl::class
    ]
)
class SponsorUseCaseTest {

    @Autowired
    private lateinit var sponsorUseCase: SponsorUseCase

    @MockBean
    private lateinit var shortUrlRepositoryService: ShortUrlRepositoryService

    @Test
    fun `hasSponsor return true if key has sponsor`() {
        given(shortUrlRepositoryService.findByKey("key"))
            .willReturn(ShortUrl("hash", Redirection("uri"),
                                      properties = ShortUrlProperties(sponsor = "true")))

        val result = sponsorUseCase.hasSponsor("key")
        assertThat(result).isEqualTo(true)
    }

    @Test
    fun `hasSponsor return false if key has not sponsor`() {
        given(shortUrlRepositoryService.findByKey("key"))
            .willReturn(ShortUrl("hash", Redirection("uri")))

        val result = sponsorUseCase.hasSponsor("key")
        assertThat(result).isEqualTo(false)
    }

}