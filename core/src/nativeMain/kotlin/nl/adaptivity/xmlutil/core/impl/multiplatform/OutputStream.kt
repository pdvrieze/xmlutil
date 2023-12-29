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

@Suppress(
    "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING",
)
public actual abstract class OutputStream : Closeable {

    public actual abstract fun write(b: Int)

    public actual open fun write(b: ByteArray) {
        write(b, 0, b.size)
    }

    public actual open fun write(b: ByteArray, off: Int, len: Int) {
        val endIdx = off + len
        require(off in 0 until b.size) { "Offset before start of array" }
        require(endIdx <= b.size) { "Range size beyond buffer size" }
        for (i in off until endIdx) {
            write(b[i].toInt())
        }
    }

    actual override fun close() {}
}
