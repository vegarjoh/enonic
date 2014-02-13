/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.image.filter.command;

import com.jhlabs.image.GaussianFilter;

import com.enonic.cms.core.image.filter.BuilderContext;

public final class BlurFilterCommand
    extends FilterCommand
{
    public BlurFilterCommand()
    {
        super( "blur" );
    }

    protected Object doBuild( BuilderContext context, Object[] args )
    {
        GaussianFilter filter = new GaussianFilter();
        filter.setRadius( getIntArg( args, 0, 2 ) );
        return filter;
    }
}
