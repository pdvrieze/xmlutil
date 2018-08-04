# Introduction

  This project is a cross-platform XML serialization library compatible with kotlin serialization. It provides
  capabilities for Android, JVM and JS (alpha quality)

  It also provides serialization support
  
## Modules

### core
Container for the core library (versions)

### core.common
All code shared between Javascript and Java (either jvm or android)

### core.common-nonshared
All code that is common, but not shared between Jvm and Android platforms

### core.android
Code specific to the Android platform (Pulls in core.java as API dependency). This is a regular jar rather than an AAR
as the only specific thing to Android is the XML library

### core.java
Implementation of the shared code for Java based platforms (both Android and JVM)

### core.js
Javascript based implementation

### core.jvm
Code unique to the JVM platform (Pulls in core.java as API dependency)