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

package nl.adaptivity.xmlutil.core.kxio

import nl.adaptivity.xmlutil.XmlDelegatingWriter
import nl.adaptivity.xmlutil.XmlWriter

/** Get a new writer that has a [handler] added to the close event. */
internal fun XmlWriter.onClose(handler: () -> Unit): XmlWriter {
    return CloseHandlingWriter(this, handler)
}

/**
 * Writer that will call the [closeHandler] when the [close] function is completed.
 */
internal class CloseHandlingWriter(delegate: XmlWriter, private val closeHandler: ()->Unit): XmlDelegatingWriter(delegate) {
    override fun close() {
        try {
            super.close()
        } finally {
            closeHandler()
        }
    }
}
