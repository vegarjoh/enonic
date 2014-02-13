/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.framework.cache.standard;

import junit.framework.TestCase;

import com.enonic.cms.framework.cache.CacheFacade;
import com.enonic.cms.core.cluster.NopClusterEventPublisher;

import com.enonic.cms.core.config.ConfigProperties;

public class StandardCacheBasicTest
    extends TestCase
{
    private StandardCacheManager cacheManager;

    public void setUp()
        throws Exception
    {

        ConfigProperties props = new ConfigProperties();
        props.setProperty( "cms.cache.xslt.memoryCapacity", "10" );
        props.setProperty( "cms.cache.localization.memoryCapacity", "2" );

        this.cacheManager = new StandardCacheManager();
        this.cacheManager.setProperties( props );
        this.cacheManager.setClusterEventPublisher( new NopClusterEventPublisher() );
        this.cacheManager.afterPropertiesSet();
    }

    public void testGeneral()
    {
        CacheFacade cache = this.cacheManager.getXsltCache();

        assertNull( cache.get( null, "key" ) );
        assertNull( cache.get( "group", "key" ) );

        cache.put( null, "key", "value" );
        cache.put( "group", "key", "value" );

        assertEquals( "value", cache.get( null, "key" ) );
        assertEquals( "value", cache.get( "group", "key" ) );

        cache.remove( null, "key" );
        assertNull( cache.get( null, "key" ) );
        assertEquals( "value", cache.get( "group", "key" ) );

        cache.removeGroup( "group" );
        assertNull( cache.get( null, "key" ) );
        assertNull( cache.get( "group", "key" ) );
    }

    public void testOverflow()
    {
        CacheFacade cache = this.cacheManager.getLocalizationCache();

        cache.put( null, "key1", "value1" );
        cache.put( null, "key2", "value2" );

        assertEquals( "value1", cache.get( null, "key1" ) );
        assertEquals( "value2", cache.get( null, "key2" ) );

        cache.put( null, "key3", "value3" );
        assertNull( cache.get( null, "key1" ) );
        assertEquals( "value2", cache.get( null, "key2" ) );
        assertEquals( "value3", cache.get( null, "key3" ) );
    }

    public void testRemove()
    {
        CacheFacade cache = this.cacheManager.getXsltCache();

        cache.put( "group1", "key1", "value1" );
        cache.put( "group2", "key1", "value1" );
        cache.put( "group1", "key2", "value2" );

        assertEquals( "value1", cache.get( "group1", "key1" ) );
        assertEquals( "value2", cache.get( "group1", "key2" ) );
        assertEquals( "value1", cache.get( "group2", "key1" ) );

        cache.removeGroup( "group1" );

        assertNull( cache.get( "group1", "key1" ) );
        assertNull( cache.get( "group1", "key2" ) );
        assertEquals( "value1", cache.get( "group2", "key1" ) );

        cache.removeAll();

        assertNull( cache.get( "group1", "key1" ) );
        assertNull( cache.get( "group1", "key2" ) );
        assertNull( cache.get( "group2", "key1" ) );
        assertEquals( 0, cache.getCount() );
    }

    public void testExpire()
        throws Exception
    {
        CacheFacade cache = this.cacheManager.getXsltCache();

        cache.put( null, "key1", "value1" );
        cache.put( null, "key2", "value2", 1 );

        assertEquals( "value1", cache.get( null, "key1" ) );
        assertEquals( "value2", cache.get( null, "key2" ) );

        Thread.sleep( 100L );

        assertEquals( "value1", cache.get( null, "key1" ) );
        assertEquals( "value2", cache.get( null, "key2" ) );

        Thread.sleep( 2000L );

        assertEquals( "value1", cache.get( null, "key1" ) );
        assertNull( cache.get( null, "key2" ) );
    }

}
