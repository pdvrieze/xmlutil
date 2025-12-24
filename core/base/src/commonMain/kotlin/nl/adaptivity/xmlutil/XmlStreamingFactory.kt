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
 * A factory that can be used to customize [xmlStreaming] to use these custom factory functions
 * when not using the explicit generic implementations.
 *
 * This is only really supported for the JVM/Android platforms.
 *
 * @see IXmlStreaming.setFactory
 */
// note that this type is deprecated on multiplatform. It only makes sense for the JVM target
public expect interface XmlStreamingFactory

/** Flag for [XmlSerializable] to indicate that the xml declaration should be omitted, when possible.  */
@Deprecated("Should no longer exist here")
public const val FLAG_OMIT_XMLDECL: Int = 1

/** Flag for [XmlSerializable] to indicate that the namespace usable should be automatically repaired. */
@Deprecated("Should no longer exist here")
public const val FLAG_REPAIR_NS: Int = 2

/** The default used flags for [XmlSerializable] */
@Deprecated("Should no longer exist here")
public const val DEFAULT_FLAGS: Int = FLAG_OMIT_XMLDECL
