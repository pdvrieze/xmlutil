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

import nl.adaptivity.xml.IterableNamespaceContext
import nl.adaptivity.xml.XmlReader
import nl.adaptivity.xml.XmlWriter

fun CompactFragment(s: String) = object : CompactFragment {
    override val namespaces: IterableNamespaceContext
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val content: CharArray
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val contentString: String
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun getXmlReader(): XmlReader {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun serialize(out: XmlWriter) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}