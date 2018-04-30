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

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.SOURCE)
actual annotation class JvmStatic

@Retention(AnnotationRetention.SOURCE)
actual annotation class JvmField actual constructor()

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER,
        AnnotationTarget.FILE)
actual annotation class JvmName actual constructor(actual val name: String)

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
actual public annotation class JvmOverloads()

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
actual annotation class JvmWildcard()


@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FILE)
actual annotation class JvmMultifileClass()


open external class URL(url:String, base:String= definedExternally) {
    val pathName: String
}

actual class URI(s: String):URL(s) {
    actual inline fun getPath(): String = pathName
}

actual inline fun createUri(s: String): URI = URI(s)