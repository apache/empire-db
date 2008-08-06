/*
 * ESTEAM Software GmbH, 02.07.2008
 */
package org.apache.empire.data.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * BeanDomain
 * This class defines Metadata for a domain of java classes.
 * Create a new class for your domain metadata and derive it from BeanDomain.
 * Then create a BeanClass subclass (preferably as a nested class) for each of your domain objects.
 * A metadata defintion consitst primarily of the domain name and a list of classes.  
 * @author Rainer
 */
public abstract class BeanDomain
{
    private String name;
    private List<BeanClass> classes = new ArrayList<BeanClass>();
    
    protected BeanDomain(String name)
    {
        this.name = name;
    }

    protected void addClass(BeanClass beanClass)
    {
        classes.add(beanClass);
        beanClass.domain = this;
    }
    
    public String getName()
    {
        return name;
    }
    
    public List<BeanClass> getClasses()
    {
        return classes;
    }
    
}
