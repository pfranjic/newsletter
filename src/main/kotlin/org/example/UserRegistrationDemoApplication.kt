package org.example

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class UserRegistrationDemoApplication

fun main(args: Array<String>) {
    runApplication<UserRegistrationDemoApplication>(*args)
}
