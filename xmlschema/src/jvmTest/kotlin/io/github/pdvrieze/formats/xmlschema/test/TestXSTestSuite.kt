/*
 * Copyright (c) 2021.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.github.pdvrieze.formats.xmlschema.test

import io.github.pdvrieze.formats.xmlschema.XmlSchemaConstants
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSchema
import io.github.pdvrieze.formats.xmlschema.resolved.SimpleResolver
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlStreaming
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.structure.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertNotNull
import org.w3.xml.xmschematestsuite.*
import java.net.URI
import java.net.URL
import kotlin.experimental.ExperimentalTypeInference
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class TestXSTestSuite {

    val xml = XML { autoPolymorphic = true }

    @DisplayName("Test suites: suite.xml")
    @TestFactory
    fun testFromTestSetRef(): List<DynamicNode> {
        val suiteURL: URL = javaClass.getResource("/xsts/suite.xml")

        val nodes = mutableListOf<DynamicNode>()
        suiteURL.withXmlReader { xmlReader ->
            val suite = xml.decodeFromReader<TSTestSuite>(xmlReader)
            val subNodes = suite.testSetRefs
//                .filter { false || it.href.contains("msMeta") }
                .filter { false || it.href.contains("sunMeta/" + "suntest.testSet") }
                .map { setRef ->

                    val setBaseUrl: URI = javaClass.getResource("/xsts/${setRef.href}").toURI()
                    val testSet = setBaseUrl.withXmlReader { r -> xml.decodeFromReader<TSTestSet>(r) }

                    val folderName = setRef.href.substring(0, setRef.href.indexOf('/')).removeSuffix("Meta")

                    val tsName = "$folderName - ${testSet.name}"

                    buildDynamicContainer("Test set $tsName") {
                        for (group in testSet.testGroups) {
                            if (true || group.name.contains("xsd013.e")) {
                                dynamicContainer("Group ${group.name}") {
                                    addSchemaTests(setBaseUrl, group)
                                }
                            }
                        }
                    }
                }
            nodes.addAll(subNodes)
            val typeTests = buildDynamicContainer("Test types") {
                val schemaUrls: List<URL> = suite.testSetRefs.flatMap { setRef ->
                    val setBaseUrl: URI = javaClass.getResource("/xsts/${setRef.href}").toURI()
                    val resolver = SimpleResolver(setBaseUrl)

                    val ts = setBaseUrl.withXmlReader { r -> xml.decodeFromReader<TSTestSet>(r) }
                    ts.testGroups.flatMap { tg ->
                        listOfNotNull(tg.schemaTest)
                    }.filter { schemaTest ->
                        schemaTest.expected?.validity == TSValidityOutcome.VALID
                    }.flatMap { schemaTest ->
                        schemaTest.schemaDocuments
                    }.map { schemaDoc ->
                        setBaseUrl.resolve(schemaDoc.href).toURL()
//                    }.filter {
//                        "/ipo.xsd" in it.path
                    }
                }
//                assertEquals(1, schemaUrls.size)
                val schemas: Sequence<XSSchema> = schemaUrls.asSequence().map { url ->
                    url.withXmlReader { reader ->
                        xml.decodeFromReader<XSSchema>(reader)
                    }
                }
                testPropertyPresences(schemas)

            }
            nodes.add(typeTests)
        }
        return nodes
    }

    private fun extractDescriptors(
        rootDescriptor: XmlDescriptor,
        collector: MutableList<XmlDescriptor> = mutableListOf()
    ): List<XmlDescriptor> {
        fun XmlDescriptor.canRecurse(): Boolean {
            return tagName.namespaceURI == XmlSchemaConstants.XS_NAMESPACE &&
                    collector.none { it.serialDescriptor == serialDescriptor }
        }

        val recurse = mutableListOf<XmlDescriptor>()
        for (elementDescriptorIdx in 0 until rootDescriptor.elementsCount) {
            val elementDescriptor = rootDescriptor.getElementDescriptor(elementDescriptorIdx)
            when (elementDescriptor) {
                is XmlCompositeDescriptor -> {
                    if (elementDescriptor.canRecurse()) {
                        recurse.add(elementDescriptor)
                        collector.add(elementDescriptor)
                    }
                }

                is XmlPolymorphicDescriptor -> {
                    for (d in elementDescriptor.polyInfo.values) {
                        if (d.canRecurse()) {
                            recurse.add(d)
                            collector.add(d)
                        }
                    }
                }

                is XmlListDescriptor,
                is XmlMapDescriptor -> {
                    if (elementDescriptor.canRecurse()
                        ) {
                        recurse.add(elementDescriptor)
                    }
                }

                else -> {}
            }
        }

        for (r in recurse) {
            extractDescriptors(r, collector)
        }

        return collector
    }

    private suspend fun SequenceScope<DynamicNode>.testPropertyPresences(schemas: Sequence<XSSchema>) {

        val rootDescriptor = xml.xmlDescriptor(XSSchema.serializer()) as XmlRootDescriptor

        val schemaIterator = schemas.iterator()

        val descriptors = extractDescriptors(rootDescriptor)

        val attributeViewer = AttributeViewer()

        for (desc in descriptors) {
            val serialName = desc.serialDescriptor.serialName
            dynamicContainer("Test presence/absence of ${serialName.substringAfterLast('.')} (${desc.tagName}) children") {
                val info = attributeViewer.structInfo(desc)
                for (i in 0 until desc.elementsCount) {
                    val optional = desc.isElementOptional(i) || desc.getElementDescriptor(i).isNullable
                    val elementInfo = info[i]
                    val displayName = desc.getElementDescriptor(i).tagName.localPart
                    dynamicTest("#$i [$displayName] Check whether element is seen") {
                        while (schemaIterator.hasNext() && (!elementInfo.seen || !(optional && elementInfo.hasBeenAbsent))) {
                            val schema = schemaIterator.next()
                            attributeViewer.encode(XSSchema.serializer(), schema, rootDescriptor)
                        }
                        assertTrue(elementInfo.seen)
                    }
                    if (optional) {
                        dynamicTest("  - #$i [$displayName] Check whether this optional element has been omitted") {
                            assertTrue(elementInfo.hasBeenAbsent)
                        }
                    }
                }
            }
        }
    }
}

data class ElementInfo(val name: QName, var hasBeenAbsent: Boolean = false, var seen: Boolean = false) {
    operator fun plusAssign(newInfo: ElementInfo) {
        require(name == newInfo.name)
        hasBeenAbsent = hasBeenAbsent || newInfo.hasBeenAbsent
        seen = seen || newInfo.seen
    }

    constructor(descriptor: XmlDescriptor) : this(descriptor.tagName)
}

private suspend fun SequenceScope<DynamicNode>.addSchemaTests(
    setBaseUrl: URI,
    group: TSTestGroup
) {
    group.schemaTest?.let { schemaTest ->
        val documentation = group.documentationString()
        if (schemaTest.schemaDocuments.size == 1) {
            val schemaDoc = schemaTest.schemaDocuments.single()
            addSchemaDocTest(setBaseUrl, schemaTest, schemaDoc, documentation)
        } else {
            dynamicContainer("Schema documents") {
                for (schemaDoc in schemaTest.schemaDocuments) {
                    addSchemaDocTest(setBaseUrl, schemaTest, schemaDoc, documentation)
                }
            }
        }
    }
}

private suspend fun SequenceScope<DynamicNode>.addSchemaDocTest(
    setBaseUrl: URI,
    schemaTest: TSSchemaTest,
    schemaDoc: TSSchemaDocument,
    documentation: String
) {
    val resolver = SimpleResolver(setBaseUrl)

    val expected = schemaTest.expected
    if (expected?.validity == TSValidityOutcome.INVALID) {
        dynamicTest("Schema document ${schemaDoc.href} exists") {
            assertNotNull(setBaseUrl.resolve(schemaDoc.href).toURL().openStream())
        }
        if (false) {
            dynamicTest("Schema document ${schemaDoc.href} should not parse or be found invalid") {
                val e = assertFails(documentation) {
                    val schemaLocation = VAnyURI(schemaDoc.href)
                    val schema = resolver.readSchema(schemaLocation)
                    val resolvedSchema = schema.resolve(resolver.delegate(schemaLocation))
                    resolvedSchema.check()
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
                    f.addSuppressed(e)
                    throw f
                }
            }
        }
    } else {
        dynamicTest("Schema document ${schemaDoc.href} parses") {
            val schema = resolver.readSchema(VAnyURI(schemaDoc.href))
            assertNotNull(schema)
        }
    }
}


@OptIn(ExperimentalTypeInference::class)
internal fun buildDynamicContainer(
    displayName: String,
    @BuilderInference block: suspend SequenceScope<DynamicNode>.() -> Unit
): DynamicContainer {
    return DynamicContainer.dynamicContainer(displayName, sequence(block).asIterable())
}

internal suspend fun SequenceScope<in DynamicTest>.dynamicTest(displayName: String, testBody: () -> Unit) {
    yield(DynamicTest.dynamicTest(displayName, testBody))
}

@OptIn(ExperimentalTypeInference::class)
internal suspend fun SequenceScope<in DynamicContainer>.dynamicContainer(
    displayName: String,
    @BuilderInference block: suspend SequenceScope<DynamicNode>.() -> Unit
) {
    yield(DynamicContainer.dynamicContainer(displayName, sequence(block).asIterable()))
}

inline fun <R> URI.withXmlReader(body: (XmlReader) -> R): R {
    return toURL().withXmlReader(body)
}

inline fun <R> URL.withXmlReader(body: (XmlReader) -> R): R {
    return openStream().use { inStream ->
        XmlStreaming.newReader(inStream, "UTF-8").use(body)
    }
}
