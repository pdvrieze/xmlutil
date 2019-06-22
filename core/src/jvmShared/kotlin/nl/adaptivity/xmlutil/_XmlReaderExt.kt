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

@file:JvmName("XmlReaderExt")

package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.util.CompactFragment
import java.io.CharArrayWriter
import java.util.*

/**
 * Functions that work on both js/jvm but have different implementations
 */

/*
 * Read the current element (and content) and all its siblings into a fragment.
 *
 * @param this The source stream.
 *
 * @return the fragment
 *
 * @throws XmlException parsing failed
 */
actual fun XmlReader.siblingsToFragment(): CompactFragment {
    val caw = CharArrayWriter()
    if (!isStarted) {
        if (hasNext()) {
            next()
        } else {
            return CompactFragment("")
        }
    }

    val startLocation = locationInfo
    try {

        val missingNamespaces = TreeMap<String, String>()
        // If we are at a start tag, the depth will already have been increased. So in that case, reduce one.
        val initialDepth = depth - if (eventType === EventType.START_ELEMENT) 1 else 0
        var type: EventType? = eventType
        while (type !== EventType.END_DOCUMENT && type !== EventType.END_ELEMENT && depth >= initialDepth) {
            @Suppress("NON_EXHAUSTIVE_WHEN")
            when (type) {
                EventType.START_ELEMENT        -> {
                    val out = XmlStreaming.newWriter(caw)
                    writeCurrent(out) // writes the start tag
                    out.addUndeclaredNamespaces(this, missingNamespaces)
                    out.writeElementContent(missingNamespaces, this) // writes the children and end tag
                    out.close()
                }

                EventType.IGNORABLE_WHITESPACE ->
                    if (text.isNotEmpty()) caw.append(text.xmlEncode())

                EventType.TEXT,
                EventType.CDSECT               ->
                    caw.append(text.xmlEncode())
            }
            type = if (hasNext()) next() else null
        }
        return CompactFragment(SimpleNamespaceContext(missingNamespaces), caw.toCharArray())
    } catch (e: XmlException) {
        throw XmlException("Failure to parse children into string at $startLocation", e)
    } catch (e: RuntimeException) {
        throw XmlException("Failure to parse children into string at $startLocation", e)
    }

}
