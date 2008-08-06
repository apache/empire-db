package org.apache.empire.struts2.jsp.controls;

import java.util.HashMap;

public final class InputControlManager
{
    static HashMap<String, InputControl> controlMap = null;
    
    static {
        
        controlMap = new HashMap<String, InputControl>();
        
        registerControl("text",     new TextInputControl());
        registerControl("select",   new SelectInputControl());
        registerControl("checkbox", new CheckboxInputControl());
        registerControl("phone",    new PhoneInputControl());
        registerControl("radio",    new RadioInputControl());
        registerControl("textarea", new TextAreaInputControl());
        registerControl("email",    new EMailInputControl());
        registerControl("hlink",    new HLinkInputControl());
        registerControl("password", new PasswordInputControl());
    }
    
    private InputControlManager()
    {
        // Default Constructor
    }
    
    public static void registerControl(String name, InputControl control)
    {
        controlMap.put(name, control);
    }
    
    public static InputControl getControl(String name)
    {
        return controlMap.get(name);
    }
}
