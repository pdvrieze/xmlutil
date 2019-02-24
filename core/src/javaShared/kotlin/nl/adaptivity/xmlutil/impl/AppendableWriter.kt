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

package nl.adaptivity.xmlutil.impl

import java.io.Writer
import java.util.concurrent.locks.ReentrantLock

internal class AppendableWriter(private val appendable: Appendable) : Writer() {
    private val lock = ReentrantLock()

    override fun write(c: Int): Unit = lock {
        appendable.append(c.toChar())
    }

    override fun write(cbuf: CharArray, off: Int, len: Int): Unit = lock {
        appendable.append(CharArraySequence(cbuf, off, len))
    }

    override fun write(str: String, off: Int, len: Int): Unit = lock {
        appendable.append(str, off, len)
    }

    override fun append(c: Char) = apply {
        lock {
            appendable.append(c)
        }
    }

    override fun append(csq: CharSequence, start: Int, end: Int) = apply {
        lock {
            appendable.append(csq, start, end)
        }
    }

    override fun flush() {}

    override fun close() {}
}

