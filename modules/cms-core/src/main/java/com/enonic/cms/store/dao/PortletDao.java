/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.store.dao;

import java.util.List;

import com.enonic.cms.core.resource.ResourceKey;
import com.enonic.cms.core.structure.SiteKey;
import com.enonic.cms.core.structure.portlet.PortletEntity;
import com.enonic.cms.store.support.EntityPageList;

public interface PortletDao
    extends EntityDao<PortletEntity>
{
    PortletEntity findByKey( int key );

    PortletEntity findBySiteKeyAndNameIgnoreCase( SiteKey siteKey, String name );

    List getResourceUsageCountStyle();

    List getResourceUsageCountBorder();

    List<PortletEntity> findByStyle( ResourceKey resourceKey );

    List<PortletEntity> findByBorder( ResourceKey resourceKey );

    void updateResourceStyleReference( ResourceKey oldResourceKey, ResourceKey newResourceKey );

    void updateResourceBorderReference( ResourceKey oldResourceKey, ResourceKey newResourceKey );

    void updateResourceStyleReferencePrefix( String oldPrefix, String newPrefix );

    void updateResourceBorderReferencePrefix( String oldPrefix, String newPrefix );

    List<PortletEntity> findAll();

    EntityPageList<PortletEntity> findAll( int index, int count );
}
