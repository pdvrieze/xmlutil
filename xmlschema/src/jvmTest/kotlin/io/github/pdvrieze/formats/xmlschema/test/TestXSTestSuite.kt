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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.XSI_OpenAttrs
import io.github.pdvrieze.formats.xmlschema.resolved.SimpleResolver
import io.github.pdvrieze.formats.xmlschema.test.TestXSTestSuite.NON_TESTED.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
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

    val xml = XML {
        defaultPolicy {
            autoPolymorphic = true
            throwOnRepeatedElement = true
        }
    }

    @DisplayName("Test suites: suite.xml")
    @TestFactory
    fun testFromTestSetRef(): List<DynamicNode> {
        val suiteURL: URL = javaClass.getResource("/xsts/suite.xml")

        val nodes = mutableListOf<DynamicNode>()
        suiteURL.withXmlReader { xmlReader ->
            val suite = xml.decodeFromReader<TSTestSuite>(xmlReader)
            val subNodes = suite.testSetRefs
//                .filter { false || it.href.contains("msMeta") }
                .filter { false || (it.href.contains("sunMeta") /*&& it.href.contains("CType")*/) }
                .map { setRef ->

                    val setBaseUrl: URI = javaClass.getResource("/xsts/${setRef.href}").toURI()
                    val testSet = setBaseUrl.withXmlReader { r -> xml.decodeFromReader<TSTestSet>(r) }

                    val folderName = setRef.href.substring(0, setRef.href.indexOf('/')).removeSuffix("Meta")

                    val tsName = "$folderName - ${testSet.name}"

                    buildDynamicContainer("Test set $tsName") {
                        for (group in testSet.testGroups) {
                            if (true || group.name.contains("annotation00101m3")) {
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
                    }.filter {
                        "particlesIc006.xsd" in it.path
                    }
                }
                assertTrue(schemaUrls.size > 0, "Expected at least 1 schema, found 0")
                val schemas: Sequence<XSSchema> = schemaUrls.asSequence().map { url ->
                    url.withXmlReader { reader ->
                        xml.decodeFromReader<XSSchema>(reader)
                    }
                }
                testPropertyPresences(schemas)

            }
//            nodes.add(typeTests)
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

    private enum class NON_TESTED(val testSeen: Boolean, val testAbsent: Boolean) {
        SEEN(false, true),
        ABSENT(true, false),
        BOTH(false, false),
        NONE(true, true)
    }

    private object TAG_ABSENT : Map<String, NON_TESTED> {
        override val entries: Set<Map.Entry<String, NON_TESTED>> get() = emptySet()
        override val keys: Set<String>
            get() = emptySet()
        override val size: Int
            get() = 1
        override val values: Collection<NON_TESTED>
            get() = listOf(BOTH)

        override fun isEmpty(): Boolean { return false }

        override fun get(key: String): NON_TESTED = BOTH

        override fun containsValue(value: NON_TESTED): Boolean {
            return value == BOTH
        }

        override fun containsKey(key: String): Boolean = true

    }

    private val expectedNonTested: Map<KSerializer<out XSI_OpenAttrs>, Map<String, NON_TESTED>> = mapOf(
        XSGroup.All.serializer() to mapOf(
            "annotations" to SEEN,
            "anys" to SEEN,
            "groups" to SEEN,
            "id" to SEEN,
            "otherAttrs" to SEEN,
        ),
        XSAll.serializer() to mapOf(
            "groups" to SEEN,
            "anys" to SEEN,
        ),
        XSAny.serializer() to mapOf(
            "processContents" to SEEN,
            "notQName" to SEEN,
            "notNamespace" to SEEN,
        ),
        XSAnyAttribute.serializer() to mapOf(
            "notQName" to SEEN,
            "notNamespace" to SEEN,
//            "processContents" to ABSENT,
        ),
        XSAppInfo.serializer() to mapOf("content" to ABSENT),
//        XSAssert.serializer() to TAG_ABSENT,
        XSAssert.serializer() to mapOf(
            "id" to SEEN,
            "asserts" to SEEN,
            "openContents" to SEEN,
        ),
        XSAssertionFacet.serializer() to TAG_ABSENT,
        XSDefaultOpenContent.serializer() to TAG_ABSENT,
        XSDocumentation.serializer() to mapOf("content" to ABSENT),
        XSExplicitTimezone.serializer() to TAG_ABSENT,
        XSField.serializer() to mapOf("xpathDefaultNamespace" to SEEN),
        XSFractionDigits.serializer() to mapOf(
            "annotations" to SEEN,
        ),
        XSMaxExclusive.serializer() to mapOf(
            "fixed" to SEEN,
            "id" to SEEN,
            "annotations" to SEEN,
        ),
        XSMaxInclusive.serializer() to mapOf(
            "fixed" to SEEN,
            "annotations" to SEEN,
        ),
        XSMaxLength.serializer() to mapOf(
            "id" to SEEN,
            "annotations" to SEEN,
        ),
        XSMinExclusive.serializer() to mapOf(
            "id" to SEEN,
            "fixed" to SEEN,
            "annotations" to SEEN,
        ),
        XSMinInclusive.serializer() to mapOf(
            "fixed" to SEEN,
            "annotations" to SEEN,
        ),
        XSMinLength.serializer() to mapOf("annotations" to SEEN),
        XSWhiteSpace.serializer() to mapOf("annotations" to SEEN),
        XSComplexContent.XSExtension.serializer() to mapOf(
            "openContents" to SEEN,
        ),
        XSComplexContent.XSRestriction.serializer() to mapOf(
            "id" to SEEN,
            "annotations" to SEEN,
            "asserts" to SEEN,
            "openContents" to SEEN,
            "simpleTypes" to SEEN,
            "facets" to SEEN,
            "otherContents" to SEEN,
        ),
        XSAnyAttribute.serializer() to mapOf(
            "notQName" to SEEN,
            "notNamespace" to SEEN,
            "processContents" to SEEN,
        ),
        XSComplexContent.XSExtension.serializer() to mapOf(
            "id" to SEEN,
            "asserts" to SEEN,
            "openContents" to SEEN,
        ),
        XSGroupRef.serializer() to mapOf(
            "id" to SEEN,
            "annotations" to SEEN,
        ),
        XSLength.serializer() to mapOf(
            "id" to SEEN,
            "annotations" to SEEN,
        ),
        XSLocalElement.serializer() to mapOf(
            // nillable particlesIc06
            "targetNamespace" to SEEN,
            "alternatives" to SEEN,
        ),
        XSLocalComplexType.Serializer to mapOf(
            "asserts" to SEEN,
            "openContents" to SEEN,
            "defaultAttributesApply" to SEEN,
        ),
        XSKey.serializer() to mapOf(
//            "selector" to ABSENT,
//            "fields" to ABSENT,
//            "name" to ABSENT,
            "ref" to SEEN,
        ),
        XSKeyref.serializer() to mapOf(
//            "selector" to ABSENT,
//            "fields" to ABSENT,
//            "name" to ABSENT,
            "ref" to SEEN,
            "refer" to SEEN,
        ),
        XSLocalAttribute.serializer() to mapOf(
            "inheritable" to SEEN,
//            "name" to ABSENT,
            "targetNamespace" to SEEN,
        ),
        XSOverride.serializer() to TAG_ABSENT,
        XSSchema.serializer() to mapOf(
            "defaultAttributes" to SEEN,
            "xpathDefaultNamespace" to SEEN,
            "overrides" to SEEN,
            "defaultOpenContent" to SEEN,
        ),
        XSSelector.serializer() to mapOf("xpathDefaultNamespace" to SEEN),
        XSSimpleRestriction.serializer() to mapOf("otherContents" to SEEN),
        XSGlobalComplexType.serializer() to mapOf(
//            "abstract" to ABSENT,
            "asserts" to SEEN,
            "defaultAttributesApply" to SEEN,
            "openContents" to SEEN,
        ),
        XSTotalDigits.serializer() to mapOf(
            "fixed" to SEEN,
            "id" to SEEN,
            "annotations" to SEEN,
        ),
        XSUnique.serializer() to mapOf(
//            "selector" to ABSENT,
//            "fields" to ABSENT,
            "ref" to SEEN,
        ),
    )

    private suspend fun SequenceScope<DynamicNode>.testPropertyPresences(schemas: Sequence<XSSchema>) {

        val rootDescriptor = xml.xmlDescriptor(XSSchema.serializer()) as XmlRootDescriptor

        val schemaIterator = schemas.iterator()

        val descriptors = extractDescriptors(rootDescriptor).sortedBy { it.serialDescriptor.serialName.substringAfterLast('.') }

        val attributeViewer = AttributeViewer()

        for (desc in descriptors) {
            val serialName = desc.serialDescriptor.serialName.substringBeforeLast("?")
            val expectations = expectedNonTested.filterKeys {
                it.descriptor.serialName == serialName
            }.values.singleOrNull() ?: emptyMap()

            val ext = if (expectations.isEmpty()) "" else " (modified: ${expectations.size})"

            dynamicContainer("Test presence/absence of ${serialName.substringAfterLast('.')} (${desc.tagName}) children$ext") {
                val info = attributeViewer.structInfo(desc)

                for (i in 0 until desc.elementsCount) {
                    val optional = desc.isElementOptional(i) || desc.getElementDescriptor(i).isNullable
                    val elementInfo = info[i]
                    val displayName = desc.serialDescriptor.getElementName(i)
                    val elementExpectation = expectations.getOrDefault(displayName, NONE)
                    val extName = elementExpectation.name
                    if (elementExpectation.testSeen) {
                        dynamicTest("#$i [$displayName] Check whether element is seen ($extName)") {
                            while (schemaIterator.hasNext() && (!elementInfo.seen || !(optional && elementInfo.hasBeenAbsent))) {
                                val schema = schemaIterator.next()
                                attributeViewer.encode(XSSchema.serializer(), schema, rootDescriptor)
                            }
                            assertTrue(elementInfo.seen)
                        }
                    }
                    if (optional && elementExpectation.testAbsent) {
                        dynamicTest("  - #$i [$displayName] Check whether this optional element has been omitted ($extName)") {
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
    var targetSchemaDoc: TSSchemaDocument? = null
    group.schemaTest?.let { schemaTest ->
        val documentation = group.documentationString()
        if (schemaTest.schemaDocuments.size == 1) {
            val schemaDoc = schemaTest.schemaDocuments.single()
            addSchemaDocTest(setBaseUrl, schemaTest, schemaDoc, documentation)
            targetSchemaDoc = schemaDoc
        } else {
            dynamicContainer("Schema documents") {
                for (schemaDoc in schemaTest.schemaDocuments) {
                    addSchemaDocTest(setBaseUrl, schemaTest, schemaDoc, documentation)
                    targetSchemaDoc = schemaDoc
                }
            }
        }
    }
    if(false && targetSchemaDoc!=null && group.instanceTests.isNotEmpty()) {

        for (instanceTest in group.instanceTests) {
            addInstanceTest(setBaseUrl, instanceTest, targetSchemaDoc!!, group.documentationString())
        }
    }
}

private suspend fun SequenceScope<DynamicNode>.addInstanceTest(
    setBaseUrl: URI,
    instanceTest: TSInstanceTest,
    schemaDoc: TSSchemaDocument,
    documentation: String
) {
    val instanceDoc = instanceTest.instanceDocument
    val resolver = SimpleResolver(setBaseUrl)
    dynamicTest("Instance document ${instanceDoc.href} exists") {
        setBaseUrl.resolve(instanceDoc.href).toURL().openStream().use { stream ->
            assertNotNull(stream)
        }
    }
    if (instanceTest.expected?.validity == TSValidityOutcome.VALID) {
        val schemaLocation = VAnyURI(schemaDoc.href)
        val schema = resolver.readSchema(schemaLocation).resolve(resolver)

    }

//    assertNotNull()
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
            setBaseUrl.resolve(schemaDoc.href).toURL().openStream().use { stream ->
                assertNotNull(stream)
            }
        }
        if (true) {
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
        val schemaLocation = VAnyURI(schemaDoc.href)
        dynamicTest("Schema document ${schemaDoc.href} parses") {
            val schema = resolver.readSchema(schemaLocation)
            assertNotNull(schema)
        }
        dynamicTest("Schema document ${schemaDoc.href} resolves and checks") {
            val resolvedSchema = resolver.readSchema(schemaLocation).resolve(resolver.delegate(schemaLocation))
            resolvedSchema.check()
            assertNotNull(resolvedSchema)
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
