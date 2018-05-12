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

actual class Locale

actual object Locales {
    actual val DEFAULT: Locale = Locale()
    actual val ENGLISH: Locale = Locale()

}

@Suppress("NOTHING_TO_INLINE")
actual inline fun CharSequence.toLowercase(locale: Locale):String =
    toString().toLowerCase()


@Suppress("NOTHING_TO_INLINE", "UnsafeCastFromDynamic")
actual inline fun Int.toHex(): String = asDynamic().toString(16)

@Suppress("NOTHING_TO_INLINE")
actual inline fun String.toCharArray(): CharArray = (this as CharSequence).toCharArray()
