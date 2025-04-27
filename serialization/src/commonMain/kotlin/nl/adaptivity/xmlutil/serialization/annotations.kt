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
@file:OptIn(ExperimentalSerializationApi::class)

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.XmlEvent

/**
 * Specify more detailed name information than can be provided by [kotlinx.serialization.SerialName].
 *
 * @property value The local part of the name.
 * @property namespace The namespace to use.
 * @property prefix the Prefix to use.
 */
@SerialInfo
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
public annotation class XmlSerialName(
    val value: String = UNSET_ANNOTATION_VALUE,
    val namespace: String = UNSET_ANNOTATION_VALUE,
    val prefix: String = UNSET_ANNOTATION_VALUE
)

/**
 * Annotation allowing to specify namespaces specifications to be generated upon the element.
 * As multiple annotations are not supported by the plugin this uses a single string. The string
 * separates the namespaces using a semicolon (`;`). Each declaration is of the form
 * (prefix)=(namespace). To specify the default namespace it is valid to omit the equals sign.
 *
 * @property value The actual specification: `"prefix1=urn:namespace1;defaultNamespace"`.
 */
@ExperimentalXmlUtilApi
@SerialInfo
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
public annotation class XmlNamespaceDeclSpec(
    val value: String,
)

@ExperimentalXmlUtilApi
public val XmlNamespaceDeclSpec.namespaces: List<Namespace>
    get() {
        return value.split(';').map { decl ->
            when (val eq = decl.indexOf('=')) {
                -1 -> XmlEvent.NamespaceImpl("", decl)
                else -> XmlEvent.NamespaceImpl(decl.substring(0, eq), decl.substring(eq + 1))
            }
        }
    }

/**
 * Indicate the valid poly children for this element.
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class XmlPolyChildren(val value: Array<String>)

/**
 * Used in lists. This causes the children to be serialized as separate tags in an outer tag. The
 * outer tag name is determined regularly.
 *
 * @property value The local part of the name.
 * @property namespace The namespace to use.
 * @property prefix the Prefix to use.
 */
@SerialInfo
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
public annotation class XmlChildrenName(
    val value: String,
    val namespace: String = UNSET_ANNOTATION_VALUE,
    val prefix: String = UNSET_ANNOTATION_VALUE
)

/**
 * Used to specify the key of map entries.
 *
 * @property value The local part of the name.
 * @property namespace The namespace to use.
 * @property prefix the Prefix to use.
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class XmlKeyName(
    val value: String,
    val namespace: String = UNSET_ANNOTATION_VALUE,
    val prefix: String = UNSET_ANNOTATION_VALUE
)

/**
 * Force a property that could be an attribute to be an element.
 *
 * @property value `true` to indicate serialization as tag, `false` to indicate serialization as
 * attribute. Note that not all values can be serialized as attribute.
 */
@SerialInfo
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
public annotation class XmlElement(val value: Boolean = true)

/**
 * Force a property to be content of a tag. This is both for text content, but if the type is
 * a list of tag-like types (`Node`, `Element`, `CompactFragment`) it will also allow mixed content
 * of tags not supported by the base type.
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class XmlValue(val value: Boolean = true)

/**
 * Annotation to mark the value as an ID attribute. This implies that the element is an attribute.
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class XmlId

/**
 * Determine whether whitespace should be ignored or preserved for the tag.
 *
 * @property value `true` if whitespace is to be ignored, `false` if preserved.
 */
@SerialInfo
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
public annotation class XmlIgnoreWhitespace(val value: Boolean = true)

/**
 * This annotation allows handling wildcard attributes. The key is preferred to be a QName,
 * alternatively it must convert to String (this could be "prefix:localName"). The value must be
 * a String type. **Note** that if the key runtime type is a `QName` the value is directly used as
 * attribute name without using the key serializer.
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class XmlOtherAttributes

/**
 * Mark the property for serialization as CData, rather than text (where appropriate). It also means
 * that the value is not written as attribute.
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class XmlCData(val value: Boolean = true)

/**
 * Older versions of the framework do not support default values. This annotation allows a default
 * value to be specified. The default value will not be written out if matched.
 *
 * @property value The default value used if no value is specified. The value is parsed as if there
 * was textual substitution of this value into the serialized XML.
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class XmlDefault(val value: String)

/**
 * Require this property to be serialized before other (sibling) properties. The names are the
 * serialNames of the properties being serialized (not XML names). Together [XmlBefore] and
 * [XmlAfter] define a partial order over the properties.
 *
 * @property value All the children that should be serialized after this one (uses the
 * [kotlinx.serialization.SerialName] value or field name).
 */
@SerialInfo
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY)
public annotation class XmlBefore(vararg val value: String)

/**
 * Require this property to be serialized after other (sibling) properties. The names are the
 * serialNames of the properties being serialized (not XML names).
 *
 * @property value All the children that should be serialized before this one (uses the
 * [kotlinx.serialization.SerialName] value or field name).
 */
@SerialInfo
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY)
public annotation class XmlAfter(vararg val value: String)

/**
 * Default value for unset annotations
 */
internal const val UNSET_ANNOTATION_VALUE = "ZXC\u0001VBNBVCXZ"
