package com.payments.gateway.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration

@ConstructorBinding
@ConfigurationProperties("payments-gateway.cache")
data class CacheProperties(
    val ttl: Duration,
)
