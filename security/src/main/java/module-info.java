module security {
    requires image;
    requires java.desktop;
    requires miglayout;
    requires java.prefs;
    requires com.google.gson;
    requires com.google.common;

    opens data to com.google.gson;
}