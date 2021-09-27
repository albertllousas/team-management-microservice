package com.teammgmt.application.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.zip
import com.teammgmt.domain.model.DomainError
import com.teammgmt.domain.model.DomainEventPublisher
import com.teammgmt.domain.model.PeopleFinder
import com.teammgmt.domain.model.PersonId
import com.teammgmt.domain.model.TeamId
import com.teammgmt.domain.model.TeamMemberJoined
import com.teammgmt.domain.model.TeamRepository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

open class AddTeamMemberService(
    private val teamRepository: TeamRepository,
    private val peopleFinder: PeopleFinder,
    private val domainEventPublisher: DomainEventPublisher
) {

    @Transactional
    open operator fun invoke(request: AddTeamMemberRequest): Either<DomainError, Unit> =
        peopleFinder.find(PersonId(request.newMemberId))
            .zip(teamRepository.find(TeamId(request.teamId)))
            .flatMap { (person, team) -> team.join(person) }
            .flatMap(teamRepository::save)
            .map { TeamMemberJoined(it, PersonId(request.newMemberId)) }
            .map(domainEventPublisher::publish)
}

data class AddTeamMemberRequest(
    val teamId: UUID,
    val newMemberId: UUID
)
