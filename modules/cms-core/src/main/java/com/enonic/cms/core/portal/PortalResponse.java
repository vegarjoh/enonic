/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal;

import java.io.UnsupportedEncodingException;

import org.joda.time.DateTime;

import com.enonic.cms.core.portal.rendering.RenderedPageResult;
import com.enonic.cms.core.portal.rendering.RenderedWindowResult;
import com.enonic.cms.core.structure.SitePath;

/**
 * May 7, 2009
 */
public class PortalResponse
{
    private String contentEncoding = "UTF-8";

    private String content;

    private String httpContentType;

    private DateTime expirationTime;

    private SitePath forwardToSitePath;

    private RedirectInstruction redirectInstruction;

    public static PortalResponse createContent( RenderedWindowResult renderedWindowResult )
    {
        PortalResponse response = new PortalResponse();
        response.content = renderedWindowResult.getContent();
        response.httpContentType = renderedWindowResult.getHttpContentType();
        if ( renderedWindowResult.getContentEncoding() != null )
        {
            response.contentEncoding = renderedWindowResult.getContentEncoding();
        }

        if ( renderedWindowResult.getExpirationTimeInCache() != null )
        {
            response.expirationTime = renderedWindowResult.getExpirationTimeInCache();
        }
        else
        {
            response.expirationTime = new DateTime();
        }
        return response;
    }

    public static PortalResponse createContent( RenderedPageResult renderedPageResult )
    {
        PortalResponse response = new PortalResponse();
        response.content = renderedPageResult.getContent();
        response.httpContentType = renderedPageResult.getHttpContentType();
        if ( renderedPageResult.getContentEncoding() != null )
        {
            response.contentEncoding = renderedPageResult.getContentEncoding();
        }

        if ( renderedPageResult.getExpirationTime() != null )
        {
            response.expirationTime = renderedPageResult.getExpirationTime();
        }
        else
        {
            response.expirationTime = new DateTime();
        }
        return response;
    }


    public static PortalResponse createForward( SitePath forwardToSitePath )
    {
        PortalResponse response = new PortalResponse();
        response.forwardToSitePath = forwardToSitePath;
        return response;
    }

    public static PortalResponse createRedirect( RedirectInstruction redirectInstruction )
    {
        PortalResponse response = new PortalResponse();
        response.redirectInstruction = redirectInstruction;
        return response;
    }

    public String getContent()
    {
        return content;
    }

    public byte[] getContentAsBytes()
        throws UnsupportedEncodingException
    {
        return content.getBytes( contentEncoding );
    }

    public void setContent( String content )
    {
        this.content = content;
    }

    public String getHttpContentType()
    {
        return httpContentType;
    }

    public boolean isForwardToSitePath()
    {
        return forwardToSitePath != null;
    }

    public SitePath getForwardToSitePath()
    {
        return forwardToSitePath;
    }

    public DateTime getExpirationTime()
    {
        return expirationTime;
    }

    public RedirectInstruction getRedirectInstruction()
    {
        return redirectInstruction;
    }

    public boolean hasRedirectInstruction()
    {
        return redirectInstruction != null;
    }

}
