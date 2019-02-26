package io.andreishilov.page.packager.core;

public class Constants {

    private Constants() {
        throw new UnsupportedOperationException();
    }

    private static final String PACKAGE_GROUP = "page-packager";
    private static final String PACKAGES_ROOT = "/etc/packages";
    public static final String GROUP_PATH = PACKAGES_ROOT + "/" + PACKAGE_GROUP;

}
