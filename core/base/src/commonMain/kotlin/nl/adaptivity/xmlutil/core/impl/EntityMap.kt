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

package nl.adaptivity.xmlutil.core.impl

import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.core.XmlEntity

internal object DefaultEntityMap {
    private class DefaultEntity(
        override val simpleValue: String,
        replacementValue: String
    ) : XmlEntity(replacementValue, false) {
        override val isSimple get() = true
    }

    val LT: XmlEntity = DefaultEntity("<", "&#60;")
    val GT: XmlEntity = DefaultEntity(">", "&#62;")
    val AMP: XmlEntity = DefaultEntity("&", "&#38;")
    val APOS: XmlEntity = DefaultEntity("'", "&#39;")
    val QUOT: XmlEntity = DefaultEntity("\"", "&#34;")

    public operator fun get(key: String): XmlEntity? {
        when (key.length) {
            2 -> when (key) {
                "gt" -> return GT
                "lt" -> return LT
            }

            3 -> when (key) {
                "amp" -> return AMP
            }

            4 -> when (key) {
                "apos" -> return APOS
                "quot" -> return QUOT
            }
        }
        // TODO return null
        return null
    }

}

@XmlUtilInternal
public class EntityMap {
    private val otherEntities = HashMap<String, XmlEntity>(8)

    public operator fun get(key: String): XmlEntity? {
        return DefaultEntityMap.get(key) ?: otherEntities[key]
    }

    public operator fun set(key: String, value: XmlEntity) {
        otherEntities[key] = value
    }

}

