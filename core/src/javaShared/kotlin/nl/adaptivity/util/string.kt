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

package nl.adaptivity.util

@Deprecated("Don't use, just use string comparison")
fun CharSequence?.contentEquals(other:CharSequence?):Boolean {
  if (this==null) return other==null
  if (other==null) return false
  if (length!=other.length) return false
  for (i in 0 until length) {
    if (this[i]!=other[i]) return false
  }
  return true
}

@Deprecated("Use string equals", ReplaceWith("this == other"))
fun String?.contentEquals(other: String?): Boolean {
    return this == other
}