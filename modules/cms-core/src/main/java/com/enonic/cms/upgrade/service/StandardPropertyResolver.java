/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.upgrade.service;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Properties;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;

import com.enonic.cms.framework.util.PropertiesUtil;

import com.enonic.cms.core.config.ConfigLoader;
import com.enonic.cms.core.home.HomeDir;
import com.enonic.cms.core.structure.SiteKey;

public final class StandardPropertyResolver
    implements PropertyResolver, InitializingBean
{
    private File homeDir;

    private File configDir;

    private HashMap<SiteKey, Properties> siteProperties;

    private Properties cmsProperties;

    public StandardPropertyResolver()
    {
        this.siteProperties = new HashMap<SiteKey, Properties>();
    }

    public void afterPropertiesSet()
        throws Exception
    {
        this.configDir = new File( this.homeDir, "config" );

        if ( this.cmsProperties == null )
        {
            this.cmsProperties = new ConfigLoader( new HomeDir( this.homeDir ) ).load();
        }
    }

    @Value("${cms.home}")
    public void setHomeDir( File homeDir )
    {
        this.homeDir = homeDir;
    }

    public void setProperties( final Properties props )
    {
        this.cmsProperties = props;
    }

    public String getProperty( String name )
    {
        return this.cmsProperties.getProperty( name );
    }

    public String getProperty( SiteKey siteKey, String name )
    {
        return getProperties( siteKey ).getProperty( name );
    }

    private synchronized Properties getProperties( SiteKey siteKey )
    {
        Properties props = this.siteProperties.get( siteKey );
        if ( props == null )
        {
            props = loadProperties( siteKey );
            if ( props != null )
            {
                this.siteProperties.put( siteKey, props );
            }
        }

        return props;
    }

    private Properties loadProperties( SiteKey siteKey )
    {
        Properties props = new Properties();
        File file = new File( this.configDir, "site-" + siteKey.toInt() + ".properties" );
        if ( file.exists() )
        {
            try
            {
                props.load( new FileInputStream( file ) );
            }
            catch ( Exception e )
            {
                // Do nothing
            }
        }

        return PropertiesUtil.interpolate( props );
    }

    public String getConfigDirPath()
    {
        return this.configDir.getPath();
    }
}
