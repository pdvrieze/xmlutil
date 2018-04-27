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

package nl.adaptivity.xml

actual typealias IOException = java.io.IOException

/**
 * Simple exception for xml related things.
 * Created by pdvrieze on 15/11/15.
 */
actual open class XmlException : IOException
{

  actual constructor() { }

    actual constructor(message: String) : super(message)

    actual constructor(message: String, cause: Throwable) : super(message, cause)

    actual constructor(cause: Throwable) : super(cause)

    actual constructor(message: String, reader: XmlReader, cause: Throwable) : super("${reader.locationInfo ?: "Unknown position"} - $message", cause)
}