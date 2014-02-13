/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.security.userstore;


import com.enonic.cms.store.dao.UserStoreDao;

public class UserStoreParser
{
    private UserStoreDao userStoreDao;

    public UserStoreParser( UserStoreDao userStoreDao )
    {
        this.userStoreDao = userStoreDao;
    }

    public UserStoreEntity parseUserStore( String string )
        throws UserStoreNotFoundException
    {

        if ( string == null )
        {
            return null;
        }

        UserStoreEntity userStore;

        if ( string.startsWith( "#" ) )
        {
            UserStoreKey userStoreKey = new UserStoreKey( string );
            userStore = userStoreDao.findByKey( userStoreKey );
            if ( userStore == null )
            {
                throw new UserStoreNotFoundException( userStoreKey );
            }
        }
        else
        {

            Integer userStoreKeyInteger;
            try
            {
                userStoreKeyInteger = new Integer( string );
                userStore = userStoreDao.findByKey( new UserStoreKey( userStoreKeyInteger ) );
            }
            catch ( NumberFormatException e )
            {
                userStore = userStoreDao.findByName( string );
            }

            if ( userStore == null )
            {
                throw new UserStoreNotFoundException( string );
            }
        }

        return userStore;
    }

}
