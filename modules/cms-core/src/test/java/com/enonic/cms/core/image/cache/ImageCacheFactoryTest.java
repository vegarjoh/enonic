/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.image.cache;

import org.junit.Test;
import org.mockito.Mockito;

import com.enonic.cms.framework.cache.CacheFacade;
import com.enonic.cms.framework.cache.CacheManager;

import static org.junit.Assert.*;

public class ImageCacheFactoryTest
{
    @Test
    public void testGetObjectType()
    {
        final ImageCacheFactory factory = new ImageCacheFactory();
        assertEquals( ImageCache.class, factory.getObjectType() );
    }

    @Test
    public void testIsSingleton()
    {
        final ImageCacheFactory factory = new ImageCacheFactory();
        assertTrue( factory.isSingleton() );
    }

    @Test
    public void testGetObject()
    {
        final CacheManager manager = Mockito.mock( CacheManager.class );
        final CacheFacade facade = Mockito.mock( CacheFacade.class );

        Mockito.when( manager.getImageCache() ).thenReturn( facade );

        final ImageCacheFactory factory = new ImageCacheFactory();
        factory.setCacheManager( manager );

        final ImageCache cache = factory.getObject();
        assertNotNull( cache );

        Mockito.verify( manager, Mockito.times( 1 ) ).getImageCache();
    }
}
