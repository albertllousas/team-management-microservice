package com.teammgmt.infrastructure.outbox

import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.TaskScheduler
import org.springframework.transaction.annotation.Transactional
import java.lang.invoke.MethodHandles
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

private const val FAIL_COUNTER = "outbox.publishing.fail"

class PollingPublisher(
    private val transactionalOutbox: TransactionalOutbox,
    private val kafkaOutboxEventProducer: KafkaOutboxEventProducer,
    private val batchSize: Int = 50,
    pollingIntervalMs: Long = 50L,
    scheduler: TaskScheduler,
    transactionManager: PlatformTransactionManager,
    private val meterRegistry: MeterRegistry,
    private val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
) {

    init {
        scheduler.scheduleWithFixedDelay(this::publish, pollingIntervalMs)
    }

    private val transactionTemplate = TransactionTemplate(transactionManager)

    open fun publish() =
        try {
            transactionTemplate.execute {
                transactionalOutbox
                    .findReadyForPublishing(batchSize)
                    .also(kafkaOutboxEventProducer::send)
            }
        } catch (exception: Exception) {
            meterRegistry.counter(FAIL_COUNTER).increment()
            logger.error("Message batch publishing failed, will be retried", exception)
            throw exception
        }
}
