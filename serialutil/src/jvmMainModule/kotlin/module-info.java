module net.devrieze.serialutil {
    requires transitive kotlin.stdlib;
    requires kotlinx.serialization.core;

    exports nl.adaptivity.serialutil;
    exports nl.adaptivity.serialutil.encoders;
}
