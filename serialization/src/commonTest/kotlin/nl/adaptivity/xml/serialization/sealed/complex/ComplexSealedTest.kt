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

@file:Suppress("unused")

package nl.adaptivity.xml.serialization.sealed.complex

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.EmptySerializersModule
import nl.adaptivity.xml.serialization.TestBase
import nl.adaptivity.xmlutil.serialization.XML

@OptIn(ExperimentalSerializationApi::class)
class ComplexSealedTest : TestBase<ComplexSealedTest.ComplexSealedHolder>(
    ComplexSealedHolder("a", 1, 1.5f, OptionB1(5, 6, 7)),
    ComplexSealedHolder.serializer(),
    EmptySerializersModule,
    XML { autoPolymorphic = true }
) {
    override val expectedXML: String
        get() = "<ComplexSealedHolder a=\"a\" b=\"1\" c=\"1.5\"><OptionB1 g=\"5\" h=\"6\" i=\"7\"/></ComplexSealedHolder>"

    override val expectedJson: String
        get() = "{\"a\":\"a\",\"b\":1,\"c\":1.5,\"options\":{\"type\":\"nl.adaptivity.xml.serialization.sealed.complex.OptionB1\",\"g\":5,\"h\":6,\"i\":7}}"

    @Serializable
    data class ComplexSealedHolder(val a: String, val b: Int, val c: Float, val options: Option?)

}

@Serializable
sealed class Option

@Serializable
sealed class OptionB : Option()

@Serializable
private class OptionA(val d: Int, val e: Int, val f: Int) : Option() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OptionA) return false

        if (d != other.d) return false
        if (e != other.e) return false
        if (f != other.f) return false

        return true
    }

    override fun hashCode(): Int {
        var result = d
        result = 31 * result + e
        result = 31 * result + f
        return result
    }
}

@Serializable
private class OptionB1(val g: Int, val h: Int, val i: Int) : OptionB() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OptionB1) return false

        if (g != other.g) return false
        if (h != other.h) return false
        if (i != other.i) return false

        return true
    }

    override fun hashCode(): Int {
        var result = g
        result = 31 * result + h
        result = 31 * result + i
        return result
    }
}

@Serializable
private class OptionB2(val j: Int, val k: Int, val l: Int) : OptionB() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OptionB2) return false

        if (j != other.j) return false
        if (k != other.k) return false
        if (l != other.l) return false

        return true
    }

    override fun hashCode(): Int {
        var result = j
        result = 31 * result + k
        result = 31 * result + l
        return result
    }
}
