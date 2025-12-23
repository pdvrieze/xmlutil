# Introduction to XMLUtil
This project is a cross-platform XML serialization (wrapping) library compatible with kotlinx.serialization.
It supports all platforms although native is at beta quality.

Based upon the core xml library, the serialization module supports automatic object
serialization based upon Kotlin's standard serialization library and plugin. 

## Usage
Simple usage for serialization (gradle):
```
implementation("io.github.pdvrieze.xmlutil:serialization:1.0.0-RC1")
```

If only the core module (XML parsing) is needed:
```
implementation("io.github.pdvrieze.xmlutil:core:1.0.0-RC1")
```

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

### Custom configuration
The format can be configured using the `XML1_0` accessor:
```kotlin
val format = XML1_0.recommended(mySerialModule) {  
    // configuration options
    xmlDeclMode = XmlDeclMode.None
    policy {
        typeDiscriminatorName = QName(XMLConstants.XSI_NS_URI, "type", XMLConstants.XSI_PREFIX)
    }
}
```

