package es.unizar.urlshortener

import es.unizar.urlshortener.core.usecases.*
import es.unizar.urlshortener.infrastructure.delivery.HashServiceImpl
import es.unizar.urlshortener.infrastructure.delivery.ValidatorServiceImpl
import es.unizar.urlshortener.infrastructure.repositories.*
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Wires use cases with service implementations, and services implementations with repositories.
 *
 * **Note**: Spring Boot is able to discover this [Configuration] without further configuration.
 */
@Configuration
class ApplicationConfiguration(
    @Autowired val shortUrlEntityRepository: ShortUrlEntityRepository,
    @Autowired val clickEntityRepository: ClickEntityRepository,
    @Autowired val rabbitTemplate: RabbitTemplate
) {
    @Bean
    fun clickRepositoryService() = ClickRepositoryServiceImpl(clickEntityRepository)

    @Bean
    fun shortUrlRepositoryService() = ShortUrlRepositoryServiceImpl(shortUrlEntityRepository)
    @Bean
    fun validatorService() = ValidatorServiceImpl()

    @Bean
    fun hashService() = HashServiceImpl()

    @Bean
    fun rmqService() = RMQServiceImpl(rabbitTemplate, shortUrlRepositoryService(), validatorService(), hashService())

    @Bean
    fun redirectUseCase() = RedirectUseCaseImpl(shortUrlRepositoryService())

    @Bean
    fun logClickUseCase() = LogClickUseCaseImpl(clickRepositoryService())

    @Bean
    fun createShortUrlUseCase() =
        CreateShortUrlUseCaseImpl(shortUrlRepositoryService(), validatorService(), hashService())

    @Bean
    fun infoSummaryUseCase() = InfoSummaryUseCaseImpl(clickRepositoryService())

    @Bean
    fun blackListUseCase() = BlackListUseCaseImpl(shortUrlRepositoryService())

    @Bean
    fun sponsorUseCase() = SponsorUseCaseImpl(shortUrlRepositoryService())

    @Bean
    fun headersInfoUseCase() = HeadersInfoUseCaseImpl(clickRepositoryService())

    @Bean
    fun createShortUrlCsvUseCase() =
        CreateShortUrlCsvUseCaseImpl(shortUrlRepositoryService(), hashService(), rmqService())

    @Bean
    fun queue(): Queue? {
        return Queue("queue", false)
    }
    @Bean
    fun exchange(): TopicExchange {
        return TopicExchange("exchange")
    }

    @Bean
    fun binding(queue: Queue, exchange: TopicExchange): Binding {
        return BindingBuilder.bind(queue).to(exchange).with("queue")
    }

    @Bean
    fun container(connectionFactory: ConnectionFactory, listenerAdapter: MessageListenerAdapter):
            SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer()
        container.connectionFactory = connectionFactory
        container.setQueueNames("queue")
        container.setMessageListener(listenerAdapter)
        return container
    }

    @Bean
    fun listenerAdapter(receiver: RMQServiceImpl?): MessageListenerAdapter {
        return MessageListenerAdapter(receiver, "listener")
    }
}