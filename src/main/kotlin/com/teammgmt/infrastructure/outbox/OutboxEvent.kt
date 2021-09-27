package com.teammgmt.infrastructure.outbox

import java.util.UUID

class OutboxEvent(val aggregateId: UUID, val stream: String, val payload: ByteArray) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OutboxEvent

        if (aggregateId != other.aggregateId) return false
        if (stream != other.stream) return false
        if (!payload.contentEquals(other.payload)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = aggregateId.hashCode()
        result = 31 * result + stream.hashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }
}
