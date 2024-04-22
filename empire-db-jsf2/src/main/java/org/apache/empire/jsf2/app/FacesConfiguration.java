/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.empire.jsf2.app;

import java.util.Iterator;
import java.util.Map;

import javax.el.ELResolver;
import javax.faces.FactoryFinder;
import javax.faces.application.Application;
import javax.faces.application.NavigationHandler;
import javax.faces.application.ProjectStage;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.event.PhaseListener;
import javax.faces.lifecycle.Lifecycle;
import javax.faces.lifecycle.LifecycleFactory;
import javax.faces.render.RenderKit;
import javax.faces.render.RenderKitFactory;
import javax.faces.render.Renderer;

import org.apache.empire.commons.ClassUtils;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.exceptions.InternalException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.ItemExistsException;
import org.apache.empire.exceptions.ItemNotFoundException;
import org.apache.empire.exceptions.ObjectNotValidException;
import org.apache.empire.exceptions.UnspecifiedErrorException;
import org.apache.empire.jsf2.impl.FacesImplementation;
import org.apache.empire.jsf2.impl.FacesImplementation.BeanStorageProvider;
import org.apache.empire.jsf2.pages.PageNavigationHandler;
import org.apache.empire.jsf2.pages.PagePhaseListener;
import org.apache.empire.jsf2.pages.PagesELResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FacesConfiguration
 * Allows programmatic Faces configuration as an alternative to the faces-config.xml
 * In order to provide custom configuration you must override the class WebAppStartupListener
 * and put it in the faces-config.xml
 * <pre> 
 *   <system-event-listener-class>de.volkswagen.jsf.FacesStartupListener</system-event-listener-class>
 * </pre>
 * Further you must call the super constructor with a FacesConfiguration class like this:
 * <pre>
 *   public FacesStartupListener()
 *   {
 *      super(FacesExtensions.class);
 *   }
 * </pre> 
 */
public class FacesConfiguration
{
    protected static final Logger log = LoggerFactory.getLogger(FacesConfiguration.class);
    
    /*
     * Initialized
     */
    private static boolean initialized = false;
    public static boolean isInitialized()
    {
        return initialized; 
    }

    /*
     * Project Stage
     * see org.apache.myfaces.application.ApplicationImpl
     * How to set:
     *   set JVM-Parameter:     "org.apache.myfaces.PROJECT_STAGE"
     *   set JNDI-Parameter:    "java:comp/env/jsf/ProjectStage"
     *   set in web.inf:        
     *      <context-param>
     *          <param-name>javax.faces.PROJECT_STAGE</param-name>
     *          <param-value>Development</param-value>
     *      </context-param>
     */
    private static ProjectStage projectStage;
    public static ProjectStage getProjectStage()
    {
        if (projectStage==null)
            throw new ObjectNotValidException(FacesConfiguration.class, "Not Initialized");
        return projectStage;
    }
    
    /**
     * Static Initializer
     * @param clazz the configuration class
     * @param startupContext the startupContext
     */
    public static <T extends FacesConfiguration> void initialize(Class<T> configClass, FacesContext startupContext, FacesImplementation facesImpl)
    {
        if (initialized)
            throw new UnspecifiedErrorException("FacesConfiguration already initialized!"); 
        try
        { // Create Instance an initialize
            FacesConfiguration fConfig = configClass.newInstance();
            fConfig.facesImpl = facesImpl;
            fConfig.initialize(startupContext);
            initialized = true;
        }
        catch (InstantiationException | IllegalAccessException e)
        {
            throw new InternalException(e);
        }
    }

    /*
     * Inject
     */
    protected FacesImplementation facesImpl;
    /*
     * Temp Variables
     */
    protected ExternalContext externalContext;
    protected Application application;
    /*
     * Lazy
     */
    private BeanStorageProvider beanStorage; // call getBeanStorageProvider() 

    public FacesConfiguration()
    {
        // Nothing
    }

    public final void initialize(FacesContext startupContext)
    {
        try
        {   // Set temporary variables
            this.externalContext = startupContext.getExternalContext();
            this.application = startupContext.getApplication();
            this.beanStorage = null;

            projectStage = application.getProjectStage();
            log.info("Initializing Faces Configuration for ProjectStage {}", projectStage.name());
            
            // Init everything
            initAll(startupContext);

            // done
            log.info("Faces Configuration complete.");
        }
        finally
        {   // cleanup
            this.beanStorage = null;
            this.application = null;
            this.externalContext = null;
            this.facesImpl.configComplete();
        }
    }
    
    /*
     * Overrideable methods
     */
    
    protected void initAll(FacesContext context)
    {
        log.debug("Init FacesParams...");
        initFacesParams();

        log.debug("Init NavigationHandler...");
        initNavigationHandler();
        
        log.debug("Init ResourceHandler...");
        initResourceHandler();

        log.debug("Registrating Converters...");
        initConverters();

        log.debug("Registrating EL-Resolvers...");
        initElResolvers();
        
        log.debug("Registrating Lifecycle...");
        initLifecycle(new LifecycleUpdater());

        log.debug("Registrating Search Expression Resolvers...");
        initSearchExpressionResolvers();

        log.debug("Registrating Components...");
        initComponents();

        log.debug("Registrating Renderers...");
        initRenderers(new RenderKitUpdater(getApplicationRenderKit(context)));

        log.debug("Registrating Managed Beans...");
        initManagedBeans();            

        log.debug("Registrating Controls...");
        initControls();
    }

    protected void initFacesParams()
    {   // set params
        setFacesInitParam(ViewHandler.FACELETS_SKIP_COMMENTS_PARAM_NAME, true);
    }
    
    protected void initNavigationHandler()
    {
        NavigationHandler wrapped = application.getNavigationHandler();
        if (wrapped instanceof PageNavigationHandler)
            return; // Already set
        // replace
        log.info("Setting NavigationHandler to {}", PageNavigationHandler.class.getName());
        application.setNavigationHandler(new PageNavigationHandler(wrapped));
    }
    
    protected void initResourceHandler()
    {
        // Not implemented
        // application.setResourceHandler(new MyResourceHandler(wrapped));
    }

    protected void initConverters()
    {
        // Noting
    }

    protected void initElResolvers()
    {
        // add
        addELResolver(DBELResolver.class);
        addELResolver(PagesELResolver.class);
    }
    
    protected void initLifecycle(LifecycleUpdater lcu)
    {
        lcu.addPhaseListener(FacesRequestPhaseListener.class);
        lcu.addPhaseListener(PagePhaseListener.class);
    }

    protected void initSearchExpressionResolvers()
    {
        // Nothing
        // SearchExpressionResolverFactory.registerResolver("@fragment", new FragmentExpressionResolver());
    }

    protected void initComponents()
    {
        // Empire Components
        String EMPIRE_FAMILY = "org.apache.empire.component";
        addComponent(EMPIRE_FAMILY, org.apache.empire.jsf2.components.ControlTag.class);
        addComponent(EMPIRE_FAMILY, org.apache.empire.jsf2.components.InputTag.class);
        addComponent(EMPIRE_FAMILY, org.apache.empire.jsf2.components.FormGridTag.class);
        addComponent(EMPIRE_FAMILY, org.apache.empire.jsf2.components.LabelTag.class);
        addComponent(EMPIRE_FAMILY, org.apache.empire.jsf2.components.LinkTag.class);
        addComponent(EMPIRE_FAMILY, org.apache.empire.jsf2.components.MenuItemTag.class);
        addComponent(EMPIRE_FAMILY, org.apache.empire.jsf2.components.MenuListTag.class);
        addComponent(EMPIRE_FAMILY, org.apache.empire.jsf2.components.RecordTag.class);
        addComponent(EMPIRE_FAMILY, org.apache.empire.jsf2.components.SelectTag.class);
        addComponent(EMPIRE_FAMILY, org.apache.empire.jsf2.components.TabPageTag.class);
        addComponent(EMPIRE_FAMILY, org.apache.empire.jsf2.components.TabViewTag.class);
        addComponent(EMPIRE_FAMILY, org.apache.empire.jsf2.components.TitleTag.class);
        addComponent(EMPIRE_FAMILY, org.apache.empire.jsf2.components.UnitTag.class);
        addComponent(EMPIRE_FAMILY, org.apache.empire.jsf2.components.ValueTag.class);
    }

    protected void initRenderers(RenderKitUpdater rku)
    {
        // Noting
        // rku.replace("javax.faces.Input", "javax.faces.Text", FacesTextInputRenderer.class);
    }
    
    protected void initManagedBeans()
    {
        // Nothing
        // addManagedBean(ConfirmPopup.MANAGED_BEAN_NAME, ConfirmPopup.class, ConfirmPopup.MANAGED_BEAN_SCOPE);
    }

    protected void initControls()
    {
        // Not implemented
        // InputControlManager.registerControl(new CustomCheckboxInputControl());
    }

    /*
     *  Helpers 
     */
    
    protected BeanStorageProvider getBeanStorageProvider()
    {
        if (this.beanStorage==null)
            this.beanStorage = facesImpl.getBeanStorageProvider(externalContext);
        return this.beanStorage; 
    }
    
    protected RenderKit getApplicationRenderKit(FacesContext context)
    {
        String renderKitId = StringUtils.coalesce(application.getDefaultRenderKitId(), RenderKitFactory.HTML_BASIC_RENDER_KIT); 
        return ((RenderKitFactory)FactoryFinder.getFactory(FactoryFinder.RENDER_KIT_FACTORY)).getRenderKit(context, renderKitId);
    }
    
    protected void setFacesInitParam(String paramName, Object paramValue, boolean overwriteExisting)
    {
        // special case
        if (ProjectStage.PROJECT_STAGE_PARAM_NAME.equals(paramName))
            throw new UnspecifiedErrorException(ProjectStage.PROJECT_STAGE_PARAM_NAME+" cannot be changed!");
        // get map
        String paramVal = StringUtils.toString(paramValue);
        String orgValue = this.externalContext.getInitParameter(paramName);
        if (ObjectUtils.compareEqual(paramVal, orgValue))
            return; // No change
        if (ObjectUtils.isNotEmpty(orgValue) && !overwriteExisting)
        {   // Ingnore
            log.info("Ignoring FacesParam \"{}\" (\"{}\"). Keeping current value of \"{}\"", paramName, paramVal, orgValue);
            return;
        }
        if (orgValue!=null)
            log.info("Setting FacesParam \"{}\" to \"{}\". Original value was \"{}\"", paramName, paramVal, orgValue);
        else
            log.info("Setting FacesParam \"{}\" to \"{}\".", paramName, paramVal);
        // add to map
        @SuppressWarnings("unchecked")
        Map<String,String> paramMap = this.externalContext.getInitParameterMap();
        paramMap.put(paramName, paramVal);
    }

    protected void setFacesInitParam(String paramName, Object paramValue)
    {
        setFacesInitParam(paramName, paramValue, false);
    }
    
    protected void setFacesInitParam(Enum<?> paramName, Object paramValue)
    {
        setFacesInitParam(paramName.toString(), paramValue, false);
    }
    
    protected void addConverter(Class<?> targetClass, Class<? extends Converter> converterClass)
    {
        log.info("Adding Type-Converter for type \"{}\" using {}", targetClass.getName(), converterClass.getName());
        application.addConverter(targetClass, converterClass.getName());
    }

    protected void addComponent(String componentFamily, Class<? extends UIComponent> clazz)
    {
        String type = StringUtils.concat(componentFamily, ".", clazz.getSimpleName());
        log.info("Adding component type \"{}\" using {}", type, clazz.getName());
        application.addComponent(type, clazz.getName());
    }
    
    protected void addManagedBean(String beanName, Class<?> beanClass, String scope)
    {
        facesImpl.registerManagedBean(beanName, beanClass.getName(), scope);
    }

    protected void replaceComponent(Class<? extends UIComponent> primeComponent, Class<? extends UIComponent> overrideComponent)
    {
        String type = (String) ClassUtils.getFieldValue(primeComponent, null, "COMPONENT_TYPE", true);
        if (StringUtils.isEmpty(type))
            throw new InvalidArgumentException("primeComponent", primeComponent.getName());
        // check
        checkComponentTypeExists(type);
        log.info("Replacing component type \"{}\" with {}", type, overrideComponent.getName());
        application.addComponent(type, overrideComponent.getName());
    }
    
    protected void checkComponentTypeExists(String componentType)
    {
        Iterator<String> types = application.getComponentTypes();
        while (types.hasNext())
        {
            String type = types.next();
            if (componentType.equals(type))
                return; // found;
        }
        throw new ItemNotFoundException("Component-Type: "+componentType);
    }
    
    protected void addELResolver(Class<? extends ELResolver> resolverClass)
    {
        boolean added = facesImpl.registerElResolver(resolverClass);
        if (added)
            log.info("Adding FacesConfigElResolver {}", resolverClass.getName());
    }
    
    /*
     * list
     */
    protected void listCompoennts()
    {
        ConfigTypeList list = new ConfigTypeList("Component-Types");
        Iterator<String> types = application.getComponentTypes();
        while (types.hasNext())
        {
            String componentType = types.next();
            // log.info("Renderer-Family: {} Type {}", componentFamily, rendererType);
            list.addItem(componentType);
        }
        log.info(list.toString());
    }
    
    /**
     * RenderKitReplacer
     * @author doebele
     */
    protected class RenderKitUpdater
    {
        private final RenderKit renderKit;
        public RenderKitUpdater(RenderKit renderKit)
        {
            this.renderKit = renderKit;            
        }
        
        public RenderKit getRenderKit()
        {
            return renderKit;
        }

        public void listAll()
        {   // list all
            ConfigTypeList list = new ConfigTypeList("Renderer-Types");
            Iterator<String> families = renderKit.getComponentFamilies();
            while (families.hasNext())
            {
                String componentFamily = families.next();
                Iterator<String> types = renderKit.getRendererTypes(componentFamily);
                while (types.hasNext())
                {
                    String rendererType = types.next();
                    // log.info("Renderer-Family: {} Type {}", componentFamily, rendererType);
                    list.addItem(componentFamily, rendererType);
                }
            }
            log.info(list.toString());
        }

        public void add(String componentFamily, String rendererType, Class<? extends Renderer> rendererClass)
        {
            Renderer check = findRenderer(componentFamily, rendererType);
            if (check!=null)
            {   if (check.getClass().equals(rendererClass))
                    return; // already there
                // Another renderer exists
                throw new ItemExistsException(StringUtils.concat(componentFamily, " / ", rendererType));
            }
            // add
            log.info("Adding Renderer type \"{}\" using {}", rendererType, rendererClass.getName());
            renderKit.addRenderer(componentFamily, rendererType, ClassUtils.newInstance(rendererClass));
        }

        public void replace(String componentFamily, String rendererType, Class<? extends Renderer> replaceClass)
        {
            // checkRenderTypeExists(componentFamily, rendererType);
            Renderer check = findRenderer(componentFamily, rendererType);
            if (check==null)
                throw new ItemNotFoundException(StringUtils.concat(componentFamily, " / ", rendererType));
            if (check.getClass().equals(replaceClass))
                return; // Already replaced
            // replace
            log.info("Replacing Renderer type \"{}\" with class {}", rendererType, replaceClass.getName());
            renderKit.addRenderer(componentFamily, rendererType, ClassUtils.newInstance(replaceClass));
        }
        
        /*
        public void replace(Class<? extends CoreRenderer> orgClass, Class<? extends CoreRenderer> replaceClass)
        {
            String name = orgClass.getName();
            int sep = name.lastIndexOf('.');
            replace(name.substring(0, sep), name.substring(sep+1), replaceClass);
        }
        */
        
        private Renderer findRenderer(String componentFamily, String rendererType)
        {
            // list all
            Iterator<String> families = renderKit.getComponentFamilies();
            while (families.hasNext())
            {
                String family = families.next();
                if (componentFamily.equals(family)) {
                    Iterator<String> types = renderKit.getRendererTypes(family);
                    while (types.hasNext())
                    {
                        String type = types.next();
                        if (rendererType.equals(type))
                            return renderKit.getRenderer(componentFamily, rendererType); // found
                    }
                }
            }
            return null;
        }
        
    }
    
    /**
    * LifecycleUpdater
    * @author doebele
    */
    protected class LifecycleUpdater
    {
        private final Lifecycle lifecycle;
        private PhaseListener[] phaseListeners;

        public LifecycleUpdater()
        {
           // The DEFAULT Lifecycle
           LifecycleFactory lifecycleFactory = (LifecycleFactory) FactoryFinder.getFactory(FactoryFinder.LIFECYCLE_FACTORY); 
           this.lifecycle = lifecycleFactory.getLifecycle(LifecycleFactory.DEFAULT_LIFECYCLE);
           this.phaseListeners = lifecycle.getPhaseListeners();
        }

        public void listAll()
        {
            ConfigTypeList list = new ConfigTypeList("Phase-Listeners");
            this.phaseListeners = lifecycle.getPhaseListeners();
            for (PhaseListener pl : phaseListeners)
            {
                list.addItem(pl.getClass(), pl.getPhaseId());
            }
            log.info(list.toString());
        }

        public void addPhaseListener(Class<? extends PhaseListener> phaseListenerClass)
        {
            for (PhaseListener pl : phaseListeners)
            {
                if (pl.getClass().equals(phaseListenerClass))
                    return; // already there
            }
            // Not found: Create and Append
            log.info("Adding Lifecycle PhaseListener {}", phaseListenerClass.getName());
            PhaseListener listener = ClassUtils.newInstance(phaseListenerClass);
            // Add to bean storage
            getBeanStorageProvider().injectBean(listener);
            // Add to lifecycle
            lifecycle.addPhaseListener(listener);
            // refresh
            // this.phaseListeners = lifecycle.getPhaseListeners();
        }
    }

    /**
     * ConfigTypeList
     * @author doebele
     */
    protected static class ConfigTypeList
    {
        private static final String CRLF = "\r\n";
        private static final String TAB = "\t";
        private final StringBuilder b;
        public ConfigTypeList(String listName)
        {
            this.b = new StringBuilder(200);
            b.append(listName);
            b.append(":");
            b.append(CRLF);
        }
        public void addItem(Object item, Object... more)
        {
            b.append(TAB);
            b.append(toString(item));
            for (int i=0; i<more.length; i++)
            {
                b.append(TAB);
                b.append(toString(more[i]));
            }
            b.append(CRLF);
        }
        protected String toString(Object o)
        {
            if (o instanceof Class<?>)
                return ((Class<?>)o).getName();
            if (o instanceof Enum<?>)
                return ((Enum<?>)o).name();
            return String.valueOf(o);
        }
        @Override
        public String toString()
        {
           return this.b.toString();
        }
    }

}
