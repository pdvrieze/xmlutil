/*
 * Copyright (c) 2024-2025.
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

import io.github.pdvrieze.formats.xmlschemaTests.io.github.pdvrieze.formats.xmlschemaTests.withXmlReader
import kotlinx.benchmark.*
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.benchmark.util.BlackholeWrapper
import nl.adaptivity.xmlutil.benchmark.util.BlackholeWrapperImpl
import nl.adaptivity.xmlutil.benchmark.util.testXmlSchemaUrls
import nl.adaptivity.xmlutil.core.KtXmlReader
import nl.adaptivity.xmlutil.serialization.XML
import org.openjdk.jmh.annotations.Fork
import java.net.URL
import java.util.concurrent.TimeUnit


@Measurement(iterations = 10)
@Fork(value = 1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
open class Parsing {

    val xml = XML.compat {
        recommended_0_87_0()
    }

    val suites: List<Pair<URL, URL>> by lazy(LazyThreadSafetyMode.NONE) {
        testXmlSchemaUrls(xml)
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Measurement(time = 2500, timeUnit = TimeUnit.MICROSECONDS)
    fun parseSuite(bh: Blackhole) {
        javaClass.getResource("/xsts/suite.xml").withXmlReader { r ->
            while(r.hasNext()) bh.consume(r.next())
        }
    }

    @Benchmark
    @Warmup(iterations = 1)
    fun benchParseSchemas(bh: Blackhole) {
        benchParseSchemas(BlackholeWrapperImpl(bh))
    }

    protected fun benchParseSchemas(bh: BlackholeWrapper) {
        for ((_, url) in suites) {
            try {
                url.openStream().use { instr ->
                    KtXmlReader(instr).use { r: KtXmlReader ->
                        for (e: EventType in r) {
                            when (e) {
                                EventType.START_DOCUMENT -> {
                                    bh.consume(r.version)
                                    bh.consume(r.relaxed)
                                    bh.consume(r.standalone)
                                }

                                EventType.END_ELEMENT, EventType.START_ELEMENT -> {
                                    for (i in 0 until r.attributeCount) {
                                        bh.consume(r.localName)
                                        bh.consume(r.namespaceURI)
                                        bh.consume(r.prefix)
                                        bh.consume(r.getAttributeValue(i))
                                    }
                                    bh.consume(r.localName)
                                    bh.consume(r.namespaceURI)
                                    bh.consume(r.prefix)
                                }

                                EventType.TEXT, EventType.CDSECT, EventType.ENTITY_REF, EventType.IGNORABLE_WHITESPACE, EventType.PROCESSING_INSTRUCTION, EventType.COMMENT -> {
                                    bh.consume(r.text)
                                }

                                EventType.DOCDECL -> {
                                    bh.consume(r.text)
                                }

                                EventType.END_DOCUMENT -> {}
                                EventType.ATTRIBUTE -> error("unexpected attribute")
                            }
                        }
                    }
                }
            } catch (e : Exception) {
                throw XmlException("Failure parsing $url", e)
            }
        }
    }

}
