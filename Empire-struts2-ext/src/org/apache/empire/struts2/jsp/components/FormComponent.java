package org.apache.empire.struts2.jsp.components;

import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.struts2.html.HtmlWriter;
import org.apache.empire.struts2.html.HtmlWriter.HtmlTag;
import org.apache.struts2.components.Form;

import com.opensymphony.xwork2.util.ValueStack;


public class FormComponent extends Form
{
    // Logger
    protected static Log log = LogFactory.getLog(ControlComponent.class);
    
    // Internal use only
    private HtmlTag formTag = null;
    private boolean readOnly = false;

    /*
    <form id="login" name="login" onsubmit="return true;" action="/dbsample/login!doLogin.action;jsessionid=5A7C79EFBDCEB97C83918726B7D7EC3D" method="post">
    <table class="wwFormTable">
    */
    
    public FormComponent(ValueStack stack, HttpServletRequest request, HttpServletResponse response)
    {
        super(stack, request, response);
    }

    @Override
    public boolean start(Writer writer)
    {
        evaluateParams(); // We need to call this!
        try {
            
            HtmlWriter htmlWriter = new HtmlWriter(writer);

            // render form Tag?
            if (readOnly==false)
            {
                formTag = htmlWriter.startTag("form");
                formTag.addAttribute("id",       this.getId());
                formTag.addAttribute("name",     this.name);
                formTag.addAttribute("onsubmit", this.onsubmit);
                formTag.addAttribute("action",   getURL(action));
                formTag.addAttribute("method",   this.method);
                formTag.addAttribute("enctype",  this.enctype);
                formTag.beginBody(true);
            }
            
        } catch (Exception e) {
            log.error("error when rendering", e);
        }
        return true;
    }
    
    @Override
    public boolean end(Writer writer, String body)
    {
        // super.end(writer, body);
        // evaluateParams();
        try {
            
            if (formTag!=null)
                formTag.endTag(true);
            
            return false; // do not evaluate body again!
            
        } catch (Exception e) {
            log.error("error when rendering", e);
            return false; // do not evaluate body again!
        }
        finally {
            popComponentStack();
        }
    }

    private String getURL(String action)
    {
        String namespace = null;
        String method = null;
        String scheme = null;
        boolean includeContext = true;
        boolean encodeResult = true;
        return this.determineActionURL(action, namespace, method, request, response, null, scheme, includeContext, encodeResult);
    }

    public void setReadOnly(boolean readOnly)
    {
        this.readOnly = readOnly;
    }
    
}
