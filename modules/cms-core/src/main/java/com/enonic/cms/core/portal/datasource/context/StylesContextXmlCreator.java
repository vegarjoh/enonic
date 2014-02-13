/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal.datasource.context;

import org.jdom.Element;

import com.enonic.cms.core.resource.ResourceFile;
import com.enonic.cms.core.resource.ResourceKey;
import com.enonic.cms.core.resource.ResourceService;

/**
 * Apr 21, 2009
 */
final class StylesContextXmlCreator
{
    private ResourceService resourceService;

    public StylesContextXmlCreator( ResourceService resourceService )
    {
        this.resourceService = resourceService;
    }

    public Element createStylesElement( ResourceKey[] cssKeys )
    {
        Element stylesEl = new Element( "styles" );

        for ( ResourceKey cssKey : cssKeys )
        {
            Element styleEl = new Element( "style" );
            stylesEl.addContent( styleEl );
            styleEl.setAttribute( "key", cssKey.toString() );
            styleEl.setAttribute( "name", cssKey.toString() );
            styleEl.setAttribute( "path", cssKey.toString() );
            styleEl.setAttribute( "type", "text/css" );

            ResourceFile cssResource = resourceService.getResourceFile( cssKey );
            if ( cssResource == null )
            {
                styleEl.setAttribute( "missing", "true" );
                continue;
            }
            String css = cssResource.getDataAsString();
            if ( css == null )
            {
                styleEl.setAttribute( "missing", "true" );
            }
        }

        return stylesEl;
    }
}
