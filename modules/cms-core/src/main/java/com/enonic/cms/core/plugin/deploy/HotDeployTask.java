/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.plugin.deploy;

import java.io.File;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.enonic.cms.core.plugin.PluginManager;

@Component
public final class HotDeployTask
{
    private final static Logger LOG = LoggerFactory.getLogger( HotDeployTask.class );

    private File deployDir;

    private long scanPeriod;

    private FileAlterationMonitor monitor;

    private PluginManager pluginManager;

    @Value("${cms.plugin.deployDir}")
    public void setDeployDir( final File deployDir )
    {
        this.deployDir = deployDir;
    }

    @Value("${cms.plugin.scanPeriod}")
    public void setScanPeriod( final long scanPeriod )
    {
        this.scanPeriod = scanPeriod;
    }

    @Autowired
    public void setPluginManager( final PluginManager pluginManager )
    {
        this.pluginManager = pluginManager;
    }

    @PostConstruct
    public void start()
    {
        try
        {
            final JarFileFilter filter = new JarFileFilter();

            final FileAlterationObserver observer = new FileAlterationObserver( this.deployDir, filter );
            observer.addListener( new HotDeployListener( this.pluginManager ) );
            observer.checkAndNotify();

            this.monitor = new FileAlterationMonitor( this.scanPeriod, observer );
            this.monitor.start();

            LOG.info( "Hot deploying plugins from [{}]. Scanning every [{}] ms.", this.deployDir.getAbsolutePath(), this.scanPeriod );
        }
        catch ( Exception e )
        {
            LOG.error( "cannot start monitor.", e );
        }
    }

    @PreDestroy
    public void stop()
    {
        try
        {
            this.monitor.stop();
        }
        catch ( Exception e )
        {
            LOG.error( "cannot stop monitor correctly.", e );
        }
    }
}
