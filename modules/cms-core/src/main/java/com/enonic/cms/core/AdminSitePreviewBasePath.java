/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.enonic.cms.core.structure.SiteKey;

/**
 * Aug 6, 2010
 */
public class AdminSitePreviewBasePath
    implements SiteBasePath
{
    private Path adminPath;

    private SiteKey siteKey;

    private Path asPath;

    public AdminSitePreviewBasePath( Path adminPath, SiteKey siteKey )
    {
        if ( adminPath == null )
        {
            throw new IllegalArgumentException( "Given adminPath cannot be null" );
        }
        if ( siteKey == null )
        {
            throw new IllegalArgumentException( "Given siteKey cannot be null" );
        }

        this.adminPath = adminPath;
        this.siteKey = siteKey;
        this.asPath = generatePath();
    }

    private Path generatePath()
    {
        Path path = new Path( "/" );
        path = path.appendPath( adminPath );
        path = path.appendPathElement( "preview" );
        path = path.appendPathElement( siteKey.toString() );
        return path;
    }

    public Path getAdminPath()
    {
        return adminPath;
    }

    public SiteKey getSiteKey()
    {
        return siteKey;
    }

    public Path getAsPath()
    {
        return asPath;
    }

    public String toString()
    {
        ToStringBuilder s = new ToStringBuilder( this, ToStringStyle.MULTI_LINE_STYLE );
        s.append( "adminPath", adminPath.toString() );
        s.append( "siteKey", siteKey.toString() );
        s.append( "asPath", asPath.toString() );
        return s.toString();
    }
}
