/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal;

import org.joda.time.DateTime;

import com.enonic.cms.core.security.user.UserKey;
import com.enonic.cms.core.structure.SitePath;

/**
 * May 11, 2009
 */
public class AbstractBasePortalRequest
{
    private DateTime requestTime;

    private SitePath originalSitePath;

    private UserKey requester;

    private String ticketId;

    public DateTime getRequestTime()
    {
        return requestTime;
    }

    public void setRequestTime( DateTime requestTime )
    {
        this.requestTime = requestTime;
    }


    public SitePath getOriginalSitePath()
    {
        return originalSitePath;
    }

    public void setOriginalSitePath( SitePath originalSitePath )
    {
        this.originalSitePath = originalSitePath;
    }

    public UserKey getRequester()
    {
        return requester;
    }

    public void setRequester( UserKey value )
    {
        this.requester = value;
    }


    public String getTicketId()
    {
        return ticketId;
    }

    public void setTicketId( String ticketId )
    {
        this.ticketId = ticketId;
    }

}
