/*
 * Copyright (c) 2021-2026.
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

package io.github.pdvrieze.xml.schematypes.values

import io.github.pdvrieze.xml.schematypes.impl.SimpleTypeSerializer
import io.github.pdvrieze.xml.schematypes.types.AnyURIType
import io.github.pdvrieze.xml.schematypes.values.instances.XSParsedUri
import io.github.pdvrieze.xml.schematypes.values.instances.XSRelaxedUri
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.xmlCollapseWhitespace

@ExperimentalXmlUtilApi
@Serializable(XSAnyURI.Companion::class)
interface XSAnyURI : XSAtomic, CharSequence {
    override val schemaType: AnyURIType<XSAnyURI>

    val value: String get() = xmlString.toString()

    operator fun component1(): String = value

    companion object Companion : SimpleTypeSerializer<XSAnyURI>("xsd.anyURI") {
        operator fun invoke(value: String) = value.toAnyUri()
        operator fun invoke(value: CharSequence) = value.toString().toAnyUri()

        override fun deserialize(
            raw: String,
            input: XmlReader?
        ): XSAnyURI {
            return XSAnyURI(raw)
        }

        val EMPTY: XSAnyURI = "".toAnyUri()

    }
}

@ExperimentalXmlUtilApi
fun String.toAnyUri(): XSAnyURI {
    val s = xmlCollapseWhitespace(this)
    return runCatching { XSParsedUri(s) }.getOrElse { XSRelaxedUri(s) }
}
