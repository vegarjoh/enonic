/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.server.service.admin.mvc.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import com.enonic.vertical.adminweb.AdminHelper;

import com.enonic.cms.framework.util.UrlPathEncoder;

import com.enonic.cms.core.Attribute;
import com.enonic.cms.core.admin.PreviewSitePathResolver;
import com.enonic.cms.core.security.PortalSecurityHolder;
import com.enonic.cms.core.security.SecurityService;
import com.enonic.cms.core.security.user.User;
import com.enonic.cms.core.structure.SitePath;

/**
 * This class implements a file controller that returns the actual referenced file in the servlet context.
 */
public class SitePreviewController
    extends AbstractController
{

    private PreviewSitePathResolver sitePathResolver;

    private SecurityService securityService;

    private String characterEncoding;

    @Autowired
    public void setSitePathResolver( PreviewSitePathResolver value )
    {
        this.sitePathResolver = value;
    }

    @Autowired
    public void setSecurityService( SecurityService value )
    {
        this.securityService = value;
    }

    private void loginAdminWebUser( HttpServletRequest request )
    {
        HttpSession session = request.getSession( false );
        if ( session != null )
        {
            User adminUser = securityService.getLoggedInAdminConsoleUser();
            if ( adminUser != null )
            {
                PortalSecurityHolder.setLoggedInUser( adminUser.getKey() );
            }
        }
    }

    protected ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        final User originalLoggedInPortalUser = securityService.getLoggedInPortalUser();
        if ( originalLoggedInPortalUser.isAnonymous() )
        {
            loginAdminWebUser( request );
            if ( securityService.getLoggedInPortalUser().isAnonymous() )
            {
                // User is not logged in, redirect to admin login
                return new ModelAndView( "redirect:" + AdminHelper.getAdminPath( request, false ) );
            }
        }
        else if ( !originalLoggedInPortalUser.equals( securityService.getLoggedInAdminConsoleUser() ) )
        {
            loginAdminWebUser( request );
            if ( securityService.getLoggedInPortalUser().isAnonymous() )
            {
                // User is not logged in, redirect to admin login
                return new ModelAndView( "redirect:" + AdminHelper.getAdminPath( request, false ) );
            }
        }

        SitePath sitePath = sitePathResolver.resolveSitePath( request );
        String url = "/site" + sitePath.asString();
        // We need to url-encode the path again,
        // since forwarding to an decoded url fails in some application servers (Oracle)
        url = UrlPathEncoder.encodeUrlPath( url, this.characterEncoding );

        request.setAttribute( Attribute.PREVIEW_ENABLED, "true" );

        Map<String, Object> model = new HashMap<String, Object>();
        model.put( "path", url );
        model.put( "requestParams", sitePath.getParams() );
        return new ModelAndView( new SiteCustomForwardView(), model );
    }

    @Value("${cms.url.characterEncoding}")
    public void setCharacterEncoding( String characterEncoding )
    {
        this.characterEncoding = characterEncoding;
    }
}
