/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.image.cache;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.enonic.cms.framework.cache.CacheManager;
import com.enonic.cms.framework.util.ImageHelper;

@Component("imageCache")
public final class ImageCacheFactory
    implements FactoryBean<ImageCache>
{
    private CacheManager cacheManager;

    @Autowired
    public void setCacheManager( final CacheManager cacheManager )
    {
        this.cacheManager = cacheManager;
    }

    @Value("${cms.portal.image.minSizeForProgressiveLoading}")
    public void setLongestSize( int minSizeForProgressiveLoading )
    {
        ImageHelper.minSizeForProgressiveLoading = minSizeForProgressiveLoading;
    }

    public ImageCache getObject()
    {
        return new WrappedImageCache( this.cacheManager.getImageCache() );
    }

    public Class getObjectType()
    {
        return ImageCache.class;
    }

    public boolean isSingleton()
    {
        return true;
    }
}
