/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.vertical.engine.handlers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.enonic.esl.sql.model.Table;
import com.enonic.vertical.engine.BaseEngine;
import com.enonic.vertical.engine.PresentationEngine;
import com.enonic.vertical.engine.dbmodel.VerticalDatabase;

import com.enonic.cms.core.AdminConsoleTranslationService;
import com.enonic.cms.core.content.ContentService;
import com.enonic.cms.core.log.LogService;
import com.enonic.cms.core.resource.ResourceService;
import com.enonic.cms.core.security.SecurityService;
import com.enonic.cms.core.security.userstore.MemberOfResolver;
import com.enonic.cms.core.service.KeyService;
import com.enonic.cms.store.dao.BinaryDataDao;
import com.enonic.cms.store.dao.CategoryDao;
import com.enonic.cms.store.dao.ContentBinaryDataDao;
import com.enonic.cms.store.dao.ContentDao;
import com.enonic.cms.store.dao.ContentTypeDao;
import com.enonic.cms.store.dao.GroupDao;
import com.enonic.cms.store.dao.LanguageDao;
import com.enonic.cms.store.dao.MenuItemDao;
import com.enonic.cms.store.dao.PageDao;
import com.enonic.cms.store.dao.PageTemplateDao;
import com.enonic.cms.store.dao.PortletDao;
import com.enonic.cms.store.dao.SiteDao;
import com.enonic.cms.store.dao.UnitDao;
import com.enonic.cms.store.dao.UserDao;
import com.enonic.cms.store.dao.UserStoreDao;

public abstract class BaseHandler
{
    protected final VerticalDatabase db = VerticalDatabase.getInstance();

    protected BaseEngine baseEngine;

    protected AdminConsoleTranslationService languageMap;

    // Services:

    @Autowired
    protected LogService logService;

    @Autowired
    protected ContentService contentService;

    private KeyService keyService;

    protected SecurityService securityService;

    // Daos:

    @Autowired
    protected BinaryDataDao binaryDataDao;

    @Autowired
    protected ContentBinaryDataDao contentBinaryDataDao;

    @Autowired
    protected ContentDao contentDao;

    @Autowired
    protected PortletDao portletDao;

    @Autowired
    protected CategoryDao categoryDao;

    @Autowired
    protected GroupDao groupDao;

    @Autowired
    protected LanguageDao languageDao;

    @Autowired
    protected MenuItemDao menuItemDao;

    @Autowired
    protected PageDao pageDao;

    @Autowired
    protected PageTemplateDao pageTemplateDao;

    @Autowired
    protected ResourceService resourceService;

    @Autowired
    protected SiteDao siteDao;

    @Autowired
    protected UserDao userDao;

    @Autowired
    protected UnitDao unitDao;

    @Autowired
    protected ContentTypeDao contentTypeDao;

    @Autowired
    protected UserStoreDao userStoreDao;

    @Autowired
    protected MemberOfResolver memberOfResolver;

    @Autowired
    protected SessionFactory sessionFactory;

    public BaseHandler()
    {

    }

    @Autowired
    public void setBaseEngine( PresentationEngine value )
    {
        this.baseEngine = value;
    }

    @Autowired
    public void setAdminConsoleTranslationService( AdminConsoleTranslationService languageMap )
    {
        this.languageMap = languageMap;
    }

    @Autowired
    public void setKeyService( KeyService value )
    {
        this.keyService = value;
    }

    @Autowired
    public void setSecurityService( SecurityService service )
    {
        securityService = service;
    }

    protected final ContentHandler getContentHandler()
    {
        return baseEngine.getContentHandler();
    }

    protected final CommonHandler getCommonHandler()
    {
        return baseEngine.getCommonHandler();
    }

    protected final ContentObjectHandler getContentObjectHandler()
    {
        return baseEngine.getContentObjectHandler();
    }

    protected final GroupHandler getGroupHandler()
    {
        return baseEngine.getGroupHandler();
    }

    protected final LanguageHandler getLanguageHandler()
    {
        return baseEngine.getLanguageHandler();
    }

    protected final MenuHandler getMenuHandler()
    {
        return baseEngine.getMenuHandler();
    }

    protected final PageHandler getPageHandler()
    {
        return baseEngine.getPageHandler();
    }

    protected final PageTemplateHandler getPageTemplateHandler()
    {
        return baseEngine.getPageTemplateHandler();
    }

    protected final SectionHandler getSectionHandler()
    {
        return baseEngine.getSectionHandler();
    }

    protected final SecurityHandler getSecurityHandler()
    {
        return baseEngine.getSecurityHandler();
    }

    protected final UserHandler getUserHandler()
    {
        return baseEngine.getUserHandler();
    }

    protected final void close( ResultSet resultSet )
    {

        baseEngine.close( resultSet );
    }

    protected final void close( Statement stmt )
    {

        baseEngine.close( stmt );
    }

    protected final Connection getConnection()
        throws SQLException
    {
        return baseEngine.getConnection();
    }

    public final int getNextKey( String tableName )
    {
        return keyService.generateNextKeySafe( tableName );
    }

    public final int getNextKey( Table table )
    {
        return keyService.generateNextKeySafe( table.getName() );
    }

    public String expandSQLStatement( String statement, StringBuffer object )
    {
        final StringBuffer string = new StringBuffer( statement );
        final int index = statement.indexOf( "%0" );
        if ( index >= 0 )
        {
            string.replace( index, index + 2, object.toString() );
        }
        return string.toString();
    }

}
