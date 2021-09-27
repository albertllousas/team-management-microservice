package com.teammgmt.fixtures

import com.github.javafaker.Faker
import com.teammgmt.domain.model.FullName
import com.teammgmt.domain.model.Person
import com.teammgmt.domain.model.PersonId
import com.teammgmt.domain.model.Team
import com.teammgmt.domain.model.TeamId
import com.teammgmt.domain.model.TeamMember
import com.teammgmt.domain.model.TeamName
import com.teammgmt.infrastructure.adapters.outbound.db.PersonReplicationInfo
import com.teammgmt.infrastructure.adapters.outbound.db.PersonStatus
import com.teammgmt.infrastructure.outbox.OutboxEvent
import java.time.LocalDateTime
import java.util.UUID

private val faker = Faker()

val DEFAULT_TEAM_ID = UUID.randomUUID()
val DEFAULT_TEAM_NAME = faker.team().name()
val DEFAULT_TEAM_MEMBERS = emptySet<TeamMember>()
val DEFAULT_PERSON_ID = UUID.randomUUID()
val DEFAULT_PERSON_FULL_NAME = faker.name().fullName()
val DEFAULT_PERSON_FIRST_NAME = faker.name().firstName()
val DEFAULT_PERSON_LAST_NAME = faker.name().lastName()

fun buildTeam(
    teamId: UUID = DEFAULT_TEAM_ID,
    teamName: String = DEFAULT_TEAM_NAME,
    members: Set<TeamMember> = DEFAULT_TEAM_MEMBERS
) = Team(
    teamId = TeamId(teamId),
    teamName = TeamName.reconstitute(teamName),
    members = members
)

fun buildPerson(
    personId: UUID = DEFAULT_PERSON_ID,
    fullName: String = DEFAULT_PERSON_FULL_NAME
) = Person(
    personId = PersonId(personId),
    fullName = FullName.reconstitute(fullName)
)

fun buildPersonReplicationInfo(
    personId: UUID = DEFAULT_PERSON_ID,
    firstName: String = DEFAULT_PERSON_FIRST_NAME,
    lastName: String = DEFAULT_PERSON_LAST_NAME,
    joinedtAt: LocalDateTime = LocalDateTime.now(),
    status: PersonStatus = PersonStatus.ACTIVE
) = PersonReplicationInfo(
    personId = personId,
    firstName = firstName,
    lastname = lastName,
    joinedAt = joinedtAt,
    status = status
)

fun buildOutboxEvent(
    key: UUID = UUID.randomUUID(),
    eventPayload: ByteArray = faker.backToTheFuture().character().toByteArray(),
    stream: String = faker.superhero().name()
) = OutboxEvent(
    aggregateId = key,
    payload = eventPayload,
    stream = stream
)
