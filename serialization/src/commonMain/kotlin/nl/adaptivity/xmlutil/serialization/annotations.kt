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
 * The default value is different from an empty value, in that it will result in a default behaviour.
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
@Deprecated("Use XmlNamespaceDeclSpecs instead that takes an array argument")
public annotation class XmlNamespaceDeclSpec(
    val value: String,
)

/**
 * Accessor that reads the declared namespaces out of an [XmlNamespaceDeclSpec] instance.
 */
@ExperimentalXmlUtilApi
@Deprecated("Use XmlNamespaceDeclSpecs instead of XmlNamespaceDeclSpec")
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
 * Annotation allowing to specify namespaces specifications to be generated upon the element.
 * As multiple annotations are not supported by the plugin this uses a single string. Each
 * declaration is of the form (prefix)=(namespace). To specify the default namespace it is valid to
 * omit the equals sign.
 *
 * @property value The actual specification: `"prefix1=urn:namespace1;defaultNamespace"`
 */
@ExperimentalXmlUtilApi
@SerialInfo
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
public annotation class XmlNamespaceDeclSpecs(
    vararg val value: String,
)

/**
 * Accessor that reads the declared namespaces out of an [XmlNamespaceDeclSpecs] instance.
 */
@ExperimentalXmlUtilApi
public val XmlNamespaceDeclSpecs.namespaces: List<Namespace>
    get() {
        return value.map { decl ->
            when (val eq = decl.indexOf('=')) {
                -1 -> XmlEvent.NamespaceImpl("", decl)
                else -> XmlEvent.NamespaceImpl(decl.substring(0, eq), decl.substring(eq + 1))
            }
        }
    }

/**
 * Indicate the valid polymorphic children for this element. This is a legacy annotation supporting
 * polymorphic (de)serialization without modules.
 *
 * @property value Each string specifies a child according to the following format:
 *     `childSerialName[=[prefix:]localName]`. The `childSerialName` is the name value of the
 *     descriptor. By default that would be the class name, but `@SerialName` will change that. If
 *     the name is prefixed with a `.` the package name of the container will be prefixed. Prefix is
 *     the namespace prefix to use (the namespace will be looked up based upon this). Localname
 *     allows to specify the local name of the tag.
 */@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class XmlPolyChildren(val value: Array<String>)

/**
 * Specify additional information about child values in collections. This is only used for
 * primitives, not for classes that have their own independent name. The outer tag name is
 * determined regularly.
 *
 * @property value The local part of the tag name to use.
 * @property namespace The namespace to use, by default the namespace of the container
 * @property prefix Suggested prefix to use, overridden by existing prefixes for the namespace. Will
 *              cause a namespace declaration/prefix to be added if needed.
 */
@SerialInfo
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
public annotation class XmlChildrenName(
    val value: String,
    val namespace: String = UNSET_ANNOTATION_VALUE,
    val prefix: String = UNSET_ANNOTATION_VALUE
)

/**
 * Specify the xml name used for the key attribute/tag of a map.
 *
 * @property value The local part of the key name
 * @property namespace The namespace of the key. By default inherited from the parent
 * @property prefix The suggested prefix to use if one is not already available.
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class XmlKeyName(
    val value: String,
    val namespace: String = UNSET_ANNOTATION_VALUE,
    val prefix: String = UNSET_ANNOTATION_VALUE
)

/**
 * Dual use annotation that both forces explicit map entry wrappers and specifies the tag name used.
 * The default is to elude the wrappers in the case that data does not contain an attribute with
 * the name of the key.
 *
 * @property value The localname of the entry
 * @property namespace The namespace of the entry. By default inherited from the parent
 * @property prefix The suggested prefix to use if one is not already available.
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class XmlMapEntryName(
    val value: String,
    val namespace: String = UNSET_ANNOTATION_VALUE,
    val prefix: String = UNSET_ANNOTATION_VALUE
)

/**
 * Force a property that could be an attribute to be an element. Note that default behaviour
 * requires this annotation to be absent.
 *
 * @property value `true` to indicate serialization as tag, `false` to indicate serialization as
 *    attribute. Note that not all values can be serialized as attribute.
 */
@SerialInfo
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
public annotation class XmlElement(val value: Boolean = true)

/**
 * Force a property to be content of the tag (for the object containing the property). This is both
 * for text content (polymorphic including a primitive), but if the type is a list of tag-like types
 * (`Node`, `Element`, `CompactFragment`) it will also allow mixed content of tags not supported by
 * the base type. Strings will be serialized/deserialized as (tag soup) string content without
 * wrapper.
 *
 * @property value `true` to indicate the value. `false` and absence are equivalent.
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class XmlValue(val value: Boolean = true)

/**
 * Annotation to mark the value as an ID attribute. This implies that the element is an attribute.
 * This will allow the serializer to enforce uniqueness.
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
 * This annotation allows handling wildcard attributes. It should be specified on a
 * `Map<QName, String>` to store unsupported attributes. The key is preferred to be a QName,
 * alternatively it must convert to String (this could be "prefix:localName"). The value must be
 * a String type. **Note** that if the key runtime type is a `QName` the value is directly used as
 * attribute name without using the key serializer.
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class XmlOtherAttributes

/**
 * Mark the property for serialization as CData, rather than text (where appropriate).
 * If used on a property this will override the annotation on a type. This is the only context in
 * which a value of `false` is different from omitting the annotation.
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class XmlCData(
    val value: Boolean = true
)

/**
 * Allow a property to be omitted with a default serialized string. This annotation primarily
 * supports older versions of the framework that do not support default attribute values. The
 * default value will not be written out if matched.
 *
 * @property value The default value used if no value is specified. The value is parsed as if there
 * was textual substitution of this value into the serialized XML.
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class XmlDefault(val value: String)

/**
 * Require this property to be serialized before other (sibling) properties. Together [XmlBefore]
 * and [XmlAfter] define a partial order over the properties. Using this annotation may cause values
 * to be serialized as elements rather than attributes where not explicitly specified as attributes.
 * If there is a conflict between serialization type (attribute/element) this takes precendence over
 * this attribute. The names are the serialNames of the properties being serialized (not XML names).
 *
 * @property value The serial names of all the children that should be serialized after this one (uses the
 * [kotlinx.serialization.SerialName] value or field name).
 * @see XmlAfter
 */
@SerialInfo
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY)
public annotation class XmlBefore(vararg val value: String)

/**
 * Require this property to be serialized after other (sibling) properties. Together [XmlBefore]
 * and [XmlAfter] define a partial order over the properties. Using this annotation may cause values
 * to be serialized as elements rather than attributes where not explicitly specified as attributes.
 * If there is a conflict between serialization type (attribute/element) this takes precendence over
 * this attribute. The names are the serialNames of the properties being serialized (not XML names).
 *
 * @property value The serial names of all the children that should be serialized before this one (uses the
 * [kotlinx.serialization.SerialName] value or field name).
 */
@SerialInfo
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY)
public annotation class XmlAfter(vararg val value: String)

/**
 * Default value for unset annotations. This is invalid/unlikely value
 */
internal const val UNSET_ANNOTATION_VALUE = "ZXC\u0001VBNBVCXZ"
