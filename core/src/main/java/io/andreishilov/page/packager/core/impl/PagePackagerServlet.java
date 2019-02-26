/*
 *  Copyright 2015 Adobe Systems Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.andreishilov.page.packager.core.impl;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.andreishilov.page.packager.core.PagePackagerService;

/**
 * Servlet that writes some sample content into the response. It is mounted for
 * all resources of a specific Sling resource type. The
 * {@link SlingSafeMethodsServlet} shall be used for HTTP methods that are
 * idempotent. For write operations use the {@link SlingAllMethodsServlet}.
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "= Fast packager Servlet",
                "sling.servlet.methods=" + HttpConstants.METHOD_GET,
                "sling.servlet.resourceTypes=" + "cq/Page",
                "sling.servlet.selectors=" + "pagepackager",
                "sling.servlet.extensions=" + "zip"
        }, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class PagePackagerServlet extends SlingSafeMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(PagePackagerServlet.class);
    private static final String APPLICATION_ZIP = "application/zip";
    private static final String CONTENT_DISPOSITION = "Content-Disposition";

    @Reference
    private transient PagePackagerService pagePackagerService;

    @Reference
    private transient ResourceResolverFactory factory;

    @Override
    protected void doGet(final SlingHttpServletRequest req,
                         final SlingHttpServletResponse resp) throws IOException {

        try (final ResourceResolver serviceResourceResolver = factory.getServiceResourceResolver(null)) {

            final Session serviceSession = serviceResourceResolver.adaptTo(Session.class);
            final Node packageNode = pagePackagerService.createPackage(req, serviceSession);

            try (final InputStream inputStream = JcrUtils.readFile(packageNode)) {

                if (inputStream != null) {
                    resp.setContentType(APPLICATION_ZIP);
                    final String packageDownloadName = packageNode.getName();
                    resp.setHeader(CONTENT_DISPOSITION, "attachment;filename=page-packager_" + packageDownloadName);

                    ServletOutputStream sos = resp.getOutputStream();
                    sos.write(IOUtils.toByteArray(inputStream));
                }
            }

        } catch (RepositoryException | PackageException | LoginException e) {
            LOGGER.error(e.getMessage(), e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(e.toString());
        }

    }
}

