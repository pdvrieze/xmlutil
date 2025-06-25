/*
 * Copyright (c) 2024.
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

package nl.adaptivity.xmlutil.benchmark.util

import io.github.pdvrieze.formats.xmlschemaTests.io.github.pdvrieze.formats.xmlschemaTests.resolve
import io.github.pdvrieze.formats.xmlschemaTests.io.github.pdvrieze.formats.xmlschemaTests.withXmlReader
import nl.adaptivity.xmlutil.serialization.DefaultXmlSerializationPolicy
import nl.adaptivity.xmlutil.serialization.XML
import org.w3.xml.xmschematestsuite.TSTestSet
import org.w3.xml.xmschematestsuite.TSTestSuite
import org.w3.xml.xmschematestsuite.override.CompactOverride
import org.w3.xml.xmschematestsuite.override.OTSSuite
import java.net.URL
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class MeasureInfo(val round: Int, val rounds: Int, val warmups: Int)

@OptIn(ExperimentalTime::class)
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
        val warmupExtra = (startTime - initTime - duration)/warmups.coerceAtLeast(1)
        println("$name: Duration time × $rounds): $duration ms (+${warmupExtra} ms)")
        return duration
    }
}


internal fun testXmlSchemaUrls(xml: XML = XML {}): List<Pair<URL, URL>> {

    val p = xml.config.policy as? DefaultXmlSerializationPolicy
    val xml2 = when (p?.autoPolymorphic) {
        true -> xml
        else -> xml.copy { autoPolymorphic = true }
    }

    val suiteURL: URL = MeasureInfo::class.java.getResource("/xsts/suite.xml")!!

    val override = MeasureInfo::class.java.getResource("/override.xml")!!.withXmlReader {
        val compact = xml2.decodeFromReader<CompactOverride>(it)
        OTSSuite(compact)
    }

    return suiteURL.withXmlReader { xmlReader ->
        val suite = xml2.decodeFromReader<TSTestSuite>(xmlReader)
        suite.testSetRefs
            //                .filter { arrayOf("sunMeta/").any { m -> it.href.contains(m) } }
            .flatMap { setRef ->
                val setBaseUrl: URL = MeasureInfo::class.java.getResource("/xsts/${setRef.href}")!!
                val testSet = override.applyTo(setBaseUrl.withXmlReader { r -> xml2.decodeFromReader<TSTestSet>(r) })

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

