/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.web.portal;

import java.io.IOException;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import junit.framework.TestCase;

import com.enonic.cms.core.MockSitePropertiesService;
import com.enonic.cms.core.SiteURLResolver;
import com.enonic.cms.core.UrlPathHelperManager;

public class SiteRedirectHelperTest
    extends TestCase
{

    private SiteRedirectHelper siteRedirectHelper;

    private PortalSitePathResolver sitePathResolver;

    private SiteURLResolver siteURLResolver;

    private MockSitePropertiesService sitePropertiesService;

    private UrlPathHelperManager urlPathHelperManager;

    private MockHttpServletRequest request;

    private MockHttpServletResponse response;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        sitePropertiesService = new MockSitePropertiesService();

        urlPathHelperManager = new UrlPathHelperManager();

        sitePathResolver = new PortalSitePathResolver();
        sitePathResolver.setUrlPathHelperManager( urlPathHelperManager );
        sitePathResolver.setSitePathPrefix( SiteURLResolver.DEFAULT_SITEPATH_PREFIX );

        siteURLResolver = new SiteURLResolver();
        siteURLResolver.setCharacterEncoding( "UTF-8" );
        siteURLResolver.setSitePropertiesService( sitePropertiesService );

        siteRedirectHelper = new SiteRedirectHelper();
        siteRedirectHelper.setSitePathResolver( sitePathResolver );
        siteRedirectHelper.setSiteURLResolver( siteURLResolver );

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    public void testSendRedirectWithHttpUrl()
        throws IOException
    {

        request.setScheme( "http" );
        siteRedirectHelper.sendRedirect( request, response, "http://someurl.com" );
        assertEquals( "http://someurl.com", response.getHeader( "Location" ) );
    }

    public void testSendRedirectWithHttpsUrl()
        throws IOException
    {

        request.setScheme( "http" );
        siteRedirectHelper.sendRedirect( request, response, "https://someurl.com" );
        assertEquals( "https://someurl.com", response.getHeader( "Location" ) );
    }
}
