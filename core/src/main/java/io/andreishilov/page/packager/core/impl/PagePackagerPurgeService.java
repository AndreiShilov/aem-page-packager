package io.andreishilov.page.packager.core.impl;

import static io.andreishilov.page.packager.core.Constants.GROUP_PATH;

import java.time.LocalDateTime;
import java.util.Iterator;

import javax.jcr.query.Query;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Designate(ocd = PagePackagerPurgeService.Config.class)
@Component(service = Runnable.class, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class PagePackagerPurgeService implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(PagePackagerPurgeService.class);

    private static final String QUERY_TEMPLATE = "SELECT * FROM [nt:file] AS s WHERE ISDESCENDANTNODE([" + GROUP_PATH + "]) "
            + "AND s.[jcr:created] <= CAST('%s' AS DATE) "
            + "AND s.[jcr:path] like '" + GROUP_PATH + "/%/%'";

    @ObjectClassDefinition(name = "Page Packager scheduled task",
            description = "Page Packager scheduled task to purge old packages")
    public @interface Config {

        @AttributeDefinition(name = "Cron-job expression")
        String scheduler_expression() default "0 0 0 1/1 * ? *";

        @AttributeDefinition(name = "Days thresh hold")
        int days() default 1;
    }

    @Reference
    private ResourceResolverFactory factory;

    private Config config;

    @Activate
    @Modified
    public void activate(Config config) {
        this.config = config;
        this.run();
        LOGGER.info("PagePackagerPurgeService activated");
    }


    @Override
    public void run() {
        LOGGER.info("PagePackagerPurgeService.run() started");

        try (ResourceResolver resolver = factory.getServiceResourceResolver(null)) {

            final LocalDateTime now = LocalDateTime.now();
            //todo naming
            final LocalDateTime deadline = now.minusDays(config.days());

            final Iterator<Resource> resourcesToDelete = resolver.findResources(String.format(QUERY_TEMPLATE, deadline.toString()), Query.JCR_SQL2);

            while (resourcesToDelete.hasNext()) {
                final Resource next = resourcesToDelete.next();
                final String resourcePath = next.getPath();

                if (!isSnapshotFolder(resourcePath)) {
                    LOGGER.info("Resource to delete = [{}]", resourcePath);

                    try {
                        resolver.delete(next);
                    } catch (PersistenceException e) {
                        LOGGER.warn("Could not delete resource = [{}]. Moving to the next resource", resourcePath);
                    }
                }
            }

            resolver.commit();
        } catch (LoginException | PersistenceException e) {
            LOGGER.error("Could not purge old packages.", e);
        }

        LOGGER.info("PagePackagerPurgeService.run() ended");
    }

    private boolean isSnapshotFolder(String resourcePath) {
        return resourcePath.startsWith(GROUP_PATH + "/.snapshot");
    }


}
