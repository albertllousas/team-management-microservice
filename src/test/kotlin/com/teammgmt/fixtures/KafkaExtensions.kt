package com.teammgmt.fixtures

import kotlinx.coroutines.delay
import org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import java.time.Duration
import java.util.Properties
import java.util.concurrent.TimeUnit.*

fun buildKafkaProducer(bootstrapServers: String) =
    KafkaProducer<String, ByteArray>(producerProperties(bootstrapServers))

fun buildKafkaConsumer(bootstrapServers: String, group: String = "test.team.mgmt.consumer") =
    KafkaConsumer<String, ByteArray>(consumerProperties(bootstrapServers, group))

private fun producerProperties(bootstrapServers: String): Properties = Properties().apply {
    this["key.serializer"] = StringSerializer::class.java.name
    this["value.serializer"] = ByteArraySerializer::class.java.name
    this["bootstrap.servers"] = bootstrapServers
}

private fun consumerProperties(bootstrapServers: String, group: String): Properties = Properties().apply {
    this["key.deserializer"] = StringDeserializer::class.java.name
    this["value.deserializer"] = ByteArrayDeserializer::class.java.name
    this["bootstrap.servers"] = bootstrapServers
    this[AUTO_OFFSET_RESET_CONFIG] = "earliest"
    this["group.id"] = group
    this["enable.auto.commit"] = "true"
    this["auto.commit.interval.ms"] = "100"
}

fun <K, V> KafkaConsumer<K, V>.consumeAndAssert(
    stream: String,
    timeout: Long = 5000L,
    assertions: (ConsumerRecord<K, V>) -> Unit
) {
    this.subscribe(listOf(stream))
    val events = this.poll(Duration.ofMillis(timeout)).records(stream)
    this.commitSync()
    if (events.count() != 1) throw Exception("Expected to consume '1' record but '${events.count()}' were present.")
    this.unsubscribe()
    assertions(events.first())
}

suspend fun <K, V> KafkaConsumer<K, V>.consumeAndAssertMultiple(
    numberOfMessages: Int = 1,
    stream: String,
    delay: Long = 3000L,
    assertions: (List<ConsumerRecord<K, V>>) -> Unit
) {
    this.subscribe(listOf(stream))
    delay(delay)
    val events = this.poll(Duration.ofMillis(delay)).records(stream).toList()
    this.commitAsync()
    if (events.count() != numberOfMessages)
        throw Exception("Expected to consume '$numberOfMessages' record but '${events.count()}' were present.")

    this.unsubscribe()
    assertions(events)
}
