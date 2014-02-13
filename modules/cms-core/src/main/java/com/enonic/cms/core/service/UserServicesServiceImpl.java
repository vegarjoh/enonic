/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.enonic.vertical.engine.UserServicesEngine;

import com.enonic.cms.framework.xml.XMLDocument;

import com.enonic.cms.core.security.user.User;

@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
@Service
public class UserServicesServiceImpl
    implements UserServicesService
{

    protected UserServicesEngine userServicesEngine;

    @Autowired
    public void setUserServicesEngine( UserServicesEngine userServicesEngine )
    {
        this.userServicesEngine = userServicesEngine;
    }

    /**
     * Transaction NB: Denne metoden er hardkodet til å ikke logge, så trenger ikke write.
     */
    public XMLDocument getContent( User user, int key, boolean publishOnly, int parenLevel, int childrenLevel, int parenChildrenLevel )
    {
        return userServicesEngine.getContent( user, key, publishOnly, parenLevel, childrenLevel, parenChildrenLevel );
    }

    public XMLDocument getContentTypeByCategory( int cKey )
    {
        return userServicesEngine.getContentTypeByCategory( cKey );
    }

    public XMLDocument getContentTypeByContent( int contentKey )
    {
        return userServicesEngine.getContentTypeByContent( contentKey );
    }

    public User getAnonymousUser()
    {
        return userServicesEngine.getAnonymousUser();
    }

    public XMLDocument getMenuItem( User user, int mikey )
    {
        return userServicesEngine.getMenuItem( user, mikey );
    }

    public int getCurrentVersionKey( int contentKey )
    {
        return userServicesEngine.getCurrentVersionKey( contentKey );
    }

}
