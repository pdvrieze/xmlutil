module net.devrieze.xmlutil.serialization {
    requires transitive net.devrieze.xmlutil.core;
    requires transitive kotlinx.serialization.core;
    requires java.xml;

    exports nl.adaptivity.xmlutil.serialization;
    exports nl.adaptivity.xmlutil.serialization.structure;
}
