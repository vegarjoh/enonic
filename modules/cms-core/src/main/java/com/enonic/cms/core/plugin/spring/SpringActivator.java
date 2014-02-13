/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.plugin.spring;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.enonic.cms.core.plugin.context.PluginContext;
import com.enonic.cms.api.plugin.ext.Extension;
import com.enonic.cms.core.plugin.util.OsgiHelper;

public final class SpringActivator
    implements BundleActivator
{
    private XmlAppContext app;

    public void start( final BundleContext context )
        throws Exception
    {
        this.app = new XmlAppContext( context.getBundle() );

        final PluginContext pluginContext = OsgiHelper.requireService( context, PluginContext.class );
        this.app.addBeanFactoryPostProcessor( new ConfigProcessor( pluginContext ) );
        this.app.addBeanFactoryPostProcessor( new BeansProcessor( pluginContext ) );

        this.app.refresh();

        for ( final Extension ext : this.app.getBeansOfType( Extension.class ).values() )
        {
            pluginContext.register( ext );
        }
    }

    public void stop( final BundleContext context )
        throws Exception
    {
        this.app.close();
    }
}
