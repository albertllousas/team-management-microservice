package com.teammgmt.infrastructure.configuration

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.teammgmt.infrastructure.adapters.outbound.client.PeopleServiceApi
import com.teammgmt.infrastructure.adapters.outbound.db.PostgresPeopleReplicationRepository
import com.teammgmt.infrastructure.adapters.outbound.db.PostgresTeamRepository
import com.teammgmt.infrastructure.adapters.outbound.event.InMemoryDomainEventPublisher
import com.teammgmt.infrastructure.adapters.outbound.event.LoggingSubscriber
import com.teammgmt.infrastructure.adapters.outbound.event.MetricsSubscriber
import com.teammgmt.infrastructure.adapters.outbound.event.OutboxSubscriber
import com.teammgmt.infrastructure.outbox.TransactionalOutbox
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.circuitbreaker.autoconfigure.CircuitBreakerProperties
import io.github.resilience4j.common.CompositeCustomizer
import io.github.resilience4j.retrofit.CircuitBreakerCallAdapter
import io.micrometer.core.instrument.MeterRegistry
import okhttp3.OkHttpClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.concurrent.TimeUnit.MILLISECONDS

@Configuration
class InfrastructureConfiguration {

    @Bean
    @Primary
    fun defaultObjectMapper(): ObjectMapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE, true)
        .registerModules(JavaTimeModule())
        .findAndRegisterModules()

    @Bean
    fun domainEventPublisher(
        transactionalOutbox: TransactionalOutbox,
        meterRegistry: MeterRegistry,
        objectMapper: ObjectMapper,
        @Value("\${spring.kafka.producer.team.topic}")
        teamEventStream: String,
    ) = InMemoryDomainEventPublisher(
        listOf(
            LoggingSubscriber(),
            MetricsSubscriber(meterRegistry),
            OutboxSubscriber(transactionalOutbox, teamEventStream, objectMapper)
        )
    )

    @Bean
    fun peopleFinder(jdbcTemplate: JdbcTemplate) = PostgresPeopleReplicationRepository(jdbcTemplate)

//    @Bean
//    fun peopleFinder(peopleServiceApi: PeopleServiceApi) = PeopleServiceHttpClient(peopleServiceApi)

    @Bean
    fun postgresTeamRepository(jdbcTemplate: JdbcTemplate) =
        PostgresTeamRepository(jdbcTemplate)

    @Bean
    fun peopleServiceCircuitBreaker(
        registry: CircuitBreakerRegistry,
        circuitBreakerProperties: CircuitBreakerProperties
    ) =
        registry.circuitBreaker(
            "people-service",
            circuitBreakerProperties.createCircuitBreakerConfig(
                "people-service",
                circuitBreakerProperties.instances["people-service"],
                CompositeCustomizer(emptyList())
            )
        )

    @Bean
    fun peopleServiceApi(
        defaultObjectMapper: ObjectMapper,
        circuitBreaker: CircuitBreaker,
        meterRegistry: MeterRegistry,
        @Value("\${clients.people-service.url}") url: String,
        @Value("\${clients.people-service.connectTimeoutMillis}") connectTimeout: Long,
        @Value("\${clients.people-service.readTimeoutMillis}") readTimeout: Long,
    ): PeopleServiceApi {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(connectTimeout, MILLISECONDS)
            .readTimeout(readTimeout, MILLISECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(JacksonConverterFactory.create(defaultObjectMapper))
            .addCallAdapterFactory(
                CircuitBreakerCallAdapter.of(
                    circuitBreaker
                ) { r -> !HttpStatus.valueOf(r.code()).is5xxServerError }
            )
            .client(okHttpClient)
            .build()
            .create(PeopleServiceApi::class.java)
    }
}
