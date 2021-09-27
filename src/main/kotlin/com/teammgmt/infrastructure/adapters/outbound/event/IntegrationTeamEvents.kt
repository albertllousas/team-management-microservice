package com.teammgmt.infrastructure.adapters.outbound.event

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.teammgmt.infrastructure.adapters.outbound.event.IntegrationEvent.*
import java.time.LocalDateTime
import java.util.UUID

/*
Integration event: A committed event that ocurred in the past within an bounded context which may be interesting to other
domains, applications or third party services.
 */

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = TeamCreatedEvent::class, name = "TeamCreatedEvent"),
    JsonSubTypes.Type(value = TeamMemberJoined::class, name = "TeamMemberJoined"),
    JsonSubTypes.Type(value = TeamMemberLeft::class, name = "TeamMemberLeft")
)
sealed class IntegrationEvent(val eventType: String) {

    abstract val occurredOn: LocalDateTime
    abstract val eventId: UUID

    data class TeamCreatedEvent(
        val team: TeamDto,
        override val occurredOn: LocalDateTime,
        override val eventId: UUID
    ) : IntegrationEvent("TeamCreatedEvent")

    data class TeamMemberJoined(
        val team: TeamDto,
        val teamMember: UUID,
        override val occurredOn: LocalDateTime,
        override val eventId: UUID
    ) : IntegrationEvent("TeamMemberJoinedEvent")

    data class TeamMemberLeft(
        val team: TeamDto,
        val teamMember: UUID,
        override val occurredOn: LocalDateTime,
        override val eventId: UUID
    ) : IntegrationEvent("TeamMemberLeftEvent")
}
data class TeamDto(val id: UUID, val name: String, val members: List<TeamMemberDto>)

data class TeamMemberDto(val id: UUID)
