/*
 * Copyright (c) 2017.
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

package nl.adaptivity.js.util

import org.w3c.dom.*
import kotlin.dom.isElement
import kotlin.dom.isText

fun Node.asElement(): Element? = if (isElement) this as Element else null
fun Node.asText(): Text? = if (isText) this as Text else null

fun Node.removeElementChildren()
{
  val top = this
  var cur = top.firstChild
  while (cur!=null) {
    val n = cur.nextSibling
    if (cur.isElement) {
      top.removeChild(cur)
    }
    cur = n
  }
}
