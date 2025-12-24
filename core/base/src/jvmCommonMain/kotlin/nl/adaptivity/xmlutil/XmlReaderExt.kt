/*
 * Copyright (c) 2024-2025.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You should have  received a copy of the license
 * with the source distribution. Alternatively, you may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

@file:JvmName("XmlReaderUtil")
@file:JvmMultifileClass
@file:MustUseReturnValues

package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.core.impl.multiplatform.use
import java.io.CharArrayWriter

/**
 * Write all the remaining content of the reader to a [CharArrayWriter]. Note that this likely
 * fails if the reader is not at the start (as the writer would fail due to an invalid XML
 * document).
 */
public fun XmlReader.toCharArrayWriter(): CharArrayWriter {
    return CharArrayWriter().also {
        @Suppress("DEPRECATION")
        XmlStreaming.newWriter(it as Appendable).use { out ->
            while (hasNext()) {
                next().writeEvent(out, this)
            }
        }
    }
}
