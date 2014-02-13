/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.security;

import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import com.enonic.cms.core.admin.AdminConsoleAccessDeniedException;
import com.enonic.cms.core.security.group.GroupEntity;
import com.enonic.cms.core.security.group.GroupKey;
import com.enonic.cms.core.security.group.GroupType;
import com.enonic.cms.core.security.group.QualifiedGroupname;
import com.enonic.cms.core.security.user.QualifiedUsername;
import com.enonic.cms.core.security.user.User;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.security.user.UserKey;
import com.enonic.cms.core.security.user.UserNotFoundException;
import com.enonic.cms.core.security.user.UserSpecification;
import com.enonic.cms.core.security.userstore.UserStoreEntity;
import com.enonic.cms.core.security.userstore.UserStoreKey;
import com.enonic.cms.core.security.userstore.UserStoreNotFoundException;
import com.enonic.cms.core.security.userstore.UserStoreService;
import com.enonic.cms.core.servlet.ServletRequestAccessor;
import com.enonic.cms.store.dao.GroupDao;
import com.enonic.cms.store.dao.GroupQuery;
import com.enonic.cms.store.dao.UserDao;
import com.enonic.cms.store.dao.UserStoreDao;

@Service("securityService")
public class SecurityServiceImpl
    implements SecurityService
{

    @Autowired
    private UserDao userDao;

    @Autowired
    private UserStoreDao userStoreDao;

    @Autowired
    private GroupDao groupDao;

    @Autowired
    private UserStoreService userStoreService;

    @Autowired
    protected AdminConsoleLoginAccessResolver adminConsoleLoginAccessResolver;

    @Value("${cms.admin.password}")
    private String adminUserPassword;

    private void initializeSecurityHolder()
    {
        if ( PortalSecurityHolder.getAnonUser() == null )
        {
            PortalSecurityHolder.setAnonUser( userDao.findBuiltInAnonymousUser().getKey() );
        }
    }

    /**
     * @inheritDoc
     */
    public UserKey getAnonymousUserKey()
    {
        return userDao.findBuiltInAnonymousUser().getKey();
    }

    public UserEntity getAnonymousUser()
    {
        return userDao.findBuiltInAnonymousUser();
    }

    /**
     * @inheritDoc
     */
    public GroupKey getEnterpriseAdministratorGroup()
    {
        return groupDao.findBuiltInEnterpriseAdministrator().getGroupKey();
    }

    public GroupEntity getAuthenticatedUsersGroup( UserStoreEntity userStore )
    {

        return groupDao.findSingleByGroupTypeAndUserStore( GroupType.AUTHENTICATED_USERS, userStore.getKey() );
    }

    public UserEntity getUser( UserKey userKey )
    {
        final UserEntity user = userDao.findByKey( userKey );
        if ( user != null && user.isDeleted() )
        {
            return null;
        }
        return user;
    }

    public UserEntity getUser( QualifiedUsername qname )
    {
        final UserEntity user = userDao.findByQualifiedUsername( qname );
        if ( user != null && user.isDeleted() )
        {
            return null;
        }
        return user;
    }

    /**
     * @inheritDoc
     */
    public UserEntity getUserFromDefaultUserStore( String username )
    {
        UserStoreEntity defaultUserStore = userStoreService.getDefaultUserStore();

        UserSpecification userSpecification = new UserSpecification();
        userSpecification.setUserStoreKey( defaultUserStore.getKey() );
        userSpecification.setName( username );
        userSpecification.setDeletedStateNotDeleted();
        return userDao.findSingleBySpecification( userSpecification );
    }

    public UserEntity getUser( User oldUserObject )
    {
        return userDao.findByKey( oldUserObject.getKey() );
    }

    /**
     * @inheritDoc
     */
    public List<UserEntity> getUsers( UserStoreKey userStoreKey, Integer index, Integer count, boolean includeDeleted )
    {

        UserStoreEntity userStore = userStoreDao.findByKey( userStoreKey );
        if ( userStore == null )
        {
            throw new UserStoreNotFoundException( userStoreKey );
        }

        return userDao.findByUserStoreKey( userStore.getKey(), index, count, includeDeleted );
    }

    /**
     * @inheritDoc
     */
    public GroupEntity getGroup( QualifiedGroupname qname )
    {

        if ( qname.isGlobal() )
        {
            return groupDao.findGlobalGroupByName( qname.getGroupname(), false );
        }
        else
        {
            return groupDao.findSingleUndeletedByUserStoreKeyAndGroupname( qname.getUserStoreKey(), qname.getGroupname() );
        }
    }

    /**
     * @inheritDoc
     */
    public GroupEntity getGroup( GroupKey key )
    {
        return groupDao.findByKey( key );
    }

    public List<GroupEntity> getGroups( GroupQuery spec )
    {
        spec.validate();
        return groupDao.findByQuery( spec );
    }

    public List<UserEntity> findUsersByQuery( UserStoreKey userStoreKey, String queryStr, String orderBy, boolean orderAscending )
    {
        return userDao.findByQuery( userStoreKey, queryStr, orderBy, orderAscending );
    }

    public List<UserStoreEntity> getUserStores()
    {
        return userStoreDao.findAll();
    }

    /**
     * @inheritDoc
     */
    public User getLoggedInAdminConsoleUser()
    {
        final UserKey userKey = doGetUserKeyForLoggedInAdminConsoleUser();
        if ( userKey == null )
        {
            return null;
        }
        return userStoreService.getUserByKey( userKey );
    }

    public User getLoggedInClientApiUser()
    {
        return userStoreService.getUserByKey( doGetUserKeyForLoggedInPortalUser() );
    }

    public UserEntity getLoggedInClientApiUserAsEntity()
    {
        return userDao.findByKey( doGetUserKeyForLoggedInPortalUser() );
    }

    public UserEntity getLoggedInAdminConsoleUserAsEntity()
    {
        final UserKey userKey = doGetUserKeyForLoggedInAdminConsoleUser();
        if ( userKey == null )
        {
            return null;
        }
        return userDao.findByKey( userKey );
    }

    public User getLoggedInPortalUser()
    {
        return userStoreService.getUserByKey( doGetUserKeyForLoggedInPortalUser() );
    }

    public UserEntity getLoggedInPortalUserAsEntity()
    {
        return userDao.findByKey( doGetUserKeyForLoggedInPortalUser() );
    }

    public UserEntity getImpersonatedPortalUser()
    {
        UserSpecification userSpecification = new UserSpecification();
        userSpecification.setKey( doGetUserKeyForImpersonatedPortalUser() );
        userSpecification.setDeletedStateNotDeleted();

        return userDao.findSingleBySpecification( userSpecification );
    }

    public boolean autoLoginPortalUser( QualifiedUsername qualifiedUsername )
    {
        try
        {
            doLoginPortalUser( qualifiedUsername, null, false );
            return true;
        }
        catch ( InvalidCredentialsException e )
        {
            return false;
        }
    }

    public User loginAdminUser( final LoginAdminUserCommand command )
    {
        return doLoginAdminUser( command.getQualifiedUsername(), command.getPassword(), command.isVerifyPassword() );
    }

    public boolean autoLoginAdminUser( final QualifiedUsername qualifiedUsername )
    {
        try
        {
            doLoginAdminUser( qualifiedUsername, null, false );
            return true;
        }
        catch ( InvalidCredentialsException e )
        {
            return false;
        }
        catch ( AdminConsoleAccessDeniedException e )
        {
            return false;
        }
    }

    public User doLoginAdminUser( final QualifiedUsername qualifiedUsername, final String password, final boolean verifyPassword )
    {
        final String uid = qualifiedUsername.getUsername();

        UserEntity user;

        if ( UserEntity.isBuiltInUser( uid ) )
        {
            UserSpecification userSpec = new UserSpecification();
            userSpec.setDeletedStateNotDeleted();
            UserKey userKey = authenticateBuiltInUser( uid, password, verifyPassword );
            userSpec.setKey( userKey );
            user = userDao.findSingleBySpecification( userSpec );
        }
        else
        {
            UserStoreEntity userStore;
            if ( qualifiedUsername.hasUserStoreSet() )
            {
                userStore = doResolveUserStore( qualifiedUsername );
            }
            else
            {
                userStore = doGetDefaultUserStore();
            }

            if ( userStore == null )
            {
                throw new InvalidCredentialsException( qualifiedUsername );
            }

            if ( verifyPassword )
            {
                userStoreService.authenticateUser( userStore.getKey(), uid, password );
            }

            userStoreService.synchronizeUser( userStore.getKey(), uid );

            UserSpecification userSpec = new UserSpecification();
            userSpec.setDeletedStateNotDeleted();
            userSpec.setUserStoreKey( userStore.getKey() );
            userSpec.setName( uid );
            user = userDao.findSingleBySpecification( userSpec );
        }

        if ( !adminConsoleLoginAccessResolver.hasAccess( user ) )
        {
            throw new AdminConsoleAccessDeniedException( qualifiedUsername );
        }

        AdminSecurityHolder.setUser( user.getKey() );

        return userStoreService.getUserByKey( user.getKey() );
    }

    public void loginPortalUser( final QualifiedUsername qualifiedUsername, final String password )
    {
        doLoginPortalUser( qualifiedUsername, password, true );
    }

    public void loginClientApiUser( final QualifiedUsername qualifiedUsername, final String password )
    {
        doLoginPortalUser( qualifiedUsername, password, true );
    }

    public void loginDavUser( final QualifiedUsername qualifiedUsername, final String password )
    {
        doLoginPortalUser( qualifiedUsername, password, true );
    }

    public void loginInstantTraceUser( final QualifiedUsername qualifiedUsername, final String password )
    {
        doLoginInstantTraceUser( qualifiedUsername, password );
    }

    private void doLoginPortalUser( final QualifiedUsername qualifiedUsername, final String password, final boolean verifyPassword )
    {
        final String uid = qualifiedUsername.getUsername();

        if ( UserEntity.isBuiltInUser( uid ) )
        {
            UserKey userKey = authenticateBuiltInUser( uid, password, verifyPassword );
            PortalSecurityHolder.setLoggedInUser( userKey );
        }
        else
        {

            UserStoreEntity userStore;
            if ( qualifiedUsername.hasUserStoreSet() )
            {
                userStore = doResolveUserStore( qualifiedUsername );
            }
            else
            {
                userStore = doGetDefaultUserStore();
            }

            if ( userStore == null )
            {
                throw new InvalidCredentialsException( qualifiedUsername );
            }

            if ( verifyPassword )
            {
                userStoreService.authenticateUser( userStore.getKey(), uid, password );
            }

            userStoreService.synchronizeUser( userStore.getKey(), uid );

            UserSpecification userSpec = new UserSpecification();
            userSpec.setDeletedStateNotDeleted();
            userSpec.setUserStoreKey( userStore.getKey() );
            userSpec.setName( uid );
            UserEntity user = userDao.findSingleBySpecification( userSpec );
            PortalSecurityHolder.setLoggedInUser( user.getKey() );
            PortalSecurityHolder.setImpersonatedUser( user.getKey() );
        }
    }

    private void doLoginInstantTraceUser( final QualifiedUsername qualifiedUsername, final String password )
    {
        final String uid = qualifiedUsername.getUsername();

        if ( UserEntity.isBuiltInUser( uid ) )
        {
            UserKey userKey = authenticateBuiltInUser( uid, password, true );
            InstantTraceSecurityHolder.setUser( userKey );
        }
        else
        {

            UserStoreEntity userStore;
            if ( qualifiedUsername.hasUserStoreSet() )
            {
                userStore = doResolveUserStore( qualifiedUsername );
            }
            else
            {
                userStore = doGetDefaultUserStore();
            }

            if ( userStore == null )
            {
                throw new InvalidCredentialsException( qualifiedUsername );
            }

            userStoreService.authenticateUser( userStore.getKey(), uid, password );

            userStoreService.synchronizeUser( userStore.getKey(), uid );

            UserSpecification userSpec = new UserSpecification();
            userSpec.setDeletedStateNotDeleted();
            userSpec.setUserStoreKey( userStore.getKey() );
            userSpec.setName( uid );
            UserEntity user = userDao.findSingleBySpecification( userSpec );
            InstantTraceSecurityHolder.setUser( user.getKey() );
        }
    }

    public UserEntity impersonatePortalUser( final ImpersonateCommand command )
    {
        Preconditions.checkNotNull( command.getUser() );

        if ( command.isRequireAccessCheck() )
        {
            final User current = getLoggedInPortalUser();
            if ( !current.isEnterpriseAdmin() )
            {
                throw new IllegalArgumentException( "Impersonate not allowed" );
            }
        }

        final UserEntity user = userDao.findByKey( command.getUser() );
        if ( user == null )
        {
            throw new UserNotFoundException( command.getUser() );
        }
        else if ( user.isAnonymous() )
        {
            throw new IllegalArgumentException( "Not allowed to impersonate anonymous user, use method removePortalImpersonation instead" );
        }
        else if ( user.isRoot() )
        {
            throw new IllegalArgumentException( "Not allowed to impersonate the admin user" );
        }

        PortalSecurityHolder.setImpersonatedUser( user.getKey() );
        return user;
    }

    @Override
    public void removePortalImpersonation()
    {
        PortalSecurityHolder.removeImpersonatedUser();
    }

    public void logoutAdminUser()
    {
        doLogoutAdminUser();
    }

    public void logoutPortalUser()
    {
        doLogoutPortalUser( false );
    }

    public void logoutClientApiUser( boolean invalidateSession )
    {
        doLogoutPortalUser( invalidateSession );
    }

    public void changePassword( final QualifiedUsername qualifiedUsername, final String newPassword )
    {
        final String uid = qualifiedUsername.getUsername();
        final UserEntity user = userDao.findByQualifiedUsername( qualifiedUsername );
        if ( user == null )
        {
            throw new InvalidCredentialsException( "Could not find user: " + qualifiedUsername );
        }
        final UserStoreKey userStoreKey = user.getUserStore() == null ? null : user.getUserStore().getKey();
        userStoreService.changePassword( userStoreKey, uid, newPassword );
    }

    private void doLogoutAdminUser()
    {
        AdminSecurityHolder.setUser( null );

        // Only invalidate session if logged out of both "portal" and "admin". Check portal user!
        if ( PortalSecurityHolder.getLoggedInUser() == null )
        {
            invalidateSession();
        }
    }

    private void doLogoutPortalUser( boolean invalidateSession )
    {
        PortalSecurityHolder.setLoggedInUser( null );
        PortalSecurityHolder.setImpersonatedUser( null );

        // Only invalidate session if logged out of both "portal" and "admin". Check admin user!
        if ( invalidateSession && AdminSecurityHolder.getUser() == null )
        {
            invalidateSession();
        }
    }

    private void invalidateSession()
    {
        HttpServletRequest request = ServletRequestAccessor.getRequest();
        HttpSession session = request.getSession( false );

        if ( null != session )
        {
            clearSession( session );
        }
    }

    private void clearSession( HttpSession session )
    {
        Enumeration attributeNames = session.getAttributeNames();
        while ( attributeNames.hasMoreElements() )
        {
            String attributeName = (String) attributeNames.nextElement();
            if ( !attributeName.startsWith( "Instant-Trace-" ) )
            {
                session.removeAttribute( attributeName );
            }
        }
    }

    private UserStoreEntity doGetDefaultUserStore()
    {
        UserStoreEntity defaultUserStore = userStoreDao.findDefaultUserStore();
        if ( defaultUserStore == null )
        {
            throw new IllegalStateException( "Expected default user store to be set" );
        }
        return defaultUserStore;
    }

    private UserStoreEntity doResolveUserStore( final QualifiedUsername qualifiedUsername )
    {
        UserStoreKey userStoreKey = qualifiedUsername.getUserStoreKey();
        if ( userStoreKey != null )
        {
            /* Key passed in as a part of the qualifiedUsername. Use it. */
            return userStoreDao.findByKey( userStoreKey );
        }

        if ( qualifiedUsername.hasUserStoreNameSet() )
        {
            /* Key not passed in as a part of the qualifiedUsername. But name is. Trying to find the key from name. */
            return userStoreDao.findByName( qualifiedUsername.getUserStoreName() );
        }

        throw new IllegalArgumentException( "Given qualified username has no user store set" );
    }

    private UserKey authenticateBuiltInUser( final String uid, final String password, final boolean verifyPassword )
    {
        final UserEntity user = userDao.findBuiltInGlobalByName( uid );

        if ( user == null )
        {
            throw new IllegalArgumentException( "Could not retrieve built-in user: " + uid );
        }

        if ( user.isRoot() )
        {
            if ( !verifyPassword || adminUserPassword.equals( password ) )
            {
                return user.getKey();
            }
            throw new InvalidCredentialsException( uid );
        }

        if ( user.isAnonymous() )
        {
            return user.getKey();
        }

        throw new IllegalArgumentException( "Cannot authenticate built in user: " + uid );
    }

    private UserKey doGetUserKeyForLoggedInPortalUser()
    {
        initializeSecurityHolder();
        return PortalSecurityHolder.getLoggedInUser();
    }

    private UserKey doGetUserKeyForImpersonatedPortalUser()
    {
        initializeSecurityHolder();
        return PortalSecurityHolder.getImpersonatedUser();
    }

    private UserKey doGetUserKeyForLoggedInAdminConsoleUser()
    {
        return AdminSecurityHolder.getUser();
    }

    @Override
    public UserEntity findUserByEmail( final String userStoreName, final String email )
    {
        UserStoreEntity userStore = null;

        if ( Strings.isNullOrEmpty( userStoreName ) )
        {
            userStore = doGetDefaultUserStore();
        }
        else
        {
            userStore = this.userStoreDao.findByName( userStoreName );
        }

        if ( userStore == null )
        {
            return null;
        }

        final UserSpecification spec = new UserSpecification();
        spec.setEmail( email );
        spec.setUserStoreKey( userStore.getKey() );
        spec.setDeletedStateNotDeleted();

        final List<UserEntity> users = this.userDao.findBySpecification( spec );
        return users.isEmpty() ? null : users.get( 0 );
    }
}
