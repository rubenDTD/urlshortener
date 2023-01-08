package es.unizar.urlshortener.infrastructure.repositories

import es.unizar.urlshortener.core.*
import java.io.*
import java.nio.charset.StandardCharsets
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

/**
 * Implementation of the port [ClickRepositoryService].
 */
class ClickRepositoryServiceImpl(
    private val clickEntityRepository: ClickEntityRepository
) : ClickRepositoryService {
    override fun save(cl: Click): Click = clickEntityRepository.save(cl.toEntity()).toDomain()

    override fun summary(key: String): List<Click> {
        val clicks = clickEntityRepository.findByHash(key)
        val clicksDom = mutableListOf<Click>()
        for (click in clicks) {
            clicksDom.add(click.toDomain())
        }
        return clicksDom
    }

    override fun updateBrowser(hash: String, data: String) = clickEntityRepository.updateBrowser(hash, data)

    override fun updatePlatform(hash: String, data: String) = clickEntityRepository.updatePlatform(hash, data)
}

/**
 * Implementation of the port [ShortUrlRepositoryService].
 */
class ShortUrlRepositoryServiceImpl(
    private val shortUrlEntityRepository: ShortUrlEntityRepository
) : ShortUrlRepositoryService {
    override fun findByKey(id: String): ShortUrl? = shortUrlEntityRepository.findByHash(id)?.toDomain()

    override fun save(su: ShortUrl): ShortUrl = shortUrlEntityRepository.save(su.toEntity()).toDomain()

    override fun updateSpam(hash: String, data: Boolean) = shortUrlEntityRepository.updateSpam(hash, data)

    override fun updateProcessing(hash: String, data: Boolean) = shortUrlEntityRepository.updateProcessing(hash, data)

}

/**
 * Implementation of the port [RMQService].
 */
@Component
@Primary
class RMQServiceImpl(
        private val rabbitTemplate: RabbitTemplate,
        private var shortUrlRepository: ShortUrlRepositoryService,
        private val validatorService: ValidatorService,
        private val hashService: HashService
        ) : RMQService {

    override fun listener(message: String) {
        val (uri,safe,ip,sponsor) = message.split("\\")
        val isSafe = safe == "si"
        if (validatorService.isValid(uri)) {
            val hash = hashService.hasUrl(uri)
            shortUrlRepository.save(ShortUrl(
                    hash = hash,
                    redirection = Redirection(target = uri),
                    properties = ShortUrlProperties(
                            safe = isSafe,
                            ip = ip,
                            sponsor = sponsor
                    )
            ))
        }
    }

    override fun send(uri: String, safe: Boolean, ip: String?, sponsor: String?) {
        // Post a message on the queue
        val s = if(safe) "si" else "no"
        val message = "$uri\\$s\\$ip\\$sponsor"
        rabbitTemplate.convertAndSend("exchange", "queue", message)
    }
}
