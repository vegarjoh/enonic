/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.plugin.config;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.enonic.cms.api.plugin.PluginConfig;
import com.enonic.cms.core.config.ConfigProperties;

@Component
public final class ConfigFactoryImpl
    implements ConfigFactory
{
    private final static Logger LOG = LoggerFactory.getLogger( ConfigFactoryImpl.class );

    private File configDir;

    private Map<String, String> globalProperties;

    @Value("${cms.plugin.configDir}")
    public void setConfigDir( final File configDir )
    {
        this.configDir = configDir;
    }

    public PluginConfig create( final Bundle bundle )
    {
        final File file = getConfigFile( bundle );

        final Map<String, String> config = new HashMap<String, String>();
        config.putAll( PluginConfigHelper.loadDefaultProperties( bundle ) );
        config.putAll( PluginConfigHelper.loadProperties( file ) );

        if ( file.exists() )
        {
            LOG.info( "Loaded configuration for bundle [{}] from [{}]", bundle.getSymbolicName(), file.getAbsolutePath() );
        }

        return new PluginConfigImpl( PluginConfigHelper.interpolate( this.globalProperties, config ) );
    }

    private File getConfigFile( final Bundle bundle )
    {
        final String id = getConfigurationId( bundle );
        return new File( this.configDir, id + ".properties" );
    }

    private String getConfigurationId( final Bundle bundle )
    {
        return bundle.getSymbolicName();
    }

    @Autowired
    public final void setGlobalProperties( final ConfigProperties properties )
    {
        this.globalProperties = properties.getMap();
    }
}
