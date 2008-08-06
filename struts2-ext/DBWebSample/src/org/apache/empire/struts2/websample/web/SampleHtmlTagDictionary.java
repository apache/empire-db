package org.apache.empire.struts2.websample.web;

import org.apache.empire.struts2.html.DefaultHtmlTagDictionary;

public class SampleHtmlTagDictionary extends DefaultHtmlTagDictionary
{
    // ------- Input Control -------

    @Override
    public boolean InputReadOnlyAsData()
    {
        return false; // Show Read Only Input as Data (not as disabled input)
    }
    
    @Override
    public int InputMaxCharSize()
    {
        return 40; // Maximum horizontal size in characters
    }

    @Override
    public String InputWrapperTag()
    {
        return "tr";
    }

    @Override
    public String InputLabelTag()
    {
        return "td";
    }

    @Override
    public String InputControlTag()
    {
        return "td";
    }

    // ------- Errors -------

    @Override
    public String ErrorItemEntryClass()
    {
        return "errorMessage";
    }

    @Override
    public String ErrorActionEntryClass()
    {
        return "errorMessage";
    }

    @Override
    public String ErrorEntryWrapperTag()
    {
        return "span";
    }

    // ------- Message -------

    @Override
    public String MessageTag()
    {
        return "div";
    }

    @Override
    public String MessageClass()
    {
        return "actionMessage";
    }

}
