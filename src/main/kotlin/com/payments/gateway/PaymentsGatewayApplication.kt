package com.payments.gateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@ConfigurationPropertiesScan("com.payments.gateway")
@SpringBootApplication
class PaymentsGatewayApplication

fun main(args: Array<String>) {
    runApplication<PaymentsGatewayApplication>(*args)
}
