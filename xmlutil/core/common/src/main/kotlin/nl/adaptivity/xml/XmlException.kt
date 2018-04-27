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


expect open class IOException: Exception
/**
 * Simple exception for xml related things.
 * Created by pdvrieze on 15/11/15.
 */
expect open class XmlException: IOException {

  constructor()

  constructor(message: String)

  constructor(message: String, cause: Throwable)

  constructor(cause: Throwable)

  constructor(message: String, reader: XmlReader, cause: Throwable)
}