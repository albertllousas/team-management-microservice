package com.teammgmt.infrastructure.adapters.outbound.event

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.teammgmt.domain.model.TeamCreated
import com.teammgmt.domain.model.TeamMember
import com.teammgmt.domain.model.TeamMemberJoined
import com.teammgmt.domain.model.TeamMemberLeft
import com.teammgmt.fixtures.buildPerson
import com.teammgmt.fixtures.buildTeam
import com.teammgmt.infrastructure.outbox.OutboxEvent
import com.teammgmt.infrastructure.outbox.TransactionalOutbox
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

class OutboxSubscriberShould {

    private val transactionalOutbox = mockk<TransactionalOutbox>(relaxed = true)

    private val objectMapper = jacksonObjectMapper()

    private val eventId = UUID.randomUUID()

    private val generateId = { eventId }

    private val now = LocalDateTime.now()

    private val clock = Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneId.of("UTC"))

    private val outboxSubscriber = OutboxSubscriber(
        transactionalOutbox = transactionalOutbox,
        teamEventStreamName = "stream",
        mapper = objectMapper,
        clock = clock,
        generateId = generateId
    )

    @Test
    fun `store team created event into the outbox`() {
        val team = buildTeam()
        val event = TeamCreated(team)

        outboxSubscriber.handle(event)

        val expectedIntegrationEvent = IntegrationEvent.TeamCreatedEvent(
            team = TeamDto(
                id = team.teamId.value,
                name = team.teamName.value,
                members = team.members.map { TeamMemberDto(it.personId.value) }
            ),
            eventId = eventId,
            occurredOn = now
        )
        verify {
            transactionalOutbox.storeForPublishing(
                OutboxEvent(
                    aggregateId = team.teamId.value,
                    stream = "stream",
                    payload = objectMapper.writeValueAsBytes(expectedIntegrationEvent)
                )
            )
        }
    }

    @Test
    fun `store team member joined event into the outbox`() {
        val teamMember = TeamMember(buildPerson().personId)
        val team = buildTeam(members = setOf(teamMember))
        val event = TeamMemberJoined(team, teamMember.personId)

        outboxSubscriber.handle(event)

        val expectedIntegrationEvent = IntegrationEvent.TeamMemberJoined(
            team = TeamDto(
                id = team.teamId.value,
                name = team.teamName.value,
                members = team.members.map { TeamMemberDto(it.personId.value) }
            ),
            eventId = eventId,
            occurredOn = now,
            teamMember = teamMember.personId.value
        )
        verify {
            transactionalOutbox.storeForPublishing(
                OutboxEvent(
                    aggregateId = team.teamId.value,
                    stream = "stream",
                    payload = objectMapper.writeValueAsBytes(expectedIntegrationEvent)
                )
            )
        }
    }

    @Test
    fun `store team member left event into the outbox`() {
        val teamMember = TeamMember(buildPerson().personId)
        val team = buildTeam(members = setOf(teamMember))
        val event = TeamMemberLeft(team, teamMember.personId)

        outboxSubscriber.handle(event)

        val expectedIntegrationEvent = IntegrationEvent.TeamMemberLeft(
            team = TeamDto(
                id = team.teamId.value,
                name = team.teamName.value,
                members = team.members.map { TeamMemberDto(it.personId.value) }
            ),
            eventId = eventId,
            occurredOn = now,
            teamMember = teamMember.personId.value
        )
        verify {
            transactionalOutbox.storeForPublishing(
                OutboxEvent(
                    aggregateId = team.teamId.value,
                    stream = "stream",
                    payload = objectMapper.writeValueAsBytes(expectedIntegrationEvent)
                )
            )
        }
    }
}
