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

package nl.adaptivity.xmlutil.util

import java.io.IOException
import java.io.Reader

/**
 * Reader that combines multiple "component" readers into one.
 * Created by pdvrieze on 01/11/15.
 */
class CombiningReader(private vararg val sources: Reader) : Reader() {

    private var currentSource: Int = 0

    override fun read(cbuf: CharArray, off: Int, len: Int): Int {
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

    override fun close() {
        sources.forEach { it.close() }
    }

    override fun ready(): Boolean {
        if (currentSource >= sources.size) {
            return false
        }
        return sources[currentSource].ready()
    }

    override fun mark(readAheadLimit: Int): Nothing {
        throw IOException("Mark not supported")
    }

    override fun reset() {
        for (i in currentSource downTo 0) {
            sources[i].reset()
            currentSource = i
        }
    }
}