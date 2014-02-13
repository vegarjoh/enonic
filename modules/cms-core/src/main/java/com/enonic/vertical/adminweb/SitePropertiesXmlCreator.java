/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.vertical.adminweb;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.jdom.Element;

import com.enonic.cms.core.structure.SitePropertyNames;

public class SitePropertiesXmlCreator
{
    private static final Set<String> keys = new HashSet<String>();

    static
    {
        for ( final SitePropertyNames name : SitePropertyNames.values() )
        {
            keys.add( name.getKeyName() );
        }
    }

    public Element createElement( final String elementName, final String childName, final Properties properties )
    {
        final Element el = new Element( elementName );

        for ( final String key : properties.stringPropertyNames() )
        {
            el.addContent( createElement( childName, key, properties.getProperty( key ), properties.keySet().contains( key ) ) );
        }

        return el;
    }

    private Element createElement( final String name, final String key, final String value, final boolean siteValue )
    {
        final boolean knownProperty = keys.contains( key ) || key.startsWith( SitePropertyNames.SITE_PROPERTY_CAPTCHA_ENABLE.getKeyName() );

        final String color = siteValue ? knownProperty ? "blue" : "green" : "black";

        final Element el = new Element( name );
        el.setAttribute( "name", key );
        el.setAttribute( "value", value );
        el.setAttribute( "color", color );
        return el;
    }

}
