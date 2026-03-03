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

package io.github.pdvrieze.xml.schematypes.values.instances

import io.github.pdvrieze.xml.schematypes.types.LanguageType
import io.github.pdvrieze.xml.schematypes.values.XSLanguage
import nl.adaptivity.xmlutil.XmlUtilInternal
import kotlin.jvm.JvmInline

@JvmInline
@XmlUtilInternal
value class XSLanguageImpl internal constructor(override val xmlString: String) :
    XSLanguage {

    override val schemaType: LanguageType<*> get() = LanguageType.Instance

    init {
        // required pattern: "[a-zA-Z]{1,8}(-[a-zA-Z0-9]{1,8})*"
        var i = 0
        val l = xmlString.length
        do {
            val start = i
            while (i < l) {
                when (xmlString[i++]) {
                    '-' -> when {
                        i - start == 1 -> throw IllegalArgumentException("Missing letter before dash in language: $xmlString")
                        else -> break
                    }

                    in 'a'..'z',
                    in 'A' .. 'Z' -> {
                        require(i-start<=8) { "Language identifier too long: ${xmlString.substring(start, i)}" }
                    }

                    else -> throw IllegalArgumentException("Invalid character '${xmlString[i - 1]}' in language: $xmlString")
                }
            }

            while (i < l && xmlString[i] in 'a'..'z') i++
            if (i == start) break
            if (i < l && xmlString[i] == '-') i++
            while (i < l && xmlString[i] in 'a'..'z' || xmlString[i] in '0'..'9') i++
        } while (i < l)
    }

    override fun toString(): String = xmlString
}
