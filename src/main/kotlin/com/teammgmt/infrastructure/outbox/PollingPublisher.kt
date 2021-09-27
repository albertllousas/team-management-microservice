package com.teammgmt.infrastructure.outbox

import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.TaskScheduler
import org.springframework.transaction.annotation.Transactional
import java.lang.invoke.MethodHandles

private const val FAIL_COUNTER = "outbox.publishing.fail"

open class PollingPublisher(
    private val transactionalOutbox: TransactionalOutbox,
    private val kafkaOutboxEventProducer: KafkaOutboxEventProducer,
    private val batchSize: Int = 50,
    pollingIntervalMs: Long = 50L,
    scheduler: TaskScheduler,
    private val meterRegistry: MeterRegistry,
    private val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
) {

    init {
        scheduler.scheduleWithFixedDelay(this::publish, pollingIntervalMs)
    }

    @Transactional(rollbackFor = [Exception::class])
    open fun publish() =
        try {
            transactionalOutbox
                .findReadyForPublishing(batchSize)
                .also(kafkaOutboxEventProducer::send)
        } catch (exception: Exception) {
            meterRegistry.counter(FAIL_COUNTER).increment()
            logger.error("Message publishing failed", exception)
            throw exception
        }
}
