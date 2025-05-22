# Example Jackson policy
This example is based upon issue #33. It shows how a policy can be used to configure
serialization to be jackson compatible (or closer to it). This example is not intended
to be a complete compatibility policy - just an example.

## Output
```xml
<Team><members><name>Joe</name><age>15</age></members></Team>
```

## Data types
Team & Person - [data.kt](src/main/kotlin/net/devrieze/serialization/examples/jackson/data.kt)
```kotlin
@Serializable
data class Person(val name: String, val age: Int)
@Serializable
data class Team(val members: List<Person>, val colors: List<String> = listOf())
```

## Example policy
[JacksonPolicy.kt](src/main/kotlin/net/devrieze/serialization/examples/jackson/JacksonPolicy.kt)
```kotlin
/**
 * Example policy that (very crudely) mimicks the way that Jackson serializes xml. It starts by eliding defaults.
 * Note that this version doesn't handle the jackson annotations, and is not configurable.
 */
class JacksonPolicy(formatCache: FormatCache = defaultSharedFormatCache(), config: Builder.() -> Unit = {}) :
    DefaultXmlSerializationPolicy(formatCache, {
        pedantic = false
        encodeDefault = XmlSerializationPolicy.XmlEncodeDefault.NEVER
        config()
    }) {

    constructor(config: Builder.() -> Unit): this(defaultSharedFormatCache(), config)

    /*
     * Rather than replacing the method wholesale, just make attributes into elements unless the [XmlElement] annotation
     * is present with a `false` value on the value attribute.
     */
    override fun effectiveOutputKind(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        canBeAttribute: Boolean
    ): OutputKind {
        val r = super.effectiveOutputKind(serializerParent, tagParent, canBeAttribute)
        return when {
            // Do take into account the XmlElement annotation
            r == OutputKind.Attribute &&
                    serializerParent.useAnnIsElement != false ->
                OutputKind.Element

            else -> r
        }
    }

    /**
     * Jackson naming policy is based upon use name only. However, for this policy we do take the type annotation
     * if it is available. If there is no annotation for the name, we get the name out of the useName in all cases
     * (the default policy is dependent on member kind and the output used (attribute vs element)).
     */
    override fun effectiveName(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        outputKind: OutputKind,
        useName: XmlSerializationPolicy.DeclaredNameInfo
    ): QName {
        return useName.annotatedName
            ?: serializerParent.elementTypeDescriptor.typeQname
            ?: serialUseNameToQName(useName, tagParent.namespace)
    }

}
```
For allowing elegant configuration, the below code prvoides for configuration.
Note that this function's implementation could be adjusted to allow for
a class (not object) policy that would also allow for further configuration.

```kotlin
fun XmlConfig.Builder.jacksonPolicy(config: Builder.() -> Unit = {}) {
    @OptIn(ExperimentalXmlUtilApi::class)
    policy = JacksonPolicy {
        setDefaults_0_91_0()
        encodeDefault = XmlSerializationPolicy.XmlEncodeDefault.NEVER
        config()
    }
}
```


## Example usage
[main.kt](src/main/kotlin/net/devrieze/serialization/examples/jackson/main.kt)
```kotlin
fun main() {
    val t = Team(listOf(Person("Joe", 15)))
    val xml = XML {
        jacksonPolicy()
    }

    val encodedString = xml.encodeToString(t) // both versions are available
    println("jackson output:\n${encodedString.prependIndent("    ")}\n")

    // the inline reified version is also available
    val reparsedData = xml.decodeFromString<Team>(encodedString)
    println("jackson input: $reparsedData")

}
```
