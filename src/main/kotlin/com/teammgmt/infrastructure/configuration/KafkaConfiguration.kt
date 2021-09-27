package com.teammgmt.infrastructure.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.SeekToCurrentErrorHandler
import org.springframework.util.backoff.ExponentialBackOff

@Configuration
class KafkaConfiguration {

    @Bean
    fun deadLetterPublishingRecoverer(bytesTemplate: KafkaTemplate<String, ByteArray>) =
        DeadLetterPublishingRecoverer(bytesTemplate)

    @Bean
    fun seekToCurrentErrorHandler(
        @Value("\${spring.kafka.error-handling.exponential-backoff.initial-interval}") initialInterval: Long,
        @Value("\${spring.kafka.error-handling.exponential-backoff.multiplier:1.5}") multiplier: Double,
        @Value("\${spring.kafka.error-handling.exponential-backoff.max-elapsed-time:30000}") maxElapsedTime: Long,
        deadLetterPublishingRecoverer: DeadLetterPublishingRecoverer,
    ): SeekToCurrentErrorHandler {
        val exponentialBackOff = ExponentialBackOff(initialInterval, multiplier)
            .apply { this.maxElapsedTime = maxElapsedTime }
        return SeekToCurrentErrorHandler(deadLetterPublishingRecoverer, exponentialBackOff)
    }
}
