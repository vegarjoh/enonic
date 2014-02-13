/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.image.filter.command;

import com.enonic.cms.core.image.filter.BuilderContext;
import com.enonic.cms.core.image.filter.effect.RoundedFilter;

public final class RoundedFilterCommand
    extends FilterCommand
{
    public RoundedFilterCommand()
    {
        super( "rounded" );
    }

    protected Object doBuild( BuilderContext context, Object[] args )
    {
        return new RoundedFilter( getIntArg( args, 0, 10 ), getIntArg( args, 1, 0 ), getIntArg( args, 2, 0x000000 ) );
    }
}
