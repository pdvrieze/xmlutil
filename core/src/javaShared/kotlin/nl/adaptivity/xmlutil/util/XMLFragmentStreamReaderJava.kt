/*
 * Copyright (c) 2018.
 *
 * This file is part of XmlUtil.
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

package nl.adaptivity.xmlutil.util

import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.util.impl.FragmentNamespaceContext

interface XMLFragmentStreamReaderJava : XmlReader {
    var localNamespaceContext: FragmentNamespaceContext
    val delegate: XmlReader


    override fun next(): EventType {
        return when (val delegateNext = delegate.next()) {
            EventType.END_DOCUMENT  -> delegateNext
            EventType.START_DOCUMENT,
            EventType.PROCESSING_INSTRUCTION,
            EventType.DOCDECL       -> next()
            EventType.START_ELEMENT -> {
                if (WRAPPERNAMESPACE.contentEquals(delegate.namespaceURI)) {
                    // Special case the wrapping namespace, dropping the element.
                    next()
                } else {
                    extendNamespace()
                    delegateNext
                }
            }
            EventType.END_ELEMENT   -> if (WRAPPERNAMESPACE.contentEquals(delegate.namespaceURI)) {
                // Drop the closing tag of the wrapper as well
                delegate.next()
            } else {
                localNamespaceContext = localNamespaceContext.parent ?: localNamespaceContext
                delegateNext
            }
            else                    -> delegateNext
        }
    }

    override val namespaceStart: Int
        get() = 0

    override val namespaceEnd: Int
        get() = localNamespaceContext.size

    override fun getNamespacePrefix(index: Int): String {
        return localNamespaceContext.getPrefix(index)
    }

    override fun getNamespaceURI(index: Int): String {
        return localNamespaceContext.getNamespaceURI(index)
    }

    override val namespaceContext: IterableNamespaceContext
        get() = localNamespaceContext

    fun extendNamespace() {
        val nsStart = delegate.namespaceStart
        val nscount = delegate.namespaceEnd - nsStart
        val prefixes = Array(nscount) { idx -> delegate.getNamespacePrefix(idx + nsStart) }
        val namespaces = Array(nscount) { idx -> delegate.getNamespaceURI(idx + nsStart) }

        localNamespaceContext = FragmentNamespaceContext(localNamespaceContext, prefixes, namespaces)
    }

    companion object {
        const val WRAPPERPPREFIX = "SDFKLJDSF"
        const val WRAPPERNAMESPACE = "http://wrapperns"

    }
}
