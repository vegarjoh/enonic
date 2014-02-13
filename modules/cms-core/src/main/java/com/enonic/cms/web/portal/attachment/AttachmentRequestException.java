/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.web.portal.attachment;

import com.enonic.cms.core.structure.SitePath;

/**
 * Feb 17, 2010
 */
public class AttachmentRequestException
    extends RuntimeException
{
    public AttachmentRequestException( SitePath sitePath, String referer, Exception e )
    {
        super( buildMessage( sitePath, referer, e ), e );
    }

    private static String buildMessage( SitePath sitePath, String referer, Exception e )
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "Failed to serve attachment request [" );
        buf.append( sitePath.getLocalPath().getPathAsString() );
        buf.append( "] on site " ).append( sitePath.getSiteKey() );
        if ( referer != null )
        {
            buf.append( "referer was [" ).append( referer ).append( "] " );
        }
        buf.append( ": " );
        buf.append( e.getMessage() );
        return buf.toString();
    }

}
