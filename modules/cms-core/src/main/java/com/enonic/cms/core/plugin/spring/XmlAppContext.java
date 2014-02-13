/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.plugin.spring;

import org.osgi.framework.Bundle;
import org.springframework.osgi.context.support.OsgiBundleXmlApplicationContext;

final class XmlAppContext
    extends OsgiBundleXmlApplicationContext
{
    public XmlAppContext( final Bundle bundle )
    {
        setClassLoader( new BundleClassLoader( bundle ) );
        setConfigLocations( new String[]{"META-INF/spring/context.xml"} );
        setBundleContext( bundle.getBundleContext() );
        setPublishContextAsService( false );
    }
}
