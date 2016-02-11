# Introduction

  This project represents a process manager system, both front-end and backend, as well as support 
  systems. Some parts are optional and configuration dependent (such as the authentication systems).
  
## Modules
### android-auth
This is an authentication package in android that authenticates against the darwin system. Currently,
it is of limited use outside that context, but in future releases, basic authentication or OAuth support
may be added.

### darwin-sql
This module contains the various sql ddl scripts for the databases needed. They assume MySQL, but could
be adapted for other database servers.

### DarwinClients
This is a purely generated module that contains client code to invoke the various services. It handles
the wrapping of parameters etc. making the invokation of the remote code appear local.

### DarwinCommon
This is a library that contains shared code specific to the darwin server, that is not related to process
management as such. That code lives in PE-Common.
 
### DarwinGenerators
This module contains the generator code that is used to generate the DarwinClients code. This module therefore
is compile-time only.

### DarwinJavaApi
This module contains mainly interfaces (with the exception of the MessagingRegistry) that need to be
shared across multiple services.

### darwinlib
This is a client library for the darwin authentication system to use on android. It handles
automatically authenticating to the server for a request. When the calling activity so directs, it can download
the authentication library automatically.

### DarwinRealm
This module implements a Tomcat Valve that handles Darwin level authentication. If used, the module is most
properly used as part of the catalina class path, but usage in individual components should work (untested).
The realm could also be replaced by an alternate authentication system.

### DarwinServices
Some utility webservices to use with the system. They allow some additional information on the system to
be retrieved. They are optional in the current configuration.
 
### java-common
A library with generic Java utility classes.

### JavaCommonApi
Some interface classes that java-common uses, that need to be on the catalina class path and therefore cannot
be included in java-common proper.

### PE-common
This module contains a large amount of process managment code that is shared among clients and server.
This will only support jdk1.7 as it is shared across GWT, Android and Tomcat. The module contains
various wrappers (such as nl.adaptivity.xml.*) to help with cross-platform compatibility.

### PE-dataservices
This is (going to be) a web service that allows for auxiliary data storage and retrieval for use by
processes. This does not replace data storage on instances, but can be used for more complex, or process
independent data storage. For now, it does not actually have any implementation.

### PE-diagram
This module contains code related to process model diagrams. It also contains client-side extensions of the process
model element classes. For consistency, this module contains the drawing instructions for diagrams, where
platforms may inject their own strategies that do the actual drawing and/or measuring.

### PE-server
A module that represents a standard configuration of tomcat (tested with version 6) for the server. It contains
a gradle task (tomcatRun) to run a test server (assuming that a database server is running and configured).

### PEUserMessageHandler
This module has a double task (for now) in that it provides the server component for user interaction, as well
as a (very stale) GWT based web-interface.

### PMEditor
This module contains the android client. This client is both an editor, as well as a task interface.

### ProcessEngine
The module containing the actual process engine server component.