package com.teammgmt.infrastructure.configuration

import com.teammgmt.application.service.AddTeamMemberService
import com.teammgmt.application.service.CreateTeamService
import com.teammgmt.application.service.RemoveTeamMemberService
import com.teammgmt.domain.model.DomainEventPublisher
import com.teammgmt.domain.model.PeopleFinder
import com.teammgmt.domain.model.TeamRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ApplicationServicesConfiguration {

    @Bean
    fun addTeamMember(
        teamRepository: TeamRepository,
        peopleFinder: PeopleFinder,
        domainEventPublisher: DomainEventPublisher
    ) = AddTeamMemberService(teamRepository, peopleFinder, domainEventPublisher)

    @Bean
    fun createTeam(teamRepository: TeamRepository, domainEventPublisher: DomainEventPublisher) =
        CreateTeamService(teamRepository, domainEventPublisher)

    @Bean
    fun removeTeamMember(teamRepository: TeamRepository, domainEventPublisher: DomainEventPublisher) =
        RemoveTeamMemberService(teamRepository, domainEventPublisher)
}
