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

package nl.adaptivity.util.xml

import nl.adaptivity.xml.SimpleNamespaceContext
import nl.adaptivity.xml.XmlReader
import nl.adaptivity.xml.XmlSerializable


/**
 * A class representing an xml fragment compactly.
 * Created by pdvrieze on 06/11/15.2
 */
interface CompactFragment : XmlSerializable {

    val isEmpty: Boolean
        get() = content.isEmpty()

    val namespaces: SimpleNamespaceContext
    val content: CharArray

    val contentString: String

    fun getXmlReader(): XmlReader = XMLFragmentStreamReader.from(this)
}
