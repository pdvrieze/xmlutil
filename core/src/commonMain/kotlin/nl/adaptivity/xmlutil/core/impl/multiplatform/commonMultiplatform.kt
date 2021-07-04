/*
 * Copyright (c) 2018.
 *
 * This file is part of XmlUtil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package nl.adaptivity.xmlutil.core.impl.multiplatform

import nl.adaptivity.xmlutil.XmlUtilInternal
import kotlin.reflect.KClass

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CONSTRUCTOR
       )
public expect annotation class Throws(vararg val exceptionClasses: KClass<out Throwable>)

@XmlUtilInternal
public expect val KClass<*>.name: String

@XmlUtilInternal
public expect fun assert(value: Boolean, lazyMessage: () -> String)

@XmlUtilInternal
public expect fun assert(value: Boolean)

public expect interface AutoCloseable {
    public fun close()
}

public expect interface Closeable : AutoCloseable

@XmlUtilInternal
public expect val KClass<*>.maybeAnnotations: List<Annotation>


public expect abstract class Writer: Appendable
public expect open class StringWriter(): Writer
