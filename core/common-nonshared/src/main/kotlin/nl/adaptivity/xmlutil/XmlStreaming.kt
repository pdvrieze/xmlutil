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

package nl.adaptivity.xmlutil

/**
 * Utility class with factories and constants for the [XmlReader] and [XmlWriter] interfaces.
 * Created by pdvrieze on 15/11/15.
 */
expect object XmlStreaming {

    fun setFactory(factory: XmlStreamingFactory?)

    inline fun <reified T : Any> deSerialize(input: String): T

    fun toString(value: XmlSerializable): String

    fun newReader(input: CharSequence): XmlReader

    fun newWriter(output: Appendable, repairNamespaces: Boolean = false, omitXmlDecl: Boolean = false): XmlWriter
}

