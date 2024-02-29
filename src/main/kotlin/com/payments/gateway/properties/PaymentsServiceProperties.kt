package com.payments.gateway.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("payments-gateway.payments-service")
data class PaymentsServiceProperties(
    val url: String,
)
