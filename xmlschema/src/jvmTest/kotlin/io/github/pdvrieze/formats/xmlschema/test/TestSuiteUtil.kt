/*
 * Copyright (c) 2026.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You should have  received a copy of the license
 * with the source distribution. Alternatively, you may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.github.pdvrieze.formats.xmlschema.test

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.toAnyUri
import io.github.pdvrieze.formats.xmlschema.resolved.SchemaVersion
import io.github.pdvrieze.formats.xmlschema.resolved.SimpleResolver
import io.github.pdvrieze.formats.xmlschemaTests.Resource
import io.github.pdvrieze.formats.xmlschemaTests.io.github.pdvrieze.formats.xmlschemaTests.withXmlReader
import io.github.pdvrieze.formats.xmlschemaTests.openStream
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.newReader
import nl.adaptivity.xmlutil.xmlStreaming
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.w3.xml.xmschematestsuite.*
import java.net.URI
import java.net.URL
import kotlin.experimental.ExperimentalTypeInference
import kotlin.test.assertEquals
import kotlin.test.assertFails

internal suspend fun SequenceScope<DynamicNode>.addSchemaTests(
    baseResource: Resource,
    group: TSTestGroup,
    testSetVersion: List<SchemaVersion>?
) {
    var targetSchemaDoc: TSSchemaDocument? = null
    group.schemaTest?.let { schemaTest ->
        val documentation = group.documentationString()
        if (schemaTest.schemaDocuments.size == 1) {
            val schemaDoc = schemaTest.schemaDocuments.single()
            addSchemaDocTest(baseResource, schemaTest, schemaDoc, documentation, group.version?.let(::listOf) ?: testSetVersion)
            targetSchemaDoc = schemaDoc
        } else {
            dynamicContainer("Schema documents") {
                for (schemaDoc in schemaTest.schemaDocuments) {
                    if (true || schemaDoc.href.contains("ipo.xsd")) {
                        addSchemaDocTest(baseResource, schemaTest, schemaDoc, documentation, group.version?.let(::listOf) ?: testSetVersion)
                        targetSchemaDoc = schemaDoc
                    }
                }
            }
        }
    }
    if (false && targetSchemaDoc != null && group.instanceTests.isNotEmpty()) {

        for (instanceTest in group.instanceTests) {
            addInstanceTest(baseResource, instanceTest, targetSchemaDoc!!, group.documentationString())
        }
    }
}

internal suspend fun SequenceScope<DynamicNode>.addInstanceTest(
    setBaseUrl: Resource,
    instanceTest: TSInstanceTest,
    schemaDoc: TSSchemaDocument,
    documentation: String
) {
    val instanceDoc = instanceTest.instanceDocument
    val resolver = SimpleResolver(setBaseUrl)
    dynamicTest("Instance document ${instanceDoc.href} exists") {
        setBaseUrl.resolve(instanceDoc.href).openStream().use { stream ->
            Assertions.assertNotNull(stream)
        }
    }
    if (instanceTest.expected.firstOrNull { it.version != "1.0" }?.validity == TSValidityOutcome.VALID) {
        val schemaLocation = schemaDoc.href.toAnyUri()
        val schema = resolver.readSchema(schemaLocation).resolve(resolver)

    }

//    assertNotNull()
}

internal suspend fun SequenceScope<DynamicNode>.addSchemaDocTest(
    setBaseResource: Resource,
    schemaTest: TSSchemaTest,
    schemaDoc: TSSchemaDocument,
    documentation: String,
    testGroupVersions: List<SchemaVersion>?,
) {
    val defaultVersions = when(schemaTest.version) {
        "1.0" -> listOf(SchemaVersion.V1_0)
        "1.1" -> listOf(SchemaVersion.V1_1)
        else -> testGroupVersions ?: SchemaVersion.entries
    }
    val resolver = SimpleResolver(setBaseResource)

    dynamicTest("Test ${schemaTest.name} - Schema document ${schemaDoc.href} exists") {
        setBaseResource.resolve(schemaDoc.href).openStream().use { stream ->
            Assertions.assertNotNull(stream)
        }
    }

    val expecteds = mutableMapOf<SchemaVersion, TSExpected>()
    for (e in schemaTest.expected) {
        val version = when (e.version) {
            "1.0" -> SchemaVersion.V1_0
            "1.1" -> SchemaVersion.V1_1
            else -> null
        }
        when (version) {
            null -> {
                for (ver in defaultVersions) {
                    expecteds.computeIfAbsent(ver) { e }
                }
            }
            else -> expecteds[version] = e
        }
    }

    for ((version, expected) in expecteds) {
        val versionLabel = " for version ${version}"

        val expectedValidity = expected.validity
        when (expectedValidity) {
            TSValidityOutcome.INVALID_LATENT,
            TSValidityOutcome.INVALID_LAX,
            TSValidityOutcome.INVALID -> {
                if (true) {
                    dynamicTest("Test ${schemaTest.name} - Schema document ${schemaDoc.href} should not parse or be found invalid${versionLabel}") {
                        val e = assertFails(documentation) {
                            val schemaLocation = schemaDoc.href.toAnyUri()
                            val schema = resolver.readSchema(schemaLocation)
                            val resolvedSchema = schema.resolve(resolver.delegate(schemaLocation), version)
                            resolvedSchema.check(isLax = expectedValidity == TSValidityOutcome.INVALID_LAX)
                        }
                        if (e is Error) throw e

                        try {

                            val exName = expected.exception
                            if (exName != null) {
                                if (exName.contains('.')) {
                                    assertEquals(exName, e.javaClass.name)
                                } else {
                                    assertEquals(exName, e.javaClass.name.substringAfterLast('.'))
                                }
                            }

                            val exMsg = expected.message?.let { Regex(it.pattern, setOf(RegexOption.UNIX_LINES)) }
                            if (exMsg != null) {
                                if (!exMsg.containsMatchIn(e.message ?: "")) {
                                    val match = exMsg.find(e.message ?: "")?.value
                                    if (match != null) {
                                        assertEquals("${exMsg.pattern}\n$match", "${exMsg.pattern}\n${e.message ?: ""}")
                                    } else {
                                        assertEquals(exMsg.pattern, e.message)
                                    }
                                }
                            } else {
                                System.err.println("Expected error: \n")
                                System.err.println(documentation.prependIndent("        "))
                                System.err.println("    Exception thrown:")
                                System.err.println(e.message?.prependIndent("        "))
                            }
                        } catch (f: AssertionError) {
                            if (f != e) {
                                f.addSuppressed(e)
                            }
                            throw f
                        }
                    }
                }
            }

            TSValidityOutcome.LAX,
            TSValidityOutcome.VALID -> {
                val schemaLocation = schemaDoc.href.toAnyUri()
                dynamicTest("Test ${schemaTest.name} - Schema document ${schemaDoc.href} parses") {
                    val schema = resolver.readSchema(schemaLocation)
                    Assertions.assertNotNull(schema)
                }
                dynamicTest("Test ${schemaTest.name} - Schema document ${schemaDoc.href} resolves and checks$versionLabel") {
                    val resolvedSchema =
                        resolver.readSchema(schemaLocation).resolve(resolver.delegate(schemaLocation), version)
                    resolvedSchema.check(expectedValidity == TSValidityOutcome.LAX)
                    Assertions.assertNotNull(resolvedSchema)
                }
            }

            TSValidityOutcome.IMPLEMENTATION_DEFINED,
            TSValidityOutcome.IMPLEMENTATION_DEPENDENT,
            TSValidityOutcome.INDETERMINATE -> { // indeterminate should parse, but may not check (implementation defined)
                val schemaLocation = schemaDoc.href.toAnyUri()
                dynamicTest("Test ${schemaTest.name} - Schema document ${schemaDoc.href} parses") {
                    val schema = resolver.readSchema(schemaLocation)
                    Assertions.assertNotNull(schema)
                }
            }

            TSValidityOutcome.RUNTIME_SCHEMA_ERROR,
            TSValidityOutcome.NOTKNOWN -> {} // ignore unknown
        }
    }
}


@OptIn(ExperimentalTypeInference::class)
internal fun buildDynamicContainer(
    displayName: String,
    block: suspend SequenceScope<DynamicNode>.() -> Unit
): DynamicContainer {
    return DynamicContainer.dynamicContainer(displayName, sequence(block).asIterable())
}

internal suspend fun SequenceScope<DynamicTest>.dynamicTest(displayName: String, testBody: () -> Unit) {
    yield(DynamicTest.dynamicTest(displayName, testBody))
}

@OptIn(ExperimentalTypeInference::class)
internal suspend fun SequenceScope<DynamicContainer>.dynamicContainer(
    displayName: String,
    block: suspend SequenceScope<DynamicNode>.() -> Unit
) {
    yield(DynamicContainer.dynamicContainer(displayName, sequence(block).asIterable()))
}

inline fun <R> URI.withXmlReader(body: (XmlReader) -> R): R {
    return toURL().withXmlReader(body)
}

inline fun <R> URL.withXmlReader(body: (XmlReader) -> R): R {
    return openStream().use { inStream ->
        xmlStreaming.newReader(inStream, "UTF-8").use(body)
    }
}

fun URL.resolve(path: String): URL {
    @Suppress("DEPRECATION")
    return URL(this, path)
}

data class MeasureInfo(val round: Int, val rounds: Int, val warmups: Int)
