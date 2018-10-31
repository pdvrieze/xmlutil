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

package nl.adaptivity.xmlutil.util

import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.XmlDeserializerFactory
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlSerializable

/**
 * A class representing an xml fragment compactly.
 * Created by pdvrieze on 06/11/15.2
 */
expect class CompactFragment : ICompactFragment {
    constructor(content: String)
    constructor(orig: ICompactFragment)
    constructor(content: XmlSerializable)
    constructor(namespaces: Iterable<Namespace>, content: CharArray?)
    constructor(namespaces: Iterable<Namespace>, content: String)

    class Factory() : XmlDeserializerFactory<CompactFragment>

    companion object {
        fun deserialize(reader: XmlReader): CompactFragment
    }
}
