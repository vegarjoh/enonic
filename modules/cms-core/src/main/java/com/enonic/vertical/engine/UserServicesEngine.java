/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.vertical.engine;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import com.enonic.vertical.engine.handlers.CategoryHandler;
import com.enonic.vertical.engine.handlers.ContentHandler;
import com.enonic.vertical.engine.handlers.LogHandler;
import com.enonic.vertical.engine.handlers.MenuHandler;
import com.enonic.vertical.engine.handlers.UserHandler;

import com.enonic.cms.framework.xml.XMLDocument;
import com.enonic.cms.framework.xml.XMLDocumentFactory;

import com.enonic.cms.core.content.category.CategoryKey;
import com.enonic.cms.core.security.user.User;

@Component
public class UserServicesEngine
    extends BaseEngine
    implements InitializingBean
{

    private CategoryHandler categoryHandler;

    private ContentHandler contentHandler;

    private LogHandler logHandler;

    private MenuHandler menuHandler;

    private UserHandler userHandler;

    @Autowired
    public void setCategoryHandler( CategoryHandler categoryHandler )
    {
        this.categoryHandler = categoryHandler;
    }

    @Autowired
    public void setContentHandler( ContentHandler contentHandler )
    {
        this.contentHandler = contentHandler;
    }

    @Autowired
    public void setLogHandler( LogHandler logHandler )
    {
        this.logHandler = logHandler;
    }

    @Autowired
    public void setMenuHandler( MenuHandler menuHandler )
    {
        this.menuHandler = menuHandler;
    }

    @Autowired
    public void setUserHandler( UserHandler userHandler )
    {
        this.userHandler = userHandler;
    }


    public void afterPropertiesSet()
        throws Exception
    {
        // event listeners
        menuHandler.addListener( logHandler );
    }

    public CategoryHandler getCategoryHandler()
    {
        return categoryHandler;
    }

    public ContentHandler getContentHandler()
    {
        return contentHandler;
    }

    public LogHandler getLogHandler()
    {
        return logHandler;
    }

    public MenuHandler getMenuHandler()
    {
        return menuHandler;
    }

    public UserHandler getUserHandler()
    {
        return userHandler;
    }

    public XMLDocument getContent( User user, int key, boolean publishedOnly, int parentLevel, int childrenLevel, int parentChildrenLevel )
    {

        if ( user == null )
        {
            user = userHandler.getAnonymousUser();
        }

        Document doc = contentHandler.getContent( user, key, publishedOnly, parentLevel, childrenLevel, parentChildrenLevel );

        return XMLDocumentFactory.create( doc );
    }

    public User getAnonymousUser()
    {
        return userHandler.getAnonymousUser();
    }

    public XMLDocument getContentTypeByContent( int contentKey )
    {
        int contentTypeKey = contentHandler.getContentTypeKey( contentKey );
        return XMLDocumentFactory.create( contentHandler.getContentType( contentTypeKey, false ) );
    }

    public XMLDocument getContentTypeByCategory( int categoryKey )
    {
        int contentTypeKey = categoryHandler.getContentTypeKey( CategoryKey.parse( categoryKey ) );
        return XMLDocumentFactory.create( contentHandler.getContentType( contentTypeKey, false ) );
    }

    public XMLDocument getMenuItem( User user, int mikey )
    {
        return XMLDocumentFactory.create( menuHandler.getMenuItem( user, mikey, false, true, false ) );
    }

    public int getCurrentVersionKey( int contentKey )
    {
        return contentHandler.getCurrentVersionKey( contentKey );
    }

}
