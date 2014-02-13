/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal;

import com.enonic.cms.core.NotFoundErrorType;
import com.enonic.cms.core.Path;
import com.enonic.cms.core.StacktraceLoggingUnrequired;
import com.enonic.cms.core.structure.SiteKey;

public class ResourceNotFoundException
    extends RuntimeException
    implements NotFoundErrorType, StacktraceLoggingUnrequired
{

    private SiteKey siteKey;

    private String path;

    private String message;

    public ResourceNotFoundException( SiteKey siteKey, String url, String referer )
    {
        this.siteKey = siteKey;
        this.path = url;
        this.message = "Resource '" + this.path + "' not found on site '" + siteKey + "'";
        if ( referer != null )
        {
            this.message = this.message + ", referer was: '" + referer + "'";
        }
    }

    public ResourceNotFoundException( SiteKey siteKey, Path localPath )
    {
        this.siteKey = siteKey;
        this.path = localPath.toString();
        this.message = "Resource '" + localPath + "' not found on site '" + siteKey + "'";
    }

    public SiteKey getSiteKey()
    {
        return siteKey;
    }

    public String getPath()
    {
        return path;
    }

    public String getMessage()
    {
        return message;
    }

    public boolean endsWithIgnoreCase( String str )
    {
        String localPath = getPath().toLowerCase();
        return localPath.endsWith( str.toLowerCase() );
    }
}
