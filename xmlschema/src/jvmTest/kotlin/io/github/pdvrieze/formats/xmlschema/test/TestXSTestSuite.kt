/*
 * Copyright (c) 2023-2026.
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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.*
import io.github.pdvrieze.formats.xmlschema.resolved.SimpleResolver
import io.github.pdvrieze.formats.xmlschema.test.TestXSTestSuite.NON_TESTED.*
import io.github.pdvrieze.formats.xmlschemaTests.Resource
import io.github.pdvrieze.formats.xmlschemaTests.getResource
import io.github.pdvrieze.formats.xmlschemaTests.openStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.XMLConstants.XSD_NS_URI
import nl.adaptivity.xmlutil.core.KtXmlReader
import nl.adaptivity.xmlutil.jdk.StAXStreamingFactory
import nl.adaptivity.xmlutil.serialization.LayeredCache
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.defaultPolicy
import nl.adaptivity.xmlutil.serialization.structure.*
import org.junit.jupiter.api.*
import org.w3.xml.xmschematestsuite.TSTestSet
import org.w3.xml.xmschematestsuite.TSTestSuite
import org.w3.xml.xmschematestsuite.TSValidityOutcome
import org.w3.xml.xmschematestsuite.override.CompactOverride
import org.w3.xml.xmschematestsuite.override.OTSSuite
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class TestXSTestSuite : AbstractTestSuiteSupport() {

    init {
        xmlStreaming.setFactory(xmlStreaming.genericFactory)
    }

    override var xml: XML = XML.v1 {
        defaultToGenericParser = true
        policy {
            formatCache = LayeredCache(7)
        }
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
        xml = XML.v1 {
            isUnchecked = false
        }
        xmlStreaming.setFactory(xmlStreaming.genericFactory)
        testDeserializeSpeed()
        xmlStreaming.setFactory(null)
    }

    @Test
    @Disabled
    fun testDeserializeStaxSpeed() {
        xml = XML.v1()
        xmlStreaming.setFactory(StAXStreamingFactory())
        testDeserializeSpeed()
        xmlStreaming.setFactory(null)
    }

    @IgnorableReturnValue
    inline fun measure(name:String, rounds: Int = 20, warmups: Int = 1, action: MeasureInfo.() -> Unit): Long {
        val initTime = System.currentTimeMillis()
        var startTime = initTime
        val iterCount = rounds+warmups
        for (i in 0 until iterCount) {
            if (i==warmups+1) {
                startTime = System.currentTimeMillis()
            }
            MeasureInfo(i - warmups, rounds, warmups).action()
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

        val schemaUrls: List<Pair<Resource, Resource>> =
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

    private fun testXmlSchemaUrls(): List<Pair<Resource, Resource>> {
        val suiteResource = getResource("/xsts/suite.xml")

        val override = getResource("/override.xml").withXmlReader {
            val compact = xml.decodeFromReader<CompactOverride>(it)
            OTSSuite(compact)
        }

        return suiteResource.withXmlReader { xmlReader ->
            val suite = xml.decodeFromReader<TSTestSuite>(xmlReader)
            suite.testSetRefs
                //                .filter { arrayOf("sunMeta/").any { m -> it.href.contains(m) } }
                .flatMap { setRef ->
                    val setBaseUrl: Resource = getResource("/xsts/${setRef.href}")
                    val testSet = override.applyTo(setBaseUrl.withXmlReader { r -> xml.decodeFromReader<TSTestSet>(r) })

                    val folderName = setRef.href.substring(0, setRef.href.indexOf('/')).removeSuffix("Meta")

                    val tsName = "$folderName - ${testSet.name}"

                    testSet.testGroups.flatMap { gr ->
                        gr.schemaTest?.takeIf { it.expected.any { it.validity.parsable } }?.schemaDocuments?.map { sd ->
                            (setBaseUrl to setBaseUrl.resolve(sd.href))
                        } ?: emptyList()
                    }
                }
        }
    }

    @DisplayName("Test suites: suite.xml")
    @TestFactory
    fun testSuite(): List<DynamicNode> {
        val nodes = getTestSets { ts ->
            ts.href.contains("msMeta/Additional")
        }.map { tsInfo ->
            val (testSetResource, _, testSet) = tsInfo

            buildDynamicContainer("Test set '${tsInfo.displayName}'") {
/*
                tsInfo.groupTest {

                }
*/

                for (group in testSet.testGroups) {
                    if (false || group.name.equals("addA005")) {
                        dynamicContainer("Group '${group.name}'") {
                            addSchemaTests(testSetResource, group, testSet.schemaVersion?.let(::listOf))
                        }
                    }
                }
            }
        }

        return nodes
    }

    @DisplayName("Test types")
    @TestFactory
    fun testTypes(): List<DynamicNode> {

        val typeTests = buildList<DynamicNode> {
            val testSets = getTestSets { true }

            val schemaResources: List<Resource> = testSets.flatMap { ts ->
                ts.testSet.testGroups.flatMap { tg ->
                    listOfNotNull(tg.schemaTest)
                }.filter { schemaTest ->
                    schemaTest.expected.firstOrNull { it.version != "1.1" }?.validity == TSValidityOutcome.VALID
                }.flatMap { schemaTest ->
                    schemaTest.schemaDocuments
                }.map { schemaDoc ->
                    ts.resource.resolve(schemaDoc.href)
                }.filter {
                    "particlesIc006.xsd" in it.path
                }
            }
            assertTrue(schemaResources.size > 0, "Expected at least 1 schema, found 0")
            val schemas: Sequence<XSSchema> = schemaResources.asSequence().map { res ->
                res.withXmlReader { reader ->
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
        return typeTests
    }

    @IgnorableReturnValue
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

    @OptIn(ExperimentalSerializationApi::class)
    private fun MutableList<DynamicNode>.testPropertyPresences(schemas: Sequence<XSSchema>) {

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

            val dc = buildDynamicContainer("Test presence/absence of ${serialName.substringAfterLast('.')} (${desc.tagName}) children$ext") {
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
            add(dc)
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


