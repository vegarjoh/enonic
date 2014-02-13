/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.vertical.engine.handlers.model;

import com.enonic.cms.core.structure.menuitem.MenuItemType;

public class MenuItemModel
{
    private final int primaryKey; // new (in new created site) menu item primary key
    private final Integer type; // menu item type ( page/ URL / label / section / shortcut )
    private final Integer shortcutKey; // old shortcut key (linked to) in case SHORTCUT type, null otherwise

    public MenuItemModel( int primaryKey, Integer type, Integer shortcutKey )
    {
        this.primaryKey = primaryKey;
        this.type = type;
        this.shortcutKey = shortcutKey;
    }
    public boolean isShortcut()
    {
        return MenuItemType.SHORTCUT.getKey().equals( type );
    }

    public Integer getShortcutKey()
    {
        return shortcutKey;
    }
    public int getPrimaryKey()
    {
        return primaryKey;
    }
}
