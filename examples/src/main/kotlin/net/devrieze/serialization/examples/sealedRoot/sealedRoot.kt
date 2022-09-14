/*
 * Copyright (c) 2022.
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

package net.devrieze.serialization.examples.sealedRoot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement

/* This is code extended from issue #98 */

@Serializable
sealed class Base {

    abstract val context: String

    @Serializable
    @SerialName("car")
    data class NoResultsFound(
        @XmlElement(false)
        override val context: String
    ) : Base()

    @Serializable
    @SerialName("cars")
    data class CarsFound(
        @XmlElement(false)
        override val context: String,
        @XmlElement(true)
        val cars: List<Car>
    ) : Base() {

        @Serializable
        @SerialName("car2")
        data class Car(val color: String)

    }

}

fun main() {

    val xml = XML { autoPolymorphic = true }

    val carsFoundXml = """
    <cars context="main">
        <car2 color="red" />
        <car2 color="blue" />
        <car2 color="green" />
    </cars>""".trimIndent()

    val noCarsFoundXml = """<car context="main" />"""

// Works
    val carsFound = xml.decodeFromString(Base.CarsFound.serializer(), carsFoundXml)
    println(carsFound)

// Works
    val noCarsFound = xml.decodeFromString(Base.NoResultsFound.serializer(), noCarsFoundXml)
    println(noCarsFound)

// Fails with UnknownXmlFieldException
    val carsFoundFromBase = xml.decodeFromString(Base.serializer(), carsFoundXml)
    println(carsFoundFromBase)

// Fails with UnknownXmlFieldException
    val noCarsFoundFromBase = xml.decodeFromString(Base.serializer(), noCarsFoundXml)
    println(noCarsFoundFromBase)
}
