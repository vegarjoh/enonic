/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.web;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.DispatcherServlet;

import com.enonic.cms.core.servlet.ServletRequestAccessor;
import com.enonic.cms.server.service.servlet.OriginalUrlResolver;

/**
 * This class implements a modification of the dispatcher servlet.
 */
public final class CmsDispatcherServlet
    extends DispatcherServlet
{
    private final static List<HttpMethod> ALLOWED_HTTP_METHODS =
        Arrays.asList( HttpMethod.GET, HttpMethod.POST, HttpMethod.HEAD, HttpMethod.OPTIONS );

    public CmsDispatcherServlet()
    {
        setContextConfigLocation( "" );
    }

    @Override
    protected void doOptions( HttpServletRequest request, HttpServletResponse response )
        throws ServletException, IOException
    {
        response.setHeader( "Allow", StringUtils.join( ALLOWED_HTTP_METHODS, "," ) );
        response.setStatus( HttpServletResponse.SC_OK );
    }

    protected void doService( HttpServletRequest req, HttpServletResponse res )
        throws Exception
    {
        final HttpMethod requestMethod = HttpMethod.valueOf( req.getMethod() );

        if ( !ALLOWED_HTTP_METHODS.contains( requestMethod ) )
        {
            res.sendError( HttpServletResponse.SC_METHOD_NOT_ALLOWED );
            return;
        }

        ServletRequestAccessor.setRequest( req );
        OriginalUrlResolver.resolveOriginalUrl( req );

        super.doService( req, res );
    }
}
