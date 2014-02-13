/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.store.dao;

import org.springframework.stereotype.Repository;

import com.enonic.cms.core.structure.menuitem.MenuItemContentEntity;
import com.enonic.cms.core.structure.menuitem.MenuItemContentKey;

@Repository("menuItemContentDao")
public final class MenuItemContentEntityDao
    extends AbstractBaseEntityDao<MenuItemContentEntity>
    implements MenuItemContentDao
{
    public MenuItemContentEntity findByKey( MenuItemContentKey key )
    {
        return get( MenuItemContentEntity.class, key );
    }
}
