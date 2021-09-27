package com.teammgmt.infrastructure.adapters.inbound.stream

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.teammgmt.application.service.RemoveTeamMemberRequest
import com.teammgmt.application.service.RemoveTeamMemberService
import com.teammgmt.domain.model.PersonId
import com.teammgmt.infrastructure.adapters.outbound.db.PersonReplicationInfo
import com.teammgmt.infrastructure.adapters.outbound.db.PersonStatus.ACTIVE
import com.teammgmt.infrastructure.adapters.outbound.db.PostgresPeopleReplicationRepository
import com.teammgmt.infrastructure.adapters.outbound.db.PostgresTeamRepository
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.Message
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Component
class KafkaPeopleEventsConsumer(
    private val removeTeamMember: RemoveTeamMemberService,
    private val postgresPeopleReplicationRepository: PostgresPeopleReplicationRepository,
    private val postgresTeamRepository: PostgresTeamRepository,
    private val mapper: ObjectMapper
) {

    @Transactional
    @KafkaListener(
        topics = ["\${spring.kafka.consumer.people.topic}"],
        groupId = "\${spring.kafka.consumer.people.group-id}"
    )
    fun listenTo(message: Message<ByteArray>): Unit =
        mapper.readValue(message.payload, PeopleEvent::class.java).let {
            when (it) {
                is PersonJoinedEvent -> reactTo(it)
                is PersonLeftEvent -> reactTo(it)
            }
        }

    fun reactTo(event: PersonJoinedEvent) =
        postgresPeopleReplicationRepository.save(
            PersonReplicationInfo(event.personId, event.firstName, event.lastname, event.joinedAt, ACTIVE)
        )

    fun reactTo(event: PersonLeftEvent) {
        postgresPeopleReplicationRepository.delete(event.personId)
        postgresTeamRepository.findAllTeamsFor(PersonId(event.personId))
            .map { removeTeamMember(RemoveTeamMemberRequest(it.teamId.value, event.personId)) }
    }
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = PersonJoinedEvent::class, name = "PersonJoinedEvent"),
    JsonSubTypes.Type(value = PersonLeftEvent::class, name = "PersonLeftEvent")
)
sealed class PeopleEvent

data class PersonJoinedEvent(
    val personId: UUID,
    val firstName: String,
    val lastname: String,
    val joinedAt: LocalDateTime
) : PeopleEvent()

data class PersonLeftEvent(val personId: UUID, val leftAt: LocalDateTime) : PeopleEvent()
