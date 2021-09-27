package com.teammgmt.infrastructure.adapters.outbound.event

import com.teammgmt.domain.model.DomainEvent
import com.teammgmt.domain.model.DomainEventSubscriber
import com.teammgmt.domain.model.TeamCreated
import com.teammgmt.domain.model.TeamMemberJoined
import com.teammgmt.domain.model.TeamMemberLeft
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

class LoggingSubscriber(
    private val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
) : DomainEventSubscriber {

    override fun handle(event: DomainEvent) {
        logger.info(event.logMessage())
    }

    private fun DomainEvent.logMessage(): String {
        val commonMessage = "domain-event: '${this::class.simpleName}', team-id: '${this.team.teamId.value}'"
        val specificMessage = when (this) {
            is TeamMemberJoined -> ", person-id: ${this.personId}"
            is TeamMemberLeft -> ", person-id: ${this.personId}"
            is TeamCreated -> ""
        }
        return "$commonMessage$specificMessage"
    }
}
