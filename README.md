# XmlUtil
![Validate Gradle Wrapper](https://github.com/pdvrieze/xmlutil/workflows/Validate%20Gradle%20Wrapper/badge.svg)
[![GitHub license](https://img.shields.io/badge/License-Apache%202-blue.svg?style=flat)](COPYING)
- Core:&nbsp;[![Download](https://img.shields.io/maven-central/v/io.github.pdvrieze.xmlutil/core)](https://search.maven.org/artifact/io.github.pdvrieze.xmlutil/core)
- Serialization:&nbsp;[![Download](https://img.shields.io/maven-central/v/io.github.pdvrieze.xmlutil/serialization)](https://search.maven.org/artifact/io.github.pdvrieze.xmlutil/serialization)
- SerialUtil:&nbsp;[![Download](https://img.shields.io/maven-central/v/io.github.pdvrieze.xmlutil/serialutil)](https://search.maven.org/artifact/io.github.pdvrieze.xmlutil/serialutil)

XmlUtil is a set of packages that supports multiplatform XML in Kotlin.

### Introduction
This project is a cross-platform XML serialization (wrapping) library compatible with kotlinx.serialization. 
It supports all platforms although native is at beta quality.

Based upon the core xml library, the serialization module supports automatic object
serialization based upon Kotlin's standard serialization library and plugin.

**Help wanted**: Any help with extending this project is welcome. Help is especially needed for the following aspects:

* Documentation updates
* Testing, in particular more extensive tests. Some tests already exist for both JVM and Android
* Native xml library support: Native is only supported through the cross-platform implementation
  that is somewhat limited in advanced features such as DTD and validation support. 
  Ideally integration with a well-developed native library as an option would be beneficial.

### Versioning scheme
This library is based upon the [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) library. 
Every effort is made to limit incompatible changes.

## How to use
The library is designed as a multiplatform Kotlin module, but platform-specific versions can also be used were appropriate.
### Add repository
The project's Maven access is hosted on OSS Sonatype (and available from Maven Central).

Releases can be added from **maven central**

Snapshots are available from the default maven central snapshot repository:
```groovy
repositories {
	maven {
		url  "https://central.sonatype.com/repository/maven-snapshots/"
	}
}
```

### Core
It should be noted that the JVM and Android packages are no longer part
of the multiplatform publication (they are combined into a `jvmCommon`)
package. The `JVM` and `Android` packages provide the native
implementations and depend on (publishing) the `jvmCommon` package.
#### multiplatform (will default to multiplatform implementation for JVM/Android)
```
   implementation("io.github.pdvrieze.xmlutil:core:1.0.0-rc1")
```
#### **Optional** JVM – uses the stax API _not available_ on Android
```
   implementation("io.github.pdvrieze.xmlutil:core-jdk:1.0.0-rc1")
```
#### **Optional** Android – Uses the android streaming library
```
   implementation("io.github.pdvrieze.xmlutil:core-android:1.0.0-rc1")
```
#### JS – Wraps DOM
```
   implementation("io.github.pdvrieze.xmlutil:core-js:1.0.0-rc1")
```

#### Native
Has platform independent implementations of xml parsing/serialization
(based upon the Android implementation) and DOM (a simple implementation
that mirrors the Java API)

### Serialization
#### multiplatform (this coordinate should be used by default)
```
   implementation("io.github.pdvrieze.xmlutil:serialization:1.0.0-rc1")
```

## Serialization help
### Hello world
To serialize a very simple type you have the following:
```kotlin
@Serializable
data class HelloWorld(val user: String)

println(XML1_0.encodeToString(HelloWorld("You!")))
```

To deserialize you would do:
```kotlin
@Serializable
data class HelloWorld(val user: String)

XML1_0.decodeFromString(HelloWorld.serializer(), "<HelloWorld user='You!' />")
```

Please look at the examples and the documentation for further features
that can influence: the tag names/namespaces used, the actual structure
used (how lists and polymorphic types are handled), etc.

### Examples
You should be able to find examples in the [Examples module](examples/README.md)
### Format
The entrypoint to the library is the `XML1_0` (for 1.0) format (or `XML` generally). There is a default, but often a child is better. 
Custom formats are created through:
```kotlin
val format = XML1_0.recommended(mySerialModule) {  
    // configuration options
    xmlDeclMode = XmlDeclMode.None
    policy {
        typeDiscriminatorName = QName(XMLConstants.XSI_NS_URI, "type", XMLConstants.XSI_PREFIX)
    }
}
```

The configuration for the XML format is separated between the configuration, and
embedded inside it a policy. The configuration mainly covers the xml serialization,
but not how serializable types are reflected in XML. The policy is designed to
allow for programmatic determination of how serialization occurs. 

The following options are available when using the XML format builder. For new
code you should use the `XML1_0` entry point. This entry point is equivalent to
the recommended configuration for version 0.92.0.

There are four functions to create a new XML format. They provide different
presets for the configuration (that can then be adjusted through the configuration
function).

| Entry point         | Description                                                                                                                                                                                         |
|---------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `recommended {}`    | The recommended configuration for version 1.0. This version defaults to indented XML1.1 with unwrapped polymorphism.                                                                                |
| `compact {}`        | Changes serialization to be more compact, omitting XML declarations and indentation.                                                                                                                |
| `fast {}`           | Changes the format to focus on performance of serialization and **deserialization**. This omits error checking and has more aggressive caching.                                                     |
| `customPolicy() {}` | Preset that sets the configuration as recommended, but allows for a customized policy to be provided. Unlike the other presets this policy does not need to extend `DefaultXmlSerializationPolicy`. |

Each presets provides access to the configuration builder with the
following options (and access to both the `policy` property for setting
the policy as well as the `policy` function for further configuring the
policy):

| Option                     | Description                                                                                                                             |
|----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------| 
| `repairNamespaces`         | Should namespaces automatically be repaired. This option will be passed on to the `XmlWriter`                                           |
| `xmlDeclMode`              | The mode to use for emitting XML declarations (<?xml ...?>). Replaces omitXmlDecl for more finegrained control                          |
| `indentString`             | The indentation to use. Must be a combination of XML whitespace or comments (this is checked). This is passed to the `XmlWriter`        |
| (val) `indent`             | Read only access to the indentation level of the `indentString` (a tab is counted as 8 spaces)                                          |
| `setIndent()`              | The indentation level (in spaces) to use. This sets the `indentString` value.                                                           |
| `nilAttribute`             | A pair of QName and string for the attribute to use to denote a nil value. If `null` elements will be omitted                           |
| `useXsiNil()`              | Shortcut to set `nilAttribute` appropriately to serialize using the XMLSchema Instance nil attribute                                    |
| `isAlwaysDecodeXsiNil`     | When set, decoding will always handle `xsi:nil` values, independent of the value of `nilAttribute`.                                     |                                                                                                                                                                                                    
| `xmlVersion`               | Which xml version will be written/declared (default XML 1.1).                                                                           |
| `isCollectingNSAttributes` | (Attempt to) collect all needed namespace declarations and emit them on the root tag, this does have a performance overhead             |
| `defaultToGenericParser`   | Use the generic (platform independent) parser, rather than the platform specific one.                                                   |
| `isUnchecked`              | When set, various correctness checks are skipped to speed up processing (but less robust handling of incorrect inputs or serializers)   |
| `policy`                   | Property that gives access to the underlying policy for configuration.                                                                  |
| `policy {}`                | Builder that allows configuring the default policy (all presets except `customPolicy`). This builder does not exist for custom policies |

The defaults are:

| Option                     | Recommended/custom    | Compact                | Fast                  |
|----------------------------|-----------------------|------------------------|-----------------------|
| `repairNamespaces`         | `false`               | `false`                | `false`               |
| `xmlDeclMode`              | `XmlDeclMode.Minimal` | **`XmlDeclMode.None`** | `XmlDeclMode.Minimal` |
| `indentString`             | 4 spaces              | **`""`**               | `""`                  |
| `nilAttribute`             | `null`                | `null`                 | `null`                | 
| `isAlwaysDecodeXsiNil`     | `true`                | `true`                 | `false`               |
| `xmlVersion`               | `XmlVersion.XML11`    | `XmlVersion.XML11`     | `XmlVersion.XML11`    |
| `isCollectingNSAttributes` | `false`               | `false`                | `false`               |
| `defaultToGenericParser`   | `false`               | `false`                | `true`                |
| `isUnchecked`              | `false`               | `false`                | `true`                |


The properties that have been moved into the policy can still be set in the builder,
but are no longer able to be read through the config object.

The following options are available as part of the default policy builder. Note that the policy
is designed to allow configuration through code, but the default policy has significant
configuration options available.

| Option                       | Description                                                                                                                                                                                                                                                                                                             |
|------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `pedantic`                   | Fail on output type specifications that are incompatible with the data, rather than silently correcting this                                                                                                                                                                                                            |
| `autoPolymorphic`            | When not specifying a custom policy this determines whether polymorphism is handled without wrappers. This replaces `XmlPolyChildren`, but changes serialization where that annotation is not applied. This option will become the default in the future although XmlPolyChildren will retain precedence (when present) |
| `encodeDefault`              | Determine whether in which cases default values should be encoded.                                                                                                                                                                                                                                                      |
| `unknownChildHandler`        | A function that is called when an unknown child is found. By default an exception is thrown but the function can silently ignore it as well.                                                                                                                                                                            |
| - `ignoreUnknownChildren()`  | Set the unknown child handler to ignore/skip unknown children                                                                                                                                                                                                                                                           |
| - `ignoreNamespaces()`       | Set the unknown child handler to handle namespace mismatches when the seen namespace is unknown                                                                                                                                                                                                                         |
| `typeDiscriminatorName`      | This property determines the type discriminator attribute used. It is always recognised, but not serialized in transparent polymorphic (autoPolymorphic) mode. If this is null, a wrapper tag with type attribute is used instead of a discriminator.                                                                   |
| `throwOnRepeatedElement`     | Rather than silently allowing a repeated element (not part of a list), throw an exception if the element occurs multiple times.                                                                                                                                                                                         |
| `verifyElementOrder`         | While element order (when specified using `@XmlBefore` and `@XmlAfter`) is always used for serialization, this flag allows checking this order on inputs.                                                                                                                                                               |
| `isStrictAttributeNames`     | Enables stricter, standard compliant attribute name mapping in respect to default/null namespaces. Mainly relevant to decoding.                                                                                                                                                                                         |
| `isStrictBoolean`            | Parse boolean variables according to the XML Schema standard.                                                                                                                                                                                                                                                           |
| `isXmlFloat`                 | Encode xml float/double values according to the XML Schema standard, not the JVM one (infinity is encoded differently)                                                                                                                                                                                                  | 
| `isInlineCollapsedDefault`   | If `true`(default) the policy will default to collapsing an  inline type (omitting the wrapper, while potentially using the inline types annotation for tag names).                                                                                                                                                     |
| `defaultPrimitiveOutputKind` | The default output kind to use for primitives (when not otherwise specified)                                                                                                                                                                                                                                            |
| `defaultObjectOutputKind`    | The default output kind for Kotlin (singleton) objects                                                                                                                                                                                                                                                                  |
| `formatCache`                | The cache used for storing type information for serialization                                                                                                                                                                                                                                                           | 

The defaults for the options are:

| Option                       | Recommended                                                       | Fast                                                              |
|------------------------------|-------------------------------------------------------------------|-------------------------------------------------------------------|
| `pedantic`                   | `false`                                                           | `false`                                                           |  
| `autoPolymorphic`            | `true`                                                            | `true`                                                            |  
| `encodeDefault`              | `XmlEncodeDefault.ANNOTATED`                                      | `XmlEncodeDefault.ANNOTATED`                                      |  
| `unknownChildHandler`        | `XmlConfig.DEFAULT_UNKNOWN_CHILD_HANDLER`                         | `XmlConfig.DEFAULT_UNKNOWN_CHILD_HANDLER`                         |
| `typeDiscriminatorName`      | `QName(XMLConstants.XSI_NS_URI, "type", XMLConstants.XSI_PREFIX)` | `QName(XMLConstants.XSI_NS_URI, "type", XMLConstants.XSI_PREFIX)` |  
| `throwOnRepeatedElement`     | `true`                                                            | `true`                                                            |  
| `verifyElementOrder`         | `false`                                                           | `false`                                                           |  
| `isInlineCollapsedDefault`   | `true`                                                            | `true`                                                            |  
| `isStrictAttributeNames`     | `true`                                                            | `true`                                                            |  
| `isStrictBoolean`            | `true`                                                            | `true`                                                            |  
| `isXmlFloat`                 | `true`                                                            | `true`                                                            |    
| `defaultPrimitiveOutputKind` | `OutputKind.Attribute`                                            | `OutputKind.Attribute`                                            |  
| `defaultObjectOutputKind`    | `OutputKind.Element`                                              | `OutputKind.Element`                                              |  
| `formatCache`                | `defaultSharedFormatCache()`                                      | `defaultSharedFormatCache()`                                      |    

The overridable functions of policies are:

| Function name                        | Description                                                                                                                         |
|--------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| `defaultOutputKind(serialKind)`      | Determine the default output kind for the given object                                                                              |
| `invalidOutputKind(message)`         | Function called on an invalid output kind (throws if pedantic)                                                                      |
| `ignoredSerialInfo(message)`         | Function called on ignored serial info (throws if pedantic)                                                                         |
| `effectiveName(..)`                  | Determine the QName for the given element                                                                                           |
| `isInlineCollapsed(..)`              | Determine whether an inline class should be collapsed (the wrapper eluded)                                                          |
| `isListEluded(..)`                   | Determine whether a list should be serialized directly as list items in the parent, without a wrapper                               |
| `isTransparentPolymorphic(..)`       | Determine whether a polymorphic element should be serialized transparently (the tag name determines the type)                       |
| `polymorphicDiscriminatorName(..)`   | The name to use for polymorphic discrimination (by default `typeDiscriminatorName`)                                                 |
| `serialTypeNameToQName(..)`          | Determine the QName for a given serial **type** name                                                                                |
| `serialUseNameToQName(..)`           | Determine the QName for a given **property** (use side) name                                                                        |
| `effectiveOutputKind(..)`            | Determine how the element should be written (tag, attribute, value...)                                                              |
| `overrideSerializerOrNull(..)`       | Function allowing to override the serializer used in a context (rather than the default one)                                        |
| `handleUnknownContentRecovering(..)` | Function that allows handling unknown content by either throwing an exception (default) or returning a list of "corrections"        |
| `onElementRepeated(..)`              | Handler (throws by default) of unexpected repeated elements (not lists).                                                            |
| `handleAttributeOrderConflict(..)`   | Handler (throws by default) of order constraints that are inconsistent with requested type (e.g. attributes ordered after elements) |
| `shouldEncodeElementDefault(..)`     | Determine whether a default value should be encoded                                                                                 |
| `initialChildReorderMap(..)`         | Initial stage of allowing for element ordering, by default uses `@XmlBefore` and `@XmlAfter`                                        |
| `updateReorderMap(..)`               | Allows for updating the reordering of children at a second stage (by default keeps the order)                                       |
| `enumEncoding(..)`                   | Determine how enum values should be encoded (by default the serial name of the element)                                             |
| `preserveSpace(..)`                  | Determine whether for the given element white space should be preserved (by default inherits from parent)                           |
| `mapKeyName`                         | The name to use for map keys (by default `key`)                                                                                     |
| `mapValueName`                       | The name to use for map values (by default `value`). Only if map entries are not collapsed (with the key used as attribute)         |
| `isMapValueCollapsed`                | Determine whether tag map values should be serialized as the value tag with the key added as attribute on the value                 |
| `elementNamespaceDecls`              | Function that allows you to request additional namespace prefix declarations to be available on the tag (if not in the parent yet)  |
| `attributeListDelimiters`            | Function that allows you to specify the delimiters to use for lists serialized as attributes (by default xml whitespace)            |
| `textListDelimiters`                 | Function that allows you to specify the delimiters to use for lists serialized as text (by default the same as attributes)          |
| `builder()`                           | Creates a builder for the type. This should be implemented by subtypes                                                             |

### Algorithms
XML and Kotlin data types are not perfectly alligned. As such there are some
algorithms that aim to automatically make a "best attempt" at structuring the XML
document. Most of this is implemented in the *default* `XmlSerializationPolicy`
implementation, but this can be customized/replaced with a policy that results
in a different structure. The policy includes the mapping from types/attributes
to tag and attribute names.

#### Storage type
In the default policy, the way a field is stored is automatically determined to be one of: Element, Attribute, Text or
Mixed. Mixed is a special type that allows for mixing of text and element content and requires some special treatment.:
- If a field is annotated with `@XmlElement` or `XmlValue` this will take precedence. The XmlValue tag will
  allow the field to hold element text content (direct only).
- If the serializer is a primitive this will normally be serialized as attribute
- If the serializer is a list, if there is an `@XmlChildrenName` annotation, this
  will trigger named list mode where a wrapper tag (element) is used. Otherwise
  the list elements, even primitives, will be written directly as tags (even
  primitives) without any wrapper list tags.
- If a list has the `@XmlValue` tag, this will allow the list to hold mixed content.
  To actually support text content it needs to be a list of `Any`. This should 
  also be polymorphic (but the annotation is required).
  - Lists of `Element`s (using `ElementSerializer`) and `CompactFragment`s support
  arbitrary content and provide it as lists of fragments or nodes. 
- If a primitive is written as tag, the type name is used as tag name,
  and value as its element content.
- A primitive written as TEXT will be text content only, but note that there are
  only few cases where this is valid.
- Polymorphic properties are treated specially in that the system does not
  use/require wrappers. Instead it will use the tag name to determine the type.
  The name used is either specified by an `@XmlPolyChildren` annotation or through the
  type's `serialDescriptor`. This also works inside lists, including
  transparent (invisible) lists. If multiple polymorphic properties have the
  same subtags, this is an error that may lead to undefined behaviour (you can
  use the `@XmlPolyChildren` to have different names).
  
  A custom policy is able to determine on individual basis whether transparent
  polymorphism should be used, but the default policy provides an overall toggle
  (which also respects the autopolymorphic property of the configuration builder).
  The default will always trigger transparent mode if `XmlPolyChildren` is present.
  
- If the serializer is polymorphic, tag mode will be enforced. If `@XmlPolyChildren`
  is specified or `autoPolymorphic` is set it triggers transparent polymorphism
  mode where the child name is used to look up the property it belongs to. (note
  that this is incorrect with multiple properties that could contain the same
  polymorphic value - unless @XmlPolyChildren overrides it).
- Otherwise it will be written as a tag.

#### Tag/attribute name
The way the name is determined is configured/implemented through the configured policy. The documentation below
is for the default policy. This is designed to allow customization by users.

Based upon the storage type, the effective name for an attribute is determined as follows:
- `@XmlSerialName` at property declaration site
- `@XmlSerialName` at type declaration site
- `@SerialName` at property declaration site
- property name at property declaration site (note that the `@SerialName` annotation is invisible to the encoder)

The effective name for a regular tag is determined as follows for normal serializers:
- `@XmlSerialName` at property declaration site
- `@XmlSerialName` at type declaration site
- `@SerialName` at type declaration site
- type name at type declaration site.
The default type declaration type name is the Kotlin/Java type name (and long). The system will try to shorten this by
eliding the package name. This is configurable in the policy. 

The effective name for a polymorphic child is determined as follows:
- If the child is transparent, the annotations/serial name of the effective type is used (unless overridden by `@XmlPolyChildren`)
- If the child is not transparent, the container is treated as a regular tag. It will have a `type` attribute to contain
  the serial name of the type (shortened to share the package name with the container). The value will use the default
  name `value`.

The implementation if serialization in the Kotlin compiler does not allow distinguishing between the automatic name and
a `@SerialName` annotation. The default implementation supposes that if there is a '`.`' character in the name, this is
a java type name and it strips the package out. (This also when it could be an attribute).

If you need to support names with dots in your format, either use the `@XmlSerialName` annotation, or use a
different policy.

### Annotations

The annotations that specify names have common attributes: `value:String`,
`namespace:String` and `prefix:string`. They have common semantics:

| parameter           | description                                                                                                                                  |
|---------------------|----------------------------------------------------------------------------------------------------------------------------------------------|
| `value: String`     | The local part of the tag name to use. If optional and not specified the serial name of the property/type will be used.                      |
| `namespace: String` | The namespace part of the tag name to use. If not specifies defaults to the namespace of the containing tag                                  |
| `prefix: String`    | Suggested prefix to use, overridden by existing prefixes for the namespace. Will cause a namespace declaration/prefix to be added if needed. |

Annotation explanation.

| Annotation                  | Property                                               | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
|-----------------------------|--------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `@XmlSerialName`            |                                                        | Specify more detailed name information than can be provided by `kotlinx.serialization.SerialName`. In particular, it is not reliably possible to distinguish between `@SerialName` and the type name. We also need to specify namespace and prefix information.                                                                                                                                                                                                                                        |
|                             | `value: String`, `namespace: String`, `prefix: String` | See table above.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| `@XmlNamespaceDeclSpecs`    |                                                        | Annotation allowing to specify namespaces specifications to be generated upon the element. As multiple annotations are not supported by the plugin this uses a single string. Each declaration is of the form (prefix)=(namespace). To specify the default namespace it is valid to omit the equals sign.                                                                                                                                                                                              |
|                             | `vararg value: String`                                 | The actual specification: `"prefix1=urn:namespace1", "defaultNamespace"`                                                                                                                                                                                                                                                                                                                                                                                                                               |
| `@XmlPolyChildren`          |                                                        | Indicate the valid polymorphic children for this element. This is a legacy annotation supporting polymorphic (de)serialization without modules.                                                                                                                                                                                                                                                                                                                                                        |
|                             | `value: Array<String>`                                 | Each string specifies a child according to the following format: `childSerialName[=[prefix:]localName]`. The `childSerialName` is the name value of the descriptor. By default that would be the class name, but `@SerialName` will change that. If the name is prefixed with a `.` the package name of the container will be prefixed. Prefix is the namespace prefix to use (the namespace will be looked up based upon this). Localname allows to specify the local name of the tag.                |
| `@XmlChildrenName`          |                                                        | Specify additional information about child values in collections. This is only used for primitives, not for classes that have their own independent name. The outer tag name is determined regularly.                                                                                                                                                                                                                                                                                                  |
|                             | `value: String`                                        | **Mandatory**, see table above.                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
|                             | `namespace: String`, `prefix: String`                  | See table above.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| `@XmlKeyName`               |                                                        | Used to specify the xml name used for the key attribute/tag of a map.                                                                                                                                                                                                                                                                                                                                                                                                                                  |
|                             | `value: String`                                        | **Mandatory**, see table above.                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
|                             | `namespace: String`, `prefix: String`                  | See table above.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| `@XmlMapEntryName`          |                                                        | Dual use annotation that both forces explicit map entry wrappers and specifies the tag name used. The default is to elude the wrappers in the case that data does not contain an attribute with the name of the key.                                                                                                                                                                                                                                                                                   |
|                             | `value: String`                                        | **Mandatory**, see table above.                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
|                             | `namespace: String`, `prefix: String`                  | See table above.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| `@XmlElement`               |                                                        | Force a property that could be an attribute to be an element. Note that default behaviour requires this annotation to be absent.                                                                                                                                                                                                                                                                                                                                                                       |
|                             | `value: Boolean`                                       | `true` to indicate serialization as tag, `false` to indicate serialization as attribute. Note that not all values can be serialized as attribute.                                                                                                                                                                                                                                                                                                                                                      |
| `@XmlValue`                 |                                                        | Force a property to be content of the tag (for the object containing the property). This is both for text content (polymorphic including a primitive), but if the type is a list of tag-like types (`Node`, `Element`, `CompactFragment`) it will also allow mixed content of tags not supported by the base type. Strings will be serialized/deserialized as (tag soup) string content without wrapper.                                                                                               |
|                             | `value: Boolean`                                       | `true` to indicate the value. `false` and absence are equivalent.                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| `@XmlId`                    |                                                        | Annotation to mark the value as an ID attribute. This implies that the element is an attribute. This will allow the serializer to enforce uniqueness.                                                                                                                                                                                                                                                                                                                                                  |
| `@XmlIgnoreWhitespace`      |                                                        | Determine whether whitespace should be ignored or preserved for the tag.                                                                                                                                                                                                                                                                                                                                                                                                                               |
|                             | `value: Boolean`                                       | `true` if whitespace is to be ignored, `false` if preserved.                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| `@XmlOtherAttributes`       |                                                        | This annotation allows handling wildcard attributes. It should be specified on a  `Map<QName, String>` to store unsupported attributes. The key is preferred to be a QName, alternatively it must convert to String (this could be "prefix:localName"). The value must be a String type. **Note** that if the key runtime type is a `QName` the value is directly used as attribute name without using the key serializer.                                                                             |
| `@XmlCData`                 |                                                        | Mark the property for serialization as CData, rather than text (where appropriate). If used on a property this will override the annotation on a type. This is the only context in which a value of `false` is different from omitting the annotation.                                                                                                                                                                                                                                                 |
| `@XmlDefault`               |                                                        | Allow a property to be omitted with a default serialized string. This annotation primarily supports older versions of the framework that do not support default attribute values. The default value will not be written out if matched.                                                                                                                                                                                                                                                                |
|                             | `value: String`                                        | The default value used if no value is specified. The value is parsed as if there was textual substitution of this value into the serialized XML.                                                                                                                                                                                                                                                                                                                                                       |
| `@XmlBefore`                |                                                        | Require this property to be serialized before other (sibling) properties. Together [XmlBefore] and [XmlAfter] define a partial order over the properties. Using this annotation may cause values to be serialized as elements rather than attributes where not explicitly specified as attributes. If there is a conflict between serialization type (attribute/element) this takes precendence over this attribute. The names are the serialNames of the properties being serialized (not XML names). |
|                             | `value: Array<String>`                                 | The serial names of all the children that should be serialized after this one (uses the [kotlinx.serialization.SerialName] value or field name).                                                                                                                                                                                                                                                                                                                                                       |
| `@XmlAfter`                 |                                                        | Require this property to be serialized after other (sibling) properties. Together [XmlBefore] and [XmlAfter] define a partial order over the properties. Using this annotation may cause values to be serialized as elements rather than attributes where not explicitly specified as attributes. If there is a conflict between serialization type (attribute/element) this takes precendence over this attribute. The names are the serialNames of the properties being serialized (not XML names).  |
|                             | `value: Array<String>`                                 | The serial names of all the children that should be serialized before this one (uses the [kotlinx.serialization.SerialName] value or field name).                                                                                                                                                                                                                                                                                                                                                      |

### Special types
These types have contextual support by default (without needed user intervention),
but the serializer can also be specified explicitly by the user. They get special
treatment to support their features.

#### `QName`
By default (configurable by the policy) QName is handled by special logic that
stores QNames in a prefix:localName manner ensuring the prefix is valid in the
tag. Many XML standards use this approach for string attributes.

#### `CompactFragment`
The `CompactFragment` class is a special class (with supporting serializer) that will be able to capture the tag soup
content of an element. Instead of using regular serialization its custom serializer will (in the case of xml serialization)
directly read all the child content of the tag and store it as string content. It will also make a best effort attempt
at retaining all namespace declarations necessary to understand this tag soup.

Alternatively the serialutil subproject contains the `nl.adaptivity.serialutil.MixedContent` type that allows for
typesafe serialization/deserialization of mixed content with the proviso that the serialModule must use Any as the
baseclass for the content. 

### Modules

#### `core`
Container for the core library (versions). It provides XML functionality and is
mostly platform independent.

#### -`core-android`-
Deprecated module for Android specific parsing. This is deprecated as the generic
implementations are improved on the basis of the Android implementation.

#### `core-io`
Module that provides kotlinx.io bindings to the core xml library.

#### `core-jdk`
Module that provides access to JDK specific types (stax) and the platform parser.
This is not needed for most cases, except where integration with JDK STaX parsing
is required.

#### `serialization`
The kotlinx.serialization format to allow serialization to XML.


#### `serialization-io`
Additional shortcut bindings that provide direct access to kotlinx.io
based streams (avoiding the need to use `core-io` directly to create
the stream).

#### `serialutil`
An auxiliary library that provides some utility functions for working with
serialization. This is not specific to XML.
