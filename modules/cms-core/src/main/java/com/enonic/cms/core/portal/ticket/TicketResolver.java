/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal.ticket;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.enonic.cms.core.portal.Ticket;

public class TicketResolver
{
    public static String resolve( HttpServletRequest request )
    {
        String prameterName = Ticket.getParameterName();
        String ticket = null;
        String enctype = request.getContentType();
        if ( enctype != null && enctype.startsWith( "multipart/form-data" ) )
        {
            Map queryValues = request.getParameterMap();
            if ( queryValues.containsKey( prameterName ) )
            {
                ticket = ( (String[]) queryValues.get( prameterName ) )[0];
            }
        }
        else
        {
            ticket = request.getParameter( prameterName );
        }
        return ticket;
    }
}
