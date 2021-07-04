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

public interface XMLFragmentStreamReaderJava : XmlReader {
    public var localNamespaceContext: FragmentNamespaceContext
    public val delegate: XmlReader


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

    override val namespaceContext: IterableNamespaceContext
        get() = localNamespaceContext

    public companion object {
        public const val WRAPPERPPREFIX: String = "SDFKLJDSF"
        public const val WRAPPERNAMESPACE: String = "http://wrapperns"

    }
}

internal fun XMLFragmentStreamReaderJava.extendNamespace() {
    val namespaceDecls = delegate.namespaceDecls
    val nscount = namespaceDecls.size
    val prefixes = Array(nscount) { idx -> namespaceDecls[idx].prefix }
    val namespaces = Array(nscount) { idx -> namespaceDecls[idx].namespaceURI }

    localNamespaceContext = FragmentNamespaceContext(localNamespaceContext, prefixes, namespaces)
}
