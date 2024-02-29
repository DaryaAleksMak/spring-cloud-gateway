package com.payments.gateway.test

import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.redis.testcontainers.RedisContainer
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.util.UUID

@DirtiesContext
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "PT10M")
abstract class BaseTest {

    @Autowired
    lateinit var webClient: WebTestClient

    protected fun randomResponse() = (0..10).associate {
        UUID.randomUUID().toString() to UUID.randomUUID().toString()
    }

    companion object {
        @JvmField
        @RegisterExtension
        val p2pMock = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().port(0))
            .build()!!

        @JvmField
        @RegisterExtension
        val p2mMock = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().port(0))
            .build()!!

        @JvmField
        @RegisterExtension
        val c2aMock = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().port(0))
            .build()!!

        @JvmField
        @RegisterExtension
        val tokenizationMock = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().port(0))
            .build()!!

        @JvmField
        @RegisterExtension
        val paymentServiceMock = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().port(0))
            .build()!!

        @Container
        @JvmStatic
        val redisContainer = RedisContainer(DockerImageName.parse("redis:6.2"))

        @JvmStatic
        @DynamicPropertySource
        fun registerWiremockUrl(registry: DynamicPropertyRegistry) {
            registry.apply {
                add("payments-gateway.routes.p2p.url") { p2pMock.baseUrl() }
                add("payments-gateway.routes.p2m.url") { p2mMock.baseUrl() }
                add("payments-gateway.routes.iban.url") { c2aMock.baseUrl() }
                add("payments-gateway.routes.tokenization.url") { tokenizationMock.baseUrl() }
                add("payments-gateway.payments-service.url") { paymentServiceMock.baseUrl() }
                add("spring.redis.host") { redisContainer.host }
                add("spring.redis.port") { redisContainer.firstMappedPort }
            }
        }
    }
}
