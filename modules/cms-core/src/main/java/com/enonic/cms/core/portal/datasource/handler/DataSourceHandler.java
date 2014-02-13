/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.portal.datasource.handler;

import org.jdom.Document;
import org.springframework.beans.factory.annotation.Autowired;

import com.enonic.cms.core.portal.datasource.service.DataSourceService;

public abstract class DataSourceHandler
{
    protected DataSourceService dataSourceService;

    private final String name;

    public DataSourceHandler( final String name )
    {
        this.name = name;
    }

    public final String getName()
    {
        return this.name;
    }

    public abstract Document handle( final DataSourceRequest req )
        throws Exception;

    @Autowired
    public void setDataSourceService( final DataSourceService dataSourceService )
    {
        this.dataSourceService = dataSourceService;
    }
}
