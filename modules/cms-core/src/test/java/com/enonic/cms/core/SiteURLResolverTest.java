/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core;

import org.springframework.mock.web.MockHttpServletRequest;

import junit.framework.TestCase;

import com.enonic.cms.core.servlet.ServletRequestAccessor;
import com.enonic.cms.core.structure.SiteKey;
import com.enonic.cms.core.structure.SitePath;
import com.enonic.cms.core.structure.SitePropertyNames;

public class SiteURLResolverTest
    extends TestCase
{

    private SiteURLResolver siteURLResolver;

    private SiteKey siteKey1 = new SiteKey( 1 );

    private MockHttpServletRequest request;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        MockSitePropertiesService sitePropertiesService = new MockSitePropertiesService();

        siteURLResolver = new SiteURLResolver();
        siteURLResolver.setCharacterEncoding( "UTF-8" );
        siteURLResolver.setSitePropertiesService( sitePropertiesService );

        request = new MockHttpServletRequest();

        ServletRequestAccessor.setRequest( request );
    }

    public void testCreateFullPathForRedirectWithoutContextPath()
    {

        String fullPath = siteURLResolver.createFullPathForRedirect( request, siteKey1, "frontpage/news" );
        assertEquals( "/site/1/frontpage/news", fullPath );
    }

    public void testCreateFullPathForRedirectWithtContextPath()
    {

        request.setContextPath( "cms-server" );
        String fullPath = siteURLResolver.createFullPathForRedirect( request, siteKey1, "frontpage/news" );
        assertEquals( "cms-server/site/1/frontpage/news", fullPath );
    }

    public void testCreateUrlWithPropertyCreateUrlAsPathTrue()
    {

        MockSitePropertiesService sitePropertiesService = new MockSitePropertiesService();
        sitePropertiesService.setProperty( siteKey1, SitePropertyNames.CREATE_URL_AS_PATH_PROPERTY, "true" );

        SiteURLResolver siteURLResolver = new SiteURLResolver();
        siteURLResolver.setCharacterEncoding( "UTF-8" );
        siteURLResolver.setSitePropertiesService( sitePropertiesService );

        request.setScheme( "http" );
        request.setServerName( "localhost" );
        request.setRequestURI( "/site/1/" );

        String url;

        url = siteURLResolver.createUrl( request, new SitePath( siteKey1, new Path( "home" ) ), true );
        assertEquals( "/site/1/home", url );

        url = siteURLResolver.createUrl( request, new SitePath( siteKey1, new Path( "/home/" ) ), true );
        assertEquals( "/site/1/home/", url );

        url = siteURLResolver.createUrl( request, new SitePath( siteKey1, new Path( "home" ) ).addParam( "balle", "rusk" ), true );
        assertEquals( "/site/1/home?balle=rusk", url );

        url = siteURLResolver.createUrl( request, new SitePath( siteKey1, new Path( "home" ) ).addParam( "balle", "rusk" ), false );
        assertEquals( "/site/1/home", url );
    }

    public void testCreateUrlWithPropertyCreateUrlAsPathFalse()
    {

        MockSitePropertiesService sitePropertiesService = new MockSitePropertiesService();
        sitePropertiesService.setProperty( siteKey1, SitePropertyNames.CREATE_URL_AS_PATH_PROPERTY, "false" );

        SiteURLResolver siteURLResolver = new SiteURLResolver();
        siteURLResolver.setCharacterEncoding( "UTF-8" );
        siteURLResolver.setSitePropertiesService( sitePropertiesService );

        request.setScheme( "http" );
        request.setServerPort( 80 );
        request.setServerName( "localhost" );
        request.setRequestURI( "/site/1/" );

        String url;

        url = siteURLResolver.createUrl( request, new SitePath( siteKey1, new Path( "home" ) ), true );
        assertEquals( "http://localhost/site/1/home", url );

        url = siteURLResolver.createUrl( request, new SitePath( siteKey1, new Path( "/home/" ) ), true );
        assertEquals( "http://localhost/site/1/home/", url );

        url = siteURLResolver.createUrl( request, new SitePath( siteKey1, new Path( "home" ) ).addParam( "balle", "rusk" ), true );
        assertEquals( "http://localhost/site/1/home?balle=rusk", url );

        url = siteURLResolver.createUrl( request, new SitePath( siteKey1, new Path( "home" ) ).addParam( "balle", "rusk" ), false );
        assertEquals( "http://localhost/site/1/home", url );
    }

    public void testCreateHttpsUrl()
    {

        MockSitePropertiesService sitePropertiesService = new MockSitePropertiesService();
        sitePropertiesService.setProperty( siteKey1, SitePropertyNames.CREATE_URL_AS_PATH_PROPERTY, "false" );

        SiteURLResolver siteURLResolver = new SiteURLResolver();
        siteURLResolver.setCharacterEncoding( "UTF-8" );
        siteURLResolver.setSitePropertiesService( sitePropertiesService );

        request.setScheme( "https" );
        request.setServerPort( 443 );
        request.setServerName( "localhost" );
        request.setRequestURI( "/site/1/" );

        String url;

        url = siteURLResolver.createUrl( request, new SitePath( siteKey1, new Path( "home" ) ), true );
        assertEquals( "https://localhost/site/1/home", url );

        url = siteURLResolver.createUrl( request, new SitePath( siteKey1, new Path( "/home/" ) ), true );
        assertEquals( "https://localhost/site/1/home/", url );

        url = siteURLResolver.createUrl( request, new SitePath( siteKey1, new Path( "home" ) ).addParam( "balle", "rusk" ), true );
        assertEquals( "https://localhost/site/1/home?balle=rusk", url );

        url = siteURLResolver.createUrl( request, new SitePath( siteKey1, new Path( "home" ) ).addParam( "balle", "rusk" ), false );
        assertEquals( "https://localhost/site/1/home", url );
    }

    public void testGetPathUrlWithVHOSTSet()
    {

        request.setAttribute( "com.enonic.cms.core.vhost.BASE_PATH", "" );

        String path;

        path = siteURLResolver.createUrl( request, new SitePath( siteKey1, new Path( "home" ) ), false );
        assertEquals( "http://localhost/home", path );
    }
}
