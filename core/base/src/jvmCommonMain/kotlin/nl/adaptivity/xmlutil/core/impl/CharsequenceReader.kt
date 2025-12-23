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

@file:MustUseReturnValues

package nl.adaptivity.xmlutil.core.impl

import java.io.Reader
import java.io.StringReader
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

@Suppress("FunctionName")
internal fun CharsequenceReader(sequence: CharSequence): Reader = when (sequence) {
    is String -> StringReader(sequence)
    else -> CharsequenceReader(sequence, 0)
}

internal class CharsequenceReader(
    private val sequence: CharSequence,
    @Suppress("UNUSED_PARAMETER") dummy: Int
) : Reader() {
    private var pos: Int = 0
    private var mark: Int = 0

    private val reentrantLock = ReentrantLock()

    override fun close() = reentrantLock {
        pos = -1
    }

    override fun read(): Int = reentrantLock {
        when {
            pos < 0 -> throw IllegalStateException("Reader closed")
            pos >= sequence.length -> -1
            else -> sequence[pos].code.apply { pos++ }
        }
    }

    override fun skip(n: Long): Long {
        reentrantLock {
            val origPos = pos
            pos = (pos + n.toInt()).coerceAtMost(sequence.length)
            return (pos - origPos).toLong()
        }
    }

    override fun ready(): Boolean {
        reentrantLock {
            return pos in sequence.indices
        }
    }

    override fun reset() = reentrantLock {
        pos = mark
    }

    override fun markSupported(): Boolean = true

    override fun mark(readAheadLimit: Int) = reentrantLock {
        mark = pos
    }

    override fun read(cbuf: CharArray, off: Int, len: Int): Int {
        reentrantLock {
            // Make sure to signal end of file
            if (pos >= sequence.length) return -1

            val origPos = pos
            for (i in off until (off + len.coerceAtMost(sequence.length - pos))) {
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
