package com.teammgmt.infrastructure.outbox

import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.annotation.Propagation.MANDATORY
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

open class PostgresOutboxEventRepository(
    private val jdbcTemplate: JdbcTemplate,
    private val generateId: () -> UUID = { UUID.randomUUID() },
    private val clock: Clock = Clock.systemUTC()
) : TransactionalOutbox {

    @Transactional(propagation = MANDATORY)
    override fun storeForPublishing(event: OutboxEvent) {
        jdbcTemplate.update(
            "INSERT INTO outbox (id, aggregate_id, event_payload, stream, created) VALUES (?,?,?,?,?)",
            generateId(),
            event.aggregateId,
            event.payload,
            event.stream,
            LocalDateTime.now(clock)
        )
    }

    @Transactional(propagation = MANDATORY)
    override fun findReadyForPublishing(batchSize: Int): List<OutboxEvent> = try {
        jdbcTemplate.query(
            """
            DELETE FROM outbox
            WHERE aggregate_id IN ( SELECT aggregate_id FROM outbox ORDER BY created ASC LIMIT $batchSize FOR UPDATE ) 
            RETURNING *
            """
        ) { rs, _ ->
            OutboxEvent(
                aggregateId = UUID.fromString(rs.getString("aggregate_id")),
                payload = rs.getBytes("event_payload"),
                stream = rs.getString("stream")
            )
        }
    } catch (exception: EmptyResultDataAccessException) {
        emptyList()
    }
}
