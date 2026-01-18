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

package nl.adaptivity.xml.serialization.regressions

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.dom2.Element
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.test.multiplatform.Target
import nl.adaptivity.xmlutil.test.multiplatform.testTarget
import kotlin.test.Test
import kotlin.test.assertEquals

class TestSuite325 {

    @Test
    fun testDeserialize() {
        if (testTarget == Target.Node) return

        @Suppress("DEPRECATION")
        val xml = XML.compat { recommended() }
        val data = xml.decodeFromString<TestSuite>(XMLDATA)
        assertEquals(2, data.testcase.size)
    }

    @Serializable
    data class TestSuite(
        var name: String? = null,
        var tests: Int? = null,
        var failures: Int? = null,
        var errors: Int? = null,
        var skipped: Int? = null,
        var time: String? = null,
        var timestamp: String? = null,
        var hostname: String? = null,
        @XmlSerialName("properties")
        @XmlElement var properties: Element? = null,
        @XmlSerialName("testcase")
        @XmlElement var testcase: List<TestCase> = listOf(),
    )

    @Serializable
    data class TestCase(
        var name: String? = null,
        var classname: String? = null,
        var time: String? = null,
        @XmlElement var failure: String? = null,
    )

    companion object {
        val XMLDATA = """
        |    <testsuite name="" tests="2" failures="1" errors="0" skipped="0" time="13.248" timestamp="2021-05-23T11:47:13" hostname="localhost">
        |    <properties />
        |    <testcase name="onboardingUniversalLinkWhenBankVerificationCompleteTest" classname="package.activity.redirect.PaymentOnBoardingFromRedirectUrlTest" time="7.534">
        |    <failure>Failure message</failure>
        |    </testcase>
        |    <testcase name="multiFeature_shouldRequestBuyBumpupForExistingAd" classname="package.features.placing.newsyi.FeatureFeesFragmentTest" time="5.714" />
        |    </testsuite>
        """.trimMargin()
    }
}
