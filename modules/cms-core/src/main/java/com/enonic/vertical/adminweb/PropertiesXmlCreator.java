/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.vertical.adminweb;

import org.jdom.Element;

import java.util.Map;
import java.util.Properties;

public class PropertiesXmlCreator
{

    public Element createElement( String elementName, String childName, Properties properties )
    {
        Element el = new Element( elementName );

        for ( Object key : properties.keySet() )
        {
            el.addContent( createElement( childName, (String) key, (String) properties.get( key ) ) );
        }

        return el;
    }


    public Element createElement( String elementName, String childName, Map<Object, Object> properties )
    {
        Element el = new Element( elementName );

        for ( Object key : properties.keySet() )
        {
            el.addContent( createElement( childName, (String) key, (String) properties.get( key ) ) );
        }

        return el;
    }

    private Element createElement( String name, String key, String value )
    {
        Element el = new Element( name );
        el.setAttribute( "name", key );
        el.setAttribute( "value", value );

        return el;
    }

}
