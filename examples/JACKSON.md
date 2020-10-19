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
 * Note that this version doesn't handle the jackson annotations.
 */
object JacksonPolicy :
    DefaultXmlSerializationPolicy(false, encodeDefault = XmlSerializationPolicy.XmlEncodeDefault.NEVER) {
    /*
     * Rather than replacing the method wholesale, just make attributes into elements unless the [XmlElement] annotation
     * is present with a `false` value on the value attribute.
     */
    override fun effectiveOutputKind(serializerParent: SafeParentInfo, tagParent: SafeParentInfo): OutputKind {
        val r = super.effectiveOutputKind(serializerParent, tagParent)
        return when {
            // Do take into account the XmlElement annotation
            r == OutputKind.Attribute &&
                    serializerParent.elementUseAnnotations.mapNotNull { it as? XmlElement }
                        .firstOrNull()?.value != false
                 -> OutputKind.Element

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
            ?: serializerParent.elemenTypeDescriptor.typeQname
            ?: serialNameToQName(useName.serialName, tagParent.namespace)
    }

}
```


## Example usage
[main.kt](src/main/kotlin/net/devrieze/serialization/examples/jackson/main.kt)
```kotlin
fun main() {
    val t = Team(listOf(Person("Joe", 15)))
    val xml = XML {
        policy = JacksonPolicy
    }

    val encodedString = xml.encodeToString(t) // both versions are available
    println("jackson output:\n${encodedString.prependIndent("    ")}\n")

    // the inline reified version is is also available
    val reparsedData = xml.decodeFromString<Team>(encodedString)
    println("jackson input: $reparsedData")

}
```