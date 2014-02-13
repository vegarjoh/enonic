/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.security.userstore.connector.local;

import org.springframework.util.Assert;

import com.enonic.cms.api.plugin.ext.auth.AuthenticationResult;
import com.enonic.cms.api.plugin.ext.userstore.UserFields;
import com.enonic.cms.core.security.InvalidCredentialsException;
import com.enonic.cms.core.security.group.DeleteGroupCommand;
import com.enonic.cms.core.security.group.GroupEntity;
import com.enonic.cms.core.security.group.GroupKey;
import com.enonic.cms.core.security.group.StoreNewGroupCommand;
import com.enonic.cms.core.security.group.UpdateGroupCommand;
import com.enonic.cms.core.security.user.DeleteUserCommand;
import com.enonic.cms.core.security.user.DisplayNameResolver;
import com.enonic.cms.core.security.user.ReadOnlyUserFieldValidator;
import com.enonic.cms.core.security.user.StoreNewUserCommand;
import com.enonic.cms.core.security.user.UpdateUserCommand;
import com.enonic.cms.core.security.user.User;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.security.user.UserImpl;
import com.enonic.cms.core.security.user.UserKey;
import com.enonic.cms.core.security.user.UserSpecification;
import com.enonic.cms.core.security.userstore.UserStoreKey;
import com.enonic.cms.core.security.userstore.connector.AbstractBaseUserStoreConnector;
import com.enonic.cms.core.security.userstore.connector.AuthenticationChain;

public class LocalUserStoreConnector
    extends AbstractBaseUserStoreConnector
{
    public LocalUserStoreConnector( final UserStoreKey userStoreKey, final String userStoreName )
    {
        super( userStoreKey, userStoreName, "local" );
    }

    public boolean canCreateUser()
    {
        return true;
    }

    public boolean canUpdateUser()
    {
        return true;
    }

    public boolean canUpdateUserPassword()
    {
        return true;
    }

    public boolean canDeleteUser()
    {
        return true;
    }

    public boolean canCreateGroup()
    {
        return true;
    }

    public boolean canReadGroup()
    {
        return true;
    }

    public boolean canUpdateGroup()
    {
        return true;
    }

    public boolean canDeleteGroup()
    {
        return true;
    }

    public UserKey storeNewUser( final StoreNewUserCommand command )
    {
        Assert.isTrue( command.getUserStoreKey().equals( userStoreKey ) );
        ensureValidUserName( command );

        return storeNewUserLocally( command, new DisplayNameResolver( getUserStore().getConfig() ) );
    }

    protected boolean isUsernameUnique( String userName )
    {
        UserEntity localUser = getLocalUserWithUsername( userName );

        return localUser == null;
    }

    public void updateUser( final UpdateUserCommand command )
    {
        final UserEntity existingUser = userDao.findSingleBySpecification( command.getSpecification() );
        final UserFields userFields = existingUser.getUserFields();
        final UserFields changedUserFields = command.getUserFields().getChangedUserFields( userFields, command.isUpdateStrategy() );
        new ReadOnlyUserFieldValidator( getUserStore().getConfig() ).validate( changedUserFields );
        updateUserLocally( command );
    }

    public void deleteUser( DeleteUserCommand command )
    {
        deleteUserLocally( command );
    }

    public GroupKey storeNewGroup( final StoreNewGroupCommand command )
    {
        return storeNewGroupLocally( command );
    }

    public void updateGroup( final UpdateGroupCommand command )
    {
        updateGroupLocally( command );
    }

    public void addMembershipToGroup( GroupEntity groupToAdd, GroupEntity groupToAddTo )
    {
        addMembershipToGroupLocally( groupToAdd, groupToAddTo );
    }

    public void removeMembershipFromGroup( GroupEntity groupToRemove, GroupEntity groupToRemoveFrom )
    {
        removeMembershipFromGroupLocally( groupToRemove, groupToRemoveFrom );
    }

    public void deleteGroup( DeleteGroupCommand command )
    {
        deleteGroupLocally( command );
    }

    private boolean verifyPassword( final UserEntity user, final String password, final AuthenticationChain authChain )
    {
        final AuthenticationResult result = authChain.authenticate( getUserStoreName(), user.getName(), password );
        if ( result == AuthenticationResult.CONTINUE )
        {
            return user.verifyPassword( password );
        }

        return result == AuthenticationResult.OK;
    }

    public String authenticateUser( final String uid, final String password, final AuthenticationChain authChain )
    {
        final UserSpecification spec = new UserSpecification();
        spec.setUserStoreKey( userStoreKey );
        spec.setName( uid );
        spec.setDeletedStateNotDeleted();

        final UserEntity user = userDao.findSingleBySpecification( spec );
        if ( user == null )
        {
            throw new InvalidCredentialsException( uid );
        }

        if ( !verifyPassword( user, password, authChain ) )
        {
            throw new InvalidCredentialsException( uid );
        }

        return user.getSync();
    }

    public void changePassword( final String uid, final String newPassword )
    {
        userStorerFactory.create( userStoreKey ).changePassword( uid, newPassword );
    }

    public User getUserByEntity( UserEntity userEntity )
    {
        return UserImpl.createFrom( userEntity );
    }

}