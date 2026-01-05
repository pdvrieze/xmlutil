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

/*
 * Example test case by github.com/users/auzatsepin in #271/#272
 */

@file:Suppress("PublicApiImplicitType")
@file:MustUseReturnValues

package net.devrieze.serialization.examples.anylist

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.serializer
import nl.adaptivity.xmlutil.serialization.*

internal val anyListModule = SerializersModule {
    polymorphic(Any::class) {
        subclass(Order1::class, serializer())
        subclass(StopOrder1::class, serializer())
        subclass(String::class, String.serializer())
    }
}

private val parser = XML.v1(anyListModule)

interface XmlEntity

@Serializable
data class Orders1(
    var orders: List<@Polymorphic Any>
) : XmlEntity

@Serializable
data class Orders2(
    @XmlIgnoreWhitespace(true)
    @XmlValue var orders: List<@Polymorphic Any>
) : XmlEntity

@Serializable
@XmlSerialName("order")
data class Order1(
    @SerialName("transactionid") val transactionId: String,
    @XmlElement @SerialName("secid") val secId: String,
) : XmlEntity

@Serializable
@XmlSerialName("stoporder")
data class StopOrder1(
    @SerialName("transactionid") val transactionId: String,
    @XmlElement @SerialName("secid") val secId: String,
) : XmlEntity

@Serializable
@SerialName("stoploss")
data class StopLoss(
    @SerialName("usecredit") val useCredit: String,
    @XmlElement @SerialName("activationprice") val activationPrice: Double,
    @XmlElement val quantity: Int,
    @XmlElement @SerialName("bymarket") val byMarket: Boolean
) : XmlEntity

fun xmlData(text: String = "") = """<orders>
    <order transactionid="32021651">
        <secid>8135</secid>
    </order>
    $text
    <stoporder transactionid="20105503">
        <secid>8135</secid>
    </stoporder>
    <stoporder transactionid="31465073">
        <secid>8135</secid>
    </stoporder>
</orders>"""

fun main() {
    val decoded1 = parser.decodeFromString(
        serializer<Orders1>(),
        xmlData("<string xmlns=\"http://www.w3.org/2001/XMLSchema\">foo</string>")
    )
    println("size " + decoded1.orders.size)
    println(decoded1)
    val decoded2 = parser.decodeFromString(serializer<Orders2>(), xmlData("Random string content"))
    println("size " + decoded2.orders.size)
    println(decoded2)
}
