package com.teammgmt.infrastructure.outbox

import com.teammgmt.fixtures.buildOutboxEvent
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.slf4j.helpers.NOPLogger
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.shaded.org.awaitility.Awaitility.await
import java.util.concurrent.TimeUnit
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.full.findAnnotation
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.SimpleTransactionStatus

class PollingPublisherShould {

    private val logger = spyk(NOPLogger.NOP_LOGGER)

    private val meterRegistry = SimpleMeterRegistry()

    private val taskScheduler = ThreadPoolTaskScheduler().also { it.initialize() }

    private val outboxEventRepository = mockk<PostgresOutboxEventRepository>(relaxed = true)

    private val kafkaOutboxEventProducer = mockk<KafkaOutboxEventProducer>(relaxed = true)

    private val threadPoolTaskScheduler: ThreadPoolTaskScheduler = ThreadPoolTaskScheduler()

    private val transactionManager = spyk(object : PlatformTransactionManager {
        override fun getTransaction(definition: TransactionDefinition?): TransactionStatus = SimpleTransactionStatus()
        override fun rollback(status: TransactionStatus) {}
        override fun commit(status: TransactionStatus) {}
    })

    init {
        threadPoolTaskScheduler.also { it.initialize() }
        PollingPublisher(
            transactionalOutbox = outboxEventRepository,
            kafkaOutboxEventProducer = kafkaOutboxEventProducer,
            pollingIntervalMs = 50,
            scheduler = taskScheduler,
            transactionManager = transactionManager,
            batchSize = 5,
            meterRegistry = meterRegistry,
            logger = logger
        )
    }

    @AfterEach
    fun `tear down`() {
        threadPoolTaskScheduler.destroy()
    }

    @Test
    fun `send eventually events to kafka when outbox have some of them ready to be sent`() {
        val outboxEvent = buildOutboxEvent()
        every { outboxEventRepository.findReadyForPublishing(5) } returns listOf(outboxEvent)

        verify(timeout = 2000) {
            kafkaOutboxEventProducer.send(listOf(outboxEvent))
            transactionManager.commit(any())
        }
    }

    @Test
    fun `not delete from the outbox the message to be sent when there is a problem sending it`() {
        val outboxEvent = buildOutboxEvent()
        val crash = RuntimeException("Boom!")
        every { outboxEventRepository.findReadyForPublishing(5) } returns listOf(outboxEvent)
        every { kafkaOutboxEventProducer.send(listOf(outboxEvent)) } throws crash

        verify(timeout = 2000) {
            logger.error("Message batch publishing failed, will be retried", crash)
            transactionManager.rollback(any())
        }
        await().atMost(2, TimeUnit.SECONDS).untilAsserted {
            assertThat(meterRegistry.counter("outbox.publishing.fail").count()).isGreaterThan(1.0)
        }
    }
}
