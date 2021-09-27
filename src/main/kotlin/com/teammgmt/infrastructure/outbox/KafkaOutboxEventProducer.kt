package com.teammgmt.infrastructure.outbox

import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.kafka.core.KafkaTemplate

class KafkaOutboxEventProducer(private val kafkaTemplate: KafkaTemplate<String, ByteArray>) {

    fun send(batch: List<OutboxEvent>) =
        batch.map(::toKafkaRecord)
            .forEach(kafkaTemplate::send)
            .also { if (batch.isNotEmpty()) kafkaTemplate.flush() }

    private fun toKafkaRecord(outboxMessage: OutboxEvent) =
        ProducerRecord(outboxMessage.stream, outboxMessage.aggregateId.toString(), outboxMessage.payload)
}
