/*
 * Copyright (c) 2018.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.util

import java.io.Reader
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

internal class CharsequenceReader(private val sequence: CharSequence): Reader() {
    private var pos: Int = 0
    private var mark: Int = 0

    private val lock = ReentrantLock()

    override fun close() = lock {
        pos = -1
    }

    override fun read(): Int = lock {
        when {
            pos<0 -> throw IllegalStateException("Reader closed")
            pos>=sequence.length -> -1
            else ->sequence[pos].toInt().apply { pos++ }
        }
    }

    override fun skip(n: Long): Long {
        lock {
            val origPos = pos
            pos = (pos+n.toInt()).coerceAtMost(sequence.length)
            return (pos - origPos).toLong()
        }
    }

    override fun ready(): Boolean {
        lock {
            return pos in 0 until sequence.length
        }
    }

    override fun reset() = lock {
        pos = mark
    }

    override fun markSupported(): Boolean = true

    override fun mark(readAheadLimit: Int) = lock {
        mark = pos
    }

    override fun read(cbuf: CharArray, off: Int, len: Int): Int {
        lock {
            val origPos = pos
            for (i in off until (off + len).coerceAtMost(sequence.length - pos)) {
                cbuf[i] = sequence[pos]
                pos++
            }
            return pos - origPos
        }
    }
}


internal inline operator fun <R> Lock.invoke(body: () -> R): R {
    lock()
    try {
        return body()
    } finally {
        unlock()
    }
}