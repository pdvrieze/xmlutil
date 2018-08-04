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

expect interface XmlStreamingFactory

/** Flag to indicate that the xml declaration should be omitted, when possible.  */
const val FLAG_OMIT_XMLDECL = 1

/** Flag to indicate that the namespace usable should be automatically repaired. */
const val FLAG_REPAIR_NS = 2

/** The default used flags */
const val DEFAULT_FLAGS = FLAG_OMIT_XMLDECL