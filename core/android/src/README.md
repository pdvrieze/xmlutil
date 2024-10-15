# Module coreAndroid
Module providing Android specific parsing implementations. As the
generic implementations are based on the Android implementations, this
module is deprecated except for those cases that require:
 - compatibility
 - integration with the native pull parser implementation (reading only)

# Package nl.adaptivity.xmlutil.core
Core package for a wrapper that provides XML pull parsing access. Note
that the implementations may do some adjustments beyond what is provided
by the underlying implementation to improve compatibility across platforms.
The access point to the package/module is (XmlStreaming)[nl.adaptivity.xml.XmlStreaming].
