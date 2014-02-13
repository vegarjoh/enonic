/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.vertical.adminweb.handlers;

import java.util.HashMap;
import java.util.Map;

import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.ContentLocationSpecification;
import com.enonic.cms.core.content.ContentLocations;
import com.enonic.cms.core.security.SecurityService;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.structure.SiteEntity;
import com.enonic.cms.core.structure.SiteKey;
import com.enonic.cms.core.structure.menuitem.ContentHomeEntity;
import com.enonic.cms.core.structure.menuitem.MenuItemAccessRightAccumulator;
import com.enonic.cms.core.structure.menuitem.MenuItemAccumulatedAccessRights;
import com.enonic.cms.core.structure.menuitem.MenuItemAndUserAccessRights;
import com.enonic.cms.core.structure.menuitem.MenuItemEntity;
import com.enonic.cms.core.structure.menuitem.MenuItemKey;
import com.enonic.cms.core.structure.page.template.PageTemplateEntity;
import com.enonic.cms.store.dao.ContentDao;

/**
 * Jan 7, 2010
 */
public class ContentEditFormModelFactory
{
    private SecurityService securityService;

    private ContentDao contentDao;

    private MenuItemAccessRightAccumulator menuItemAccessRightAccumulator;

    public ContentEditFormModelFactory( ContentDao contentDao, SecurityService securityService,
                                        MenuItemAccessRightAccumulator menuItemAccessRightAccumulator )
    {
        this.contentDao = contentDao;
        this.securityService = securityService;
        this.menuItemAccessRightAccumulator = menuItemAccessRightAccumulator;
    }

    public ContentEditFormModel createContentEditFormModel( ContentKey contentKey, UserEntity executor )
    {
        ContentEntity content = contentDao.findByKey( contentKey );
        ContentLocationSpecification contentLocationSpecificaiton = new ContentLocationSpecification();
        contentLocationSpecificaiton.setIncludeInactiveLocationsInSection( true );
        ContentLocations contentLocations = content.getLocations( contentLocationSpecificaiton );

        ContentEditFormModel contentEditFormModel = new ContentEditFormModel();
        contentEditFormModel.setContentLocations( contentLocations );

        Map<MenuItemKey, MenuItemAndUserAccessRights> menuItemAndUserAccessRightsMapByMenuItemKey =
            new HashMap<MenuItemKey, MenuItemAndUserAccessRights>();

        for ( MenuItemEntity menuItem : contentLocations.getMenuItems() )
        {
            MenuItemAndUserAccessRights menuItemAndUserAccessRights =
                new MenuItemAndUserAccessRights( menuItem, resolveUserRights( menuItem, executor ),
                                                 resolveUserRights( menuItem, securityService.getAnonymousUser() ) );

            menuItemAndUserAccessRightsMapByMenuItemKey.put( menuItem.getKey(), menuItemAndUserAccessRights );
        }
        contentEditFormModel.setMenuItemAndUserAccessRightsMapByMenuItemKey( menuItemAndUserAccessRightsMapByMenuItemKey );

        Map<SiteKey, PageTemplateEntity> pageTemplateBySiteKey = new HashMap<SiteKey, PageTemplateEntity>();
        for ( SiteEntity site : contentLocations.getSites() )
        {
            ContentHomeEntity contentHome = content.getContentHome( site.getKey() );
            if ( contentHome != null && contentHome.getPageTemplate() != null )
            {
                pageTemplateBySiteKey.put( site.getKey(), contentHome.getPageTemplate() );
            }
        }
        contentEditFormModel.setPageTemplateBySite( pageTemplateBySiteKey );

        return contentEditFormModel;
    }

    private MenuItemAccumulatedAccessRights resolveUserRights( final MenuItemEntity menuItem, final UserEntity user )
    {
        return menuItemAccessRightAccumulator.getAccessRightsAccumulated( menuItem, user );
    }
}
