package com.teammgmt.infrastructure.adapters.outbound.event

import com.teammgmt.domain.model.DomainEvent
import com.teammgmt.domain.model.DomainEventSubscriber
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag

class MetricsSubscriber(private val metrics: MeterRegistry) : DomainEventSubscriber {

    override fun handle(event: DomainEvent) {
        val tags = listOf(Tag.of("type", event::class.simpleName!!))
        metrics.counter("domain.event", tags).increment()
    }
}
