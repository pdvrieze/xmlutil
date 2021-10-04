# 0.83.1
*()<br />*
Features:
- Add `@XmlCData` annotation to force serialization as CData for a type (#71)
- Allow compact fragments to be used transparently in conjunction with the 
  `@XmlValue` annotation (it requires use of the `CompactFragmentSerializer`).
- Add `XmlBufferReader` as a reader of lists of events (the counterpoint to 
  `@XmlBufferedWriter`)
- Support serializing lists of primitives (or inlines of primitives, or qnames).
  This is derived from the xml schema standard (and needed for xml schema).
- Support storing unknown attributes in a `Map<QName, String>`
  (Using [QNameSerializer] or contextual). The field needs to be annotated with
  `@XmlOtherAttributes`. (Technically other types than QName and String are
  supported, but only if all values will be able to read from that string value)
- add methods on [XmlInput] and [XmlOutput] to allow custom serializers the
  ability to ensure a prefix is registered/look it up.

Fixes:
- Update to kotlinx.serialization-1.3.0-RC
- Fix/change XmlReader.namespaceDecls (make it a member, not an extension)
- Fix compact fragments that redeclare the default namespace
- Fix deserialization of empty types where decodeElementIndex is never called
  (this applies for object)
- Fix XmlBufferedReader's use of it's own namespace context and initializing
  it properly for the initial event (adding all visible namespaces).

# 0.83.0 – Changes, here we come

*(Sep 4, 2021)<br />*
Features:

- There is a ktor module available that makes xml available at server level
- Support for determine the order of items/reordering:
    - This can be configured by the policy.
    - The default policy works using: `@XmlBefore` and `@XmlAfter` to specify
      order relationships. There is an update to the policy API where an
      additional parameter determines whether the child can be an attribute.
    - Ordering is two-phase, the initial phase is done before determining the
      output kind. The second phase reorders after the output kind of a child is
      determined and can override the attributes (attributes are always before
      element children).
- The policy can now be used to determine the string used to represent an enum
  constant (by default the name of the enum element)
- Provide a serializer for `QName` instances. This serializer is available in
  the default context and special cased to handle prefixes correctly. There are
  many related fixes to the namespace code in the core module.
- Allow the policy to override the serializer/deserializer used for a specific
  child.
- Add an option to XmlConfig (`isCollectingNSAttributes`) that allows namespace
  attributes to be serialized on the root tag.
- There is now a `KtXmlWriter` multiplatform class that replaces then android
  default xml writer (it merges the wrapper with an adapted form of Android's
  XmlSerializer). This serializer is available on all targets.
- There is a lot of cleanup of the APIs, especially in the core module.
- Allow explicitly specifying the root tag name for serialization
- Mostly support default annotation values. Please note that at this time there
  is still some issue with the JVM IR compiler with the default values (code
  will not generate correctly). However the defaults are available and have been
  tested where relevant.

Fixes:

- Don't elide lists if the serialized content is a list (xml documents must have
  a single root tag)
- Support `@XmlValue(false)` correcty
- Support Kotlin 1.5 and kotlinx.serialization-1.2.2
- Allow DomSource to work on for a stax parser (by using a string intermediate)
- Fix generating namespace prefixes when missing
- Per the standard attributes without prefix are always in the default (empty)
  namespace, handle this correctly.
- Give the CharArrayAsString serializer a different name as the new library
  doesn't like overlapping names (and there already is a standard chararray
  serializer).

# 0.82.0 – Progress it is

*(Apr 17, 2021)<br />*
This release supports [kotlin 1.5.0](https://www.kotlinlang.org)
and [kotlinx.serialization 1.2.0](https://github.com/Kotlin/kotlinx.serialization)

Key changes are:

- Add `xmlDescriptors` to `XML` to provide access to the serialization
  structure (as also used for serialization). This can be used for writing
  schemas (but to change it you will have to implement your own policy)
- Fix bug [#44](https://github.com/pdvrieze/xmlutil/issues/44)
- `serialNameToQname` is split into two separate functions separating the policy
  for when the name is declared on the attribute or on the type. This in
  response to [#50](https://github.com/pdvrieze/xmlutil/issues/50)
- Mark various deprecated API elements as errors. They will be removed soon (the
  plan for release 0.90 is to tidy things up)
- Create a module for use in ktor that allows for xml serialization in ktor.
  This differs from the json/generic version in that it uses its own
  streamconverter and can serialize directly from/to streams without a string
  intermediate (this should also support xml based encoding specification).

# 0.81.2 – Goodbye JCenter

*(Apr 17, 2021)<br />*
As part of a goodbye to jcenter configure publication on github packages. Note
that this comes with a new maven coordinate, and a reverting to the default
publishing as provided by kotlin multiplatform.

# 0.81.1 – To null or not to null

*(Feb 25, 2021)<br />*
Small update:

- handle custom serializers for null values correctly (when using the composite
  encoder) - fixes [#53](https://github.com/pdvrieze/xmlutil/issues/53).
- Update to 1.1.0 release of kotlinx.serialization

# 0.81.0 – Let's all keep in line

*(Feb 16, 2021)<br />*
New version that is compatible with 1.1.0-RC

- Compiled against 1.1.0-RC
- Supports inline classes (when using the experimental IR compiler - a
  restriction of the compiler, not this library)
    - Inline classes can use `@XmlSerialName` to override the name use used by
      the type

# 0.80.1 – Some bug fixes

*(Oct 17, 2020)<br />*
Fix some edge case issues:

- Handling of entity
  references [#44](https://github.com/pdvrieze/xmlutil/issues/44)
- The naming of the value tag for polymorphic values in non-auto-polymorphic
  mode
- Fix handling of the empty namespace in certain cases

# 0.80.0 – Let's go

*(Oct 10, 2020)<br />*
Update the release candidate to work with the released
kotlinx.serialization-1.0.0. In addition to pure compatibility fixes it also
adds configurability of encoding of default values (by default the original
behaviour using `@XmlDefault`)

# 0.80.0-RC – Getting ready for stabilization

*(Aug 31, 2020)<br />*
This version provides support for Kotlin 1.4 and kotlinx.serialization-1.0.0-RC.
This code should be fairly stable, but has not had API rationalization
completed (working towards API stabilization). This doesn't mean that the API is
going to change, just that some code should be private but isn't. Documentation
should still be improved.

In addition, there is a new configuration approach to the serialization with
policies (which was also in the v0.20.0.10 release). This needs feedback, but
would even allow for custom annotations as well as support things like jackson
compatibility.

# 0.20.0.10 – Replace the engine under the hood

*(Aug 19, 2020)<br />*
This version has extensive redesign under the hood. It now creates a complete
descriptor tree with names, output kinds and all that is then used by the
serialization/deserialization code. This has likely fixed some bugs, but at
least clears up the code to be much more comprehensible (and less complex). In
particular it makes encoder/decoder matching that much easier as they share
information. The configuration has been changed by adding a policy interface
that drives more important serialization decisions. This should allow for
serialization to be much more customized.

Highlighted features:

- Configuration now has a policy attribute/type that can be used to determine
  tag names used, form of serialization etc.
- The `XmlDescriptor` class is created to allow introspecting the entire xml
  structure with names etc. This is used internally, but can also be used to
  generate things like xml schema documents (future feature).
- Add `MixedContent` to the serialutil module. This allows for typesafe
  serialization of mixed content (with text) - #30. Various fixes were included
  to make it actually work. Those fixes should fix other custom serializers too.
- By default support serialization/deserialization of `org.w3c.dom.Node`
  and `org.w3c.dom.Element` typed properties
- Various bug fixes such as: missing/invalid namespace attributes #36, recursive
  type definitions #32,

# 0.20.0.1 – Indent it all

*(May 17, 2020)<br />*
This release is mostly a maintenance release. It properly allows for
configurable indentation as well as other features:

- \#27 omitXmlDecl=false didn't work on Android. This has been replaced by a
  more comprehensive configuration option that also allows configuration of the
  xml declaration.
- \#28, #24 String indentation is fixed/enhanced. This version directly exposes
  the indentation string to the serialization (accidentally omitted in 0.20.0.0)
  . Indentation can still be specified by a number, but can now be specified as
  a string. Internally it will parse the string and store it as a sequence of
  ignorable whitespace and comment events. As such, invalid indentation will now
  throw an exception.
- \#25 When using the default java xml library (instead of woodstox) it should
  now correctly work when no namespaces are used.
- \#13 Make direct closing tags available on JVM without a custom xml library to
  do it. Instead this is now done at wrapper level. It was already functional
  for Android. JS depends on the dom provided serialization.

# 0.20.0.0 – Keeping going

*(Mar 5, 2020)<br />*
This is a release compatible with version 0.20.0 of the kotlinx.serialization
library and Kotlin 1.3.70. In addition there are two other significant changes:

- Fixed the way XmlPolyChildren works in relation to renamed classes (it will
  now use the declared XmlSerialName if not renamed in the annotation
- Made it possible (on XmlWriter to set indentation as string). For now you need
  to create your `XmlWriter` directly for this to be available. The
  configuration for the serializer format will follow.

# 0.14.0.3 – Have the features

*(Mar 5, 2020)<br />*
A backport of the fixes from 0.20.0.0 to a 0.14.0 (kotlin 1.3.61) compatible
version of the library.

# 0.14.0.2 – Getting there

*(Dec 20, 2019)<br />*
Fix bug [#23](https://github.com/pdvrieze/xmlutil/issues/23) - decoding of a
nullable list of elements now works. Note that in case of an empty list this
will deserialize as null, not an empty list.

# 0.13.0.2 – The past likes some love too

*(Dec 20, 2019)<br />*
Backport the fix to bug [#23](https://github.com/pdvrieze/xmlutil/issues/23) to
the 0.13.0 (kotlin 1.3.50) branch

# 0.14.0.1 – Seal it better

*(Dec 18, 2019)<br />*
Fix deserialization of sealed classes in
bug [#22](https://github.com/pdvrieze/xmlutil/issues/22).

# 0.14.0.0 – Let's seal it up

*(Nov 20, 2019)<br />*
Update to support the 1.3.60 Kotlin plugin changes. The updated
kotlinx.serialization 0.14.0 library, and importantly (using the updated
library) it supports sealed classes properly without requiring modules.

# 0.13.0.1 – Squash some bugs

*(Oct 22, 2019)<br />*
Bugfix release:

- handle reading CDATA sections
- handle certain cases where the default JVM parser wouldn't work with char
  sequences (bug in CharSequenceReader)

# 0.13.0.0 – Following on

*(Sep 17, 2019)<br />*
This version makes the library compatible with kotlinx.serialization 0.13.0. A
recommended update as there was a problematic regression in 0.12.0/0.12.0.0

# 0.12.0.0 – Updating

*(Aug 27, 2019)<br />*
Update the library for Kotlin 1.3.50 and kotlinx.serialization version 0.12.0

# 0.11.1.2 – Full equity now

*(Jul 17, 2019)<br />*
This release finally supports full serialization on Javascript, new
configuration and automatic polymorphism support:

- `XmlPolyChildren` has changed shorthand and updated documentation - it is
  serial name, not class name. Class name shorthand now requires a '`.`' prefix.
- The `autoPolymorphic` option to the xml format will reduce the need
  for `@XmlPolyChildren` and will use the module to look up possible child
  deserializers
- Serialization works properly with javascript targets for all cases.

# 0.11.1.1 – *&lt;Unlabeled&gt;*

*(Jun 30, 2019)<br />*
Fix common dependency

# 0.11.1.0 – The modular update

*(Jun 30, 2019)<br />*

- This version has been refactored to allow for a modular version. A separate
  modular branch exists as well, but some testing of it would be beneficial.
- Make it work properly again with the new serialization generation in 1.3.30 (
  this version targets 1.3.40)
- Refactored packages. Mostly the packages that contain utility classes that
  were intended mainly for internal use.
- Update to work against kotlinx.serialization 0.11.0/0.11.1
- Change versioning system. Instead of independent versions, use a version that
  reflects the underlying kotlinx.serialization version. At least as long as
  kotlinx.serialization is still unstable.
- Split out a serialutil package that contains helpers for implementing
  serializers in classes.

# 0.9.0 – JavaScript here we come

*(Apr 3, 2019)<br />*
This version adds support for Javascript serialization and deserialization. It
fixes the Javascript core library as well. Further it contains various bug fixes
in the existing java-ish platform support.

# 0.8.1 – Follow on to 0.10.0 of the serialization framework

*(Feb 8, 2019)<br />*

- Update to kotlinx.serialization 0.10.0
- Remove the Canary code as elementDescriptors are now generated
- Some caching and stability is still provided
- **NOTE**: Custom serializers do need to provide elementDescriptors

# 0.8.0 – Kotlin 1.3.0 update

*(Nov 3, 2018)<br />*
Update the package to work with Kotlin 1.3.0 and serialization library 0.9.0.
This means that some of the API has changed a bit (parameter order) and
reflection based access is marked experimental in line with the serialization
library itself as that doesn't work on non-java platforms.

# 0.7.0 – Rework for stability

*(Oct 3, 2018)<br />*
This release updates the serialization plugin to be based upon the design
described in the
[serialization keep](https://github.com/Kotlin/KEEP/blob/serialization/proposals/extensions/serialization.md)
. This has improved architecture and dependability.

- Naming is now well defined: If present on type or use site the XmlName tag
  wins. Otherwise for tags the serialization name at the type declaration wins
  (XML tags should be named the same independent of use). For attributes the use
  site name wins (attributes are generally primitives and generic type names are
  not useful)

# 0.6.0 – Initial release

*(Oct 3, 2018)<br />*
Initial project release
