package org.jboss.jbossas.cdiTck.container;


import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.context.ContainerContext;
import org.jboss.arquillian.core.impl.loadable.LoadableExtensionLoader;
import org.jboss.arquillian.core.spi.Manager;
import org.jboss.arquillian.core.spi.ManagerBuilder;
import org.jboss.as.arquillian.container.managed.ManagedContainerConfiguration;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.testharness.api.DeploymentException;
import org.jboss.testharness.spi.Containers;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;


/**
 * Adaptor between an arquillian {@link DeployableContainer} and test harnesses
 * {@link Containers}
 *
 * @author Stuart Douglas
 */
public class ArquillianContainerAdaptor implements Containers {
    private DeployableContainer<ManagedContainerConfiguration> deployableContainer;
    private Container container;
    private ManagedContainerConfiguration configuration;
    private Archive<?> swArchive;
    private org.jboss.arquillian.container.spi.client.container.DeploymentException exception;
    private Manager manager;
    public void setup() throws IOException {
        deployableContainer = loadDeployableContainer();
        manager = (ManagerBuilder.from()).extension(LoadableExtensionLoader.class).create();
        manager.getContext(ContainerContext.class).activate("AS7 Managegd");

        configuration = new ManagedContainerConfiguration();
        manager.inject(deployableContainer);
        deployableContainer.setup(configuration);
        try {
            deployableContainer.start();
        } catch (LifecycleException e) {
            throw new RuntimeException(e);
        }
    }

    public void cleanup() throws IOException {
        try {
            deployableContainer.stop();
        } catch (LifecycleException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean deploy(InputStream archive, String name) throws IOException {
        exception = null;
        if (name.endsWith("ear")) {
            swArchive = ShrinkWrap.create(EnterpriseArchive.class, name);
        } else if (name.endsWith("war")) {
            swArchive = ShrinkWrap.create(WebArchive.class, name);
        } else if (name.endsWith("jar")) {
            swArchive = ShrinkWrap.create(JavaArchive.class, name);
        } else {
            throw new RuntimeException("Unkown archive extension: " + name);
        }
        swArchive.as(ZipImporter.class).importFrom(archive);
        try {
            deployableContainer.deploy(this.swArchive);
            return true;
        } catch (org.jboss.arquillian.container.spi.client.container.DeploymentException e) {
            exception = e;
            return false;
        }
    }

    public DeploymentException getDeploymentException() {
        return new DeploymentException(exception.getCause().getClass().getName(), exception.getCause());
    }

    public void undeploy(String name) throws IOException {
        try {
            deployableContainer.undeploy(swArchive);
        } catch (org.jboss.arquillian.container.spi.client.container.DeploymentException e) {
            throw new RuntimeException(e);
        }
    }

    private static final DeployableContainer loadDeployableContainer() {
        final String arquillianContainer = System.getProperty("org.jboss.har2arq.container");
        if (arquillianContainer != null) {
            try {
                Class<?> clazz = Class.forName(arquillianContainer.trim());
                return (DeployableContainer) clazz.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        // first we need to load the deployable containers
        final ServiceLoader<DeployableContainer> container = ServiceLoader.load(DeployableContainer.class);
        final Set<DeployableContainer> containers = new HashSet<DeployableContainer>();
        for (DeployableContainer aContainer : container) {
            containers.add(aContainer);
        }
        if (containers.isEmpty()) {
            throw new RuntimeException("No Arquillian DeployableContainer found on the class path.");
        }
        if (containers.size() > 1) {
            throw new RuntimeException("More than one DeployableContainer found on the class path. " + containers);
        }
        return containers.iterator().next();
    }
}
