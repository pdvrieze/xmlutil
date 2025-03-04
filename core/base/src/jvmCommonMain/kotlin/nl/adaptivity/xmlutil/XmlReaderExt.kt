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

@file:JvmName("XmlReaderUtil")
@file:JvmMultifileClass

package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.core.impl.multiplatform.use
import nl.adaptivity.xmlutil.util.CompactFragment
import java.io.CharArrayWriter

/*
 * Functions that work on both js/jvm but have different implementations
 */

/**
 * Read the current element (and content) and all its siblings into a fragment.
 *
 * @param this The source stream.
 *
 * @return the fragment
 *
 * @throws XmlException parsing failed
 */
public actual fun XmlReader.siblingsToFragment(): CompactFragment {
    return siblingsToFragmentImpl()
}


public fun XmlReader.toCharArrayWriter(): CharArrayWriter {
    return CharArrayWriter().also {
        @Suppress("DEPRECATION")
        XmlStreaming.newWriter(it as Appendable).use { out ->
            while (hasNext()) {
                next()
                writeCurrent(out)
            }
        }
    }
}

internal fun XmlReader.toCharArrayWriterImpl(): CharArrayWriter {
    return CharArrayWriter().also {
        @Suppress("DEPRECATION")
        XmlStreaming.newWriter(it as Appendable).use { out ->
            while (hasNext()) {
                next()
                writeCurrent(out)
            }
        }
    }
}
