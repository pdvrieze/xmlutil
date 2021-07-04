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

package nl.adaptivity.xmlutil.util

import nl.adaptivity.xmlutil.XmlReader


/**
 * Simple baseclass for a delating XmlReader.
 * It merely functions as a delegate With Kotlin it's not really needed, but nice.
 */
@Deprecated("This has moved to the main package", ReplaceWith("XmlDelegatingReader", "nl.adaptivity.xmlutil"))
public open class XmlDelegatingReader protected constructor(delegate: XmlReader) :
    nl.adaptivity.xmlutil.XmlDelegatingReader(delegate)
