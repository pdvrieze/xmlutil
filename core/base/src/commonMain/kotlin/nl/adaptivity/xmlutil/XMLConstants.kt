/*
 * Copyright (c) 2024.
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

package nl.adaptivity.xmlutil

/**
 * Various namespace constants and their corresponding default prefixes.
 */
public object XMLConstants {
    public const val DEFAULT_NS_PREFIX: String = ""
    public const val NULL_NS_URI: String = ""

    public const val XMLNS_ATTRIBUTE: String = "xmlns"
    public const val XMLNS_ATTRIBUTE_NS_URI: String = "http://www.w3.org/2000/xmlns/"

    public const val XML_NS_PREFIX: String = "xml"
    public const val XML_NS_URI: String = "http://www.w3.org/XML/1998/namespace"

    public const val XSI_PREFIX: String = "xsi"
    public const val XSI_NS_URI: String = "http://www.w3.org/2001/XMLSchema-instance"

    public const val XSD_PREFIX: String = "xsd"
    public const val XSD_NS_URI: String = "http://www.w3.org/2001/XMLSchema"

    public const val XLINK_PREFIX: String = "xlink"
    public const val XLINK_NAMESPACE: String = "http://www.w3.org/1999/xlink"

    public const val XPATH_FUNCTIONS_PREFIX: String = "fn"
    public const val XPATH_FUNCTIONS_NAMESPACE: String = "http://www.w3.org/2005/xpath-functions"

    public const val XHTML_PREFIX: String = "xhtml"
    public const val XHTML_NAMESPACE: String = "http://www.w3.org/1999/xhtml"

}
