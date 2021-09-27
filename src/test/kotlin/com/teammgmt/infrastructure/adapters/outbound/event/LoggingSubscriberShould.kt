package com.teammgmt.infrastructure.adapters.outbound.event

import com.teammgmt.domain.model.TeamCreated
import com.teammgmt.domain.model.TeamMember
import com.teammgmt.domain.model.TeamMemberJoined
import com.teammgmt.domain.model.TeamMemberLeft
import com.teammgmt.fixtures.buildPerson
import com.teammgmt.fixtures.buildTeam
import io.mockk.spyk
import io.mockk.verify
import org.slf4j.helpers.NOPLogger

class LoggingSubscriberShould {

    private val logger = spyk(NOPLogger.NOP_LOGGER)

    private val loggingDomainEventSubscriber = LoggingSubscriber(logger)

    fun `log a team created event`() {
        val teamCreated = TeamCreated(buildTeam())

        loggingDomainEventSubscriber.handle(teamCreated)

        verify {
            logger.info(
                "domain-event: 'TeamCreated', team-id: '${teamCreated.team.teamId.value}'"
            )
        }
    }

    fun `log a team member joined event`() {
        val person = buildPerson()
        val team = buildTeam(members = setOf(TeamMember(person.personId)))
        val teamMemberJoined = TeamMemberJoined(team = team, personId = person.personId)

        loggingDomainEventSubscriber.handle(teamMemberJoined)

        verify {
            logger.info(
                "domain-event: 'TeamMemberJoined', team-id: '${teamMemberJoined.team.teamId.value}', person-id: ${person.personId}"
            )
        }
    }

    fun `log a team member left event`() {
        val person = buildPerson()
        val team = buildTeam(members = setOf(TeamMember(person.personId)))
        val teamMemberLeft = TeamMemberLeft(team = team, personId = person.personId)

        loggingDomainEventSubscriber.handle(teamMemberLeft)

        verify {
            logger.info(
                "domain-event: 'TeamMemberLeft', team-id: '${teamMemberLeft.team.teamId.value}', person-id: ${person.personId}"
            )
        }
    }
}
