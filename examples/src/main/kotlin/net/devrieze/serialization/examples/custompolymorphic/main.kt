/*
 * Copyright (c) 2020-2025.
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

package net.devrieze.serialization.examples.custompolymorphic

import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import nl.adaptivity.xmlutil.serialization.XML

val fruits: List<Fruit> = listOf(
    Apple("MyApple", 5),
    Tomato("MyTomato", "red")
)

fun main() {
    val xml = XML {
        recommended_0_91_0 {
            autoPolymorphic = false
            pedantic = true
        }
    }

    println("Example fruits as XML:")
    fruits.forEach {
        println(xml.encodeToString(it))
    }

    println("\nExample fruits as data class objects: (using serializer of sealed superclass)")
    fruits.forEach {
        val encoded = xml.encodeToString(it)
        println("$encoded\n  -> ${xml.decodeFromString(Fruit.serializer(), encoded)}")
    }


    println("\nExample fruits as data class objects: (using serializer of subclass)")
    fruits.forEach { fruit ->
        val s: KSerializer<out Fruit>
        val encoded = when (fruit) {
            is Apple -> {
                s = Apple.serializer()
                val a: Apple = fruit
                xml.encodeToString(a)
            }

            is Tomato -> {
                s = Tomato.serializer()
                val t: Tomato = fruit
                xml.encodeToString(t)
            }
        }
        println("$encoded\n  -> ${xml.decodeFromString(s, encoded)}")
    }
}
