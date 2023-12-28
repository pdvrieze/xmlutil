/*
 * Copyright (c) 2023.
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

package nl.adaptivity.xmlutil.util.impl

import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.core.impl.multiplatform.Closeable
import nl.adaptivity.xmlutil.core.impl.multiplatform.Reader

/**
 * Helper reader that presents a reader that reads the given sources in order.
 */
@XmlUtilInternal
public class CombiningReader(private vararg val sources: Reader) : Reader(), Closeable {

    private var currentSource: Int = 0

    /**
     * Read data into the buffer. Note that this will only ever read from one of the sources. As
     * such the buffer may not be filled up to [len] size, even in the middle of the stream. Make
     * sure to use a **negative** return value to determine the end of the reader.
     */
    public override fun read(buf: CharArray, offset: Int, len: Int): Int {
        if (currentSource >= sources.size) return -1

        val source = sources[currentSource]
        val i = source.read(buf, offset, len)
        if (i < 0) {
            (source as? Closeable)?.close()
            ++currentSource
            return read(buf, offset, len)
        }
        return i
    }

    public override fun close() {
        sources.forEach { (it as? Closeable)?.close() }
    }
}
