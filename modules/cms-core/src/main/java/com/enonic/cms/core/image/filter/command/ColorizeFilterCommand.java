/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.image.filter.command;

import com.enonic.cms.core.image.filter.BuilderContext;
import com.enonic.cms.core.image.filter.effect.ColorizeFilter;

public final class ColorizeFilterCommand
    extends FilterCommand
{
    public ColorizeFilterCommand()
    {
        super( "colorize" );
    }

    protected Object doBuild( BuilderContext context, Object[] args )
    {
        float r = getFloatArg( args, 0, 1f );
        float g = getFloatArg( args, 1, 1f );
        float b = getFloatArg( args, 2, 1f );
        return new ColorizeFilter( r, g, b );
    }
}