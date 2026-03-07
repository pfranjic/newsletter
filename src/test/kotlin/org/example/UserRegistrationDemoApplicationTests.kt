package org.example

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class UserRegistrationDemoApplicationTests {

    @MockitoBean
    private lateinit var mailSender: JavaMailSender
    @Test
    fun contextLoads() {
    }

}
