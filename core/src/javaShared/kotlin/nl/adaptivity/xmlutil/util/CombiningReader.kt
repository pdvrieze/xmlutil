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

import java.io.IOException
import java.io.Reader

/**
 * Reader that combines multiple "component" readers into one.
 * Created by pdvrieze on 01/11/15.
 */
public class CombiningReader(private vararg val sources: Reader) : Reader() {

    private var currentSource: Int = 0

    public override fun read(cbuf: CharArray, off: Int, len: Int): Int {
        if (currentSource >= sources.size) return -1

        val source = sources[currentSource]
        val i = source.read(cbuf, off, len)
        if (i < 0) {
            source.close()
            ++currentSource
            return read(cbuf, off, len)
        }
        return i
    }

    public override fun close() {
        sources.forEach { it.close() }
    }

    public override fun ready(): Boolean {
        if (currentSource >= sources.size) {
            return false
        }
        return sources[currentSource].ready()
    }

    public override fun mark(readAheadLimit: Int): Nothing {
        throw IOException("Mark not supported")
    }

    public override fun reset() {
        for (i in currentSource downTo 0) {
            sources[i].reset()
            currentSource = i
        }
    }
}
