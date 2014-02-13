/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.security;

import java.util.List;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.enonic.cms.core.security.group.GroupEntity;
import com.enonic.cms.core.security.group.GroupKey;
import com.enonic.cms.core.security.group.QualifiedGroupname;
import com.enonic.cms.core.security.user.QualifiedUsername;
import com.enonic.cms.core.security.user.User;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.security.user.UserKey;
import com.enonic.cms.core.security.userstore.UserStoreEntity;
import com.enonic.cms.core.security.userstore.UserStoreKey;
import com.enonic.cms.store.dao.GroupQuery;

public interface SecurityService
{

    UserKey getAnonymousUserKey();

    UserEntity getAnonymousUser();

    GroupKey getEnterpriseAdministratorGroup();

    GroupEntity getAuthenticatedUsersGroup( UserStoreEntity userStore );

    UserEntity findUserByEmail( String userStore, String email );

    /**
     * Finds the specified user unless it is deleted.
     *
     * @param qname The fully qualified userName, which includes user store and uid.
     * @return Only non-deleted users.
     */
    UserEntity getUser( QualifiedUsername qname );

    /**
     * Finds the specified user unless it is deleted.
     *
     * @param username The id of the user in the given user store.
     * @return Only non-deleted users.
     */
    UserEntity getUserFromDefaultUserStore( String username );

    /**
     * Finds specified user unless it is deleted;
     *
     * @return Only non-deleted users.
     */
    UserEntity getUser( UserKey userKey );

    UserEntity getUser( User oldUserObject );

    List<UserEntity> getUsers( UserStoreKey userStoreKey, Integer index, Integer count, boolean includeDeleted );

    List<UserEntity> findUsersByQuery( UserStoreKey userStoreKey, String queryStr, String orderBy, boolean orderAscending );

    /**
     * Finds group specified by qname. Does not find deleted groups.
     *
     * @param qname The qualified group name.
     * @return The requested group.
     */
    GroupEntity getGroup( QualifiedGroupname qname );

    GroupEntity getGroup( GroupKey key );

    List<GroupEntity> getGroups( GroupQuery spec );

    List<UserStoreEntity> getUserStores();

    User loginAdminUser( LoginAdminUserCommand command );

    void loginPortalUser( QualifiedUsername qualifiedUsername, String password );

    void loginClientApiUser( QualifiedUsername qualifiedUsername, String password );

    void loginDavUser( QualifiedUsername qualifiedUsername, String password );

    void loginInstantTraceUser( QualifiedUsername qualifiedUsername, String password );

    UserEntity impersonatePortalUser( ImpersonateCommand command );

    void removePortalImpersonation();

    @Transactional(propagation = Propagation.REQUIRED)
    User getLoggedInPortalUser();

    UserEntity getLoggedInPortalUserAsEntity();

    User getLoggedInAdminConsoleUser();

    UserEntity getLoggedInAdminConsoleUserAsEntity();

    User getLoggedInClientApiUser();

    UserEntity getLoggedInClientApiUserAsEntity();

    /**
     * @return The user you are currently running as. The run-as user will only be different from logged in user if you have impersonated
     *         some other user.
     */
    UserEntity getImpersonatedPortalUser();

    void logoutAdminUser();

    void logoutPortalUser();

    void logoutClientApiUser( boolean invalidateSession );

    /**
     * Auto login user. Used by plugins.
     *
     * @param qualifiedUsername The fully qualified userName, which includes user store and uid.
     * @return <code>true</code> if successful.
     */
    boolean autoLoginPortalUser( QualifiedUsername qualifiedUsername );

    boolean autoLoginAdminUser( QualifiedUsername qualifiedUsername );

    void changePassword( QualifiedUsername qualifiedUsername, String newPassword );
}
