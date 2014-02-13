/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.portal.datasource.handler;

import java.util.Map;

import com.google.common.collect.Maps;

import com.enonic.cms.core.portal.datasource.DataSourceContext;

public final class DataSourceRequest
    extends DataSourceContext
{
    private String name;

    private boolean cache;

    private final Map<String, String> paramMap;

    public DataSourceRequest()
    {
        this.paramMap = Maps.newHashMap();
    }

    public String getName()
    {
        return this.name;
    }

    public void setName( final String name )
    {
        this.name = name;
    }

    public Map<String, String> getParams()
    {
        return this.paramMap;
    }

    public void addParam( final String name, final String value )
    {
        this.paramMap.put( name, value );
    }

    public boolean isCache()
    {
        return cache;
    }

    public void setCache( final boolean cache )
    {
        this.cache = cache;
    }
}
