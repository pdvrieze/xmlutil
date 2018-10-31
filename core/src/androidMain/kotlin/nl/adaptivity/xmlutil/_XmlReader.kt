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

@file:JvmName("XmlReaderUtilJava")
package nl.adaptivity.xmlutil

import java.io.CharArrayWriter


@Throws(XmlException::class)
fun XmlReader.toCharArrayWriter(): CharArrayWriter {
    return CharArrayWriter().apply {
        XmlStreaming.newWriter(this).use { out ->
            while (hasNext()) {
                writeCurrent(out)
            }
        }
    }
}
