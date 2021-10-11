package com.example

import io.ktor.http.*
import kotlin.test.*
import io.ktor.server.testing.*

class ApplicationTest {
    @Test
    fun testRoot() {
        withTestApplication({ module() }) {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("Hello World!", response.content)
            }
        }
    }

    @Test
    fun testXml() {
        withTestApplication({ module() }) {
            handleRequest(HttpMethod.Get, "/xml/kotlinx-serialization").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("application/xml", response.headers["Content-Type"]?.substringBefore(";"))
                assertEquals("<ExampleResponse data=\"Hello World!\"/>", response.content)
            }
        }
    }
}
