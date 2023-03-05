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

import kotlin.io.use as ktUse

public actual fun assert(value: Boolean, lazyMessage: () -> String) {
    kotlin.assert(value, lazyMessage)
}

public actual fun assert(value: Boolean): Unit = kotlin.assert(value)

public actual typealias AutoCloseable = java.lang.AutoCloseable

public actual inline fun <T : Closeable?, R> T.use(block: (T) -> R): R {
    return this.ktUse(block)
}

public actual typealias Closeable = java.io.Closeable

public actual typealias Writer = java.io.Writer
public actual typealias StringWriter = java.io.StringWriter

public actual typealias Reader = java.io.Reader
public actual typealias StringReader = java.io.StringReader

public actual typealias InputStream = java.io.InputStream
public actual typealias OutputStream = java.io.OutputStream

public actual typealias Language = org.intellij.lang.annotations.Language
