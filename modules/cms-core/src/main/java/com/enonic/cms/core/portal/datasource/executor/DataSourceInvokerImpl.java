/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.portal.datasource.executor;

import java.util.Map;

import org.jdom.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import com.enonic.cms.core.portal.datasource.DataSourceException;
import com.enonic.cms.core.portal.datasource.handler.DataSourceHandler;
import com.enonic.cms.core.portal.datasource.handler.DataSourceRequest;

@Component
public final class DataSourceInvokerImpl
    implements DataSourceInvoker
{
    private final Map<String, DataSourceHandler> handlers;

    public DataSourceInvokerImpl()
    {
        this.handlers = Maps.newHashMap();
    }

    public Document execute( final DataSourceRequest req )
        throws DataSourceException
    {
        final DataSourceHandler handler = getHandler( req.getName() );

        try
        {
            return handler.handle( req );
        }
        catch ( final DataSourceException e )
        {
            throw e;
        }
        catch ( final Exception e )
        {
            throw new DataSourceException( "Error invoking data source [{0}]", req.getName() ).withCause( e );
        }
    }

    private DataSourceHandler getHandler( final String name )
    {
        final DataSourceHandler handler = this.handlers.get( name );
        if ( handler != null )
        {
            return handler;
        }

        throw new DataSourceException( "Unknown data source by name [{0}]", name );
    }

    @Autowired
    public void setHandlers( final DataSourceHandler... handlers )
    {
        for ( final DataSourceHandler handler : handlers )
        {
            this.handlers.put( handler.getName(), handler );
        }
    }
}
