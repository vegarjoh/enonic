/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.store.dao;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.enonic.cms.core.security.userstore.UserStoreEntity;
import com.enonic.cms.core.security.userstore.UserStoreKey;
import com.enonic.cms.store.support.EntityPageList;

@Repository("userStoreDao")
public final class UserStoreEntityDao
    extends AbstractBaseEntityDao<UserStoreEntity>
    implements UserStoreDao
{

    public UserStoreEntity findByKey( UserStoreKey key )
    {
        return get( UserStoreEntity.class, key );
    }

    public UserStoreEntity findByName( String name )
    {
        name = name.replace( '%', ' ' ); // Usikker p� hva dette er til, sjekk om dette kan fjernes
        name = name.toLowerCase();
        return findSingleByNamedQuery( UserStoreEntity.class, "UserStoreEntity.findByName", "name", name );
    }

    public UserStoreEntity findDefaultUserStore()
    {
        return findSingleByNamedQuery( UserStoreEntity.class, "UserStoreEntity.findDefaultUserStore" );
    }

    public List<UserStoreEntity> findAll()
    {
        return findByNamedQuery( UserStoreEntity.class, "UserStoreEntity.findAll" );
    }

    public EntityPageList<UserStoreEntity> findAll( int index, int count )
    {
        return findPageList( UserStoreEntity.class, "x.deleted = 0", index, count );
    }
}