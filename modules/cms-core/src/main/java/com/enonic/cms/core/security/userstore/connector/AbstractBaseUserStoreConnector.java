/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.security.userstore.connector;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import com.enonic.vertical.engine.handlers.NameGenerator;

import com.enonic.cms.core.security.group.DeleteGroupCommand;
import com.enonic.cms.core.security.group.GroupEntity;
import com.enonic.cms.core.security.group.GroupKey;
import com.enonic.cms.core.security.group.StoreNewGroupCommand;
import com.enonic.cms.core.security.group.UpdateGroupCommand;
import com.enonic.cms.core.security.user.DeleteUserCommand;
import com.enonic.cms.core.security.user.DisplayNameResolver;
import com.enonic.cms.core.security.user.StoreNewUserCommand;
import com.enonic.cms.core.security.user.UpdateUserCommand;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.security.user.UserKey;
import com.enonic.cms.core.security.user.UserSpecification;
import com.enonic.cms.core.security.user.UsernameResolver;
import com.enonic.cms.core.security.userstore.GroupStorerFactory;
import com.enonic.cms.core.security.userstore.UserStoreEntity;
import com.enonic.cms.core.security.userstore.UserStoreKey;
import com.enonic.cms.core.security.userstore.UserStorerFactory;
import com.enonic.cms.store.dao.GroupDao;
import com.enonic.cms.store.dao.UserDao;
import com.enonic.cms.store.dao.UserStoreDao;

/**
 * Jun 25, 2009
 */
public abstract class AbstractBaseUserStoreConnector
    implements UserStoreConnector
{
    protected final UserStoreKey userStoreKey;

    protected final String userStoreName;

    protected final String connectorName;

    protected UserDao userDao;

    protected GroupDao groupDao;

    protected UserStoreDao userStoreDao;

    protected GroupStorerFactory groupStorerFactory;

    protected UserStorerFactory userStorerFactory;

    protected abstract boolean isUsernameUnique( String username );

    protected AbstractBaseUserStoreConnector( UserStoreKey userStoreKey, String userStoreName, String connectorName )
    {
        this.userStoreKey = userStoreKey;
        this.userStoreName = userStoreName;
        this.connectorName = connectorName;
    }

    public String getUserStoreName()
    {
        return userStoreName;
    }

    public String getConnectorName()
    {
        return connectorName;
    }

    protected UserStoreEntity getUserStore()
    {
        return userStoreDao.findByKey( userStoreKey );
    }

    protected void ensureValidUserName( final StoreNewUserCommand command )
    {
        boolean usernameProvided = StringUtils.isNotBlank( command.getUsername() );
        if ( usernameProvided )
        {
            return;
        }

        String resolvedUsername = new UsernameResolver( getUserStore().getConfig() ).resolveUsername( command );

        String createdUniqueUsername = getUniqueUsername( resolvedUsername );

        command.setUsername( createdUniqueUsername );
    }

    private String getUniqueUsername( String suggestedUsername )
    {
        Assert.isTrue( StringUtils.isNotBlank( suggestedUsername ) );

        suggestedUsername = NameGenerator.transcribeName( suggestedUsername );

        int i = 0;

        String baseName = suggestedUsername;

        while ( true )
        {
            if ( isUsernameUnique( suggestedUsername ) )
            {
                return suggestedUsername;
            }
            else
            {
                i++;
                suggestedUsername = baseName + i;
            }

            Assert.isTrue( i < 100, "Not able to resolve user name within 100 attempts to create unique" );
        }
    }

    protected UserEntity getLocalUserWithUsername( String userName )
    {
        UserSpecification userSpec = new UserSpecification();
        userSpec.setUserStoreKey( userStoreKey );
        userSpec.setName( userName );
        userSpec.setDeletedStateNotDeleted();

        return userDao.findSingleBySpecification( userSpec );
    }

    protected UserKey storeNewUserLocally( StoreNewUserCommand command, DisplayNameResolver displayNameResolver )
    {
        return userStorerFactory.create( userStoreKey ).storeNewUser( command, displayNameResolver );
    }

    protected void updateUserLocally( UpdateUserCommand command )
    {
        userStorerFactory.create( userStoreKey ).updateUser( command );
    }

    protected void deleteUserLocally( DeleteUserCommand command )
    {
        userStorerFactory.create( userStoreKey ).deleteUser( command.getSpecification() );
    }

    protected GroupKey storeNewGroupLocally( StoreNewGroupCommand command )
    {
        return groupStorerFactory.create( userStoreKey ).storeNewGroup( command );
    }

    protected void updateGroupLocally( UpdateGroupCommand command )
    {
        groupStorerFactory.create( userStoreKey ).updateGroup( command );
    }

    protected void removeMembershipFromGroupLocally( GroupEntity groupToRemove, GroupEntity groupToRemoveFrom )
    {
        groupStorerFactory.create( userStoreKey ).removeMembershipFromGroup( groupToRemove, groupToRemoveFrom );
    }

    protected void addMembershipToGroupLocally( GroupEntity groupToAdd, GroupEntity groupToAddTo )
    {
        groupStorerFactory.create( userStoreKey ).addMembershipToGroup( groupToAdd, groupToAddTo );
    }

    protected void deleteGroupLocally( DeleteGroupCommand command )
    {
        groupStorerFactory.create( userStoreKey ).deleteGroup( command );
    }

    public void setUserDao( UserDao value )
    {
        this.userDao = value;
    }

    public void setGroupDao( GroupDao groupDao )
    {
        this.groupDao = groupDao;
    }

    public void setGroupStorerFactory( GroupStorerFactory value )
    {
        this.groupStorerFactory = value;
    }

    public void setUserStorerFactory( UserStorerFactory value )
    {
        this.userStorerFactory = value;
    }

    public void setUserStoreDao( UserStoreDao value )
    {
        this.userStoreDao = value;
    }
}
