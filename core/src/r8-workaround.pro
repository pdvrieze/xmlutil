# Needed to work around a bug in r8 where a switch over these values triggers an r8 bug.
#noinspection ShrinkerUnresolvedReference
-keep enum nl.adaptivity.xmlutil.EventType { *;}
