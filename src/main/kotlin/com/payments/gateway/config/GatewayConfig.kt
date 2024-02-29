package com.payments.gateway.config

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import com.payments.gateway.data.PaymentType
import com.payments.gateway.dto.CheckPaymentRequest
import com.payments.gateway.dto.PatchPaymentRequest
import com.payments.gateway.dto.PaymentRequest
import com.payments.gateway.properties.RoutingProperties
import com.payments.gateway.services.PaymentsService
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.cloud.gateway.route.builder.RouteLocatorDsl
import org.springframework.cloud.gateway.route.builder.filters
import org.springframework.cloud.gateway.route.builder.routes
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.buffer.DefaultDataBuffer
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.PATCH
import org.springframework.http.HttpMethod.POST
import org.springframework.web.server.ServerWebExchange
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono

@Configuration
class GatewayConfig(
    private val mapper: ObjectMapper,
    private val paymentsService: PaymentsService,
    private val routingProperties: RoutingProperties,
) {

    companion object {
        const val BASE_PATH = "/payments"
    }

    @Bean
    fun createRoutes(builder: RouteLocatorBuilder): RouteLocator = builder.routes {
        PaymentType.values().forEach {
            buildCheckRoute(it)
            buildPaymentsRoute(it)
            buildGetPaymentRoute(it)
            buildRepeatPaymentRoute(it)
            buildCancelPaymentRoute(it)
        }
    }

    private fun RouteLocatorDsl.buildCheckRoute(paymentType: PaymentType) {
        val route = paymentType.toRoute()

        if (route.check == null) return

        route(id = "$paymentType-check") {
            method(POST)
                .and().path("$BASE_PATH/check")
                .and().readBody(CheckPaymentRequest::class.java) {
                    it.creditDstType in paymentType.creditDestinationTypes
                }

            filters {
                setPath(route.check)
                addPaymentTypeToResponse(paymentType)
            }

            uri(route.url)
        }
    }

    private fun RouteLocatorDsl.buildPaymentsRoute(paymentType: PaymentType) {
        val route = paymentType.toRoute()

        if (route.payment == null) return

        route(id = "$paymentType-payments") {
            method(POST)
                .and().path(BASE_PATH)
                .and().readBody(PaymentRequest::class.java) {
                    it.retrieveCreditDstType(mapper) in paymentType.creditDestinationTypes
                }

            filters {
                setPath(route.payment)
                addPaymentTypeToResponse(paymentType)
            }

            uri(route.url)
        }
    }

    private fun RouteLocatorDsl.buildGetPaymentRoute(paymentType: PaymentType) {
        val route = paymentType.toRoute()

        if (route.status == null) return

        route(id = "get-$paymentType") {
            method(GET)
                .and().path("$BASE_PATH/{payment_id}")
                .and().asyncPredicate { exchange ->
                    exchange
                        ?.getPathVariable("payment_id")
                        ?.let { paymentId ->
                            paymentsService.getPaymentTypeById(paymentId).map { it == paymentType }
                                .switchIfEmpty { false.toMono() }
                        }
                        ?: false.toMono()
                }

            filters {
                setPath(route.status)
                addPaymentTypeToResponse(paymentType)
            }

            uri(route.url)
        }
    }

    private fun RouteLocatorDsl.buildRepeatPaymentRoute(paymentType: PaymentType) {
        val route = paymentType.toRoute()

        if (route.repeat == null) return

        route(id = "repeat-$paymentType") {
            method(GET)
                .and().path("$BASE_PATH/repeat/{operation_id}")
                .and().asyncPredicate { exchange ->
                    exchange
                        ?.getPathVariable("operation_id")
                        ?.let { operationId ->
                            paymentsService.getPaymentTypeByOperationId(operationId).map { it == paymentType }
                                .switchIfEmpty { false.toMono() }
                        }
                        ?: false.toMono()
                }

            filters {
                setPath(route.repeat)
                addPaymentTypeToResponse(paymentType)
            }

            uri(route.url)
        }
    }

    private fun RouteLocatorDsl.buildCancelPaymentRoute(paymentType: PaymentType) {
        val route = paymentType.toRoute()

        if (route.cancel == null) return

        route(id = "cancel-$paymentType") {
            method(PATCH)
                .and().path("$BASE_PATH/cancel")
                .and().asyncPredicate { exchange ->
                    ServerWebExchangeUtils.cacheRequestBody(exchange) { _ ->
                        val bodyBytes = exchange.attributes[CACHED_REQUEST_BODY_ATTR] as DefaultDataBuffer
                        val body = mapper.readValue<PatchPaymentRequest>(bodyBytes.asByteBuffer().array())
                        paymentsService.getPaymentTypeById(body.paymentId)
                            .map { it == paymentType }
                            .switchIfEmpty { false.toMono() }
                    }
                }

            filters {
                setPath(route.cancel)
                addPaymentTypeToResponse(paymentType)
            }

            uri(route.url)
        }
    }

    private fun GatewayFilterSpec.addPaymentTypeToResponse(paymentType: PaymentType) {
        modifyResponseBody(JsonNode::class.java, JsonNode::class.java) { _, body ->
            if (body is ObjectNode) {
                body.put("payment_type", paymentType.toString())
            }
            body.toMono()
        }
    }

    private fun PaymentType.toRoute() = when (this) {
        PaymentType.P2P -> routingProperties.p2p
        PaymentType.P2M -> routingProperties.p2m
        PaymentType.IBAN -> routingProperties.iban
        PaymentType.TOKENIZATION -> routingProperties.tokenization
    }

    private fun ServerWebExchange.getPathVariable(name: String) =
        ServerWebExchangeUtils.getUriTemplateVariables(this)
            ?.get(name)
}
