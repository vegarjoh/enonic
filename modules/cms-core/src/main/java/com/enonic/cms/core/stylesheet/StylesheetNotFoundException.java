/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.stylesheet;

import com.enonic.cms.core.resource.ResourceKey;


public class StylesheetNotFoundException
    extends RuntimeException
{

    private ResourceKey stylesheetKey;

    public StylesheetNotFoundException( ResourceKey stylesheetKey )
    {
        super( "Stylesheet not found: " + stylesheetKey );
        this.stylesheetKey = stylesheetKey;
    }

    public ResourceKey getStylesheetKey()
    {
        return stylesheetKey;
    }
}
