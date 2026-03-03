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

import io.github.pdvrieze.xml.schematypes.impl.SimpleTypeSerializer
import io.github.pdvrieze.xml.schematypes.types.AnyURIType
import io.github.pdvrieze.xml.schematypes.values.XSAnyURI
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.xmlCollapseWhitespace

/**
 * Instance of a URI that has not been parsed into components.
 */
@ExperimentalXmlUtilApi
@Serializable(XSRelaxedUri.Companion::class)
class XSRelaxedUri(override val xmlString: String) : XSAnyURI {
    constructor(charSequence: CharSequence) : this(charSequence.toString())

    init {
        // This can not be AnyURIType as it is used in defining AtomicDataType
        require(value == xmlCollapseWhitespace(value)) {
            "Not initialised with normalised value"
        }
    }


    override val length: Int get() = xmlString.length

    override val schemaType: AnyURIType<*> get() = AnyURIType.Instance

    override fun get(index: Int): Char = xmlString.get(index)

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
        xmlString.subSequence(startIndex, endIndex)

    override fun toString(): String = xmlString

    companion object : SimpleTypeSerializer<XSRelaxedUri>("xsd.anyURI.relaxed") {
        override fun deserialize(raw: String, input: XmlReader?): XSRelaxedUri {
            return XSRelaxedUri(xmlCollapseWhitespace(raw))
        }
    }
}
