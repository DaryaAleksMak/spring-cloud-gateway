package com.payments.gateway.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.NestedConfigurationProperty

@ConstructorBinding
@ConfigurationProperties("payments-gateway.routes")
data class RoutingProperties(
    @NestedConfigurationProperty
    val p2p: Route,
    @NestedConfigurationProperty
    val p2m: Route,
    @NestedConfigurationProperty
    val iban: Route,
    @NestedConfigurationProperty
    val tokenization: Route,
)
