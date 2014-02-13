/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal.livetrace;

import com.enonic.cms.core.security.user.UserEntity;

/**
 * Nov 25, 2010
 */
public class PageRenderingTracer
{
    public static PageRenderingTrace startTracing( final LivePortalTraceService livePortalTraceService )
    {
        final PortalRequestTrace portalRequestTrace = livePortalTraceService.getCurrentPortalRequestTrace();

        if ( portalRequestTrace != null )
        {
            return livePortalTraceService.startPageRenderTracing( portalRequestTrace );
        }
        else
        {
            return null;
        }
    }

    public static void stopTracing( final PageRenderingTrace trace, final LivePortalTraceService livePortalTraceService )
    {
        if ( trace != null )
        {
            livePortalTraceService.stopTracing( trace );
        }
    }

    public static void traceRequester( final PageRenderingTrace trace, final UserEntity renderer )
    {
        if ( trace != null && renderer != null )
        {
            trace.setRenderer( User.createUser( renderer.getQualifiedName() ) );
        }
    }

    public static void traceUsedCachedResult( final PageRenderingTrace trace, boolean cacheable, boolean usedCachedResult )
    {
        if ( trace != null )
        {
            trace.getCacheUsage().setCacheable( cacheable );
            trace.getCacheUsage().setUsedCachedResult( usedCachedResult );
        }
    }

    public static void startConcurrencyBlockTimer( PageRenderingTrace trace )
    {
        if ( trace != null )
        {
            trace.getCacheUsage().startConcurrencyBlockTimer();
        }
    }

    public static void stopConcurrencyBlockTimer( PageRenderingTrace trace )
    {
        if ( trace != null )
        {
            trace.getCacheUsage().stopConcurrencyBlockTimer();
        }
    }
}
