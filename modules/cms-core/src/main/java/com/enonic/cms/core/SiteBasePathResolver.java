/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Preconditions;

import com.enonic.cms.core.portal.rendering.tracing.RenderTrace;
import com.enonic.cms.core.structure.SiteKey;

/**
 * Sep 3, 2010
 */
public class SiteBasePathResolver
{
    public static SiteBasePath resolveSiteBasePath( HttpServletRequest httpRequest, SiteKey sitekey )
    {
        Preconditions.checkNotNull( httpRequest );
        Preconditions.checkNotNull( sitekey );

        SiteBasePath siteBasePath;

        if ( isInDebugMode() )
        {
            Path adminPath = new Path( DeploymentPathResolver.getAdminDeploymentPath( httpRequest ) );
            siteBasePath = new AdminSiteDebugBasePath( adminPath, sitekey );
        }
        else if ( isInPreviewMode( httpRequest ) )
        {
            Path adminPath = new Path( DeploymentPathResolver.getAdminDeploymentPath( httpRequest ) );
            siteBasePath = new AdminSitePreviewBasePath( adminPath, sitekey );
        }
        else
        {
            Path sitePrefixPath = new Path( DeploymentPathResolver.getSiteDeploymentPath( httpRequest ) );
            siteBasePath = new PortalSiteBasePath( sitePrefixPath, sitekey );
        }

        return siteBasePath;
    }

    private static boolean isInDebugMode()
    {
        return RenderTrace.isTraceOn();
    }

    private static boolean isInPreviewMode( HttpServletRequest httpRequest )
    {
        String previewEnabled = (String) httpRequest.getAttribute( Attribute.PREVIEW_ENABLED );
        return "true".equals( previewEnabled );
    }
}
