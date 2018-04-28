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

package nl.adaptivity.util.multiplatform

actual typealias JvmStatic = kotlin.jvm.JvmStatic

actual typealias JvmField = kotlin.jvm.JvmField

actual typealias JvmName = kotlin.jvm.JvmName

actual typealias JvmMultifileClass = kotlin.jvm.JvmMultifileClass

actual typealias URI = java.net.URI

actual inline fun createUri(s: String): URI = URI.create(s)