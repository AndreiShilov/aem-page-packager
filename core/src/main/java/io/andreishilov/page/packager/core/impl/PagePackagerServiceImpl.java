package io.andreishilov.page.packager.core.impl;

import static io.andreishilov.page.packager.core.Constants.GROUP_PATH;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.reference.ReferenceProvider;
import com.google.common.collect.Sets;

import io.andreishilov.page.packager.core.PagePackagerService;

@Component(service = PagePackagerService.class, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class PagePackagerServiceImpl implements PagePackagerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PagePackagerServiceImpl.class);

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    @Reference
    private Packaging packaging;

    @Reference(
            service = ReferenceProvider.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC
    )
    private final List<ReferenceProvider> referenceProviders = new CopyOnWriteArrayList<>();


    @Override
    public Node createPackage(SlingHttpServletRequest request, Session serviceSession) throws IOException, RepositoryException, PackageException {

        final ResourceResolver resourceResolver = request.getResourceResolver();

        final JcrPackageManager packageManager = packaging.getPackageManager(serviceSession);

        final Set<com.day.cq.wcm.api.reference.Reference> references = getReferences(request);

        final LocalDateTime now = LocalDateTime.now();
        final String path = request.getResource().getPath();
        final String name = request.getResource().getName();

        final Node rootNode = getRootNode(serviceSession, resourceResolver.getUserID());
        final String packageName = name + "_" + now.format(DATE_TIME_FORMATTER);
        try (final JcrPackage jcrPackage = packageManager.create(rootNode, packageName)) {

            final DefaultWorkspaceFilter filter = new DefaultWorkspaceFilter();
            final PathFilterSet set = new PathFilterSet(path);

            filter.add(set);
            references.forEach(reference -> filter.add(new PathFilterSet(reference.getResource().getPath())));

            final JcrPackageDefinition definition = jcrPackage.getDefinition();

            if (definition == null) {
                final Node jcrPackageNode = jcrPackage.getNode();
                LOGGER.warn("Could not get package Definition. Package node path = [{}]", jcrPackageNode == null ? null : jcrPackageNode.getPath());
                return null;
            }

            definition.setFilter(filter, true);

            final String downloadName = jcrPackage.getPackage().getId().getDownloadName();
            LOGGER.debug("Starting to assemble package = [{}]", downloadName);

            packageManager.assemble(jcrPackage, new PagePackagerProgressTrackerListener());

            LOGGER.debug("Ending to assemble package = [{}]", downloadName);
            return jcrPackage.getNode();
        }

    }

    private Node getRootNode(Session session, String userID) throws RepositoryException {

        final Node packageGroupNode = session.getNode(GROUP_PATH);

        if (packageGroupNode.hasNode(userID)) {
            return packageGroupNode.getNode(userID);
        }

        return packageGroupNode.addNode(userID);
    }

    private Set<com.day.cq.wcm.api.reference.Reference> getReferences(SlingHttpServletRequest request) {
        final Resource child = request.getResource().getChild(JcrConstants.JCR_CONTENT);

        final Set<com.day.cq.wcm.api.reference.Reference> references = Sets.newHashSet();

        for (ReferenceProvider referenceProvider : referenceProviders) {
            references.addAll(referenceProvider.findReferences(child));
        }
        return references;
    }


    protected void bindReferenceProviders(ReferenceProvider referenceProvider) {
        LOGGER.debug("Reference Provide [{}] was added", referenceProvider.getClass().getCanonicalName());
        this.referenceProviders.add(referenceProvider);
    }

    protected void unbindReferenceProviders(ReferenceProvider referenceProvider) {
        LOGGER.debug("Reference Provide [{}] was removed", referenceProvider.getClass().getCanonicalName());
        this.referenceProviders.remove(referenceProvider);
    }

    private static class PagePackagerProgressTrackerListener implements ProgressTrackerListener {

        @Override
        public void onMessage(Mode mode, String action, String path) {
            LOGGER.debug("Mode = [{}], action = [{}], path = [{}]", mode, action, path);
        }

        //todo might be a good to throw a dedicated exception and abort package assembling
        @Override
        public void onError(Mode mode, String path, Exception e) {
            LOGGER.error("Error on path = [{}] with mode = [{}]", path, mode, e);
        }
    }
}
