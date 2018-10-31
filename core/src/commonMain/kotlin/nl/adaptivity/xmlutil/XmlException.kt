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

import nl.adaptivity.xmlutil.multiplatform.IOException


/**
 * Simple exception for xml related things.
 * Created by pdvrieze on 15/11/15.
 */
open class XmlException : IOException {

    constructor()

    constructor(message: String) : super(message)

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(cause: Throwable) : super(cause)

    constructor(message: String, reader: XmlReader, cause: Throwable) :
        super("${reader.locationInfo ?: "Unknown position"} - $message", cause)

    @Deprecated("Only use in Java, in kotlin just throw", ReplaceWith("throw this"))
    fun doThrow(): Nothing { throw this }
}