/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.resolver;

/**
 * Created by rmy - Date: May 5, 2009
 */
public interface CacheResolverValueService
{

    public String getCachedResolverValue( ResolverContext context, String sessionDeviceClassKey );

    public boolean clearCachedResolverValue( ResolverContext context, String cacheKey );

    public boolean setCachedResolverValue( ResolverContext context, String deviceClass, String cacheKey );


}
