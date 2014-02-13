/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.portal.livetrace;

import com.enonic.cms.core.image.ImageRequest;
import com.enonic.cms.core.image.ImageResponse;

/**
 * Nov 25, 2010
 */
public class ImageRequestTracer
{
    public static ImageRequestTrace startTracing( final LivePortalTraceService livePortalTraceService )
    {
        PortalRequestTrace currentPortalRequestTrace = livePortalTraceService.getCurrentPortalRequestTrace();

        if ( currentPortalRequestTrace != null )
        {
            return livePortalTraceService.startImageRequestTracing( currentPortalRequestTrace );
        }
        else
        {
            return null;
        }
    }

    public static void stopTracing( final ImageRequestTrace trace, final LivePortalTraceService livePortalTraceService )
    {
        if ( trace != null )
        {
            livePortalTraceService.stopTracing( trace );
        }
    }

    public static void traceImageRequest( final ImageRequestTrace trace, final ImageRequest imageRequest )
    {
        if ( trace != null && imageRequest != null )
        {
            if ( imageRequest.getContentKey() != null )
            {
                trace.setContentKey( imageRequest.getContentKey() != null ? imageRequest.getContentKey().toString() : null );
                trace.setLabel( imageRequest.getLabel() );
                trace.setImageParamFilter( imageRequest.getParams().getFilter() );
                trace.setImageParamFormat( imageRequest.getFormat() );
                trace.setImageParamBackgroundColor( imageRequest.getParams().getBackgroundColorAsString() );
                trace.setImageParamQuality( imageRequest.getParams().getQualityAsString() );
            }
            else
            {
                // request to user image...
                trace.setLabel( imageRequest.getLabel() );
                trace.setImageParamFilter( imageRequest.getParams().getFilter() );
                trace.setImageParamFormat( imageRequest.getFormat() );
                trace.setImageParamBackgroundColor( imageRequest.getParams().getBackgroundColorAsString() );
                trace.setImageParamQuality( imageRequest.getParams().getQualityAsString() );
            }
        }
    }

    public static void traceSize( ImageRequestTrace trace, Long sizeInBytes )
    {
        if ( trace != null && sizeInBytes != null )
        {
            trace.setSizeInBytes( sizeInBytes );
        }
    }

    public static void traceUsedCachedResult( ImageRequestTrace trace, boolean value )
    {
        if ( trace != null )
        {
            trace.getCacheUsage().setUsedCachedResult( value );
        }
    }

    public static void traceImageResponse( ImageRequestTrace trace, ImageResponse imageResponse )
    {
        if ( trace != null && imageResponse != null )
        {
            // TODO: this name is not good for nothing.
            //trace.setImageName( imageResponse.getName() );
        }
    }

    public static void startConcurrencyBlockTimer( ImageRequestTrace trace )
    {
        if ( trace != null )
        {
            trace.getCacheUsage().startConcurrencyBlockTimer();
        }
    }

    public static void stopConcurrencyBlockTimer( ImageRequestTrace trace )
    {
        if ( trace != null )
        {
            trace.getCacheUsage().stopConcurrencyBlockTimer();
        }
    }
}
