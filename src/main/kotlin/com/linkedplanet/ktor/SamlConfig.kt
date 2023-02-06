/*
 * #%L
 * ktor-onelogin-saml
 * %%
 * Copyright (C) 2022 linked-planet GmbH
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

import com.onelogin.saml2.settings.Saml2Settings
import com.onelogin.saml2.settings.SettingsBuilder
import com.typesafe.config.ConfigFactory
import java.net.URL

object SamlConfig {

    private val config = ConfigFactory.load().getConfig("saml")

    private val ipConfig = config.getConfig("identityProvider")
    private val spConfig = config.getConfig("serviceProvider")

    private val samlBaseUrl = spConfig.getString("samlBaseUrl")
    private val samlMetadataEndpoint = spConfig.getString("samlMetadataEndpoint")
    private val samlConsumerServiceEndpoint = spConfig.getString("samlConsumerServiceEndpoint")
    private val samlSingleLogoutServiceEndpoint = spConfig.getString("samlSingleLogoutServiceEndpoint")

    val saml2Settings: Saml2Settings =
            SettingsBuilder().fromValues(
                    mapOf<String, Any>(
                            // General
                            SettingsBuilder.STRICT_PROPERTY_KEY to config.getBoolean("strict"),
                            SettingsBuilder.DEBUG_PROPERTY_KEY to config.getBoolean("debug"),

                            // Identifier of the SP entity (must be a URI)
                            SettingsBuilder.SP_ENTITYID_PROPERTY_KEY to "$samlBaseUrl/$samlMetadataEndpoint",

                            // URL Location where the <Response> from the IdP will be returned
                            SettingsBuilder.SP_ASSERTION_CONSUMER_SERVICE_URL_PROPERTY_KEY to URL("$samlBaseUrl/$samlConsumerServiceEndpoint"),

                            // Specifies info about where and how the <Logout Response> message MUST be
                            // returned to the requester, in this case our SP
                            SettingsBuilder.SP_SINGLE_LOGOUT_SERVICE_URL_PROPERTY_KEY to URL("$samlBaseUrl/$samlSingleLogoutServiceEndpoint"),

                            // How to identify subjects
                            SettingsBuilder.SP_NAMEIDFORMAT_PROPERTY_KEY to spConfig.getString("nameIdFormat"),

                            // Identifier of the IdP entity (must be a URI)
                            SettingsBuilder.IDP_ENTITYID_PROPERTY_KEY to ipConfig.getString("entityId"),

                            // URL Target of the IdP where the SP will send the Authentication Request Message
                            SettingsBuilder.IDP_SINGLE_SIGN_ON_SERVICE_URL_PROPERTY_KEY to ipConfig.getString("loginUrl"),

                            // URL Location of the IdP where the SP will send the SLO Request
                            SettingsBuilder.IDP_SINGLE_LOGOUT_SERVICE_URL_PROPERTY_KEY to ipConfig.getString("logoutUrl"),

                            // Public x509 certificate of the IdP
                            SettingsBuilder.IDP_X509CERT_PROPERTY_KEY to ipConfig.getString("certificate"),

                            // Organization data
                            SettingsBuilder.ORGANIZATION_NAME to spConfig.getString("organizationName"),
                            SettingsBuilder.ORGANIZATION_DISPLAYNAME to spConfig.getString("organizationDisplayName"),
                            SettingsBuilder.ORGANIZATION_URL to spConfig.getString("organizationUrl"),
                            SettingsBuilder.ORGANIZATION_LANG to spConfig.getString("organizationLang")
                    )
            ).build()

}
