# Installing PMI
The configuration is based upon usage in Tomcat 6.0.x. It may work in other versions of tomcat, and mostly the code
is not dependent on any specifics of tomcat.

## Shared classpath
- mysql driver (needed for DarwinRealm, as well as the individual components) mysql:mysql-connector-java
- DarwinRealm (the shared authentication valve)
- JavaCommonApi (Some classes needed for DarwinJava api)
- DarwinJavaApi (Some shared interfaces - including the messenger ones) that allow efficient transfer of
  messages between components.

## Components
- ProcessEngine - The actual process engine
- PEUserMessageHandler - The component that actually handles user interaction.

## Environment
- Create the database(s) and tables as defined in darwin-sql. 
- The configuration can be found in the PE-server module. Make sure to update the context.xml files to have passwords
  mapping to your own database. (The ones provided here are not secure and should not be used for non-development, 
  non-localhost contexts.