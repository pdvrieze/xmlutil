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

package org.w3.xml.xmschematestsuite

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class TSBugUriT(val uri: String) {
    init {
        if (! PATTERN.matches(uri)) {
            throw IllegalArgumentException("Uri provided is not a valid bug uri: $uri")
        }
    }

    companion object {
        private val PATTERN = Regex("http://www\\.w3\\.org/Bugs/Public/show_bug\\.cgi\\?id=[0-9]*")
    }
}
