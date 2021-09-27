package com.teammgmt.infrastructure.adapters.outbox

import com.teammgmt.Kafka
import com.teammgmt.fixtures.buildKafkaConsumer
import com.teammgmt.fixtures.buildKafkaProducer
import com.teammgmt.fixtures.buildOutboxEvent
import com.teammgmt.fixtures.consumeAndAssert
import com.teammgmt.infrastructure.outbox.KafkaOutboxEventProducer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.KafkaTemplate

@Tag("integration")
class KafkaOutboxEventProducerShould {

    companion object {

        val kafka = Kafka()
    }

    private val firstKafkaConsumer = buildKafkaConsumer(kafka.container.bootstrapServers, "consumer-1")
        .also { it.subscribe(listOf("topic")) }

    private val secondKafkaConsumer = buildKafkaConsumer(kafka.container.bootstrapServers, "consumer-2")
        .also { it.subscribe(listOf("topic-2")) }

    private val kafkaTemplate = KafkaTemplate {
        buildKafkaProducer(kafka.container.bootstrapServers)
    }

    private val kafkaOutboxEventProducer = KafkaOutboxEventProducer(kafkaTemplate)

    @Test
    fun `send a batch of outbox messages successfully to different streams`() {
        val oneOutboxEvent = buildOutboxEvent(stream = "topic")
        val anotherOutboxEvent = buildOutboxEvent(stream = "topic-2")

        kafkaOutboxEventProducer.send(listOf(oneOutboxEvent, anotherOutboxEvent))

        firstKafkaConsumer.consumeAndAssert(stream = "topic") { record ->
            assertThat(record.key()).isEqualTo(oneOutboxEvent.aggregateId.toString())
            assertThat(record.value()).isEqualTo(oneOutboxEvent.payload)
        }
        secondKafkaConsumer.consumeAndAssert(stream = "topic-2") { record ->
            assertThat(record.key()).isEqualTo(anotherOutboxEvent.aggregateId.toString())
            assertThat(record.value()).isEqualTo(anotherOutboxEvent.payload)
        }
    }
}
