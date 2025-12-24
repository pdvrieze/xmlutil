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

package nl.adaptivity.xmlutil

/**
 * XmlReader that supports at least lightweight peeking. This could be full buffering, or
 * being able to "undo" the [next] function so that calling it again will give the same result.
 * Note that the implementation may not allow for inspecting the event in pushed back state.
 */
@XmlUtilInternal
public interface XmlPeekingReader: XmlReader {
    /**
     * Are we peeking (is the underlying buffer ahead). If `true` it may not be valid to
     * inspect the state.
     */
    public val hasPeekItems: Boolean

    /**
     * Put the current element in the peek buffer. This is basically a very limited pushback.
     * This may only be supported for a single element.
     */
    public fun pushBackCurrent()

    /**
     * Peek the next event only (this doesn't give access to the peeked information).
     * @return The [eventType] if it exists, or `null` if there is no next event.
     */
    public fun peekNextEvent(): EventType?

}
