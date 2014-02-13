/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.store.dao;

import java.util.Collection;
import java.util.List;

import com.enonic.cms.core.resource.ResourceKey;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.structure.SiteEntity;
import com.enonic.cms.core.structure.SiteKey;
import com.enonic.cms.store.support.EntityPageList;

public interface SiteDao
    extends EntityDao<SiteEntity>
{
    SiteEntity findByKey( int siteKey );

    SiteEntity findByKey( SiteKey siteKey );

    List<SiteEntity> findByPublishPossible( final int contentTypeKey, final UserEntity user );

    Collection<SiteEntity> findByDefaultCss( ResourceKey resourceKey );

    List getResourceUsageCountDefaultCSS();

    void updateResourceCSSReference( ResourceKey oldResourceKey, ResourceKey newResourceKey );

    void updateResourceCSSReferencePrefix( String oldPrefix, String newPrefix );

    List<SiteEntity> findAll();

    EntityPageList<SiteEntity> findAll( int index, int count );

    void removeUsage( UserEntity user );
}
