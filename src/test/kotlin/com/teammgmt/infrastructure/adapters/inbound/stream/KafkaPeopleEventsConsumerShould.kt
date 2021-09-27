package com.teammgmt.infrastructure.adapters.inbound.stream

import com.teammgmt.Kafka
import com.teammgmt.application.service.RemoveTeamMemberService
import com.teammgmt.domain.model.PersonId
import com.teammgmt.fixtures.buildKafkaProducer
import com.teammgmt.fixtures.buildTeam
import com.teammgmt.infrastructure.adapters.outbound.db.PersonReplicationInfo
import com.teammgmt.infrastructure.adapters.outbound.db.PersonStatus
import com.teammgmt.infrastructure.adapters.outbound.db.PostgresPeopleReplicationRepository
import com.teammgmt.infrastructure.adapters.outbound.db.PostgresTeamRepository
import com.teammgmt.infrastructure.configuration.InfrastructureConfiguration
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import java.time.LocalDateTime.now
import java.util.UUID.randomUUID

@ContextConfiguration(classes = [TestConfiguration::class])
@SpringBootTest(webEnvironment = NONE, classes = [KafkaPeopleEventsConsumer::class, ConsumerDependencies::class])
@TestPropertySource(properties = ["spring.flyway.enabled=false"])
@EnableAutoConfiguration(exclude = [DataSourceAutoConfiguration::class])
class KafkaPeopleEventsConsumerShould {

    companion object {
        val kafka = Kafka()
    }

    @BeforeEach
    fun clear() {
        clearMocks(removeTeamMember, postgresPeopleReplicationRepository, postgresTeamRepository)
    }

    @AfterEach
    fun `tear down`() {
//        kafka.container.stop()
    }

    @Autowired
    private lateinit var removeTeamMember: RemoveTeamMemberService

    @Autowired
    private lateinit var postgresPeopleReplicationRepository: PostgresPeopleReplicationRepository

    @Autowired
    private lateinit var postgresTeamRepository: PostgresTeamRepository

    private val mapper = InfrastructureConfiguration().defaultObjectMapper()

    @Nested
    inner class ConsumePersonJoinedEvent {

        @Test
        fun `replicate person information when it is created`() {
            val joinedEvent = PersonJoinedEvent(randomUUID(), "John", "Doe", now())

            buildKafkaProducer(kafka.container.bootstrapServers)
                .send(
                    ProducerRecord(
                        "public.person.v1",
                        joinedEvent.personId.toString(),
                        mapper.writeValueAsBytes(joinedEvent)
                    )
                )

            verify(timeout = 3000) {
                postgresPeopleReplicationRepository.save(
                    PersonReplicationInfo(
                        joinedEvent.personId,
                        joinedEvent.firstName,
                        joinedEvent.lastname,
                        joinedEvent.joinedAt,
                        PersonStatus.ACTIVE
                    )
                )
            }
        }
    }

    @Nested
    inner class ConsumePersonLeftEvent {

        @Test
        fun `remove replication and remove it from any team that is a team member when a person leaves`() {
            val leftEvent = PersonLeftEvent(randomUUID(), now())
            val firstTeam = buildTeam(randomUUID())
            val secondTeam = buildTeam(randomUUID())
            every {
                postgresTeamRepository.findAllTeamsFor(PersonId(leftEvent.personId))
            } returns listOf(firstTeam, secondTeam)
            every {
                postgresPeopleReplicationRepository.delete(leftEvent.personId)
            } just Runs

            buildKafkaProducer(kafka.container.bootstrapServers)
                .send(
                    ProducerRecord(
                        "public.person.v1",
                        leftEvent.personId.toString(),
                        mapper.writeValueAsBytes(leftEvent)
                    )
                )

            verify(timeout = 3000, exactly = 2) { removeTeamMember(any()) }
        }
    }
}

@ActiveProfiles("test")
@EnableKafka
@TestConfiguration
private class ConsumerDependencies {

    @Bean
    fun removeTeamMember(): RemoveTeamMemberService = mockk(relaxUnitFun = true)

    @Bean
    fun postgresPeopleReplicationRepository(): PostgresPeopleReplicationRepository = mockk(relaxUnitFun = true)

    @Bean
    fun postgresTeamRepository(): PostgresTeamRepository = mockk(relaxUnitFun = true)
}
