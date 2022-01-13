# XmlUtil
[![Build Status](https://dev.azure.com/pdvrieze/xmlutil/_apis/build/status/pdvrieze.xmlutil?branchName=master)](https://dev.azure.com/pdvrieze/xmlutil/_build/latest?definitionId=1&branchName=master)
[![GitHub license](https://img.shields.io/badge/License-Apache%202-blue.svg?style=flat)](COPYING)
core:&nbsp;[![Download](https://img.shields.io/maven-central/v/io.github.pdvrieze.xmlutil/core)](https://search.maven.org/artifact/io.github.pdvrieze.xmlutil/core)
serialization:&nbsp;[![Download](https://img.shields.io/maven-central/v/io.github.pdvrieze.xmlutil/serialization)](https://search.maven.org/artifact/io.github.pdvrieze.xmlutil/serialization)
serialutil:&nbsp;[![Download](https://img.shields.io/maven-central/v/io.github.pdvrieze.xmlutil/serialutil)](https://search.maven.org/artifact/io.github.pdvrieze.xmlutil/serialutil)
ktor:&nbsp;[![Download](https://img.shields.io/maven-central/v/io.github.pdvrieze.xmlutil/ktor)](https://search.maven.org/artifact/io.github.pdvrieze.xmlutil/ktor)

XmlUtil is a set of packages that supports multiplatform XML in Kotlin (only Javascript/JVM/Android currently).

### Introduction
* Gradle wrapper validation: ![Validate Gradle Wrapper](https://github.com/pdvrieze/xmlutil/workflows/Validate%20Gradle%20Wrapper/badge.svg)

This project is a cross-platform XML serialization (wrapping) library compatible with kotlin serialization. It provides
capabilities for Android, JVM and JS (alpha quality)

It also provides serialization support

**Help wanted**: Any help with extending this project is welcome. Help is especially needed for the following aspects:

* Documentation updates
* Testing, in particular more extensive tests. Some tests already exist for both JVM and Android
* Javascript support
  * Core Javascript support needs testing but should work. It is based on DOM so may be slow
  * Javascript serialization support: make serialization work on Javascript once possible
* Native support: Currently there is no implementation for Kotlin native. 

### Versioning scheme
This library is based upon the unstable [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) library. 
Until that library is stable, this library will use the kotlinx.serialization library version as a prefix and append a
release number.
While every effort is made to limit incompatible changes, this cannot be guaranteed even in "minor" versions when
the changes are due to bugs. These changes should be limited mainly to the serialization part of the library.

## How to use
The library is designed as a multiplatform kotlin module, but platform-specific versions can also be used were appropriate.
### Add repository
The project's maven access is hosted on OSS Sonatype (and available from Maven Central).

Releases can be added from maven central or:
```groovy
repositories {
	maven {
		url  "https://s01.oss.sonatype.org/content/repositories/releases/"
	}
}
```
Snapshots are available from:
```groovy
repositories {
	maven {
		url  "https://s01.oss.sonatype.org/content/repositories/snapshots/"
	}
}
```

### Core
#### multiplatform
```
   implementation("io.github.pdvrieze.xmlutil:core:0.84.0")
```
#### JVM -- uses the stax API not available on Android
```
   implementation("io.github.pdvrieze.xmlutil:core-jvm:0.84.0")
```
#### Android -- Uses the android streaming library
```
   implementation("io.github.pdvrieze.xmlutil:core-android:0.84.0")
```
#### JS -- Wraps DOM
```
   implementation("io.github.pdvrieze.xmlutil:core-js:0.84.0")
```
### Serialization
#### multiplatform
```
   implementation("io.github.pdvrieze.xmlutil:serialization:0.84.0")
```
#### JVM
```
   implementation("io.github.pdvrieze.xmlutil:serialization-jvm:0.84.0")
```
#### Android
```
   implementation("io.github.pdvrieze.xmlutil:serialization-android:0.84.0")
```
#### js
```
   implementation("io.github.pdvrieze.xmlutil:serialization-js:0.84.0")
```

### Ktor
```groovy
  implementation("io.github.pdvrieze.xmlutil:ktor:0.84.0")
```
This module provides simple ktor support for xml (de)serialization. To use
in your application use:
```kotlin
import nl.adaptivity.xmlutil.serialization.ktor.xml

fun Application.module(testing: Boolean = false) {

    install(ContentNegotiation) {
        xml()
    }
  /* ... */
}
```

## Serialization help
### Examples
You should be able to find examples in the [Examples module](examples/README.md)
### Format
The entrypoint to the library is the `XML` format. There is a default, but often a child is better. A custom format
is created through:
```kotlin
val format = XML(mySerialModule) {  
    // configuration options
    autoPolymorphism = true 
}
```
The options available are:

| option                | description |
| --- | --- | 
| `repairNamespaces`      | Should namespaces automatically be repaired. This option will be passed on to the `XmlWriter` |
| `xmlDeclMode`           | The mode to use for emitting XML declarations (<?xml ...?>). Replaces omitXmlDecl for more finegrained control |
| -`omitXmlDecl`-         | *Deprecated* (use `xmlDeclMode`). Should the generated XML contain an XML declaration or not. This is passed to the `XmlWriter` |
| `indentString`          | The indentation to use. Must be a combination of XML whitespace or comments (this is checked). This is passed to the `XmlWriter` |
| `indent`                | *Deprecated for reading*: The indentation level (in spaces) to use. This is backed by `indentString`. Reading is "invalid" for `indentString` values that are not purely string sequences. Writing it will set indentation as the specified amount of spaces. |
| -`autoPolymorphic`-     | *Deprecated into policy* Should polymorphic information be retrieved using `SerializersModule` configuration. This replaces `XmlPolyChildren`, but changes serialization where that annotation is not applied. This option will become the default in the future although XmlPolyChildren will retain precedence (when present) |
| -`unknownChildHandler`- | *Deprecated into policy* A function that is called when an unknown child is found. By default an exception is thrown but the function can silently ignore it as well. |
| `policy`                | This is a class that can be used to define a custom policy that informs how the kotlin structure is translated to XML. It drives most complex configuration|

The properties that have been moved into the policy can still be set in the builder but are no longer able to be read
through the config object.

### Algorithms
XML and Kotlin data types are not perfectly alligned. As such there are some algorithms that aim to automatically do the
best thing. Most of this has been moved to the *default* `XmlSerializationPolicy` implementation, but you can customize
this to change the eventual structure. This includes determining the names used.
#### Storage type
In the default policy, the way a field is stored is determined as follows to be one of: Element, Attribute, Text or
Mixed. Mixed is a special type that allows for mixing of text and element content and needs some special treatment.:
- If the field has an annotation such as `@XmlElement` or `XmlValue`  this will take precedence. The XmlValue tag will
  allow the field to hold element text content (direct only).
- If the serializer is a primitive this will normally be serialized as attribute
- If the serializer is a list, if there is an `@XmlChildrenName` annotation, this will trigger named list mode where a wrapper
  tag is used (element). Otherwise the list elements will be written directly as tags (even primitives) using a "transparent/anonymous" list.
- If a list has the @XmlValue tag (to actually support text content it needs to be a list of Any) this will allow the
  list to hold mixed content. This should also be polymorphic (but the annotation is required).   
- If a primitive is written as tag this will use the name as tag name, and value as element content.
- A primitive written as TEXT will be text content only.
- Polymorphic properties are treated specially in that the system does not use/require wrappers. Instead it will use the
  tag name to determine the type. Either specified by `@XmlPolyChildren` or through the serialDescriptor of the type.
  This also works inside lists, even transparent (invisible) lists. If multiple polymorphic properties have the same
  subtags this is undefined/an error (you can use @XmlPolyChildren to have different names).
  
  A custom policy is able to determine on individual bases whether this transparent polymorphism should be used, but the
  default policy provides an overall toggle (which also respects the autopolymorphic property of the configuration
  builder). The default will always trigger transparent mode if `XmlPolyChildren` is present.
  
- If the serializer is polymorphic this enforces tag mode. If `@XmlPolyChildren` is specified or `autoPolymorphic` is set
  it triggers transparent polymorphism mode where the child name is used to look up the property it belongs to. (note that this
  is incorrect with multiple properties that could contain the same polymorphic value - unless @XmlPolyChildren overrides it).
- Otherwise it will be written as a tag   

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

| Annotation | property | description |
| --- | --- | --- |
|`@XmlSerialName` | | Specify more detailed name information than can be provided by `kotlinx.serialization.SerialName`. In particular, it is not reliably possible to distinguish between `@SerialName` and the type name. We also need to specify namespace and prefix information. |
| | `value: String` | the local part of the name |
| | `namespace: String` | the namespace to use |
| | `val prefix: String` | The prefix to use |
|`@XmlPolyChildren` | | Mostly legacy annotation that allows specifying valid child tags for polymorphic resolution.
| | `value: Array<String>` | Each string specifies a child according to the following format: `childSerialName[=[prefix:]localName]`. The `childSerialName` is the name value of the descriptor. By default that would be the class name, but `@SerialName` will change that. If the name is prefixed with a `.` the package name of the container will be prefixed. Prefix is the namespace prefix to use (the namespace will be looked up based upon this). Localname allows to specify the local name of the tag.
| `@XmlChildrenName` | |  Used in lists. This causes the children to be serialized as separate tags in an outer tag. The outer tag name is determined regularly.
|`@XmlElement` | | Force a property to be either serialized as tag or attribute. |
| | `value: Boolean` | `true` to indicate serialization as tag, `false` to indicate serialization as attribute. Note that not all values can be serialized as attribute |
|`@XmlValue` | | Force a property to be element content. Note that only one field can be element content and tags would not be expected. When applied on `CompactFragment` this is treated specially. |
| `@XmlDefault` | | Older versions of the framework do not support default values. This annotation allows a default value to be specified. The default value will not be written out if matched. |
| | `value: String` | The default value used if no value is specified. The value is parsed as if there was textual substitution of this value into the serialized XML. |
|`@XmlBefore` | | Annotation to support serialization order. |
| | `value: Array<String>`| All the children that should be serialized after this one (uses the `@SerialName` value or field name) |
|`@XmlAfter` | | Annotation to support serialization order. |
| | `value: Array<String>`| All the children that should be serialized before this one (uses the `@SerialName` value or field name) |
|`@XmlCData` | | Force serialization as CData. |
|`@XmlMixed` | | When specified on a polymorphic list it will store mixed content (like html) where text is done as Strings. |
|`@XmlOtherAttributes` | | Can be specified on a `Map<QName, String>` to store unsupported attributes. |


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

#### core
Container for the core library (versions)

#### core.common
All code shared between Javascript and Java (either jvm or android)

#### core.common-nonshared
All code that is common, but not shared between Jvm and Android platforms

#### core.android
Code specific to the Android platform (Pulls in core.java as API dependency). This is a regular jar rather than an AAR
as the only specific thing to Android is the XML library

#### core.java
Implementation of the shared code for Java based platforms (both Android and JVM)

#### core.js
Javascript based implementation

#### core.jvm
Code unique to the JVM platform (Pulls in core.java as API dependency)

#### Serialization
The kotlinx.serialization plugin to allow serialization to XML

#### Serialization.java
The java version of the serialization plugin. Please note that it does not pull in the platform specific library. The
core library is dependent on the actual platform used (JVM or Android). This library only pulls in the shared Java code.

#### Serialization.jvm
The JVM version merely uses the jvm platform xml library but the serialization is
#### Serialization.android

#### Serialization.js
The Javascript version of the serialization plugin. This is not yet implemented due to missing annotation support for
javascript and the 0.6.0 version of kotlinx.serialization not supporting type annotations.

#### Serialization.test-android
An android test project to test serialization on Android.
