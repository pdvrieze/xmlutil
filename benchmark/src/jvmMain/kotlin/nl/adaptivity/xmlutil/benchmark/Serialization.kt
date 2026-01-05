/*
 * Copyright (c) 2024-2026.
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

package nl.adaptivity.xmlutil.benchmark

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSchema
import kotlinx.benchmark.*
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.benchmark.util.BlackholeWrapper
import nl.adaptivity.xmlutil.benchmark.util.BlackholeWrapperImpl
import nl.adaptivity.xmlutil.benchmark.util.testXmlSchemaUrls
import nl.adaptivity.xmlutil.core.KtXmlWriter
import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.core.impl.multiplatform.StringWriter
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.recommended
import java.net.URL
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@Measurement(iterations = 10)
@Warmup(iterations = 2)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class Serialization {
    lateinit var retainedXml: XML

    val suites: List<Pair<URL, URL>> = testXmlSchemaUrls(XML.compat { recommended_0_87_0() })

    val schemas: List<Pair<URL, XSSchema>> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val xml = XML.v1.recommended()
        suites.map { (_, u) ->
            u.openStream().use { input ->
                xmlStreaming.newGenericReader(input).use { reader ->
                    u to xml.decodeFromReader(reader)
                }
            }
        }
    }

    @Param("true"/*, "false"*/)
    var fast: Boolean = true

    @Setup
    fun setup() {
        println("Setup called (fast: $fast)")
        retainedXml = when {
            fast -> XML.v1.fast()
            else -> XML.v1.recommended()
        }
    }

    @Benchmark
    fun testSerializeGenericSpeed(bh : Blackhole) = testSerializeGenericSpeedImpl(BlackholeWrapperImpl(bh))

    fun testSerializeGenericSpeedImpl(bh: BlackholeWrapper) {
        val xml = when {
            fast -> XML.v1.fast()
            else -> XML.v1.recommended {
                policy { throwOnRepeatedElement = true }
            }
        }

        xmlStreaming.setFactory(xmlStreaming.genericFactory)
        testSerializeAndParseSpeed(bh, xml)
        xmlStreaming.setFactory(null)
    }

    private fun testSerializeAndParseSpeed(bh : BlackholeWrapper, xml: XML) {
        for ((uri, schema) in schemas) {
            try {
                val out = KtXmlWriter(StringWriter(), xmlVersion = XmlVersion.XML10, xmlDeclMode = XmlDeclMode.None)
                retainedXml.encodeToWriter(out, XSSchema.serializer(), schema)
                bh.consume(bh)
            } catch (e: Exception) {
                System.err.println("Failure to write schema: $uri \n${e.message?.prependIndent("        ")}")
                throw e
            }
        }
    }


}
