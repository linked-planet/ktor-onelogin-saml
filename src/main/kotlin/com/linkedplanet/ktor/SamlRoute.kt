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
package com.linkedplanet.ktor

import com.onelogin.saml2.Auth
import com.onelogin.saml2.settings.Saml2Settings
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.locations.*
import io.ktor.server.locations.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.util.pipeline.*
import kotlinx.html.*


const val SAMLEndpointBasePath: String = "/sso/saml"

@KtorExperimentalLocationsAPI
@Location("$SAMLEndpointBasePath/metadata")
class Metadata

@KtorExperimentalLocationsAPI
@Location("$SAMLEndpointBasePath/acs")
class AttributeConsumerService

@KtorExperimentalLocationsAPI
@Location("$SAMLEndpointBasePath/sls")
class SingleLogoutService

@Suppress("unused")
@KtorExperimentalLocationsAPI
inline fun <reified S : Any> Route.saml(
    samlEnabled: Boolean,
    crossinline authorizer: (Auth) -> Boolean,
    crossinline createSession: (String) -> S
) {

    get<Metadata> {
        requireSAMLEnabled(samlEnabled) {
            val settings = SamlConfig.saml2Settings
            val metadata = settings.spMetadata
            val errors = Saml2Settings.validateMetadata(metadata)
            requireValid(errors) {
                call.respond(metadata)
            }
        }
    }

    post<AttributeConsumerService> {
        requireSAMLEnabled(samlEnabled) {
            withSAMLAuth { auth ->
                // saml auth / ktor "consume" the form parameters so we won't be able to get the relay state anymore
                val servletRequest = call.getServletRequest()
                val relayState = servletRequest.getParameter("RelayState")
                call.application.environment.log.debug("RelayState: $relayState")

                auth.processResponse()
                requireValid(auth) {
                    if (!auth.isAuthenticated) {
                        call.respond(HttpStatusCode.BadRequest, "Not authenticated")
                    } else if (!authorizer(auth)) {
                        call.respond(HttpStatusCode.Forbidden, "Not permitted")
                    } else {
                        val session = createSession(auth.nameId)
                        call.sessions.set(session)

                        if (relayState != null) {
                            call.respondRedirect(relayState)
                        } else {
                            call.respondRedirect { encodedPath = "/" }
                        }
                    }
                }
            }
        }
    }

    get<SingleLogoutService> {
        requireSAMLEnabled(samlEnabled) {
            withSAMLAuth { auth ->
                // SLORequest: Will validate and redirect to IdP
                // SLOResponse: Will validate and clear session
                // In any case, keepLocalSession as the library's way of clearing the session is incompatible with ktor
                auth.processSLO(true, null)
                // in case of SLOResponse, we are still here
                requireValid(auth) {
                    call.sessions.clear<S>()
                    call.respondRedirect { encodedPath = "/" }
                }
            }
        }
    }

}


suspend fun PipelineContext<Unit, ApplicationCall>.requireSAMLEnabled(
    samlEnabled: Boolean,
    handler: suspend () -> Unit
) {
    if (!samlEnabled) call.respond(HttpStatusCode.BadRequest) else handler()
}

suspend fun PipelineContext<Unit, ApplicationCall>.requireValid(errors: List<String>, handler: suspend () -> Unit) {
    requireValid(errors, handler) { _ ->
        call.application.environment.log.error(errors.joinToString())
        call.respondHtml(HttpStatusCode.BadRequest) {
            body {
                ul {
                    errors.forEach {
                        li { +it }
                    }
                }
            }
        }
    }
}

suspend fun PipelineContext<Unit, ApplicationCall>.requireValid(auth: Auth, handler: suspend () -> Unit) {
    requireValid(auth, handler) { errors ->
        call.application.environment.log.error(errors.joinToString())
        call.respondHtml(HttpStatusCode.BadRequest) {
            body {
                val lastErrorReason = auth.lastErrorReason
                if (auth.isDebugActive && !lastErrorReason.isNullOrBlank()) {
                    p { +lastErrorReason }
                }
                ul {
                    errors.forEach {
                        li { +it }
                    }
                }
            }
        }
    }
}
