package com.teammgmt.infrastructure.outbox

interface TransactionalOutbox {

    fun storeForPublishing(event: OutboxEvent)

    fun findReadyForPublishing(batchSize: Int): List<OutboxEvent>
}
