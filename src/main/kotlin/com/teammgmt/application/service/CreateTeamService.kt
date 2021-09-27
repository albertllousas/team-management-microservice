package com.teammgmt.application.service

import arrow.core.Either
import arrow.core.flatMap
import com.teammgmt.domain.model.DomainError
import com.teammgmt.domain.model.DomainEventPublisher
import com.teammgmt.domain.model.Team
import com.teammgmt.domain.model.TeamCreated
import com.teammgmt.domain.model.TeamRepository
import com.teammgmt.domain.model.peek
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

open class CreateTeamService(
    private val teamRepository: TeamRepository,
    private val domainEventPublisher: DomainEventPublisher,
    private val generateId: () -> UUID = { UUID.randomUUID() }
) {

    @Transactional
    open operator fun invoke(request: CreateTeamRequest): Either<DomainError, CreateTeamResponse> =
        Team.create(generateId(), request.teamName)
            .flatMap(teamRepository::save)
            .peek { domainEventPublisher.publish(TeamCreated(it)) }
            .map { CreateTeamResponse(it.teamId.value) }
}

data class CreateTeamRequest(val teamName: String)

data class CreateTeamResponse(val newTeamId: UUID)
