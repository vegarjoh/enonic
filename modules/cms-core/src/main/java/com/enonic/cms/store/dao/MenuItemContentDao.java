/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.store.dao;

import com.enonic.cms.core.structure.menuitem.MenuItemContentEntity;
import com.enonic.cms.core.structure.menuitem.MenuItemContentKey;


public interface MenuItemContentDao
    extends EntityDao<MenuItemContentEntity>
{
    MenuItemContentEntity findByKey( MenuItemContentKey key );
}
