/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.vertical.adminweb;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.springframework.beans.factory.annotation.Value;

import com.enonic.esl.net.URL;
import com.enonic.esl.net.URLUtil;
import com.enonic.esl.xml.XMLTool;

import com.enonic.cms.core.xslt.admin.AdminXsltProcessorHelper;

public class SplashServlet
    extends AdminHandlerBaseServlet
{
    private String characterEncoding;

    @Value("${cms.url.characterEncoding}")
    public void setCharacterEncoding( String characterEncoding )
    {
        this.characterEncoding = characterEncoding;
    }

    public void doGet( HttpServletRequest request, HttpServletResponse response )
        throws ServletException, IOException
    {
        performTask( request, response );
    }

    public void doPost( HttpServletRequest request, HttpServletResponse response )
        throws ServletException, IOException
    {
        performTask( request, response );
    }

    protected void performTask( HttpServletRequest request, HttpServletResponse response )
    {
        response.setContentType( "text/html;charset=UTF-8" );
        String redirect = request.getParameter( "redirect" );
        if ( redirect == null || "".equals( redirect ) )
        {
            String urlString = AdminHelper.getAdminPath( request, true ) + "/adminpage?";
            URL tempURL = new URL( urlString );
            Map queryValues = URLUtil.decodeParameterMap( request.getParameterMap(), characterEncoding );
            for ( Iterator iter = queryValues.entrySet().iterator(); iter.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) iter.next();
                String key = (String) entry.getKey();
                if ( "waitscreen".equals( key ) == false )
                {
                    String[] values = (String[]) entry.getValue();
                    for ( int i = 0; i < values.length; i++ )
                    {
                        tempURL.addParameter( key, values[i] );
                    }
                }
            }
            redirect = tempURL.toString();
        }

        // source and result streams
        final HttpSession session = request.getSession( true );
        final Source xslSource = AdminStore.getStylesheet( session, "waitsplash.xsl" );
        final Source input = new DOMSource( XMLTool.createDocument( "dummy" ) );

        new AdminXsltProcessorHelper( this.xsltProcessorFactory ).stylesheet( xslSource, null ).input( input ).param( "redirect", redirect ).process( response );
    }
}
