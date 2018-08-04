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

@Suppress("UNCHECKED_CAST")
/**
 * Interface that factories need to implement to handle be deserialization in a "shared"
 * non-reflective approach.

 * Created by pdvrieze on 27/08/15.
 */
interface XmlDeserializerFactory<T> {

    /** Deserialize the object */
    fun deserialize(reader: XmlReader): T
}
