package com.teammgmt.infrastructure.adapters.outbound.event

import com.teammgmt.domain.model.DomainEventSubscriber
import com.teammgmt.domain.model.TeamCreated
import com.teammgmt.fixtures.buildTeam
import io.mockk.mockk
import io.mockk.verify

class InMemoryDomainEventPublisherShould {

    fun `forward domain event to subscribers`() {
        val event = TeamCreated(buildTeam())
        val firstSubscriber = mockk<DomainEventSubscriber>(relaxed = true)
        val secondSubscriber = mockk<DomainEventSubscriber>(relaxed = true)
        val thirdSubscriber = mockk<DomainEventSubscriber>(relaxed = true)
        val publisher = InMemoryDomainEventPublisher(
            listOf(firstSubscriber, secondSubscriber, thirdSubscriber)
        )

        publisher.publish(event)

        verify {
            firstSubscriber.handle(event)
            secondSubscriber.handle(event)
            thirdSubscriber.handle(event)
        }
    }
}
