package org.demo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class NewsletterDemoApplication

fun main(args: Array<String>) {
    runApplication<NewsletterDemoApplication>(*args)
}
