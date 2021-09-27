package com.teammgmt.domain.model

import arrow.core.Either

interface TeamRepository {

    fun find(teamId: TeamId): Either<TeamNotFound, Team>
    fun save(team: Team): Either<TeamNameAlreadyTaken, Team>
}
