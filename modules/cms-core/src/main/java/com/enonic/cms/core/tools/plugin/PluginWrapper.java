/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.tools.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.joda.time.format.DateTimeFormatterBuilder;

import com.enonic.cms.core.plugin.PluginHandle;

public final class PluginWrapper
{
    private final PluginHandle plugin;

    public PluginWrapper( final PluginHandle plugin )
    {
        this.plugin = plugin;
    }

    public long getKey()
    {
        return this.plugin.getKey();
    }

    public String getId()
    {
        return this.plugin.getId();
    }

    public String getName()
    {
        return this.plugin.getName();
    }

    public String getVersion()
    {
        return this.plugin.getVersion();
    }

    public String getTimestamp()
    {
        return new DateTimeFormatterBuilder().appendYear( 4, 4 ).appendLiteral( '-' ).appendMonthOfYear( 2 ).appendLiteral(
            '-' ).appendDayOfMonth( 2 ).appendLiteral( ' ' ).appendHourOfDay( 2 ).appendLiteral( ':' ).appendMinuteOfHour(
            2 ).appendLiteral( ':' ).appendSecondOfMinute( 2 ).toFormatter().print(this.plugin.getTimestamp());
    }

    public boolean isActive()
    {
        return this.plugin.isActive();
    }

    public Map<String, String> getConfig()
    {
        if (this.plugin.isActive()) {
            return this.plugin.getConfig();
        } else {
            return Collections.emptyMap();
        }
    }

    public static Collection<PluginWrapper> toWrapperList( final List<PluginHandle> list )
    {
        final ArrayList<PluginWrapper> target = new ArrayList<PluginWrapper>();
        for ( final PluginHandle item : list )
        {
            target.add( new PluginWrapper( item ) );
        }

        return target;
    }
}
