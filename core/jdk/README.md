# Module coreJdk
Module providing Jdk specific parsing implementations. Including this module 
changes the default to us the jdk's xml parser/serializer implementations
(the JDK uses serviceLoaders itself, so you could this way use implementations
such as woodstox).

# Package nl.adaptivity.xmlutil.jdk
Core package for a wrapper that provides XML pull parsing access. Note
that the implementations may do some adjustments beyond what is provided
by the underlying implementation to improve compatibility across platforms.
The access point to the package/module is (xmlStreaming)[nl.adaptivity.xml.xmlStreaming].
