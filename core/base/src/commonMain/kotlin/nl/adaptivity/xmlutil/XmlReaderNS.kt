/*
 * Copyright (c) 2024-2025.
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


@file:JvmName("XmlReaderUtil")
@file:JvmMultifileClass

package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.core.KtXmlWriter
import nl.adaptivity.xmlutil.core.impl.multiplatform.use
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.ICompactFragment
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * Differs from [.siblingsToFragment] in that it skips the current event.
 *
 * @throws XmlException
 */
public fun XmlReader.elementContentToFragment(): ICompactFragment {
    val r = this
    r.skipPreamble()
    if (r.hasNext()) {
        r.require(EventType.START_ELEMENT, null, null)
        r.next()
        return r.siblingsToFragment()
    }
    return CompactFragment("")
}

public fun XmlReader.siblingsToFragment(): CompactFragment {
    val appendable: Appendable = StringBuilder()
    if (!isStarted) {
        if (hasNext()) {
            next()
        } else {
            return CompactFragment("")
        }
    }

    val startLocation = extLocationInfo
    try {

        val missingNamespaces: MutableMap<String, String> = mutableMapOf()
        // If we are at a start tag, the depth will already have been increased. So in that case, reduce one.
        val initialDepth = depth - if (eventType === EventType.START_ELEMENT) 1 else 0


        var type: EventType? = eventType
        while (type !== EventType.END_DOCUMENT && (type !== EventType.END_ELEMENT || depth > initialDepth)) {
            when (type) {
                EventType.START_ELEMENT ->
                    KtXmlWriter(appendable, isRepairNamespaces = false, xmlDeclMode = XmlDeclMode.None).use { out ->
                        out.indentString = "" // disable indents
                        val namespaceForPrefix = out.getNamespaceUri(prefix)
                        writeCurrent(out) // writes the start tag
                        if (namespaceForPrefix != namespaceURI) {
                            @Suppress("DEPRECATION")
                            out.addUndeclaredNamespaces(this, missingNamespaces)
                        }
                        out.writeElementContent(missingNamespaces, this) // writes the children and end tag
                    }

                EventType.IGNORABLE_WHITESPACE ->
                    if (text.isNotEmpty()) appendable.append(text.xmlEncode())

                EventType.ENTITY_REF,
                EventType.TEXT,
                EventType.CDSECT ->
                    appendable.append(text.xmlEncode())

                else -> {
                } // ignore, note this also removes any xml comments
            }
            type = if (hasNext()) next() else break
        }

        if (missingNamespaces[""] == "") missingNamespaces.remove("")

        return CompactFragment(SimpleNamespaceContext(missingNamespaces), appendable.toString())
    } catch (e: XmlException) {
        throw XmlException("Failure to parse children into string at $startLocation", e)
    } catch (e: RuntimeException) {
        throw XmlException("Failure to parse children into string at $startLocation", e)
    }
}


@Suppress("DeprecatedCallableAddReplaceWith", "DEPRECATION", "KotlinRedundantDiagnosticSuppress")
@Deprecated("This is inefficient in Javascript")
public fun XmlReader.siblingsToCharArray(): CharArray = siblingsToFragment().content

/**
 * Read the current element (and content) *only* into a fragment.
 *
 * @receiver The source stream.
 *
 * @return the fragment
 *
 * @throws XmlException parsing failed
 */
public fun XmlReader.elementToFragment(): CompactFragment {
    val output = StringBuilder()
    if (!isStarted) {
        if (hasNext()) {
            next()
        } else {
            return CompactFragment("")
        }
    }

    val startLocation = extLocationInfo
    try {

        val missingNamespaces = mutableMapOf<String, String>()
        // If we are at a start tag, the depth will already have been increased. So in that case, reduce one.

        if (eventType.isTextElement || eventType == EventType.IGNORABLE_WHITESPACE) return CompactFragment(text) // Allow for text only compact fragment.

        require(EventType.START_ELEMENT, null, null)
        KtXmlWriter(output, isRepairNamespaces = false, xmlDeclMode = XmlDeclMode.None).use { out ->
            out.indentString = "" // disable indents
            while (eventType == EventType.IGNORABLE_WHITESPACE) {
                out.ignorableWhitespace(text)
                next()
            }
            if (eventType == EventType.END_ELEMENT || eventType == EventType.END_DOCUMENT) {
                return CompactFragment(output.toString())
            }

            require(EventType.START_ELEMENT, null, null)


            val namespaceForPrefix = out.getNamespaceUri(prefix)
            writeCurrent(out) // writes the start tag
            @Suppress("DEPRECATION")
            out.addUndeclaredNamespaces(this, missingNamespaces)
            out.writeElementContent(missingNamespaces, this) // writes the children and end tag
        }

        if (missingNamespaces[""] == "") missingNamespaces.remove("")

        return CompactFragment(SimpleNamespaceContext(missingNamespaces), output.toString())
    } catch (e: XmlException) {
        throw XmlException("Failure to parse children into string", startLocation, e)
    } catch (e: RuntimeException) {
        throw XmlException("Failure to parse children into string", startLocation, e)
    }

}
