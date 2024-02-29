package com.payments.gateway.config

import com.github.tomakehurst.wiremock.http.RequestMethod
import com.marcinziolo.kotlin.wiremock.contains
import com.marcinziolo.kotlin.wiremock.equalTo
import com.marcinziolo.kotlin.wiremock.get
import com.marcinziolo.kotlin.wiremock.patch
import com.marcinziolo.kotlin.wiremock.post
import com.marcinziolo.kotlin.wiremock.returnsJson
import com.marcinziolo.kotlin.wiremock.verify
import com.payments.gateway.config.GatewayConfig.Companion.BASE_PATH
import com.payments.gateway.data.PaymentType
import com.payments.gateway.test.BaseTest
import com.payments.gateway.test.toJson
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.springframework.http.HttpMethod
import java.util.Base64
import java.util.UUID
import java.util.stream.Stream

class GatewayConfigTest : BaseTest() {

    private class CheckArgumentSource : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<Arguments> = Stream.of(
            arguments("pan", "/check", PaymentType.P2P),
            arguments("panId", "/check", PaymentType.P2P),
            arguments("external_card_id", "/check", PaymentType.P2P),
            arguments("card_token", "/check", PaymentType.P2P),
            arguments("mobile", "/smpay/check", PaymentType.P2M),
            arguments("iban", "/smpay/c2a/check", PaymentType.IBAN),
        )
    }

    @ParameterizedTest
    @ArgumentsSource(CheckArgumentSource::class)
    fun `should correctly route post check payment request`(
        destinationType: String,
        expectedPath: String,
        paymentType: PaymentType,
    ) {
        val expectedServer = paymentType.getWiremock()
        val requestBody = mapOf(
            "credit_dst_id" to UUID.randomUUID().toString(),
            "credit_dst_type" to destinationType,
            "debit_src_id" to UUID.randomUUID().toString(),
            "debit_src_type" to UUID.randomUUID().toString(),
            "credit_amount" to 123,
        )

        val expectedResponse = randomResponse()

        expectedServer.post {
            url equalTo expectedPath
            body equalTo requestBody.toJson()
        } returnsJson {
            body = expectedResponse.toJson()
        }

        webClient
            .post()
            .uri("$BASE_PATH/check")
            .bodyValue(requestBody)
            .exchange()
            .expectStatus().isOk
            .expectBody().json(expectedResponse.toJsonWithType(paymentType), true)

        expectedServer.verify {
            method = RequestMethod.POST
            url equalTo expectedPath
            body equalTo requestBody.toJson()
        }
    }

    private class PostPaymentsArgumentSource : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<Arguments> = Stream.of(
            arguments("pan", "/payments", PaymentType.P2P),
            arguments("panId", "/payments", PaymentType.P2P),
            arguments("external_card_id", "/payments", PaymentType.P2P),
            arguments("card_token", "/payments", PaymentType.P2P),
            arguments("card_token", "/payments", PaymentType.P2P),
            arguments("mobile", "/smpay/payments", PaymentType.P2M),
            arguments("iban", "/smpay/c2a", PaymentType.IBAN),
            arguments(null, "/smpay/tokenization", PaymentType.TOKENIZATION),
        )
    }

    @ParameterizedTest
    @ArgumentsSource(PostPaymentsArgumentSource::class)
    fun `should correctly route post payments payment request`(
        destinationType: String?,
        expectedPath: String,
        paymentType: PaymentType,
    ) {
        val expectedServer = paymentType.getWiremock()
        val requestBody = mapOf(
            "intent" to Base64.getEncoder().encodeToString(
                mapOf(
                    "debit_src_type" to UUID.randomUUID().toString(),
                    "credit_dst_type" to destinationType,
                ).toJson().toByteArray(),
            ),
            "sign" to UUID.randomUUID().toString(),
        )

        val expectedResponse = randomResponse() + mapOf(
            "id" to UUID.randomUUID().toString(),
            "operation_id" to UUID.randomUUID().toString(),
        )

        expectedServer.post {
            url equalTo expectedPath
            body equalTo requestBody.toJson()
        } returnsJson {
            body = expectedResponse.toJson()
        }

        webClient
            .post()
            .uri(BASE_PATH)
            .bodyValue(requestBody)
            .exchange()
            .expectStatus().isOk
            .expectBody().json(expectedResponse.toJsonWithType(paymentType), true)

        expectedServer.verify {
            method = RequestMethod.POST
            url equalTo expectedPath
            body equalTo requestBody.toJson()
        }
    }

    private class GetPaymentArgumentSource : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<Arguments> = Stream.of(
            arguments("UAPI", "/payments", PaymentType.P2P),
            arguments("TRANZZO", "/payments", PaymentType.P2P),
            arguments("TRANZZO_P2P", "/payments", PaymentType.P2P),
            arguments("P2M_PROVIDER", "/smpay/payments", PaymentType.P2M),
            arguments("MOBILE", "/smpay/payments", PaymentType.P2M),
            arguments("IBAN", "/smpay/c2a", PaymentType.IBAN),
            arguments("TRANZZO_TOKENIZATION", "/smpay/tokenization", PaymentType.TOKENIZATION),
        )
    }

    @ParameterizedTest
    @ArgumentsSource(GetPaymentArgumentSource::class)
    fun `should correctly route get status request`(
        creditProvider: String?,
        expectedPath: String,
        paymentType: PaymentType,
    ) {
        val expectedServer = paymentType.getWiremock()
        val paymentId = UUID.randomUUID().toString()

        paymentServiceMock.get {
            urlPath equalTo "/payments/routing_direction"
            queryParams contains "payment_id" equalTo paymentId
        } returnsJson {
            body = mapOf("credit_provider" to creditProvider).toJson()
        }

        val expectedResponse = randomResponse()

        expectedServer.get {
            url equalTo "$expectedPath/$paymentId"
        } returnsJson {
            body = expectedResponse.toJson()
        }

        webClient
            .get()
            .uri("$BASE_PATH/$paymentId")
            .exchange()
            .expectStatus().isOk
            .expectBody().json(expectedResponse.toJsonWithType(paymentType), true)

        paymentServiceMock.verify {
            method = RequestMethod.GET
            urlPath equalTo "/payments/routing_direction"
            queryParams contains "payment_id" equalTo paymentId
        }
        expectedServer.verify {
            method = RequestMethod.GET
            url equalTo "$expectedPath/$paymentId"
        }
    }

    private class RepeatPaymentArgumentSource : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<Arguments> = Stream.of(
            arguments("UAPI", "/repeat", PaymentType.P2P),
            arguments("TRANZZO", "/repeat", PaymentType.P2P),
            arguments("TRANZZO_P2P", "/repeat", PaymentType.P2P),
            arguments("P2M_PROVIDER", "/smpay/repeat", PaymentType.P2M),
            arguments("MOBILE", "/smpay/repeat", PaymentType.P2M),
        )
    }

    @ParameterizedTest
    @ArgumentsSource(RepeatPaymentArgumentSource::class)
    fun `should correctly route get repeat request`(
        creditProvider: String?,
        expectedPath: String,
        paymentType: PaymentType,
    ) {
        val expectedServer = paymentType.getWiremock()
        val operationId = UUID.randomUUID().toString()

        paymentServiceMock.get {
            urlPath equalTo "/payments/routing_direction"
            queryParams contains "operation_id" equalTo operationId
        } returnsJson {
            body = mapOf("credit_provider" to creditProvider).toJson()
        }

        val expectedResponse = randomResponse()

        expectedServer.get {
            url equalTo "$expectedPath/$operationId"
        } returnsJson {
            body = expectedResponse.toJson()
        }

        webClient
            .get()
            .uri("$BASE_PATH/repeat/$operationId")
            .exchange()
            .expectStatus().isOk
            .expectBody().json(expectedResponse.toJsonWithType(paymentType), true)

        paymentServiceMock.verify {
            method = RequestMethod.GET
            urlPath equalTo "/payments/routing_direction"
            queryParams contains "operation_id" equalTo operationId
        }
        expectedServer.verify {
            method = RequestMethod.GET
            url equalTo "$expectedPath/$operationId"
        }
    }

    private class CancelPaymentArgumentSource : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<Arguments> = Stream.of(
            arguments("UAPI", "/cancel-payment", PaymentType.P2P),
            arguments("TRANZZO", "/cancel-payment", PaymentType.P2P),
            arguments("TRANZZO_P2P", "/cancel-payment", PaymentType.P2P),
            arguments("P2M_PROVIDER", "/smpay/cancel-payment", PaymentType.P2M),
            arguments("MOBILE", "/smpay/cancel-payment", PaymentType.P2M),
        )
    }

    @ParameterizedTest
    @ArgumentsSource(CancelPaymentArgumentSource::class)
    fun `should correctly route patch cancel request`(
        creditProvider: String?,
        expectedPath: String,
        paymentType: PaymentType,
    ) {
        val expectedServer = paymentType.getWiremock()
        val paymentId = UUID.randomUUID().toString()

        paymentServiceMock.get {
            urlPath equalTo "/payments/routing_direction"
            queryParams contains "payment_id" equalTo paymentId
        } returnsJson {
            body = mapOf("credit_provider" to creditProvider).toJson()
        }

        val expectedResponse = randomResponse()

        expectedServer.patch {
            url equalTo expectedPath
        } returnsJson {
            body = expectedResponse.toJson()
        }

        webClient
            .patch()
            .uri("$BASE_PATH/cancel")
            .bodyValue(mapOf("payment_id" to paymentId))
            .exchange()
            .expectStatus().isOk
            .expectBody().json(expectedResponse.toJsonWithType(paymentType), true)

        paymentServiceMock.verify {
            method = RequestMethod.GET
            urlPath equalTo "/payments/routing_direction"
            queryParams contains "payment_id" equalTo paymentId
        }
        expectedServer.verify {
            method = RequestMethod.PATCH
            url equalTo expectedPath
        }
    }

    private class NotFoundArgumentSource : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<Arguments> = Stream.of(
            arguments("/check", HttpMethod.POST),
            arguments("/payments", HttpMethod.POST),
            arguments("/payments/payment_id", HttpMethod.GET),
            arguments("/repeat/operation_id", HttpMethod.GET),
            arguments("cancel-payment", HttpMethod.PATCH),
        )
    }

    private fun PaymentType.getWiremock() = when (this) {
        PaymentType.P2P -> p2pMock
        PaymentType.P2M -> p2mMock
        PaymentType.IBAN -> c2aMock
        PaymentType.TOKENIZATION -> tokenizationMock
    }

    @ParameterizedTest
    @ArgumentsSource(NotFoundArgumentSource::class)
    fun `should return not found if unknown destination`(path: String, method: HttpMethod) {
        webClient
            .method(method)
            .uri(path)
            .exchange()
            .expectStatus().isNotFound
    }

    private fun Map<String, Any>.toJsonWithType(type: PaymentType) = (this + ("payment_type" to type)).toJson()

    @Test
    fun `should allow get actuator health`() {
        webClient
            .get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().isOk
    }
}
