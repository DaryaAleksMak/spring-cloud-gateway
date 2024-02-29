package com.payments.gateway

import com.ftr.dgb.core.config.annotation.EnableDgbErrorHandling
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@EnableDgbErrorHandling
@ConfigurationPropertiesScan("com.payments.gateway")
@SpringBootApplication
class PaymentsGatewayApplication

fun main(args: Array<String>) {
    runApplication<PaymentsGatewayApplication>(*args)
}
