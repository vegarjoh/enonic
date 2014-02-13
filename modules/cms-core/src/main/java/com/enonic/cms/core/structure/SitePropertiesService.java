/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.structure;

public interface SitePropertiesService
{
    void registerSitePropertiesListener( SitePropertiesListener listener );

    void reloadSiteProperties( SiteKey siteKey );

    SiteProperties getSiteProperties( SiteKey siteKey );

    String getSiteProperty( SiteKey siteKey, SitePropertyNames key );
}
