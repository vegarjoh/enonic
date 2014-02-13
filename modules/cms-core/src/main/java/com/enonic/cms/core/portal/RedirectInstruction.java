/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal;

import com.enonic.cms.core.structure.SitePath;

/**
 * Created by IntelliJ IDEA.
 * User: rmh
 * Date: Oct 20, 2010
 * Time: 1:30:01 PM
 */
public class RedirectInstruction
{

    SitePath redirectSitePath;

    String redirectUrl;

    boolean isPermanentRedirect = true;


    public RedirectInstruction( SitePath redirectSitePath )
    {
        this.redirectSitePath = redirectSitePath;
    }

    public RedirectInstruction( String redirectUrl )
    {
        this.redirectUrl = redirectUrl;
    }


    public boolean hasRedirectSitePath()
    {
        return redirectSitePath != null;
    }

    public boolean hasRedirectUrl()
    {
        return redirectUrl != null;
    }

    public SitePath getRedirectSitePath()
    {
        return redirectSitePath;
    }

    public String getRedirectUrl()
    {
        return redirectUrl;
    }

    public boolean isPermanentRedirect()
    {
        return isPermanentRedirect;
    }

    public void setPermanentRedirect( boolean permanentRedirect )
    {
        isPermanentRedirect = permanentRedirect;
    }
}

