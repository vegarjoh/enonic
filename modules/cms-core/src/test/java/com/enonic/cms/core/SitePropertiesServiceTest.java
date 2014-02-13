/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.google.common.collect.Lists;

import com.enonic.cms.core.structure.SiteEntity;
import com.enonic.cms.core.structure.SiteKey;
import com.enonic.cms.core.structure.SitePropertiesServiceImpl;
import com.enonic.cms.core.structure.SitePropertyNames;
import com.enonic.cms.store.dao.SiteDao;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.*;

public class SitePropertiesServiceTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static final int TEST_SITE_KEY_ID = 0;

    private static final SitePropertyNames BOOLEAN_TRUE = SitePropertyNames.ATTACHMENT_CACHE_HEADERS_ENABLED;

    private static final SitePropertyNames BOOLEAN_FALSE = SitePropertyNames.ATTACHMENT_CACHE_HEADERS_FORCENOCACHE;

    private static final SitePropertyNames BOOLEAN_TRUE_WHITESPACE = SitePropertyNames.ATTACHMENT_CACHE_HEADERS_MAXAGE;

    private static final SitePropertyNames BOOLEAN_FALSE_WHITESPACE = SitePropertyNames.RESOURCE_CACHE_HEADERS_ENABLED;

    private static final SitePropertyNames INTEGER_0 = SitePropertyNames.RESOURCE_CACHE_HEADERS_FORCENOCACHE;

    private static final SitePropertyNames INTEGER_POSITIVE_WHITESPACES = SitePropertyNames.IMAGE_CACHE_HEADERS_ENABLED;

    private static final SitePropertyNames INTEGER_POSITIVE = SitePropertyNames.RESOURCE_CACHE_HEADERS_MAXAGE;

    private static final SitePropertyNames TEXT_PROPERTY = SitePropertyNames.IMAGE_CACHE_HEADERS_FORCENOCACHE;

    private static final SitePropertyNames EMPTY_PROPERTY = SitePropertyNames.PAGE_CACHE_HEADERS_ENABLED;

    private static final SitePropertyNames EMPTY_PROPERTY_WHITESPACES = SitePropertyNames.PAGE_CACHE_HEADERS_FORCENOCACHE;

    private static final Integer INTEGER_POSITIVE_VALUE = 100;

    private SitePropertiesServiceImpl sitePropertiesService;

    private ResourceLoader resourceLoader = createMock( ResourceLoader.class );

    private SiteDao siteDao = createMock( SiteDao.class );

    private SiteKey siteKey = new SiteKey( TEST_SITE_KEY_ID );

    @Before
    public void setUp()
        throws Exception
    {
        mockAndLoadTestProperties();
        setupSitePropertiesService();
    }

    private void mockAndLoadTestProperties()
    {
        expect( resourceLoader.getResource( isA( String.class ) ) ).andReturn( getLocalTestDefaultPropertyResouce() );
        expect( resourceLoader.getResource( isA( String.class ) ) ).andReturn( getLocalTestSitePropertyResouce() );
        replay( resourceLoader );

        SiteEntity site = new SiteEntity();
        site.setKey( siteKey.toInt() );

        expect( siteDao.findAll() ).andReturn( Lists.newArrayList( site ) );
        replay( siteDao );
    }

    private void setupSitePropertiesService()
        throws Exception
    {
        sitePropertiesService = new SitePropertiesServiceImpl();
        sitePropertiesService.setSiteDao( siteDao );
        sitePropertiesService.setHomeDir( folder.newFolder( "cms-home" ) );
        sitePropertiesService.setResourceLoader( resourceLoader );
        sitePropertiesService.start();
    }

    private Resource getLocalTestSitePropertyResouce()
    {
        ResourceLoader testResourceLoader = new FileSystemResourceLoader();
        Resource testResource = testResourceLoader.getResource( "classpath:com/enonic/cms/core/test.site.properties" );

        if ( !testResource.exists() )
        {
            fail( "Could not load test resource: " + testResource );
        }
        return testResource;
    }

    private Resource getLocalTestDefaultPropertyResouce()
    {
        ResourceLoader testResourceLoader = new FileSystemResourceLoader();
        Resource testResource = testResourceLoader.getResource( "classpath:com/enonic/cms/core/test.default.site.properties" );

        if ( !testResource.exists() )
        {
            fail( "Could not load test resource: " + testResource );
        }
        return testResource;
    }

    /* Shorthands */

    private String getProp( SitePropertyNames propertyName )
    {
        return sitePropertiesService.getSiteProperties( siteKey ).getProperty( propertyName.getKeyName() );
    }

    private Boolean getBooleanProp( SitePropertyNames propertyName )
    {
        return sitePropertiesService.getPropertyAsBoolean( propertyName, siteKey );
    }

    private Integer getIntegerProp( SitePropertyNames propertyName )
    {
        return sitePropertiesService.getSiteProperties( siteKey ).getPropertyAsInteger( propertyName );
    }

    @Test
    public void testGetPropertyAsInteger()
    {
        assertEquals( new Integer( 0 ), getIntegerProp( INTEGER_0 ) );
        assertEquals( "INTEGER_POSITIVE should be " + INTEGER_POSITIVE_VALUE, INTEGER_POSITIVE_VALUE, getIntegerProp( INTEGER_POSITIVE ) );
        assertEquals( "INTEGER_POSITIVE_WHITESPACES should be " + INTEGER_POSITIVE_VALUE, INTEGER_POSITIVE_VALUE,
                      getIntegerProp( INTEGER_POSITIVE_WHITESPACES ) );
    }

    @Test
    public void testBooleanPropertiesGetter()
    {
        assertTrue( "BOOLEAN_TRUE expected to be true", getBooleanProp( BOOLEAN_TRUE ) );
        assertFalse( "BOOLEAN_FALSE expected to be false", getBooleanProp( BOOLEAN_FALSE ) );
        assertTrue( "BOOLEAN_TRUE_WHITESPACE expected to be true", getBooleanProp( BOOLEAN_TRUE_WHITESPACE ) );
        assertFalse( "BOOLEAN_FALSE_WHITESPACE expected to be false", getBooleanProp( BOOLEAN_FALSE_WHITESPACE ) );
    }

    @Test
    public void testEmptyProperty()
    {
        assertNull( "EMPTY_PROPERTY expected to be null", getProp( EMPTY_PROPERTY ) );
        assertNull( "EMPTY_PROPERTY_WHITESPACES expected to be null", getProp( EMPTY_PROPERTY_WHITESPACES ) );
    }

    @Test
    public void testEmptyBooleanProperty()
    {
        assertFalse( "EMPTY_PROPERTY expected to be false", getBooleanProp( EMPTY_PROPERTY ) );
        assertFalse( "EMPTY_PROPERTY_WHITESPACES expected to be false", getBooleanProp( EMPTY_PROPERTY_WHITESPACES ) );
    }

    @Test
    public void testEmptyIntegerProperty()
    {
        assertNull( "EMPTY_PROPERTY expected to be null", getIntegerProp( EMPTY_PROPERTY ) );
        assertNull( "EMPTY_PROPERTY_WHITESPACES expected to be null", getIntegerProp( EMPTY_PROPERTY_WHITESPACES ) );
    }

    @Test
    public void testInvalidBooleanProperty()
    {
        assertFalse( "Invalid boolean-value, expected to be false", getBooleanProp( TEXT_PROPERTY ) );
    }

    @Test
    public void testSiteOverridesDefaultProperty()
    {
        String testProp = getProp( TEXT_PROPERTY );
        assertEquals( "text.property expected to be 'bar', not 'foo'", "bar", testProp );
    }

    @Test(expected = NumberFormatException.class)
    public void testInvalidIntegerProperty()
    {
        getIntegerProp( BOOLEAN_TRUE );
    }

    @After
    public void verifyAndTeardown()
    {
        verify( resourceLoader );
    }

}
