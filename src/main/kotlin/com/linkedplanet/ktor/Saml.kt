/*
 * #%L
 * ktor-onelogin-saml
 * %%
 * Copyright (C) 2021 linked-planet GmbH
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

package com.linkedplanet.ktor

import com.onelogin.saml2.Auth
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.routing.*
import io.ktor.server.servlet.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.*
import org.eclipse.jetty.server.Request
import javax.servlet.http.*

suspend fun PipelineContext<Unit, ApplicationCall>.redirectToIdentityProvider() {
    withSAMLAuth { auth ->
        withContext(Dispatchers.IO) {
            auth.login()
        }
    }
}

suspend fun PipelineContext<Unit, ApplicationCall>.withSAMLAuth(handler: suspend (Auth) -> Unit) {
    val auth = Auth(SamlConfig.saml2Settings, call.getServletRequest(), call.getServletResponse())
    call
    handler(auth)
}

fun ApplicationCall.getServletRequest(): HttpServletRequest {
    val servletRequest = getAsyncServletApplicationCall().request.servletRequest
    // when running behind proxy with ssl offloading, the request must be customized to use the original scheme
    // Jetty request customizers won't be executed by ktor so we have to do it manually here
    (servletRequest as Request).scheme = request.origin.scheme
    return servletRequest
}

fun ApplicationCall.getServletResponse(): HttpServletResponse {
    val servletApplicationResponse = getAsyncServletApplicationCall().response
    val responseField = ServletApplicationResponse::class.java.getDeclaredField("servletResponse")
    responseField.isAccessible = true
    return responseField.get(servletApplicationResponse) as HttpServletResponse
}

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
