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

package io.github.pdvrieze.formats.xpath.impl

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xpath.XPathVersion
import io.github.pdvrieze.xml.schematypes.values.XsdNCName
import nl.adaptivity.xmlutil.QName

@XPathInternal
internal sealed interface QNameSpec {

    fun asNodeTest(version: XPathVersion): NodeTest


    class EQName(val namespace: String?, val localName: String, val prefix: String?) : QNameSpec {
        override fun asNodeTest(version: XPathVersion): NodeTest {
            if (namespace == null && prefix == null) {
                NodeType.maybeValueOf(localName, version)?.let {
                    return NodeTypeTest(it)
                }
            }
            return NodeTest.QNameTest(asQName())
        }

        fun asQName(): QName {
            return QName(namespace ?: "", localName, prefix ?: "")
        }
    }

    sealed interface WildCard : QNameSpec {
        override fun asNodeTest(version: XPathVersion): NodeTest = asNodeTest()
        fun asNodeTest(): NodeTest
    }

    object Any : WildCard {
        override fun asNodeTest(): NodeTest = NodeTest.AnyNameTest
    }

    class LocalNameWC(val localName: String) : WildCard {
        override fun asNodeTest(): NodeTest {
            return NodeTest.LocalNameTest(localName)
        }
    }

    class Namespace(val namespace: String, val prefix: String? = null) : WildCard {
        override fun asNodeTest(): NodeTest {
            return NodeTest.NSTest(VAnyURI(namespace), prefix?.let { XsdNCName(it) })
        }
    }
}

