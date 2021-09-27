package com.teammgmt.application.service

import arrow.core.Either
import arrow.core.flatMap
import com.teammgmt.domain.model.DomainError
import com.teammgmt.domain.model.DomainEventPublisher
import com.teammgmt.domain.model.PersonId
import com.teammgmt.domain.model.TeamId
import com.teammgmt.domain.model.TeamMemberLeft
import com.teammgmt.domain.model.TeamRepository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

open class RemoveTeamMemberService(
    private val teamRepository: TeamRepository,
    private val domainEventPublisher: DomainEventPublisher
) {
    @Transactional
    open operator fun invoke(request: RemoveTeamMemberRequest): Either<DomainError, Unit> =
        teamRepository.find(TeamId(request.teamId))
            .flatMap { team -> team.leave(PersonId(request.teamMemberId)) }
            .flatMap(teamRepository::save)
            .map { TeamMemberLeft(it, PersonId(request.teamMemberId)) }
            .map(domainEventPublisher::publish)
}

data class RemoveTeamMemberRequest(
    val teamId: UUID,
    val teamMemberId: UUID
)
