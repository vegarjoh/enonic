/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.web.portal.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.enonic.cms.core.Path;
import com.enonic.cms.core.security.InstantTraceSecurityHolder;
import com.enonic.cms.core.structure.SitePath;
import com.enonic.cms.web.portal.PortalWebContext;
import com.enonic.cms.web.portal.SiteRedirectAndForwardHelper;
import com.enonic.cms.web.portal.instanttrace.InstantTracePathInspector;
import com.enonic.cms.web.portal.instanttrace.InstantTraceRequestInspector;

@Component
public class InstantTraceInterceptor
    implements RequestInterceptor
{
    @Autowired
    private SiteRedirectAndForwardHelper siteRedirectAndForwardHelper;

    @Override
    public boolean preHandle( final PortalWebContext context )
        throws Exception
    {
        if ( noPreHandle( context ) )
        {
            return true;
        }

        forwardToAuthenticationForm( context );
        return false;
    }

    @Override
    public void postHandle( final PortalWebContext context )
        throws Exception
    {
        // nothing
    }

    private void forwardToAuthenticationForm( final PortalWebContext context )
        throws Exception
    {
        final HttpServletRequest request = context.getRequest();
        final HttpServletResponse response = context.getResponse();
        InstantTraceRequestInspector.setAttributeOriginalUrl( context.getSitePath().getPathAndParams(), context.getRequest() );
        SitePath sitePath = context.getSitePath().createNewInSameSite( new Path( "/_itrace/authenticate" ) );
        siteRedirectAndForwardHelper.forward( request, response, sitePath );
    }

    private boolean noPreHandle( final PortalWebContext context )
    {
        if ( !InstantTraceRequestInspector.isClientEnabled( context.getRequest() ) )
        {
            return true;
        }

        final Path localPath = context.getSitePath().getLocalPath();

        if ( localPath.containsSubPath( "_public" ) )
        {
            return true;
        }
        else if ( InstantTracePathInspector.isResourcePath( localPath ) )
        {
            return true;
        }
        else if ( InstantTraceRequestInspector.isAuthenticationSubmitted( context.getRequest() ) )
        {
            return true;
        }
        else if ( InstantTraceSecurityHolder.isAuthenticated() )
        {
            return true;
        }
        else if ( InstantTracePathInspector.isAuthenticationPagePath( context.getSitePath().getLocalPath() ) )
        {
            return true;
        }

        return false;
    }

}