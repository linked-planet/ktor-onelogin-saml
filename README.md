# ktor-onelogin-saml
Integrates [ktor](ktor.io) with onelogin's
[https://github.com/onelogin/java-saml](java-saml) library.


## Limitations
Projects using this library will incur following limitations
on themselves:

- Must use Jetty engine, as `java-saml` requires servlet classes
- Breaks ktor public API using reflection, which could lead to
  errors if using a more recent ktor version than this library.
  You might need to fix it yourself. Pull requests are welcome ;-)

There are no automated integration tests but the code is used
productively in at least one business-critical application with
strong uptime requirements.


## Configuration
Please refer to [src/main/resources/reference.conf](reference.conf).


## General Usage
Within your route, you can use `withSAMLAuth` to get a fully configured
SAML Auth object.

```kotlin
withSAMLAuth { auth ->
   // do whatever with auth
}
```

Some of the Auth methods are implemented in a blocking way. To handle
this, use IO dispatcher context:

```kotlin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

withSAMLAuth { auth ->
    withContext(Dispatchers.IO) {
        auth.login(targetUrl)
    }
}
```


## Error Handling
It is recommended to define error handler functions based on the provided
`requireValid` functions:

```kotlin
suspend fun PipelineContext<Unit, ApplicationCall>.requireValid(
    errors: List<String>, handler: suspend () -> Unit) {
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
```

```kotlin
suspend fun PipelineContext<Unit, ApplicationCall>.requireValid(
    auth: Auth, handler: suspend () -> Unit) {
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
```

You can then use these error handlers like in the following example:

```kotlin
post<AttributeConsumerService> {
    withSAMLAuth { auth ->
        auth.processResponse()
        requireValid(auth) {
            if (!auth.isAuthenticated) {
                call.respond(HttpStatusCode.BadRequest, "Not authenticated")
            } else {
                val nameId = auth.nameId
                // ...
            }
        }
    }
}
```


## Background & Alternatives
- [https://wiki.shibboleth.net/confluence/display/OpenSAML/Home](OpenSAML)
  reached end of life.
- Custom implementation of Auth on top of
  [https://github.com/onelogin/java-saml/tree/master/core](java-saml) is
  what should be done. But it is quite some work.
- Please see https://github.com/ktorio/ktor/issues/1212 for more
details.