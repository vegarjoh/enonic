/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.web.portal.page;


import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.enonic.cms.core.Path;
import com.enonic.cms.core.portal.PortalRequest;
import com.enonic.cms.core.portal.PortalResponse;
import com.enonic.cms.core.portal.RedirectInstruction;
import com.enonic.cms.core.portal.rendering.tracing.RenderTrace;
import com.enonic.cms.core.servlet.ServletRequestAccessor;
import com.enonic.cms.core.structure.SiteKey;
import com.enonic.cms.core.structure.SitePath;
import com.enonic.cms.server.DeploymentAndRequestSetup;

import static org.junit.Assert.*;

/**
 * Aug 10, 2010
 */
public class PortalResponseProcessorTest
{
    private static final Logger LOG = LoggerFactory.getLogger( PortalResponseProcessorTest.class.getName() );

    private PortalResponseProcessor portalResponseProcessor = new PortalResponseProcessor();

    private MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();

    private MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();

    private PortalRequest portalRequest = new PortalRequest();


    @Before
    public void before()
    {

        httpServletRequest.setServerPort( 80 );

        ServletRequestAccessor.setRequest( httpServletRequest );
        portalResponseProcessor.setHttpRequest( httpServletRequest );
        portalResponseProcessor.setHttpResponse( httpServletResponse );
    }

    /**
     * site-0.properties cms.site.createUrlAsPath = false
     * Vhost: www.mysite.com = /site/0
     * Deployed @ /
     * Request is via portal @ http://www.mysite.com/political news shortcut
     */
    @Test
    public void redirecToSitePath_when_deployed_at_root_with_vhost_and_portal_requested_at_vhost_with_absolute_urls()
        throws Exception
    {
        // setup
        portalRequest.setOriginalUrl( "http://www.mysite.com/political news shortcut" );

        new DeploymentAndRequestSetup().appDeployedAtRoot().addVirtualHost( "www.mysite.com", "/site/0" ).originalRequest( "www.mysite.com",
                                                                                                                           "/political news shortcut" ).requestedSite(
            0, "political news shortcut" ).requestedPortalAt().siteSetupAtRoot()

            .back().setup( httpServletRequest );

        final SitePath sitePath = new SitePath( new SiteKey( 0 ), new Path( "/news/local politics" ) );

        RedirectInstruction redirectInstruction = createRedirectInstruction( sitePath );

        PortalResponse portalResponse = PortalResponse.createRedirect( redirectInstruction );
        portalResponseProcessor.setResponse( portalResponse );

        // exercise
        portalResponseProcessor.serveResponse();

        // verify
        assertEquals( "/news/local+politics", httpServletResponse.getHeader( "Location" ) );
    }

    private RedirectInstruction createRedirectInstruction( SitePath sitePath )
    {
        RedirectInstruction redirectInstruction = new RedirectInstruction( sitePath );
        redirectInstruction.setPermanentRedirect( true );
        return redirectInstruction;
    }

    /**
     * site-0.properties cms.site.createUrlAsPath = true
     * Vhost: www.mysite.com/en = /site/0
     * Deployed @ /
     * Request is via portal @ http://www.mysite.com/en/political news shortcut
     */
    @Test
    public void redirecToSitePath_when_deployed_at_root_with_vhost_at_subpath_and_portal_requested_at_vhost_with_relative_urls()
        throws Exception
    {
        // setup
        portalRequest.setOriginalUrl( "http://www.mysite.com/en/political news shortcut" );

        new DeploymentAndRequestSetup().appDeployedAtRoot().addVirtualHost( "www.mysite.com/en", "/site/0" ).originalRequest(
            "www.mysite.com", "/en/political news shortcut" ).requestedSite( 0,
                                                                             "political news shortcut" ).requestedPortalAt().siteSetupAtPath(
            "/en" ).back().setup( httpServletRequest );

        final SitePath sitePath = new SitePath( new SiteKey( 0 ), new Path( "/news/local politics" ) );
        PortalResponse portalResponse = PortalResponse.createRedirect( createRedirectInstruction( sitePath ) );
        portalResponseProcessor.setResponse( portalResponse );

        // exercise
        portalResponseProcessor.serveResponse();

        // verify
        assertEquals( "/en/news/local+politics", httpServletResponse.getHeader( "Location" ) );
    }

    /**
     * site-0.properties cms.site.createUrlAsPath = false
     * Vhost: www.mysite.com/en = /site/0
     * Deployed @ /
     * Request is via portal @ http://www.mysite.com/en/political news shortcut
     */
    @Test
    public void redirecToSitePath_when_deployed_at_root_with_vhost_at_subpath_and_portal_requested_at_vhost_with_absolute_urls()
        throws Exception
    {
        // setup
        portalRequest.setOriginalUrl( "http://www.mysite.com/en/political news shortcut" );

        new DeploymentAndRequestSetup().appDeployedAtRoot().addVirtualHost( "www.mysite.com/en", "/site/0" ).originalRequest(
            "www.mysite.com", "/en/political news shortcut" ).requestedSite( 0,
                                                                             "political news shortcut" ).requestedPortalAt().siteSetupAtPath(
            "/en" )

            .back().setup( httpServletRequest );

        PortalResponse portalResponse = PortalResponse.createRedirect(
            createRedirectInstruction( new SitePath( new SiteKey( 0 ), new Path( "/news/local politics" ) ) ) );
        portalResponseProcessor.setResponse( portalResponse );

        // exercise
        portalResponseProcessor.serveResponse();

        // verify
        assertEquals( "/en/news/local+politics", httpServletResponse.getHeader( "Location" ) );
    }

    /**
     * site-0.properties cms.site.createUrlAsPath = false
     * Deployed @ /
     * Request is via portal @ http://localhost/site/0/political news shortcut
     */
    @Test
    public void redirecToSitePath_when_deployed_at_root_with_no_vhost_and_portal_requested_at_localhost_with_absolute_urls()
        throws Exception
    {
        // setup
        portalRequest.setOriginalUrl( "http://localhost/site/0/political news shortcut" );

        new DeploymentAndRequestSetup().appDeployedAtRoot().originalRequest( "localhost", "/site/0/political news shortcut" ).requestedSite(
            0, "political news shortcut" ).requestedPortalAt().siteSetupAtDefaultPath().back().setup( httpServletRequest );

        final SitePath sitePath = new SitePath( new SiteKey( 0 ), new Path( "/news/local politics" ) );
        PortalResponse portalResponse = PortalResponse.createRedirect( createRedirectInstruction( sitePath ) );
        portalResponseProcessor.setResponse( portalResponse );

        // exercise
        portalResponseProcessor.serveResponse();

        // verify
        assertEquals( "/site/0/news/local+politics", httpServletResponse.getHeader( "Location" ) );
    }

    /**
     * site-0.properties cms.site.createUrlAsPath = true
     * Deployed @ /
     * Request is via portal @ http://localhost/site/0/political news shortcut
     */
    @Test
    public void redirecToSitePath_when_deployed_at_root_with_no_vhost_and_portal_requested_at_localhost_with_relative_urls()
        throws Exception
    {
        // setup
        portalRequest.setOriginalUrl( "http://localhost/site/0/political news shortcut" );

        new DeploymentAndRequestSetup().appDeployedAtRoot().originalRequest( "localhost", "/site/0/political news shortcut" ).requestedSite(
            0, "political news shortcut" ).requestedPortalAt().siteSetupAtDefaultPath().back().setup( httpServletRequest );

        final SitePath sitePath = new SitePath( new SiteKey( 0 ), new Path( "/news/local politics" ) );
        PortalResponse portalResponse = PortalResponse.createRedirect( createRedirectInstruction( sitePath ) );
        portalResponseProcessor.setResponse( portalResponse );

        // exercise
        portalResponseProcessor.serveResponse();

        // verify
        assertEquals( "/site/0/news/local+politics", httpServletResponse.getHeader( "Location" ) );
    }

    /**
     * site-0.properties cms.site.createUrlAsPath = false
     * Deployed @ /cms
     * Request is via portal @ http://localhost/cms/site/0/political news shortcut
     */
    @Test
    public void redirectToSitePath_when_deployed_at_cms_with_no_vhost_and_portal_requested_at_localhost_with_absolute_urls()
        throws Exception
    {
        // setup
        portalRequest.setOriginalUrl( "http://localhost/cms/site/0/political news shortcut" );

        new DeploymentAndRequestSetup().appDeployedAt( "/cms" ).originalRequest( "localhost",
                                                                                 "/cms/site/0/political news shortcut" ).requestedSite( 0,
                                                                                                                                        "political news shortcut" ).requestedPortalAt().siteSetupAtDefaultPath().back().setup(
            httpServletRequest );

        final SitePath sitePath = new SitePath( new SiteKey( 0 ), new Path( "/news/local politics" ) );
        PortalResponse portalResponse = PortalResponse.createRedirect( createRedirectInstruction( sitePath ) );
        portalResponseProcessor.setResponse( portalResponse );

        // exercise
        portalResponseProcessor.serveResponse();

        // verify
        assertEquals( "/cms/site/0/news/local+politics", httpServletResponse.getHeader( "Location" ) );
    }

    /**
     * site-0.properties cms.site.createUrlAsPath = true
     * Deployed @ /cms
     * Request is via portal @ http://localhost/cms/site/0/political news shortcut
     */
    @Test
    public void redirectToSitePath_when_deployed_at_cms_with_no_vhost_and_portal_requested_at_localhost_with_relative_urls()
        throws Exception
    {
        // setup
        portalRequest.setOriginalUrl( "http://localhost/cms/site/0/political news shortcut" );

        new DeploymentAndRequestSetup().appDeployedAt( "/cms" ).originalRequest( "localhost",
                                                                                 "/cms/site/0/political news shortcut" ).requestedSite( 0,
                                                                                                                                        "political news shortcut" ).requestedPortalAt().siteSetupAtDefaultPath().back().setup(
            httpServletRequest );

        final SitePath sitePath = new SitePath( new SiteKey( 0 ), new Path( "/news/local politics" ) );
        PortalResponse portalResponse = PortalResponse.createRedirect( createRedirectInstruction( sitePath ) );
        portalResponseProcessor.setResponse( portalResponse );

        // exercise
        portalResponseProcessor.serveResponse();

        // verify
        assertEquals( "/cms/site/0/news/local+politics", httpServletResponse.getHeader( "Location" ) );
    }

    /**
     * site-0.properties cms.site.createUrlAsPath = false
     * Deployed @ /cms
     * Request is via portal @ http://www.mysite.com/cms/site/0/political news shortcut
     */
    @Test
    public void redirectToSitePath_when_deployed_at_cms_with_no_vhost_and_portal_requested_at_namedhost_with_absolute_urls()
        throws Exception
    {
        // setup
        portalRequest.setOriginalUrl( "http://www.mysite.com/cms/site/0/political news shortcut" );

        new DeploymentAndRequestSetup().appDeployedAt( "/cms" ).originalRequest( "www.mysite.com",
                                                                                 "/cms/site/0/political news shortcut" ).requestedSite( 0,
                                                                                                                                        "political news shortcut" ).requestedPortalAt().siteSetupAtDefaultPath().back().setup(
            httpServletRequest );

        final SitePath sitePath = new SitePath( new SiteKey( 0 ), new Path( "/news/local politics" ) );
        PortalResponse portalResponse = PortalResponse.createRedirect( createRedirectInstruction( sitePath ) );
        portalResponseProcessor.setResponse( portalResponse );

        // exercise
        portalResponseProcessor.serveResponse();

        // verify
        assertEquals( "/cms/site/0/news/local+politics", httpServletResponse.getHeader( "Location" ) );
    }

    /**
     * site-0.properties cms.site.createUrlAsPath = true
     * Deployed @ /cms
     * Request is via portal @ http://www.mysite.com/cms/site/0/political news shortcut
     */
    @Test
    public void redirectToSitePath_when_deployed_at_cms_with_no_vhost_and_portal_requested_at_namedhost_with_realtive_urls()
        throws Exception
    {
        // setup
        portalRequest.setOriginalUrl( "http://www.mysite.com/cms/site/0/political news shortcut" );

        new DeploymentAndRequestSetup().appDeployedAt( "/cms" ).originalRequest( "www.mysite.com",
                                                                                 "/cms/site/0/political news shortcut" ).requestedSite( 0,
                                                                                                                                        "political news shortcut" ).requestedPortalAt().siteSetupAtDefaultPath().back().setup(
            httpServletRequest );

        final SitePath sitePath = new SitePath( new SiteKey( 0 ), new Path( "/news/local politics" ) );
        PortalResponse portalResponse = PortalResponse.createRedirect( createRedirectInstruction( sitePath ) );
        portalResponseProcessor.setResponse( portalResponse );

        // exercise
        portalResponseProcessor.serveResponse();

        // verify
        assertEquals( "/cms/site/0/news/local+politics", httpServletResponse.getHeader( "Location" ) );
    }

    /**
     * site-0.properties cms.site.createUrlAsPath = false
     * Deployed @ /
     * Request is via portal @ http://localhost/site/0/påskenyheter shortcut
     */
    @Test
    public void redirecToSitePath_when_deployed_at_root_with_no_vhost_and_portal_requested_at_localhost_with_absolute_urls_and_non_english_characther_in_redirect_path()
        throws Exception
    {
        // setup
        portalRequest.setOriginalUrl( "http://localhost/site/0/påskenyheter shortcut" );

        new DeploymentAndRequestSetup().appDeployedAtRoot().originalRequest( "localhost", "/site/0/påskenyheter shortcut" ).requestedSite(
            0, "political news shortcut" ).requestedPortalAt().siteSetupAtDefaultPath().back().setup( httpServletRequest );

        URLDecoder.decode( "/J%C3%B8rund", "UTF-8" );
        LOG.info( URLEncoder.encode( "/Nyheter til påske", "UTF-8" ) );
        final SitePath sitePath = new SitePath( new SiteKey( 0 ), new Path( "/news/" + decode( "Nyheter+til+p%C3%A5ske" ) ) );
        PortalResponse portalResponse = PortalResponse.createRedirect( createRedirectInstruction( sitePath ) );
        portalResponseProcessor.setResponse( portalResponse );

        // exercise
        portalResponseProcessor.serveResponse();

        // verify
        assertEquals( "/site/0/news/Nyheter+til+p%C3%A5ske", httpServletResponse.getHeader( "Location" ) );
    }

    /**
     * site-0.properties cms.site.createUrlAsPath = true
     * Deployed @ /
     * Request is via admin preview @ http://localhost/admin/preview/0/political news shortcut
     */
    @Test
    public void redirecToSitePath_when_deployed_at_root_with_no_vhost_and_preview_requested_at_localhost_with_relative_urls()
        throws Exception
    {
        // setup
        portalRequest.setOriginalUrl( "http://localhost/admin/preview/0/political news shortcut?" );

        new DeploymentAndRequestSetup().appDeployedAtRoot().originalRequest( "localhost",
                                                                             "/admin/preview/0/political news shortcut" ).requestedSite( 0,
                                                                                                                                         "political news shortcut" ).requestedAdminPreviewAt().setupAtDefaultPath().back().setup(
            httpServletRequest );

        final SitePath sitePath = new SitePath( new SiteKey( 0 ), new Path( "/news/local politics" ) );
        PortalResponse portalResponse = PortalResponse.createRedirect( createRedirectInstruction( sitePath ) );
        portalResponseProcessor.setResponse( portalResponse );

        // exercise
        portalResponseProcessor.serveResponse();

        // verify
        assertEquals( "/admin/preview/0/news/local+politics", httpServletResponse.getHeader( "Location" ) );
    }

    /**
     * site-0.properties cms.site.createUrlAsPath = false
     * Deployed @ /
     * Request is via admin preview @ http://localhost/admin/preview/0/political news shortcut
     */
    @Test
    public void redirecToSitePath_when_deployed_at_root_with_no_vhost_and_preview_requested_at_localhost_with_absolute_urls()
        throws Exception
    {
        // setup
        portalRequest.setOriginalUrl( "http://localhost/admin/preview/0/political news shortcut?" );

        new DeploymentAndRequestSetup().appDeployedAtRoot().originalRequest( "localhost",
                                                                             "/admin/preview/0/political news shortcut" ).requestedSite( 0,
                                                                                                                                         "political news shortcut" ).requestedAdminPreviewAt().setupAtDefaultPath().back().setup(
            httpServletRequest );

        final SitePath sitePath = new SitePath( new SiteKey( 0 ), new Path( "/news/local politics" ) );
        PortalResponse portalResponse = PortalResponse.createRedirect( createRedirectInstruction( sitePath ) );
        portalResponseProcessor.setResponse( portalResponse );

        // exercise
        portalResponseProcessor.serveResponse();

        // verify
        assertEquals( "/admin/preview/0/news/local+politics", httpServletResponse.getHeader( "Location" ) );
    }


    /**
     * site-0.properties cms.site.createUrlAsPath = false
     * Deployed @ /
     * Request is via admin debug @ http://localhost/admin/site/0/political news shortcut
     */
    @Test
    public void redirecToSitePath_when_deployed_at_root_with_no_vhost_and_debug_requested_at_localhost_with_absolute_urls()
        throws Exception
    {
        // setup
        portalRequest.setOriginalUrl( "http://localhost/admin/site/0/political news shortcut?" );

        new DeploymentAndRequestSetup().appDeployedAtRoot().originalRequest( "localhost",
                                                                             "/admin/site/0/political news shortcut" ).requestedSite( 0,
                                                                                                                                      "political news shortcut" ).requestedAdminDebugAt().setupAtDefaultPath().back().setup(
            httpServletRequest );

        RenderTrace.enter();
        final SitePath sitePath = new SitePath( new SiteKey( 0 ), new Path( "/news/local politics" ) );
        PortalResponse portalResponse = PortalResponse.createRedirect( createRedirectInstruction( sitePath ) );
        portalResponseProcessor.setResponse( portalResponse );

        // exercise
        portalResponseProcessor.serveResponse();
        RenderTrace.exit();

        // verify
        assertEquals( "/admin/site/0/news/local+politics", httpServletResponse.getHeader( "Location" ) );
    }


    /**
     * site-0.properties cms.site.createUrlAsPath = false
     * Vhost: admin.enonic-cms.com = /admin
     * Deployed @ /
     * Request is via debug @ http://admin.enonic-cms.com/site/0/political news shortcut
     */
    @Test
    public void redirecToSitePath_when_deployed_at_root_with_vhost_and_debug_requested_at_vhost_with_absolute_urls()
        throws Exception
    {
        // setup
        portalRequest.setOriginalUrl( "http://admin.enonic-cms.com/site/0/political news shortcut?" );

        new DeploymentAndRequestSetup().appDeployedAtRoot().addVirtualHost( "admin.enonic-cms.com", "/admin" ).originalRequest(
            "admin.enonic-cms.com", "/site/0/political news shortcut" ).requestedSite( 0,
                                                                                       "political news shortcut" ).requestedAdminDebugAt().setupAtRoot().back().setup(
            httpServletRequest );

        RenderTrace.enter();
        final SitePath sitePath = new SitePath( new SiteKey( 0 ), new Path( "/news/local politics" ) );
        PortalResponse portalResponse = PortalResponse.createRedirect( createRedirectInstruction( sitePath ) );
        portalResponseProcessor.setResponse( portalResponse );

        // exercise
        portalResponseProcessor.serveResponse();
        RenderTrace.exit();

        // verify
        assertEquals( "/site/0/news/local+politics", httpServletResponse.getHeader( "Location" ) );
    }

    /**
     * site-0.properties cms.site.createUrlAsPath = false
     * Vhost: www.mysite.com/admin = /admin
     * Deployed @ /
     * Request is via debug @ http://www.mysite.com/admin/site/0/political news shortcut
     */
    @Test
    public void redirecToSitePath_when_deployed_at_root_with_vhost_and_debug_requested_at_vhost_under_admin_with_absolute_urls()
        throws Exception
    {
        // setup
        portalRequest.setOriginalUrl( "http://www.mysite.com/admin/site/0/political news shortcut?" );

        new DeploymentAndRequestSetup().appDeployedAtRoot().addVirtualHost( "www.mysite.com/admin", "/admin" ).originalRequest(
            "www.mysite.com", "/admin/site/0/political news shortcut" ).requestedSite( 0,
                                                                                       "political news shortcut" ).requestedAdminDebugAt().setupAtDefaultPath().back().setup(
            httpServletRequest );

        RenderTrace.enter();
        final SitePath sitePath = new SitePath( new SiteKey( 0 ), new Path( "/news/local politics" ) );
        PortalResponse portalResponse = PortalResponse.createRedirect( createRedirectInstruction( sitePath ) );
        portalResponseProcessor.setResponse( portalResponse );

        // exercise
        portalResponseProcessor.serveResponse();
        RenderTrace.exit();

        // verify
        assertEquals( "/admin/site/0/news/local+politics", httpServletResponse.getHeader( "Location" ) );
    }

    /**
     * site-0.properties cms.site.createUrlAsPath = true
     * Vhost: www.mysite.com/admin = /admin
     * Deployed @ /
     * Request is via debug @ http://www.mysite.com/admin/site/0/political news shortcut
     */
    @Test
    public void redirecToSitePath_when_deployed_at_root_with_vhost_and_debug_requested_at_vhost_under_admin_with_relative_urls()
        throws Exception
    {
        // setup
        portalRequest.setOriginalUrl( "http://www.mysite.com/admin/site/0/political news shortcut?" );

        new DeploymentAndRequestSetup().appDeployedAtRoot().addVirtualHost( "www.mysite.com/admin", "/admin" ).originalRequest(
            "www.mysite.com", "/admin/site/0/political news shortcut" ).requestedSite( 0,
                                                                                       "political news shortcut" ).requestedAdminDebugAt().setupAtDefaultPath().back().setup(
            httpServletRequest );

        RenderTrace.enter();
        final SitePath sitePath = new SitePath( new SiteKey( 0 ), new Path( "/news/local politics" ) );
        PortalResponse portalResponse = PortalResponse.createRedirect( createRedirectInstruction( sitePath ) );
        portalResponseProcessor.setResponse( portalResponse );

        // exercise
        portalResponseProcessor.serveResponse();
        RenderTrace.exit();

        // verify
        assertEquals( "/admin/site/0/news/local+politics", httpServletResponse.getHeader( "Location" ) );
    }

    /**
     * site-0.properties cms.site.createUrlAsPath = true
     * Deployed @ /
     * Vhost: admin.enonic-cms.com = /admin
     * Request is via admin debug @ http://admin.enonic-cms.com/site/0/political news shortcut
     */
    @Test
    public void redirecToSitePath_when_deployed_at_root_with_vhost_and_debug_requested_at_vhost_with_relative_urls()
        throws Exception
    {
        // setup
        portalRequest.setOriginalUrl( "http://admin.enonic-cms.com/site/0/political news shortcut?" );

        new DeploymentAndRequestSetup().appDeployedAtRoot().addVirtualHost( "admin.enonic-cms.com", "/admin" ).originalRequest(
            "admin.enonic-cms.com", "/site/0/political news shortcut" ).requestedSite( 0,
                                                                                       "political news shortcut" ).requestedAdminDebugAt().setupAtRoot().back().setup(
            httpServletRequest );

        RenderTrace.enter();
        final SitePath sitePath = new SitePath( new SiteKey( 0 ), new Path( "/news/local politics" ) );
        PortalResponse portalResponse = PortalResponse.createRedirect( createRedirectInstruction( sitePath ) );
        portalResponseProcessor.setResponse( portalResponse );

        // exercise
        portalResponseProcessor.serveResponse();
        RenderTrace.exit();

        // verify
        assertEquals( "/site/0/news/local+politics", httpServletResponse.getHeader( "Location" ) );
    }

    /**
     * site-0.properties cms.site.createUrlAsPath = true
     * Deployed @ /
     * Request is via admin debug @ http://localhost/admin/site/0/political news shortcut
     */
    @Test
    public void redirecToSitePath_when_deployed_at_root_with_no_vhost_and_debug_requested_at_localhost_with_relative_urls()
        throws Exception
    {
        // setup
        portalRequest.setOriginalUrl( "http://localhost/admin/site/0/political news shortcut?" );

        new DeploymentAndRequestSetup().appDeployedAtRoot().originalRequest( "localhost",
                                                                             "/admin/site/0/political news shortcut" ).requestedSite( 0,
                                                                                                                                      "political news shortcut" ).requestedAdminDebugAt().setupAtDefaultPath().back().setup(
            httpServletRequest );

        RenderTrace.enter();
        final SitePath sitePath = new SitePath( new SiteKey( 0 ), new Path( "/news/local politics" ) );
        PortalResponse portalResponse = PortalResponse.createRedirect( createRedirectInstruction( sitePath ) );
        portalResponseProcessor.setResponse( portalResponse );

        // exercise
        portalResponseProcessor.serveResponse();
        RenderTrace.exit();

        // verify
        assertEquals( "/admin/site/0/news/local+politics", httpServletResponse.getHeader( "Location" ) );
    }

    private static String decode( String s )
        throws UnsupportedEncodingException
    {
        return URLDecoder.decode( s, "UTF-8" );
    }
}
