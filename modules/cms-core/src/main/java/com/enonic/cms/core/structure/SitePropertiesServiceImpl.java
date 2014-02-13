/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.structure;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import com.enonic.cms.store.dao.SiteDao;

@Component("sitePropertiesService")
public class SitePropertiesServiceImpl
    implements SitePropertiesService, ApplicationListener
{
    private static final Logger LOG = LoggerFactory.getLogger( SitePropertiesService.class );

    private Properties defaultProperties;

    private final Map<SiteKey, SiteProperties> sitePropertiesMap = new ConcurrentHashMap<SiteKey, SiteProperties>();

    private File homeDir;

    private ResourceLoader resourceLoader = new FileSystemResourceLoader();

    private SiteDao siteDao;

    private List<SitePropertiesListener> sitePropertiesListeners = new ArrayList<SitePropertiesListener>();

    private boolean started = false;

    @Override
    public void registerSitePropertiesListener( final SitePropertiesListener listener )
    {
        LOG.info( "Registered site properties listener: " + listener.getClass().getSimpleName() );
        sitePropertiesListeners.add( listener );
    }

    @Override
    public void onApplicationEvent( final ApplicationEvent event )
    {
        if ( event instanceof ContextRefreshedEvent )
        {
            start();
        }
    }

    public void restart()
    {
        stop();
        start();
    }

    public void stop()
    {
        sitePropertiesMap.clear();
        started = false;
    }

    public void start()
    {
        if ( started )
        {
            return;
        }

        Resource resource = resourceLoader.getResource( "classpath:com/enonic/cms/business/render/site-default.properties" );
        try
        {
            defaultProperties = new Properties();
            defaultProperties.load( resource.getInputStream() );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Failed to load site-default.properties", e );
        }

        // Load properties for all sites
        final List<SiteEntity> allSites = siteDao.findAll();
        for ( final SiteEntity currSite : allSites )
        {
            loadSiteProperties( currSite.getKey() );
        }

        // Broadcast properties loaded for all sites
        for ( final SiteEntity currSite : allSites )
        {
            final SiteProperties siteProperties = sitePropertiesMap.get( currSite.getKey() );
            for ( SitePropertiesListener listener : sitePropertiesListeners )
            {
                listener.sitePropertiesLoaded( siteProperties );
            }
        }

        started = true;
    }


    public SiteProperties getSiteProperties( SiteKey siteKey )
    {
        final SiteProperties siteProperties = doGetSiteProperties( siteKey );
        if ( siteProperties == null )
        {
            throw new IllegalArgumentException( "No properties for site " + siteKey );
        }
        return siteProperties;
    }

    private String getProperty( String key, SiteKey siteKey )
    {
        SiteProperties props = doGetSiteProperties( siteKey );
        if ( props == null )
        {
            throw new IllegalArgumentException( "No properties for site " + siteKey );
        }

        return StringUtils.trimToNull( props.getProperty( key ) );
    }

    public String getSiteProperty( SiteKey siteKey, SitePropertyNames key )
    {
        return getSiteProperties( siteKey ).getProperty( key.getKeyName() );
    }

    public Boolean getPropertyAsBoolean( SitePropertyNames key, SiteKey siteKey )
    {
        String svalue = getProperty( key.getKeyName(), siteKey );

        return svalue == null ? Boolean.FALSE : Boolean.valueOf( svalue );
    }

    private SiteProperties doGetSiteProperties( final SiteKey siteKey )
    {
        SiteProperties props;
        synchronized ( sitePropertiesMap )
        {
            props = sitePropertiesMap.get( siteKey );
            if ( props == null )
            {
                props = loadSiteProperties( siteKey );
            }
        }
        return props;
    }

    public void reloadSiteProperties( final SiteKey siteKey )
    {
        synchronized ( sitePropertiesMap )
        {
            loadSiteProperties( siteKey );
            final SiteProperties siteProperties = sitePropertiesMap.get( siteKey );
            for ( SitePropertiesListener listener : sitePropertiesListeners )
            {
                listener.sitePropertiesReloaded( siteProperties );
            }
        }
    }

    private SiteProperties loadSiteProperties( final SiteKey siteKey )
    {
        final Properties properties = new Properties( defaultProperties );

        final String relativePathToCmsHome = "/config/site-" + siteKey + ".properties";

        String propertiesType = "default";

        try
        {
            final String resourcePath = this.homeDir.toURI().toURL() + relativePathToCmsHome;
            final Resource resource = resourceLoader.getResource( resourcePath );

            if ( resource.exists() )
            {
                final InputStream stream = resource.getInputStream();
                properties.load( stream );
                stream.close();

                propertiesType = "custom";
            }
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Failed to load site properties file: " + relativePathToCmsHome, e );
        }

        LOG.info( "Loaded {} properties for site #{}", propertiesType, siteKey );

        final SiteProperties siteProperties = new SiteProperties( siteKey, properties );
        sitePropertiesMap.put( siteKey, siteProperties );
        return siteProperties;
    }

    @Value("${cms.home}")
    public void setHomeDir( File homeDir )
    {
        this.homeDir = homeDir;
    }

    public void setResourceLoader( ResourceLoader resourceLoader )
    {
        this.resourceLoader = resourceLoader;
    }

    @Autowired
    public void setSiteDao( final SiteDao siteDao )
    {
        this.siteDao = siteDao;
    }
}
