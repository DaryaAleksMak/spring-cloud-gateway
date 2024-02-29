package com.payments.gateway.config

import com.payments.gateway.properties.PaymentsServiceProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class PaymentsServiceIntegrationConfig {
    @Bean(PAYMENT_SERVICE_WEB_CLIENT)
    fun createClient(paymentsServiceProperties: PaymentsServiceProperties) =
        WebClient.create(paymentsServiceProperties.url)

    companion object {
        const val PAYMENT_SERVICE_WEB_CLIENT = "payment_service_web_client"
    }
}
