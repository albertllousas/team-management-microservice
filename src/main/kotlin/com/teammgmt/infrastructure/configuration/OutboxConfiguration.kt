package com.teammgmt.infrastructure.configuration

import com.teammgmt.infrastructure.outbox.KafkaOutboxEventProducer
import com.teammgmt.infrastructure.outbox.PollingPublisher
import com.teammgmt.infrastructure.outbox.PostgresOutboxEventRepository
import com.teammgmt.infrastructure.outbox.TransactionalOutbox
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@Configuration
class OutboxConfiguration {

    @Bean
    fun transactionalOutbox(jdbcTemplate: JdbcTemplate): TransactionalOutbox =
        PostgresOutboxEventRepository(jdbcTemplate)

    @Bean
    fun kafkaOutboxEventProducer(kafkaTemplate: KafkaTemplate<String, ByteArray>) = KafkaOutboxEventProducer(
        kafkaTemplate
    )

    @Bean
    fun pollingPublisher(
        transactionalOutbox: TransactionalOutbox,
        kafkaOutboxEventProducer: KafkaOutboxEventProducer,
        taskScheduler: TaskScheduler,
        meterRegistry: MeterRegistry
    ) = PollingPublisher(
        transactionalOutbox = transactionalOutbox,
        kafkaOutboxEventProducer = kafkaOutboxEventProducer,
        batchSize = 10,
        pollingIntervalMs = 1000,
        scheduler = taskScheduler,
        meterRegistry = meterRegistry
    )
}
