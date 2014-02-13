/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal.livetrace;

import com.enonic.cms.core.content.binary.AttachmentRequest;

/**
 * Nov 25, 2010
 */
public class AttachmentRequestTracer
{
    public static AttachmentRequestTrace startTracing( final LivePortalTraceService livePortalTraceService )
    {
        PortalRequestTrace currentPortalRequestTrace = livePortalTraceService.getCurrentPortalRequestTrace();

        if ( currentPortalRequestTrace != null )
        {
            return livePortalTraceService.startAttachmentRequestTracing( currentPortalRequestTrace );
        }
        else
        {
            return null;
        }
    }

    public static void stopTracing( final AttachmentRequestTrace trace, final LivePortalTraceService livePortalTraceService )
    {
        if ( trace != null )
        {
            livePortalTraceService.stopTracing( trace );
        }
    }

    public static void traceAttachmentRequest( final AttachmentRequestTrace trace, final AttachmentRequest attachmentRequest )
    {
        if ( trace != null && attachmentRequest != null )
        {
            trace.setContentKey( attachmentRequest.getContentKey().toString() );
            trace.setBinaryDataKey( attachmentRequest.getBinaryDataKey().toString() );
        }
    }

    public static void traceSize( AttachmentRequestTrace trace, Long sizeInBytes )
    {
        if ( trace != null && sizeInBytes != null )
        {
            trace.setSizeInBytes( sizeInBytes );
        }
    }
}
