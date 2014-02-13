/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.web.portal.services;

import junit.framework.TestCase;

import com.enonic.cms.core.MockSitePropertiesService;
import com.enonic.cms.core.structure.SiteKey;
import com.enonic.cms.core.structure.SitePropertyNames;
import com.enonic.cms.core.structure.SiteService;

import static org.easymock.EasyMock.isA;
import static org.easymock.classextension.EasyMock.createMock;

public class UserServicesAccessManagerTest
    extends TestCase
{

    private UserServicesAccessManager userServicesAccessManager;

    private MockSitePropertiesService sitePropertiesService;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        SiteService siteService = createMock( SiteService.class );
        siteService.checkSiteExist( isA( SiteKey.class ) );

        sitePropertiesService = new MockSitePropertiesService();

        UserServicesAccessManagerImpl userSvcAccessMan = new UserServicesAccessManagerImpl();
        userSvcAccessMan.setSitePropertiesService( sitePropertiesService );
        userSvcAccessMan.setSiteService( siteService );
        userServicesAccessManager = userSvcAccessMan;
    }

    private void assertDenied( boolean allowed )
    {
        assertFalse( "Access to http service should have been denied", allowed );
    }

    private void assertAllowed( boolean allowed )
    {
        assertTrue( "Access to http service should have been allowed", allowed );
    }

    public void testDefaultDeny()
    {
        SiteKey site = new SiteKey( 0 );

        // default is to deny access: cms.site.httpServices.deny = *
        boolean allowed = userServicesAccessManager.isOperationAllowed( site, "user", "create" );
        assertDenied( allowed );

        allowed = userServicesAccessManager.isOperationAllowed( site, "content", "modify" );
        assertDenied( allowed );

        allowed = userServicesAccessManager.isOperationAllowed( site, "content", "create" );
        assertDenied( allowed );

        allowed = userServicesAccessManager.isOperationAllowed( site, "portal", "forceLocale" );
        assertDenied( allowed );
    }

    public void testDefaultAllow()
    {
        SiteKey site = new SiteKey( 0 );
        sitePropertiesService.setProperty( site, SitePropertyNames.HTTP_SERVICES_ALLOW_PROPERTY, "*" );

        boolean allowed = userServicesAccessManager.isOperationAllowed( site, "user", "create" );
        assertAllowed( allowed );

        allowed = userServicesAccessManager.isOperationAllowed( site, "content", "modify" );
        assertAllowed( allowed );

        allowed = userServicesAccessManager.isOperationAllowed( site, "content", "create" );
        assertAllowed( allowed );

        allowed = userServicesAccessManager.isOperationAllowed( site, "portal", "forceLocale" );
        assertAllowed( allowed );
    }

    public void testDenyService()
    {
        SiteKey site = new SiteKey( 0 );
        sitePropertiesService.setProperty( site, SitePropertyNames.HTTP_SERVICES_ALLOW_PROPERTY, "*" );
        sitePropertiesService.setProperty( site, SitePropertyNames.HTTP_SERVICES_DENY_PROPERTY, "content.*" );

        boolean allowed = userServicesAccessManager.isOperationAllowed( site, "user", "create" );
        assertAllowed( allowed );

        allowed = userServicesAccessManager.isOperationAllowed( site, "user", "modify" );
        assertAllowed( allowed );

        allowed = userServicesAccessManager.isOperationAllowed( site, "content", "modify" );
        assertDenied( allowed );

        allowed = userServicesAccessManager.isOperationAllowed( site, "content", "create" );
        assertDenied( allowed );

        allowed = userServicesAccessManager.isOperationAllowed( site, "content", "changepwd" );
        assertDenied( allowed );

        allowed = userServicesAccessManager.isOperationAllowed( site, "portal", "forceLocale" );
        assertAllowed( allowed );
    }

    public void testAllowServices()
    {
        SiteKey site = new SiteKey( 0 );
        sitePropertiesService.setProperty( site, SitePropertyNames.HTTP_SERVICES_DENY_PROPERTY, "*" );
        sitePropertiesService.setProperty( site, SitePropertyNames.HTTP_SERVICES_ALLOW_PROPERTY, "content.*,portal.*" );

        boolean allowed = userServicesAccessManager.isOperationAllowed( site, "user", "create" );
        assertDenied( allowed );

        allowed = userServicesAccessManager.isOperationAllowed( site, "user", "modify" );
        assertDenied( allowed );

        allowed = userServicesAccessManager.isOperationAllowed( site, "content", "modify" );
        assertAllowed( allowed );

        allowed = userServicesAccessManager.isOperationAllowed( site, "content", "create" );
        assertAllowed( allowed );

        allowed = userServicesAccessManager.isOperationAllowed( site, "content", "changepwd" );
        assertAllowed( allowed );

        allowed = userServicesAccessManager.isOperationAllowed( site, "portal", "forceLocale" );
        assertAllowed( allowed );
    }

    public void testAllowOperations()
    {
        SiteKey site = new SiteKey( 0 );
        sitePropertiesService.setProperty( site, SitePropertyNames.HTTP_SERVICES_DENY_PROPERTY, "*" );
        sitePropertiesService.setProperty( site, SitePropertyNames.HTTP_SERVICES_ALLOW_PROPERTY, "content.modify,content.changepwd,portal.forceLocale" );

        boolean allowed = userServicesAccessManager.isOperationAllowed( site, "user", "create" );
        assertDenied( allowed );

        allowed = userServicesAccessManager.isOperationAllowed( site, "user", "modify" );
        assertDenied( allowed );

        allowed = userServicesAccessManager.isOperationAllowed( site, "content", "modify" );
        assertAllowed( allowed );

        allowed = userServicesAccessManager.isOperationAllowed( site, "content", "create" );
        assertDenied( allowed );

        allowed = userServicesAccessManager.isOperationAllowed( site, "content", "changepwd" );
        assertAllowed( allowed );

        allowed = userServicesAccessManager.isOperationAllowed( site, "portal", "forceLocale" );
        assertAllowed( allowed );

        allowed = userServicesAccessManager.isOperationAllowed( site, "portal", "forceDeviceClass" );
        assertDenied( allowed );
    }

    public void testDenyOperations()
    {
        SiteKey site = new SiteKey( 0 );
        sitePropertiesService.setProperty( site, SitePropertyNames.HTTP_SERVICES_DENY_PROPERTY, "*,content.modify,content.changepwd,portal.forceLocale" );
        sitePropertiesService.setProperty( site, SitePropertyNames.HTTP_SERVICES_ALLOW_PROPERTY, "content.*,portal.*" );

        boolean allowed = userServicesAccessManager.isOperationAllowed( site, "user", "create" );
        assertDenied( allowed );

        allowed = userServicesAccessManager.isOperationAllowed( site, "user", "modify" );
        assertDenied( allowed );

        allowed = userServicesAccessManager.isOperationAllowed( site, "content", "modify" );
        assertDenied( allowed );

        allowed = userServicesAccessManager.isOperationAllowed( site, "content", "create" );
        assertAllowed( allowed );

        allowed = userServicesAccessManager.isOperationAllowed( site, "content", "changepwd" );
        assertDenied( allowed );

        allowed = userServicesAccessManager.isOperationAllowed( site, "portal", "forceLocale" );
        assertDenied( allowed );

        allowed = userServicesAccessManager.isOperationAllowed( site, "portal", "forceDeviceClass" );
        assertAllowed( allowed );
    }

    public void testInvalidConfigurationDuplicated()
    {
        SiteKey site = new SiteKey( 0 );
        sitePropertiesService.setProperty( site, SitePropertyNames.HTTP_SERVICES_DENY_PROPERTY,
                                           "*,content.modify , content.changepwd , portal.forceLocale , content.*" );
        sitePropertiesService.setProperty( site, SitePropertyNames.HTTP_SERVICES_ALLOW_PROPERTY, "content.* , portal.*" );

        try
        {
            userServicesAccessManager.isOperationAllowed( site, "user", "create" );
            fail( "Expected IllegalArgumentException" );
        }
        catch ( Exception e )
        {
            assertTrue( e instanceof IllegalArgumentException );
            assertTrue( e.getMessage().contains( "content.*" ) );
        }
    }

    public void testInvalidConfigurationDuplicatedAllowDeny()
    {
        SiteKey site = new SiteKey( 0 );
        sitePropertiesService.setProperty( site, SitePropertyNames.HTTP_SERVICES_DENY_PROPERTY, "*" );
        sitePropertiesService.setProperty( site, SitePropertyNames.HTTP_SERVICES_ALLOW_PROPERTY, "*" );

        try
        {
            userServicesAccessManager.isOperationAllowed( site, "user", "create" );
            fail( "Expected IllegalArgumentException" );
        }
        catch ( Exception e )
        {
            assertTrue( e instanceof IllegalArgumentException );
        }
    }

}
