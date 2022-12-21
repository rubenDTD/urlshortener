package es.unizar.urlshortener.infrastructure.repositories

import es.unizar.urlshortener.core.*
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
}

/**
 * Implementation of the port [ShortUrlRepositoryService].
 */
class ShortUrlRepositoryServiceImpl(
    private val shortUrlEntityRepository: ShortUrlEntityRepository
) : ShortUrlRepositoryService {
    override fun findByKey(id: String): ShortUrl? = shortUrlEntityRepository.findByHash(id)?.toDomain()

    override fun save(su: ShortUrl): ShortUrl = shortUrlEntityRepository.save(su.toEntity()).toDomain()
}

@Component
@Primary
class RMQServiceImpl(
        private val rabbitTemplate: RabbitTemplate,

        ) : RMQService {

    override fun listener(message: String) {
        val (i,url) = message.split("\\")
        println(url)
        // Condición para la demo, url not verified
        if(i.toInt() == 0){
            println("Primera")
        }
    }
    override fun send(id: String, uri: String) {
        // Envía un mensaje a la cola
        val message = "$id\\$uri"
        rabbitTemplate.convertAndSend("exchange", "queue", message)
        println("Mensaje enviado: $message")
    }

}