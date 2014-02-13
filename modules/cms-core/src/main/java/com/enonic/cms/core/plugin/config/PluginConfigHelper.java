/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.plugin.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.text.StrSubstitutor;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

final class PluginConfigHelper
{
    private final static String DEFAULT_CONFIG = "META-INF/cms/default.properties";

    private final static Logger LOG = LoggerFactory.getLogger( PluginConfigHelper.class );

    public static Map<String, String> loadDefaultProperties( final Bundle bundle )
    {
        return loadProperties( bundle.getEntry( DEFAULT_CONFIG ) );
    }

    private static Map<String, String> loadProperties( final URL url )
    {
        if ( url == null )
        {
            return Collections.emptyMap();
        }

        try
        {
            final InputStream in = url.openStream();

            try
            {
                return loadProperties( in );
            }
            finally
            {
                in.close();
            }
        }
        catch ( Exception e )
        {
            LOG.warn("Error occurred loading properties from [{}]", url.toExternalForm(), e);
        }

        return Collections.emptyMap();
    }

    public static Map<String, String> loadProperties( final File file )
    {
        if ( !file.exists() || !file.isFile() )
        {
            return Collections.emptyMap();
        }

        try
        {
            final InputStream in = new FileInputStream( file );

            try
            {
                return loadProperties( in );
            }
            finally
            {
                in.close();
            }
        }
        catch ( Exception e )
        {
            LOG.warn("Error occurred loading properties from [{}]", file.getAbsolutePath(), e);
        }

        return Collections.emptyMap();
    }

    private static Map<String, String> loadProperties( final InputStream in )
        throws IOException
    {
        final Properties props = new Properties();
        props.load( in );
        return toMap( props );
    }

    private static Map<String, String> toMap( final Properties props )
    {
        final HashMap<String, String> map = new HashMap<String, String>();
        for ( Object o : props.keySet() )
        {
            map.put( o.toString(), props.getProperty( o.toString() ) );
        }

        return map;
    }

    public static Map<String, String> interpolate( final Map<String, String> globalProperties, final Map<String, String> source )
    {
        final Map<String, String> submap = Maps.newHashMap();
        submap.putAll( globalProperties );
        submap.putAll( source );

        final Map<String, String> target = new HashMap<String, String>();
        final StrSubstitutor substitutor = new StrSubstitutor( submap );

        for ( String key : source.keySet() )
        {
            final String value = substitutor.replace( source.get( key ) );
            target.put( key, value );
        }

        return target;
    }
}
