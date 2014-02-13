/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.vertical.adminweb;

import com.enonic.cms.core.structure.DefaultSiteAccumulatedAccessRights;
import com.enonic.cms.core.structure.SiteEntity;
import com.enonic.cms.core.structure.SiteProperties;
import com.enonic.cms.core.structure.SiteXmlCreator;
import com.enonic.cms.core.structure.menuitem.MenuItemEntity;
import com.enonic.cms.core.structure.menuitem.MenuItemKey;
import com.enonic.cms.core.structure.menuitem.MenuItemXMLCreatorSetting;
import com.enonic.cms.core.structure.menuitem.MenuItemXmlCreator;
import com.enonic.cms.core.structure.page.PageEntity;
import com.enonic.cms.core.structure.page.template.PageTemplateEntity;
import com.enonic.cms.framework.xml.XMLDocument;
import com.enonic.cms.framework.xml.XMLDocumentFactory;
import org.jdom.Document;
import org.jdom.Element;

import java.util.List;

/**
 * model for edit menu item page
 */
public class MenuItemFormModel
{

    private boolean newMenuItem;

    private SiteEntity site;

    private SiteProperties siteProperties;

    private DefaultSiteAccumulatedAccessRights userRightsForSite;

    private List<MenuItemEntity> selectedMenuItemPath;

    public MenuItemFormModel( MenuItemKey selectedMenuItemKey )
    {
        this.newMenuItem = selectedMenuItemKey == null;
    }

    public XMLDocument toXML()
    {
        Element modelEl = new Element( "model" );

        modelEl.addContent( createSelectedMenuElement() );
        modelEl.addContent( createSelectedMenuItemPathElement() );

        return XMLDocumentFactory.create( new Document( modelEl ) );
    }

    private Element createSelectedMenuElement()
    {
        SiteXmlCreator siteXmlCreator = new SiteXmlCreator( null );
        return siteXmlCreator.createMenuElement( site, siteProperties, userRightsForSite );
    }

    private Element createSelectedMenuItemPathElement()
    {
        Element selectedMenuItemPathEl = new Element( "menuitem-parents" );

        if ( selectedMenuItemPath != null )
        {
            int i = 0;
            for ( MenuItemEntity currMenuItem : selectedMenuItemPath )
            {
                MenuItemXMLCreatorSetting miPathXmlCreatorSetting = new MenuItemXMLCreatorSetting();
                miPathXmlCreatorSetting.includeTypeSpecificXML = false;
                miPathXmlCreatorSetting.includeParents = false;
                miPathXmlCreatorSetting.includeChildren = false;
                MenuItemXmlCreator menuItemPathXmlCreator = new MenuItemXmlCreator( miPathXmlCreatorSetting, null );
                Element currMenuItemEl = menuItemPathXmlCreator.createMenuItemElement( currMenuItem );
                selectedMenuItemPathEl.addContent( currMenuItemEl );

                // do not add last element (it is the selected one, if not new)
                if ( ( ++i == selectedMenuItemPath.size() - 1 ) && !newMenuItem )
                {
                    break;
                }
            }
        }

        return selectedMenuItemPathEl;
    }


    public int findParentPageTemplateKey()
    {
        int template = -1;

        if ( selectedMenuItemPath != null )
        {
            int self = selectedMenuItemPath.size() - 1;

            for ( int path = self; path >= 0; path-- ) // backward
            {
                PageEntity page = selectedMenuItemPath.get( path ).getPage();

                if ( page != null )
                {
                    PageTemplateEntity pageTemplate = page.getTemplate();

                    if ( pageTemplate != null )
                    {
                        template = pageTemplate.getKey();
                        break;
                    }
                }
            }
        }

        return template;
    }

    /* getters and setters */

    public void setSite( SiteEntity site )
    {
        this.site = site;
    }

    public void setSiteProperties( SiteProperties siteProperties )
    {
        this.siteProperties = siteProperties;
    }

    public void setUserRightsForSite( DefaultSiteAccumulatedAccessRights value )
    {
        this.userRightsForSite = value;
    }

    public void setSelectedMenuItemPath( List<MenuItemEntity> selectedMenuItemPath )
    {
        this.selectedMenuItemPath = selectedMenuItemPath;
    }
}
