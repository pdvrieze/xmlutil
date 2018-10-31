/*
 * Copyright (c) 2018.
 *
 * This file is part of xmlutil.
 *
 * xmlutil is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * xmlutil is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with xmlutil.  If not,
 * see <http://www.gnu.org/licenses/>.
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

