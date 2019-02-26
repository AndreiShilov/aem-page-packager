package io.andreishilov.page.packager.core;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.sling.api.SlingHttpServletRequest;

public interface PagePackagerService {

    Node createPackage(SlingHttpServletRequest request, Session serviceSession) throws IOException, RepositoryException, PackageException;

}
