package com.teammgmt.infrastructure.adapters.outbound.event

import com.teammgmt.domain.model.DomainEvent
import com.teammgmt.domain.model.DomainEventPublisher
import com.teammgmt.domain.model.DomainEventSubscriber

class InMemoryDomainEventPublisher(val subscribers: List<DomainEventSubscriber>) : DomainEventPublisher {

    override fun publish(event: DomainEvent) = subscribers.forEach { it.handle(event) }
}
