/*
 * Copyright (c) 2021.
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

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.posix_errno
import platform.posix.strerror

public actual open class IOException : Exception {
    public actual constructor() : super()

    public actual constructor(message: String?) : super(message)

    public actual constructor(message: String?, cause: Throwable?) : super(message, cause)

    public actual constructor(cause: Throwable?) : super(cause)

    public companion object {
        @OptIn(ExperimentalForeignApi::class)
        public fun fromErrno(errno: Int = posix_errno()): IOException {
            val errMsg = strerror(errno)?.toKString()
            return IOException("Error ($errno): $errMsg")
        }
    }
}
