package com.teammgmt.domain.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.teammgmt.domain.model.TeamValidationError.TooLongName
import java.util.UUID

data class TeamId(val value: UUID)

data class TeamName private constructor(val value: String) {
    companion object {

        fun create(value: String): Either<TooLongName, TeamName> =
            if (value.length > 50) TooLongName.left() else TeamName(value).right()

        fun reconstitute(value: String) = TeamName(value)
    }
}

inline class FullName private constructor(val value: String) {
    companion object {

        fun create(firstName: String, lastName: String): FullName = FullName("$firstName $lastName")

        fun reconstitute(value: String) = FullName(value)
    }
}

data class TeamMember(val personId: PersonId) // TODO: Add role

data class Team(val teamId: TeamId, val teamName: TeamName, val members: Set<TeamMember>) {

    companion object {

        fun create(teamId: UUID, teamName: String): Either<TeamValidationError, Team> =
            TeamName.create(teamName)
                .map { Team(TeamId(teamId), it, emptySet()) }
    }

    fun join(person: Person): Either<AlreadyPartOfTheTeam, Team> =
        this.members
            .find { teamMember -> teamMember.personId == person.personId }
            ?.let { AlreadyPartOfTheTeam.left() }
            ?: this.copy(members = this.members + TeamMember(person.personId)).right()

    fun leave(personId: PersonId): Either<TeamMemberNotFound, Team> =
        this.members
            .find { teamMember -> teamMember.personId == personId }
            ?.let { this.removePerson(personId).right() }
            ?: TeamMemberNotFound.left()

    private fun removePerson(personId: PersonId): Team =
        this.copy(members = this.members.filter { teamMember -> teamMember.personId != personId }.toSet())
}
