/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.config;

import java.util.Map;
import java.util.Properties;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;

public final class ConfigProperties
    extends Properties
{
    public Map<String, String> getMap()
    {
        return Maps.fromProperties( this );
    }

    public Map<String, String> getSubMap( final Predicate<String> predicate )
    {
        return Maps.filterKeys( getMap(), predicate );
    }

}
