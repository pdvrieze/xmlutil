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

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization

import io.github.pdvrieze.formats.xmlschema.resolved.BuiltinSchemaXmlschema
import io.github.pdvrieze.formats.xmlschema.resolved.SchemaVersion
import nl.adaptivity.xmlutil.*

class VersionFilter(delegate: XmlBufferedReader) : XmlDelegatingReader(delegate) {

    constructor(delegate: XmlReader) : this(delegate as? XmlBufferedReader ?: XmlBufferedReader(delegate))

    override fun nextTag(): EventType {
        val e = delegate.nextTag()
        return when {
            e == EventType.START_ELEMENT && shouldSkip() -> {
                delegate.skipElement()
                nextTag()
            }

            else -> e
        }
    }

    override fun hasNext(): Boolean {
        val d = delegate as XmlBufferedReader
        val p = (d.peek() ?: return false) as? XmlEvent.StartElementEvent ?: return true

        return when {
            shouldSkip(p.attributes) -> {
                delegate.next()
                delegate.skipElement()
                hasNext()
            }

            else -> true
        }
    }

    override fun next(): EventType {
        val e = delegate.next()
        return when {
            e == EventType.START_ELEMENT && shouldSkip() -> {
                delegate.skipElement()
                next()
            }

            else -> e
        }
    }


    private fun shouldSkip(): Boolean {
        return shouldSkip(delegate.attributes)
    }

    private fun shouldSkip(attributes: Array<out XmlEvent.Attribute>): Boolean {
        var minVersion: SchemaVersion? = null
        var maxVersion: SchemaVersion? = null
        var typeAvailable: List<QName>? = null
        var typeUnavailable: List<QName>? = null
        var facetAvailable: List<QName>? = null
        var facetUnavailable: List<QName>? = null

        for (attr in attributes) {
            if (attr.namespaceUri == XMLConstants.XSVER_NS_URI) {
                val attrVal = xmlCollapseWhitespace(attr.value)
                when (attr.localName) {
                    "minVersion" -> minVersion = SchemaVersion.fromXml(attrVal)
                    "maxVersion" -> maxVersion = SchemaVersion.fromXml(attrVal)
                    "typeAvailable" -> typeAvailable = attrVal.split(' ').map { it.toQName() }
                    "typeUnavailable" -> typeUnavailable = attrVal.split(' ').map { it.toQName() }
                    "facetAvailable" -> facetAvailable = attrVal.split(' ').map { it.toQName() }
                    "facetUnavailable" -> facetUnavailable = attrVal.split(' ').map { it.toQName() }
                }
            }
        }

        if (minVersion != null && minVersion > SchemaVersion.V1_1) return true
        if (maxVersion != null && maxVersion <= SchemaVersion.V1_0) return true

        if (typeAvailable != null) {
            if (typeAvailable.any {
                it.namespaceURI != BuiltinSchemaXmlschema.targetNamespace.value ||
                BuiltinSchemaXmlschema.maybeType(it) == null
            }) return true
        }

        if (typeUnavailable != null) {
            if (typeUnavailable.all {
                    it.namespaceURI == BuiltinSchemaXmlschema.targetNamespace.value &&
                BuiltinSchemaXmlschema.maybeType(it) != null
            }) return true
        }

        if (facetAvailable != null) {
            if (facetAvailable.any { !isSupportedFacet(it) }) return true
        }

        if (facetUnavailable != null) {
            if (facetUnavailable.all { isSupportedFacet(it) }) return true
        }

        return false
    }

    private fun isSupportedFacet(name: QName): Boolean {
        if (name.namespaceURI != XMLConstants.XSD_NS_URI) return false
        return when (name.localPart) {
            "assertion",
            "enumeration",
            "explicitTimezone",
            "fractionDigits",
            "length",
            "minExclusive",
            "minInclusive",
            "minLength",
            "maxExclusive",
            "maxInclusive",
            "maxLength",
            "pattern",
            "totalDigits",
            "whitespace" -> true

            else -> false
        }
    }

    private fun String.toQName(): QName {
        val cPos = indexOf(':')
        val prefix: String
        val localname: String
        if (cPos < 0) {
            prefix = ""
            localname = this
        } else {
            prefix = substring(0, cPos)
            localname = substring(cPos + 1)
        }

        val namespace = delegate.namespaceContext.getNamespaceURI(prefix) ?: ""
        return QName(namespace, localname, prefix)
    }

}
