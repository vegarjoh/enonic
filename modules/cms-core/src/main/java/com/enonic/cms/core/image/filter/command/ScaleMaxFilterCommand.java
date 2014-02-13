/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.image.filter.command;

import com.enonic.cms.core.image.filter.BuilderContext;
import com.enonic.cms.core.image.filter.effect.ScaleMaxFilter;

public final class ScaleMaxFilterCommand
    extends FilterCommand
{
    public ScaleMaxFilterCommand()
    {
        super( "scalemax" );
    }

    protected Object doBuild( BuilderContext context, Object[] args )
    {
        return new ScaleMaxFilter( getIntArg( args, 0, 0 ) );
    }
}