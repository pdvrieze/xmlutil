/*
 * Copyright (c) 2016.
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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xml

/**
 * Created by pdvrieze on 15/11/15.
 */
class XmlException : Exception {

  constructor() {
  }

  constructor(message: String) : super(message) {
  }

  constructor(message: String, cause: Throwable) : super(message, cause) {
  }

  constructor(cause: Throwable) : super(cause) {
  }

  constructor(message: String, `in`: XmlReader, cause: Throwable) : super(message, cause) {
  }// TODO do something witht the reader state
}
