/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal.livetrace;

import javax.servlet.http.HttpServletRequest;

import com.enonic.cms.framework.util.UrlPathDecoder;

import com.enonic.cms.core.portal.PortalResponse;
import com.enonic.cms.core.portal.rendering.tracing.RenderTrace;
import com.enonic.cms.core.preview.PreviewService;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.structure.SiteEntity;
import com.enonic.cms.core.structure.SitePath;

/**
 * Nov 25, 2010
 */
public class PortalRequestTracer
{
    public static PortalRequestTrace startTracing( final String originalUrl, final LivePortalTraceService livePortalTraceService )
    {
        if ( livePortalTraceService.tracingEnabled() )
        {
            String originalUrlDecoded = UrlPathDecoder.decode( originalUrl );
            return livePortalTraceService.startPortalRequestTracing( originalUrlDecoded );
        }
        else
        {
            return null;
        }
    }

    public static void stopTracing( PortalRequestTrace trace, LivePortalTraceService livePortalTraceService )
    {
        if ( trace != null )
        {
            livePortalTraceService.stopTracing( trace );
        }
    }

    public static void traceMode( final PortalRequestTrace trace, final PreviewService previewService )
    {
        if ( trace != null )
        {
            if ( RenderTrace.isTraceOn() )
            {
                trace.setMode( RequestMode.ICE );
            }
            else if ( previewService.isInPreview() )
            {
                trace.setMode( RequestMode.PREVIEW );
            }
            else
            {
                trace.setMode( RequestMode.PORTAL );
            }
        }
    }

    public static void traceHttpRequest( final PortalRequestTrace trace, final HttpServletRequest httpRequest )
    {
        if ( trace != null && httpRequest != null )
        {
            HttpRequest httpInfo = new HttpRequest();
            httpInfo.setRemoteAddress( httpRequest.getRemoteAddr() );
            httpInfo.setCharacterEncoding( httpRequest.getCharacterEncoding() );
            httpInfo.setContentType( httpRequest.getContentType() );
            httpInfo.setUserAgent( httpRequest.getHeader( "User-Agent" ) );
            trace.setHttpRequest( httpInfo );
        }
    }

    public static void traceRequestedSitePath( final PortalRequestTrace trace, final SitePath requestedSitePath )
    {
        if ( trace != null && requestedSitePath != null )
        {
            trace.setSitePath( requestedSitePath );
        }
    }

    public static void traceRequester( final PortalRequestTrace trace, final UserEntity requester )
    {
        if ( trace != null && requester != null )
        {
            trace.setRequester( User.createUser( requester.getQualifiedName() ) );
        }
    }

    public static void traceRequestedSite( final PortalRequestTrace trace, final SiteEntity requestedSite )
    {
        if ( trace != null && requestedSite != null )
        {
            trace.setSite( requestedSite );
        }
    }

    public static void tracePortalResponse( final PortalRequestTrace trace, final PortalResponse portalResponse )
    {
        if ( trace != null && portalResponse != null )
        {
            if ( portalResponse.isForwardToSitePath() )
            {
                trace.setResponseForward( portalResponse.getForwardToSitePath().asString() );
            }
            else if ( portalResponse.hasRedirectInstruction() && portalResponse.getRedirectInstruction().hasRedirectSitePath() )
            {
                trace.setResponseRedirect( portalResponse.getRedirectInstruction().getRedirectSitePath().asString() );
            }
            else if ( portalResponse.hasRedirectInstruction() && portalResponse.getRedirectInstruction().hasRedirectUrl() )
            {
                trace.setResponseRedirect( portalResponse.getRedirectInstruction().getRedirectUrl() );
            }
        }
    }
}
