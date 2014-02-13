/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.structure;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component("siteContextManager")
public class SiteContextManager
{

    private Map<SiteKey, SiteContext> siteContextMap = new HashMap<SiteKey, SiteContext>();

    public void registerSiteContext( SiteContext siteContext )
    {
        siteContextMap.put( siteContext.getSiteKey(), siteContext );
    }

    public void unregisterSiteContext( SiteKey siteKey )
    {
        siteContextMap.remove( siteKey );
    }

    public SiteContext getSiteContext( SiteKey siteKey )
    {
        return siteContextMap.get( siteKey );
    }

    public boolean isRegistered( SiteKey siteKey )
    {
        return siteContextMap.containsKey( siteKey );
    }
}
