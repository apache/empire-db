package org.apache.empire.struts2.jsp.components;

import java.io.Writer;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.Options;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.struts2.html.HtmlWriter;
import org.apache.empire.struts2.jsp.controls.InputControl;
import org.apache.struts2.components.UIBean;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.LocaleProvider;
import com.opensymphony.xwork2.TextProvider;
import com.opensymphony.xwork2.util.ValueStack;


public abstract class ControlComponent extends UIBean implements InputControl.ValueInfo
{
    // Logger
    protected static Log log = LogFactory.getLog(ControlComponent.class);

    // Properties
    protected Column     column;
    protected Options    options;
    protected Object     recordValue;
    protected Object     nullValue;
    protected String     bodyUsage;
    protected String     format;

    // The input control
    private InputControl control;                                        // the control
    private TextProvider textProvider;

    protected ControlComponent(InputControl control, ValueStack stack, HttpServletRequest req, HttpServletResponse res)
    {
        super(stack, req, res);
        // set the control
        this.control = control;
    }

    @Override
    protected String getDefaultTemplate()
    {
        return null;
    }

    @Override
    public boolean start(Writer writer)
    {
        return super.start(writer);
    }

    @Override
    public final boolean end(Writer writer, String body)
    {
        // evaluateParams();
        try
        { // No Value
            if (recordValue == ObjectUtils.NO_VALUE)
                return false;

            // Render value
            HtmlWriter hw = new HtmlWriter(writer);
            render(hw, body, control);

            return false; // do not evaluate body again!

        } catch (Exception e)
        {
            log.error("error when rendering", e);
            return false; // do not evaluate body again!
        } finally
        {
            popComponentStack();
        }
    }

    protected Object getAction()
    {
        try
        {
            return ActionContext.getContext().getActionInvocation().getAction();
        } catch (Exception e)
        {
            log.fatal("Unable to detect Action. Action Invocation not available!");
            return "";
        }
    }

    private TextProvider getTextProvider(Object action)
    {
        if (action instanceof TextProvider)
            return ((TextProvider) action);
        // Error
        return null;
    }

    public Locale getUserLocale()
    {
        Object action = getAction();
        if (action instanceof LocaleProvider)
            return ((LocaleProvider) action).getLocale();
        // Default Locale
        return ActionContext.getContext().getLocale();
    }

    public final String getTranslation(String text)
    {
        if (text == null || text.length() == 0 || !text.startsWith("!"))
            return text;
        // Get key. If key starts with ! then return key
        String key = text.substring(1);
        if (key.startsWith("!"))
            return key;
        // Get Text Provider
        if (textProvider == null)
        {
            textProvider = getTextProvider(getAction());
            if (textProvider == null)
            { // Text Provider found
                log.error("No Text Provider available for control component");
                return "[" + key + "]";
            }
        }
        // Tranlate text
        String result = textProvider.getText(key);
        if (result == null)
        { // no Translation found
            log.error("No translation found for key=" + key);
            return "[" + key + "]";
        }
        // done
        return result;
    }

    protected abstract void render(HtmlWriter hw, String body, InputControl control);

    @Override
    public boolean usesBody()
    {
        return StringUtils.isValid(bodyUsage);
    }

    public void setColumn(Column column)
    {
        this.column = column;
    }

    public void setOptions(Options options)
    {
        this.options = options;
    }

    public void setRecordValue(Object recordValue)
    {
        this.recordValue = recordValue;
    }

    public void setBodyUsage(String bodyUsage)
    {
        this.bodyUsage = bodyUsage;
    }

    public void setFormat(String format)
    {
        this.format = format;
    }

    // InputControl.ValueInfo

    @Override
    public String getId()
    {
        return id;
    }

    public final Column getColumn()
    {
        return column;
    }

    public final Options getOptions()
    {
        return options;
    }

    public final Object getValue()
    {
        return recordValue;
    }

    public final Object getNullValue()
    {
        return nullValue;
    }

    public final String getCssClass()
    {
        return cssClass;
    }

    public final String getCssStyle()
    {
        return cssStyle;
    }

    public final String getOnclick()
    {
        return onclick;
    }

    public final String getOndblclick()
    {
        return ondblclick;
    }

    public final String getFormat()
    {
        return format;
    }

    public void setNullValue(Object nullValue)
    {
        this.nullValue = nullValue;
    }
}
