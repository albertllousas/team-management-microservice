package com.teammgmt.infrastructure.adapters.outbound.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.teammgmt.domain.model.DomainEvent
import com.teammgmt.domain.model.DomainEventSubscriber
import com.teammgmt.domain.model.Team
import com.teammgmt.domain.model.TeamCreated
import com.teammgmt.domain.model.TeamMember
import com.teammgmt.domain.model.TeamMemberJoined
import com.teammgmt.domain.model.TeamMemberLeft
import com.teammgmt.infrastructure.adapters.outbound.event.IntegrationEvent.*
import com.teammgmt.infrastructure.outbox.OutboxEvent
import com.teammgmt.infrastructure.outbox.TransactionalOutbox
import java.time.Clock
import java.time.LocalDateTime.now
import java.util.UUID

class OutboxSubscriber(
    private val transactionalOutbox: TransactionalOutbox,
    private val teamEventStreamName: String,
    private val mapper: ObjectMapper,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val generateId: () -> UUID = { UUID.randomUUID() }
) : DomainEventSubscriber {

    override fun handle(event: DomainEvent) {
        when (event) {
            is TeamCreated -> TeamCreatedEvent(event.team.asDto(), now(clock), generateId())
            is TeamMemberJoined -> TeamMemberJoined(event.team.asDto(), event.personId.value, now(clock), generateId())
            is TeamMemberLeft -> TeamMemberLeft(event.team.asDto(), event.personId.value, now(clock), generateId())
        }
            .let {
                transactionalOutbox.storeForPublishing(
                    OutboxEvent(
                        stream = teamEventStreamName,
                        payload = mapper.writeValueAsBytes(it),
                        aggregateId = event.team.teamId.value
                    )
                )
            }
    }

    private fun Team.asDto() = TeamDto(teamId.value, teamName.value, members.map { it.asDto() })

    private fun TeamMember.asDto() = TeamMemberDto(personId.value)
}
