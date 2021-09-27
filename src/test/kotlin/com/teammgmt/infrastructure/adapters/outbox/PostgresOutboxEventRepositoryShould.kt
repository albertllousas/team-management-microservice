package com.teammgmt.infrastructure.adapters.outbox

import com.teammgmt.Postgres
import com.teammgmt.fixtures.buildOutboxEvent
import com.teammgmt.infrastructure.outbox.OutboxEvent
import com.teammgmt.infrastructure.outbox.PostgresOutboxEventRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.support.TransactionTemplate

class PostgresOutboxEventRepositoryShould {

    private val postgres = Postgres()

    private val transactionTemplate = TransactionTemplate(DataSourceTransactionManager(postgres.datasource))

    private val jdbcTemplate = JdbcTemplate(postgres.datasource)

    private val repository = PostgresOutboxEventRepository(jdbcTemplate)

    @Test
    fun `store an outbox event for publishing`() {
        val outboxEvent = buildOutboxEvent()

        repository.storeForPublishing(outboxEvent)

        val record = jdbcTemplate.queryForMap("SELECT aggregate_id, event_payload, stream FROM outbox LIMIT 1")
        assertThat(record).containsAllEntriesOf(
            mapOf(
                "aggregate_id" to outboxEvent.aggregateId,
                "stream" to outboxEvent.stream,
                "event_payload" to outboxEvent.payload
            )
        )
    }

    @Test
    fun `find last events ready for publishing and delete them`() {
        val firstEvent = buildOutboxEvent().also(repository::storeForPublishing)
        val secondEvent = buildOutboxEvent().also(repository::storeForPublishing)
        val thirdEvent = buildOutboxEvent().also(repository::storeForPublishing)

        val outboxMessage = repository.findReadyForPublishing(batchSize = 2)

        assertThat(outboxMessage).isEqualTo(listOf(firstEvent, secondEvent))
        val allIds = jdbcTemplate.query("SELECT aggregate_id FROM outbox") { rs, _ -> rs.getObject("aggregate_id") }
        assertThat(allIds).containsOnly(thirdEvent.aggregateId)
    }

    @Test
    fun `ensure events found for publishing are blocked till they are processed`() {
        val oldestEvent = buildOutboxEvent().also(repository::storeForPublishing)
        val newestEvent = buildOutboxEvent().also(repository::storeForPublishing)
        val findReadyForPublishing = { repository.findReadyForPublishing(1) }

        val result = runBlocking(Dispatchers.IO) {
            val reallyLongExecution = async { withinTransaction(executionDelay = 500, code = findReadyForPublishing) }
            delay(50)
            val execution = async { withinTransaction(executionDelay = 100, code = findReadyForPublishing) }
            delay(50)
            val immediateExecution = async { withinTransaction(executionDelay = 0, code = findReadyForPublishing) }
            Triple(reallyLongExecution.await(), execution.await(), immediateExecution.await())
        }

        assertThat(result.first).isEqualTo(listOf(oldestEvent))
        assertThat(result.second).isEqualTo(listOf(newestEvent))
        assertThat(result.third).isEqualTo(emptyList<OutboxEvent>())
    }

    private fun <T> withinTransaction(executionDelay: Long = 0, code: () -> T?): T? =
        transactionTemplate.execute {
            val result = code()
            Thread.sleep(executionDelay)
            result
        }
}
