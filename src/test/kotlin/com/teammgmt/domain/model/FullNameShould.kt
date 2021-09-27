package com.teammgmt.domain.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FullNameShould {

    @Test
    fun `create a full name from a first and last name`() {
        assertThat(FullName.create(firstName = "John", lastName = "Doe"))
            .isEqualTo(FullName.reconstitute("John Doe"))
    }
}
