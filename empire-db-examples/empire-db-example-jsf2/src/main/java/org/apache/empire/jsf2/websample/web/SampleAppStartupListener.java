package org.apache.empire.jsf2.websample.web;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

import org.apache.empire.jsf2.app.FacesConfiguration;
import org.apache.empire.jsf2.app.WebAppStartupListener;
import org.apache.empire.jsf2.app.WebApplication;
import org.apache.empire.jsf2.impl.FacesImplementation;

/**
 * Custom StartupListener
 * Faces Configuration is done programmatically 
 * @author rainer
 */
public class SampleAppStartupListener extends WebAppStartupListener
{
    public final SampleConfig config = new SampleConfig();
    
    public SampleAppStartupListener()
    {
        super(FacesConfiguration.class);
    }
    
    @Override
    protected void initFacesConfiguration(FacesContext startupContext, FacesImplementation facesImplementation)
    {
        ServletContext servletContext = (ServletContext)startupContext.getExternalContext().getContext();
        config.init(servletContext.getRealPath("WEB-INF/config.xml"));
        // Load Configuration
        super.initFacesConfiguration(startupContext, facesImplementation);
    }
    
    @Override
    protected void initWebApplication(WebApplication facesApp, FacesContext startupContext, FacesImplementation facesImplementation)
    {
        // Set Configuration
        ((SampleApplication)facesApp).setConfig(config);
        // init now
        super.initWebApplication(facesApp, startupContext, facesImplementation);
    }
}
