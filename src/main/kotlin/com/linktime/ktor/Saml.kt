@file:Suppress("unused")

package com.linktime.ktor

import com.onelogin.saml2.Auth
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.features.origin
import io.ktor.routing.RoutingApplicationCall
import io.ktor.server.engine.EngineAPI
import io.ktor.server.servlet.AsyncServletApplicationCall
import io.ktor.server.servlet.ServletApplicationResponse
import io.ktor.util.pipeline.PipelineContext
import org.eclipse.jetty.server.Request
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


suspend fun PipelineContext<Unit, ApplicationCall>.withSAMLAuth(handler: suspend (Auth) -> Unit) {
    val auth = Auth(SamlConfig.saml2Settings, call.getServletRequest(), call.getServletResponse())
    handler(auth)
}

@UseExperimental(EngineAPI::class)
fun ApplicationCall.getServletRequest(): HttpServletRequest {
    val servletRequest = getAsyncServletApplicationCall().request.servletRequest
    // when running behind proxy with ssl offloading, the request must be customized to use the original scheme
    // Jetty request customizers won't be executed by ktor so we have to do it manually here
    (servletRequest as Request).scheme = request.origin.scheme
    return servletRequest
}

@UseExperimental(EngineAPI::class)
fun ApplicationCall.getServletResponse(): HttpServletResponse {
    val servletApplicationResponse = getAsyncServletApplicationCall().response
    val responseField = ServletApplicationResponse::class.java.getDeclaredField("servletResponse")
    responseField.isAccessible = true
    return responseField.get(servletApplicationResponse) as HttpServletResponse
}

@UseExperimental(EngineAPI::class)
private fun ApplicationCall.getAsyncServletApplicationCall(): AsyncServletApplicationCall {
    val routingApplicationCall = (request.call as RoutingApplicationCall)
    val callField = RoutingApplicationCall::class.java.getDeclaredField("call")
    callField.isAccessible = true
    return callField.get(routingApplicationCall) as AsyncServletApplicationCall
}


suspend fun requireValid(auth: Auth, handler: suspend () -> Unit, respondErrors: suspend (List<String>) -> Unit) {
    val errors = auth.errors
    if (errors.isEmpty()) handler() else respondErrors(errors)
}

suspend fun requireValid(
        errors: List<String>,
        handler: suspend () -> Unit,
        respondErrors: suspend (List<String>) -> Unit
) {
    if (errors.isEmpty()) handler() else respondErrors(errors)
}