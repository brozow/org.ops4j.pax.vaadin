/*
 * Copyright 2012 Achim Nierbeck.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.vaadin.internal.extender;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.http.HttpServlet;

import org.ops4j.pax.vaadin.VaadinResourceService;
import org.ops4j.pax.vaadin.internal.servlet.VaadinApplicationServlet;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.BundleTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.Application;

public class PaxVaadinBundleTracker extends BundleTracker {

    public static final String ALIAS = "alias";
    private static final String VAADIN_PATH = "/VAADIN";
    private final Logger logger = LoggerFactory.getLogger(PaxVaadinBundleTracker.class.getName());
    private final Map<Bundle, ServiceRegistration> registeredServlets = new HashMap<Bundle, ServiceRegistration>();

    public PaxVaadinBundleTracker(final BundleContext context) {
        super(context, Bundle.ACTIVE, null);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Object addingBundle(final Bundle bundle, final BundleEvent event) {

        if (isApplicationBundle(bundle)) {
            logger.debug("found a vaadin-app bundle: {}", bundle);
            final String applicationClass = (String) bundle.getHeaders().get(org.ops4j.pax.vaadin.Constants.VAADIN_APPLICATION);
            final String alias = (String) bundle.getHeaders().get("Vaadin-Alias");
            Application application = null;
            try {
                final Class appClazz = bundle.loadClass(applicationClass);

                final Constructor[] ctors = appClazz.getDeclaredConstructors();
                Constructor ctor = null;
                for (int i = 0; i < ctors.length; i++) {
                    ctor = ctors[i];
                    if (ctor.getGenericParameterTypes().length == 0)
                        break;
                }
                ctor.setAccessible(true);
                application = (Application) ctor.newInstance();

            } catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (SecurityException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InstantiationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            final String widgetset = findWidgetset(bundle);

            if (application != null) {
                final VaadinApplicationServlet servlet = new VaadinApplicationServlet(application);

                final Map<String, Object> props = new Hashtable<String, Object>();
                props.put(ALIAS, alias);

                if (widgetset != null) {
                    props.put("widgetset", widgetset);
                }

                final ServiceRegistration registeredServlet = bundle.getBundleContext().registerService(HttpServlet.class.getName(), servlet, (Dictionary) props);
                registeredServlets.put(bundle, registeredServlet);
            }

        }

        if (isThemeBundle(bundle)) {
            logger.debug("found a vaadin-resource bundle: {}", bundle);
            // TODO do VAADIN Themes handling
            final ServiceReference serviceReference = bundle.getBundleContext().getServiceReference(VaadinResourceService.class.getName());
            final VaadinResourceService service = (VaadinResourceService) bundle.getBundleContext().getService(serviceReference);
            service.addResources(bundle);
        }

        return super.addingBundle(bundle, event);
    }

    @SuppressWarnings("rawtypes")
    protected String findWidgetset(final Bundle bundle) {
        final Enumeration widgetEntries = bundle.findEntries("", "*.gwt.xml", true);
        if (widgetEntries == null || !widgetEntries.hasMoreElements()) return null;

        final URL widgetUrl = (URL) widgetEntries.nextElement();
        String path = widgetUrl.getPath();
        path = path.substring(1, path.length() - 8);
        path = path.replace("/", ".");
        return path;
    }

    @Override
    public void removedBundle(final Bundle bundle, final BundleEvent event, final Object object) {
        final ServiceRegistration registeredServlet = registeredServlets.get(bundle);
        if (registeredServlet != null) registeredServlet.unregister();

        if (isThemeBundle(bundle)) {
            logger.debug("found a vaadin-resource bundle: {}", bundle);
            // TODO do VAADIN Themes handling
            final ServiceReference serviceReference = bundle.getBundleContext().getServiceReference(VaadinResourceService.class.getName());
            final VaadinResourceService service = (VaadinResourceService) bundle.getBundleContext().getService(serviceReference);
            service.removeResources(bundle);
        }

        super.removedBundle(bundle, event, object);
    }

    private boolean isApplicationBundle(final Bundle bundle) {
        if (!isVaadinBundle(bundle)) return false;

        final String applicationClass = (String) bundle.getHeaders().get(org.ops4j.pax.vaadin.Constants.VAADIN_APPLICATION);

        if (applicationClass != null && !applicationClass.isEmpty()) return true;

        return false;
    }

    @SuppressWarnings("rawtypes")
    private boolean isThemeBundle(final Bundle bundle) {
        if ("com.vaadin".equals(bundle.getSymbolicName())) return false;

        final Enumeration vaadinPaths = bundle.getEntryPaths(VAADIN_PATH);

        if (vaadinPaths == null || !vaadinPaths.hasMoreElements()) return false;

        return true;
    }

    private boolean isVaadinBundle(final Bundle bundle) {
        final String importedPackages = (String) bundle.getHeaders().get(Constants.IMPORT_PACKAGE);
        if (importedPackages == null) return false;
        if (importedPackages.contains("com.vaadin")) return true;

        return false;
    }
}
