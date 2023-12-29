/*
 * Copyright (c) 2023.
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

package nl.adaptivity.xmlutil.core.impl.multiplatform

import kotlin.io.use as ktUse

public actual fun assert(value: Boolean, lazyMessage: () -> String) {
    kotlin.assert(value, lazyMessage)
}

public actual fun assert(value: Boolean): Unit = kotlin.assert(value)

public actual inline fun <T : Closeable?, R> T.use(block: (T) -> R): R {
    return this.ktUse(block)
}

// The error for the type here is a problem with the compiler (the defaults are equal, and needed)
public actual typealias Language = org.intellij.lang.annotations.Language

public actual typealias MpJvmDefaultWithoutCompatibility = JvmDefaultWithoutCompatibility

public actual typealias MpJvmDefaultWithCompatibility = JvmDefaultWithCompatibility

public actual typealias Throws = kotlin.jvm.Throws
