/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.web.webdav;

import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;

import com.enonic.cms.core.servlet.ServletRequestAccessor;
import com.enonic.cms.core.vhost.VirtualHostHelper;

final class DavLocatorFactoryImpl
    implements DavLocatorFactory
{
    private final static String PREFIX = "/dav";

    @Override
    public DavResourceLocator createResourceLocator( final String prefix, String href )
    {
        final StringBuilder buff = new StringBuilder();

        if ( href == null )
        {
            href = "";
        }

        if ( prefix != null && prefix.length() > 0 )
        {
            buff.append( prefix );
            if ( href.startsWith( prefix ) )
            {
                href = href.substring( prefix.length() );
            }
        }

        final String basePath = getBasePath();

        if ( href.startsWith( PREFIX ) )
        {
            href = href.substring( PREFIX.length() );
        }
        else if ( href.startsWith( basePath ) )
        {   // dav prefix may be not rewritten ! ( example:  Destination: parameter in headers )
            href = href.substring( basePath.length() );
        }

        if ( "".equals( href ) )
        {
            href = "/";
        }

        buff.append( basePath );

        return new DavResourceLocatorImpl( buff.toString(), Text.unescape( href ), this );
    }

    private String getBasePath()
    {
        final String basePath = VirtualHostHelper.getBasePath( ServletRequestAccessor.getRequest() );
        return basePath != null ? basePath : PREFIX;
    }

    @Override
    public DavResourceLocator createResourceLocator( final String prefix, final String workspacePath, final String resourcePath )
    {
        return createResourceLocator( prefix, workspacePath, resourcePath, true );
    }

    @Override
    public DavResourceLocator createResourceLocator( final String prefix, final String workspacePath, final String path,
                                                     final boolean isResourcePath )
    {
        return new DavResourceLocatorImpl( prefix, path, this );
    }
}
