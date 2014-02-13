/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.language;

import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.structure.SiteEntity;
import com.enonic.cms.core.structure.menuitem.MenuItemEntity;

/**
 * May 11, 2009
 */
public class LanguageResolver
{
    public static LanguageEntity resolve( ContentEntity content, SiteEntity site, MenuItemEntity menuItem )
    {
        if ( menuItem.getLanguage() != null )
        {
            return menuItem.getLanguage();
        }
        else if ( content.getLanguage() != null )
        {
            return content.getLanguage();
        }
        else
        {
            return site.getLanguage();
        }
    }

    public static LanguageEntity resolve( SiteEntity site, MenuItemEntity menuItem )
    {
        if ( menuItem != null && menuItem.getLanguage() != null )
        {
            return menuItem.getLanguage();
        }

        return site.getLanguage();
    }
}
