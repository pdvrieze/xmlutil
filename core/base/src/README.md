# Module core
Core Xml wrapper that provides xmlpull parsing functionality. This module
is designed to wrap the actually present xml functionality in the platform.

In this version the core module is sufficient, and separate JVM/Android modules
are not needed. In such case the platform independent implementation is always
used. However, if the android/jvm module is used then service loaders will cause
platform specific implementations to be used by default.

# Package nl.adaptivity.js.util
Package with various extension functions to make working with DOM nodes
easier.

# Package nl.adaptivity.xmlutil
Core package for a wrapper that provides XML pull parsing access. Note
that the implementations may do some adjustments beyond what is provided
by the underlying implementation to improve compatibility across platforms.
The access point to the package/module is (XmlStreaming)[nl.adaptivity.xml.XmlStreaming].

# Package nl.adaptivity.xmlutil.util
Package with various utility types that are generally allow for a more
convenient way of using the library.
