module security {
    exports com.udacity.security.service;
    opens com.udacity.security.data;
    exports com.udacity.security.application;
    requires java.xml;
    requires java.desktop;
    requires guava;
    requires java.prefs;
    requires miglayout;
    requires com.google.gson;
}