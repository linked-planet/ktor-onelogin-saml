# ktor-onelogin-saml

[![Maven Central](https://img.shields.io/maven-central/v/com.linked-planet/ktor-onelogin-saml.svg?label=maven%20central)](https://search.maven.org/search?q=g:%22com.linked-planet%22%20AND%20a:%22ktor-onelogin-saml%22)
![Build Status](https://github.com/linked-planet/ktor-onelogin-saml/workflows/Gradle/badge.svg)
[![GitHub License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)

Integrates [ktor](ktor.io) with onelogin's
[java-saml](https://github.com/onelogin/java-saml) library.


## Limitations
Projects using this library will incur following limitations
on themselves:

- Must use Jetty engine, as `java-saml` requires servlet classes
- Breaks ktor public API using reflection, which could lead to
  errors if using a more recent ktor version than this library.
  You might need to fix it yourself. Pull requests are welcome ;-)
- Ties your app to a particular version of ktor

There are no automated integration tests but the code is used
productively in at least one business-critical application with
strong uptime requirements.


## Configuration
Please refer to [reference.conf](src/main/resources/reference.conf).


## Usage

### Basic Installation
#### 1) Instantiate SAML route in routes configuration:

```kotlin
routing {
   saml<Session>(
        AppConfig.samlEnabled,
        // lambda to add custom authorization logic after successful authentication
        authorizer = {_ -> true},
        // create session object after authentication + authorization are successful
        createSession = { name -> Session(name) })
}
```

#### 2) Redirect users with no session to identity provider

in index route:
```kotlin
// if the user does not have a session and saml-sso is enabled, we redirect the user to the identity provider
if (session == null && ssoEnabled) {
    redirectToIdentityProvider()
}
``` 


### Advanced Usage
We declared all components of the library public, so you can build the
behavior you need by yourself if the basic installation is not sufficient
for you.

You could even opt to not use the predefined SamlRoute at all and build
a custom one from scratch. However, please also consider the alternative
of filing a pull request to make the route provided by this library more
configurable.

Within your route, you can use `withSAMLAuth` to get a fully configured
SAML Auth object.

```kotlin
withSAMLAuth { auth ->
   // do whatever with auth
}
```

Some Auth methods are implemented in a blocking way. To handle
this, use IO dispatcher context:

```kotlin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

withSAMLAuth { auth ->
    withContext(Dispatchers.IO) {
        auth.login()
    }
}
```


## Background & Alternatives
- [OpenSAML](https://wiki.shibboleth.net/confluence/display/OpenSAML/Home)
  has reached end of life.
- Custom implementation of Auth on top of
  [java-saml](https://github.com/onelogin/java-saml/tree/master/core) is
  what should be done., but it is quite some work.
- Please see https://github.com/ktorio/ktor/issues/1212 for more
  details.
