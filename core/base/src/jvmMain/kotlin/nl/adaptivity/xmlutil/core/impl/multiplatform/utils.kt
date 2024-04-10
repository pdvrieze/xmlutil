/*
 * Copyright (c) 2024.
 *
 * This file is part of xmlutil.
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

@file:Suppress(
    "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING",
    "ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER_WARNING",
    "ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_SUPERTYPES_AS_NON_FINAL_EXPECT_CLASSIFIER_WARNING"
)

package nl.adaptivity.xmlutil.core.impl.multiplatform

import kotlin.reflect.KClass
import java.io.StringWriter as JavaStringWriter

public actual typealias AutoCloseable = java.lang.AutoCloseable

public actual typealias Closeable = java.io.Closeable

public actual abstract class Writer : Appendable {
    public actual open fun write(text: String) {
        append(text)
    }

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE", "KotlinRedundantDiagnosticSuppress")
    actual abstract override fun append(value: Char): Appendable

    actual abstract override fun append(value: CharSequence?, startIndex: Int, endIndex: Int): Appendable

    actual override fun append(value: CharSequence?): Appendable {
        return append(value, 0, value?.length ?: 0)
    }

    public actual open fun flush() {
    }
}

public actual open class StringWriter actual constructor() : Writer() {
    private val delegate = JavaStringWriter()

    override fun append(value: Char): Appendable {
        delegate.append(value)
        return this
    }

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE", "KotlinRedundantDiagnosticSuppress")
    override fun append(value: CharSequence?, startIndex: Int, endIndex: Int): Appendable {
        delegate.append(value, startIndex, endIndex)
        return this
    }

    actual override fun toString(): String {
        return delegate.toString()
    }
}

public actual typealias Reader = java.io.Reader
public actual typealias StringReader = java.io.StringReader

public actual typealias InputStream = java.io.InputStream
public actual typealias OutputStream = java.io.OutputStream

internal expect val <T : Any> KClass<T>.javaCompat: Class<T>

internal expect val <T : Any> T.javaClassCompat: Class<T>



internal actual val <T : Any> KClass<T>.javaCompat: Class<T> get() = this.java
internal actual val <T : Any> T.javaClassCompat: Class<T>
    get() = javaClass
