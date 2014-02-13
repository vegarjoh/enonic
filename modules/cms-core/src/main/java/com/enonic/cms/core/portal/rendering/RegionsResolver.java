/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal.rendering;

import com.enonic.cms.core.portal.PageRequestType;
import com.enonic.cms.core.structure.menuitem.MenuItemEntity;
import com.enonic.cms.core.structure.page.RegionFactory;
import com.enonic.cms.core.structure.page.Regions;
import com.enonic.cms.core.structure.page.template.PageTemplateEntity;

/**
 * Sep 28, 2009
 */
public class RegionsResolver
{

    public static Regions resolveRegionsForPageRequest( final MenuItemEntity menuItem, final PageTemplateEntity pageTemplate,
                                                        PageRequestType pageRequestType )
    {
        if ( PageRequestType.CONTENT.equals( pageRequestType ) )
        {
            return RegionFactory.createRegionsOriginatingPageTemplate( pageTemplate, menuItem );
        }
        else
        {
            return RegionFactory.createRegionsForPage( pageTemplate, menuItem );
        }
    }
}
