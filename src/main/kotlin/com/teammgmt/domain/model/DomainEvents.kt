package com.teammgmt.domain.model

sealed class DomainEvent {
    abstract val team: Team
}

data class TeamCreated(override val team: Team) : DomainEvent()
data class TeamMemberJoined(override val team: Team, val personId: PersonId) : DomainEvent()
data class TeamMemberLeft(override val team: Team, val personId: PersonId) : DomainEvent()
// data class TeamDeleted(override val team: Team) : DomainEvent()

interface DomainEventPublisher {
    fun publish(event: DomainEvent)
}

interface DomainEventSubscriber {
    fun handle(event: DomainEvent)
}
