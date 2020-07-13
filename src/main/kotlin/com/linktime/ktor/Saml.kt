/*
 * #%L
 * ktor-onelogin-saml
 * %%
 * Copyright (C) 2019 link-time GmbH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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

@OptIn(EngineAPI::class)
fun ApplicationCall.getServletRequest(): HttpServletRequest {
    val servletRequest = getAsyncServletApplicationCall().request.servletRequest
    // when running behind proxy with ssl offloading, the request must be customized to use the original scheme
    // Jetty request customizers won't be executed by ktor so we have to do it manually here
    (servletRequest as Request).scheme = request.origin.scheme
    return servletRequest
}

@OptIn(EngineAPI::class)
fun ApplicationCall.getServletResponse(): HttpServletResponse {
    val servletApplicationResponse = getAsyncServletApplicationCall().response
    val responseField = ServletApplicationResponse::class.java.getDeclaredField("servletResponse")
    responseField.isAccessible = true
    return responseField.get(servletApplicationResponse) as HttpServletResponse
}

@OptIn(EngineAPI::class)
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