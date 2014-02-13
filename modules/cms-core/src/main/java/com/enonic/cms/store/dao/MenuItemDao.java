/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.store.dao;

import java.util.Collection;
import java.util.List;

import com.enonic.cms.core.structure.menuitem.MenuItemEntity;
import com.enonic.cms.core.structure.menuitem.MenuItemKey;
import com.enonic.cms.core.structure.menuitem.MenuItemSpecification;
import com.enonic.cms.store.support.EntityPageList;

public interface MenuItemDao
    extends EntityDao<MenuItemEntity>
{
    Collection<MenuItemEntity> findAll();

    List<MenuItemEntity> findBySpecification( MenuItemSpecification spec );

    @Deprecated
    MenuItemEntity findByKey( int menuItemKey );

    MenuItemEntity findByKey( MenuItemKey menuItemKey );

    List<MenuItemEntity> findByKeys( Collection<MenuItemKey> menuItemKeys );

    /**
     * Find menu items by site key.
     */
    Collection<MenuItemEntity> findBySiteKey( int siteKey );

    Collection<MenuItemEntity> findByPageTemplate( int pageTemplateKey );

    /**
     * Find top level menu items.
     */
    Collection<MenuItemEntity> findTopMenuItems( int siteKey );

    /**
     * Find content page.
     */
    MenuItemEntity findContentPage( int siteKey, int contentKey );

    EntityPageList<MenuItemEntity> findAll( int index, int count );
}
