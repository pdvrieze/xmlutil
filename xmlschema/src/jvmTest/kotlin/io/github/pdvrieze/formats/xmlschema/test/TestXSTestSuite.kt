/*
 * Copyright (c) 2023-2025.
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

@file:MustUseReturnValues

package io.github.pdvrieze.formats.xmlschema.test

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.toAnyUri
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.*
import io.github.pdvrieze.formats.xmlschema.resolved.SchemaVersion
import io.github.pdvrieze.formats.xmlschema.resolved.SimpleResolver
import io.github.pdvrieze.formats.xmlschema.test.TestXSTestSuite.NON_TESTED.*
import kotlinx.serialization.KSerializer
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.XMLConstants.XSD_NS_URI
import nl.adaptivity.xmlutil.core.KtXmlReader
import nl.adaptivity.xmlutil.jdk.StAXStreamingFactory
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XML1_0
import nl.adaptivity.xmlutil.serialization.defaultPolicy
import nl.adaptivity.xmlutil.serialization.structure.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertNotNull
import org.w3.xml.xmschematestsuite.*
import org.w3.xml.xmschematestsuite.override.CompactOverride
import org.w3.xml.xmschematestsuite.override.OTSSuite
import java.net.URI
import java.net.URL
import kotlin.experimental.ExperimentalTypeInference
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class TestXSTestSuite {

    init {
        xmlStreaming.setFactory(xmlStreaming.genericFactory)
    }
    var xml: XML = XML.compat {
        recommended_0_87_0()
        defaultToGenericParser = true
    }

    @Test
    @Disabled
    fun testParseGenericSpeed() {
        val urls = testXmlSchemaUrls()
        var dummy1: Any?
        var dummy2: Any?
        var dummy3: Any?
        var dummy4: Any?
        measure("Parse xml ${urls.size} schema documents") {
            for ((_, url) in urls) {
                url.openStream().use { instr ->
                    KtXmlReader(instr).use { r ->
                        for (e in r) {
                            when (e) {
                                EventType.START_DOCUMENT -> {
                                    dummy1 = r.version
                                    dummy2 = r.relaxed
                                    dummy3 = r.standalone
                                }
                                EventType.END_ELEMENT,
                                EventType.START_ELEMENT -> {
                                    for (i in 0 until r.attributeCount) {
                                        dummy1 = r.localName
                                        dummy2 = r.namespaceURI
                                        dummy3 = r.prefix
                                        dummy4 = r.getAttributeValue(i)
                                    }
                                    dummy1 = r.localName
                                    dummy2 = r.namespaceURI
                                    dummy3 = r.prefix
                                }
                                EventType.TEXT,
                                EventType.CDSECT,
                                EventType.ENTITY_REF,
                                EventType.IGNORABLE_WHITESPACE,
                                EventType.PROCESSING_INSTRUCTION,
                                EventType.COMMENT -> {
                                    dummy1 = r.text
                                }
                                EventType.DOCDECL -> { dummy1 = r.text}
                                EventType.END_DOCUMENT -> {}
                                EventType.ATTRIBUTE -> error("unexpected attribute")
                            }
                        }
                    }
                }

            }
        }
    }

    @Test
    @Disabled
    fun testDeserializeGenericSpeed() {
        xml = XML1_0.recommended {
            isUnchecked = false
        }
        xmlStreaming.setFactory(xmlStreaming.genericFactory)
        testDeserializeSpeed()
        xmlStreaming.setFactory(null)
    }

    @Test
    @Disabled
    fun testDeserializeStaxSpeed() {
        xml = XML1_0.recommended()
        xmlStreaming.setFactory(StAXStreamingFactory())
        testDeserializeSpeed()
        xmlStreaming.setFactory(null)
    }

    inline fun measure(name:String, rounds: Int = 20, warmups: Int = 1, action: MeasureInfo.() -> Unit): Long {
        val initTime = System.currentTimeMillis()
        var startTime = initTime
        val iterCount = rounds+warmups
        for (i in 0 until iterCount) {
            if (i==warmups+1) {
                startTime = System.currentTimeMillis()
            }
            MeasureInfo(i-warmups, rounds, warmups).action()
        }
        val endTime = System.currentTimeMillis()
        println ("Init: ${Instant.fromEpochMilliseconds(initTime)}")
        println ("Start: ${Instant.fromEpochMilliseconds(startTime)}")
        println ("End: ${Instant.fromEpochMilliseconds(endTime)}")
        if (rounds==0) {
            val duration = (endTime - initTime)/warmups
            println("$name: Duration time: $duration ms")
            return duration
        } else {
            val duration = (endTime - startTime)/rounds
            val warmupExtra = (startTime - initTime - duration)
            println("$name: Duration time × $rounds): $duration ms (+${warmupExtra} ms)")
            return duration
        }
    }

    fun testDeserializeSpeed() {

        val schemaUrls: List<Pair<URL, URL>> =
            testXmlSchemaUrls()//.filter { it.second.toString().contains("wildcards") }

        val xml = this.xml.copy {
            isUnchecked = true
            defaultPolicy {
                autoPolymorphic = true
                throwOnRepeatedElement = true
                verifyElementOrder = true
                isStrictAttributeNames = true
            }
        }
        assertTrue(xml.config.isUnchecked)

        print("Iterating: ")
        val duration = measure("Parsing and deserializing ${schemaUrls.size} schema documents") {
            if(round<0) print("*") else if (round+1<rounds) print("×") else println("×")
            for ((setBaseUri, uri) in schemaUrls) {
                val resolver = SimpleResolver(xml, setBaseUri)
                try {
                    val _ = resolver.readSchema(VAnyURI(uri.toString()))
                } catch (e: Exception) {
                    System.err.println("Failure to read schema: $uri \n${e.message?.prependIndent("        ")}")
                }
            }
        }
        println()
        assertTrue(duration<10000, "Duration expected less than 10 seconds" )
    }

    private fun testXmlSchemaUrls(): List<Pair<URL, URL>> {
        val suiteURL: URL = javaClass.getResource("/xsts/suite.xml")

        val override = javaClass.getResource("/override.xml").withXmlReader {
            val compact = xml.decodeFromReader<CompactOverride>(it)
            OTSSuite(compact)
        }

        return suiteURL.withXmlReader { xmlReader ->
            val suite = xml.decodeFromReader<TSTestSuite>(xmlReader)
            suite.testSetRefs
                //                .filter { arrayOf("sunMeta/").any { m -> it.href.contains(m) } }
                .flatMap { setRef ->
                    val setBaseUrl: URL = javaClass.getResource("/xsts/${setRef.href}")
                    val testSet = override.applyTo(setBaseUrl.withXmlReader { r -> xml.decodeFromReader<TSTestSet>(r) })

                    val folderName = setRef.href.substring(0, setRef.href.indexOf('/')).removeSuffix("Meta")

                    val tsName = "$folderName - ${testSet.name}"

                    testSet.testGroups.flatMap { gr ->
                        gr.schemaTest?.takeIf { it.expected.any { it.validity.parsable } }?.schemaDocuments?.mapNotNull { sd ->
                            (setBaseUrl to setBaseUrl.resolve(sd.href))
                        } ?: emptyList()
                    }
                }
        }
    }

    @DisplayName("Test suites: suite.xml")
    @TestFactory
    fun testFromTestSetRef(): List<DynamicNode> {
        val suiteURL: URL = javaClass.getResource("/xsts/suite.xml")

        val override = javaClass.getResource("/override.xml").withXmlReader {
            val compact = XML1_0.recommended().decodeFromReader<CompactOverride>(it)
            OTSSuite(compact)
        }

        val nodes = mutableListOf<DynamicNode>()
        suiteURL.withXmlReader { xmlReader ->
            val suite = xml.decodeFromReader<TSTestSuite>(xmlReader)
            val subNodes = suite.testSetRefs
//                .filter { it.href.contains("sunMeta/suntest") }
//                .filter { it.href.contains("msMeta/Additional") }
//                .filter { (it.href.contains("nistMeta/") /*&& it.href.contains("CType")*/) }
                .filter { arrayOf("sunMeta/", "nistMeta/", "boeingMeta/", "msMeta/",
                    "wgMeta").any { m -> it.href.contains(m) } }
//                .filter { arrayOf("msMeta/Notation", "msMeta/Schema", "msMeta/SimpleType",
//                    "msMeta/Wildcards").any { m -> it.href.contains(m) } }
//                .filter { (it.href.contains("msMeta/")) }
//                .filter { (it.href.contains("wgMeta/")) }
                .map { setRef ->

                    val setBaseUrl: URL = javaClass.getResource("/xsts/${setRef.href}")
                    val testSet = override.applyTo(setBaseUrl.withXmlReader { r -> xml.decodeFromReader<TSTestSet>(r) })

                    val folderName = setRef.href.substring(0, setRef.href.indexOf('/')).removeSuffix("Meta")

                    val tsName = "$folderName - ${testSet.name}"

                    buildDynamicContainer("Test set '$tsName'") {
                        for (group in testSet.testGroups) {
                            if (true || group.name.equals("addB182")) {
                                dynamicContainer("Group '${group.name}'") {
                                    addSchemaTests(setBaseUrl, group, testSet.schemaVersion?.let(::listOf))
                                }
                            }
                        }
                    }
                }
            nodes.addAll(subNodes)
            val typeTests = buildDynamicContainer("Test types") {
                val schemaUrls: List<URL> = suite.testSetRefs.flatMap { setRef ->
                    val setBaseUrl: URL = javaClass.getResource("/xsts/${setRef.href}")
                    val resolver = SimpleResolver(setBaseUrl)

                    val ts = setBaseUrl.withXmlReader { r -> xml.decodeFromReader<TSTestSet>(r) }
                    ts.testGroups.flatMap { tg ->
                        listOfNotNull(tg.schemaTest)
                    }.filter { schemaTest ->
                        schemaTest.expected.firstOrNull { it.version != "1.1" }?.validity == TSValidityOutcome.VALID
                    }.flatMap { schemaTest ->
                        schemaTest.schemaDocuments
                    }.map { schemaDoc ->
                        setBaseUrl.resolve(schemaDoc.href)
                    }.filter {
                        "particlesIc006.xsd" in it.path
                    }
                }
                assertTrue(schemaUrls.size > 0, "Expected at least 1 schema, found 0")
                val schemas: Sequence<XSSchema> = schemaUrls.asSequence().map { url ->
                    url.withXmlReader { reader ->
                        xml.decodeFromReader<XSSchema>(reader).also {
                            if (reader.eventType != EventType.END_DOCUMENT) {
                                var e: EventType
                                do {
                                    e = reader.next()
                                } while (e.isIgnorable && e != EventType.END_DOCUMENT)
                                require(e == EventType.END_DOCUMENT) {
                                    "Trailing content in document $reader"
                                }
                            }
                        }
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
            return tagName.namespaceURI == XSD_NS_URI &&
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

        override fun isEmpty(): Boolean = false

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
        XSKeyRef.serializer() to mapOf(
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
    setBaseUrl: URL,
    group: TSTestGroup,
    testSetVersion: List<SchemaVersion>?
) {
    var targetSchemaDoc: TSSchemaDocument? = null
    group.schemaTest?.let { schemaTest ->
        val documentation = group.documentationString()
        if (schemaTest.schemaDocuments.size == 1) {
            val schemaDoc = schemaTest.schemaDocuments.single()
            addSchemaDocTest(setBaseUrl, schemaTest, schemaDoc, documentation, group.version?.let(::listOf) ?: testSetVersion)
            targetSchemaDoc = schemaDoc
        } else {
            dynamicContainer("Schema documents") {
                for (schemaDoc in schemaTest.schemaDocuments) {
                    if (true || schemaDoc.href.contains("ipo.xsd")) {
                        addSchemaDocTest(setBaseUrl, schemaTest, schemaDoc, documentation, group.version?.let(::listOf) ?: testSetVersion)
                        targetSchemaDoc = schemaDoc
                    }
                }
            }
        }
    }
    if (false && targetSchemaDoc != null && group.instanceTests.isNotEmpty()) {

        for (instanceTest in group.instanceTests) {
            addInstanceTest(setBaseUrl, instanceTest, targetSchemaDoc!!, group.documentationString())
        }
    }
}

private suspend fun SequenceScope<DynamicNode>.addInstanceTest(
    setBaseUrl: URL,
    instanceTest: TSInstanceTest,
    schemaDoc: TSSchemaDocument,
    documentation: String
) {
    val instanceDoc = instanceTest.instanceDocument
    val resolver = SimpleResolver(setBaseUrl)
    dynamicTest("Instance document ${instanceDoc.href} exists") {
        setBaseUrl.resolve(instanceDoc.href).openStream().use { stream ->
            assertNotNull(stream)
        }
    }
    if (instanceTest.expected.firstOrNull { it.version != "1.0" }?.validity == TSValidityOutcome.VALID) {
        val schemaLocation = schemaDoc.href.toAnyUri()
        val schema = resolver.readSchema(schemaLocation).resolve(resolver)

    }

//    assertNotNull()
}

private suspend fun SequenceScope<DynamicNode>.addSchemaDocTest(
    setBaseUrl: URL,
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
    val resolver = SimpleResolver(setBaseUrl.toURI())

    dynamicTest("Test ${schemaTest.name} - Schema document ${schemaDoc.href} exists") {
        setBaseUrl.resolve(schemaDoc.href).openStream().use { stream ->
            assertNotNull(stream)
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
                    expecteds.getOrPut(ver) { e }
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
                    assertNotNull(schema)
                }
                dynamicTest("Test ${schemaTest.name} - Schema document ${schemaDoc.href} resolves and checks$versionLabel") {
                    val resolvedSchema =
                        resolver.readSchema(schemaLocation).resolve(resolver.delegate(schemaLocation), version)
                    resolvedSchema.check(expectedValidity == TSValidityOutcome.LAX)
                    assertNotNull(resolvedSchema)
                }
            }

            TSValidityOutcome.IMPLEMENTATION_DEFINED,
            TSValidityOutcome.IMPLEMENTATION_DEPENDENT,
            TSValidityOutcome.INDETERMINATE -> { // indeterminate should parse, but may not check (implementation defined)
                val schemaLocation = schemaDoc.href.toAnyUri()
                dynamicTest("Test ${schemaTest.name} - Schema document ${schemaDoc.href} parses") {
                    val schema = resolver.readSchema(schemaLocation)
                    assertNotNull(schema)
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
    @BuilderInference block: suspend SequenceScope<DynamicNode>.() -> Unit
): DynamicContainer {
    return DynamicContainer.dynamicContainer(displayName, sequence(block).asIterable())
}

internal suspend fun SequenceScope<DynamicTest>.dynamicTest(displayName: String, testBody: () -> Unit) {
    yield(DynamicTest.dynamicTest(displayName, testBody))
}

@OptIn(ExperimentalTypeInference::class)
internal suspend fun SequenceScope<DynamicContainer>.dynamicContainer(
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
        xmlStreaming.newReader(inStream, "UTF-8").use(body)
    }
}

fun URL.resolve(path: String): URL {
    return URL(this, path)
}

data class MeasureInfo(val round: Int, val rounds: Int, val warmups: Int)
