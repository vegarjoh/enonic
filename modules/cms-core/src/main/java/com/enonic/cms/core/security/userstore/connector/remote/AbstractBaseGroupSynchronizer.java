/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.security.userstore.connector.remote;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.enonic.cms.api.plugin.ext.userstore.RemoteUserStore;
import com.enonic.cms.core.security.group.GroupEntity;
import com.enonic.cms.core.security.group.GroupSpecification;
import com.enonic.cms.core.security.group.GroupType;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.security.user.UserSpecification;
import com.enonic.cms.core.security.userstore.UserStoreEntity;
import com.enonic.cms.core.security.userstore.UserStoreKey;
import com.enonic.cms.core.security.userstore.connector.synchronize.status.SynchronizeStatus;
import com.enonic.cms.api.plugin.ext.userstore.RemoteGroup;
import com.enonic.cms.api.plugin.ext.userstore.RemotePrincipal;
import com.enonic.cms.api.plugin.ext.userstore.RemoteUser;
import com.enonic.cms.store.dao.GroupDao;
import com.enonic.cms.store.dao.UserDao;

/**
 * Jun 30, 2009
 */
public abstract class AbstractBaseGroupSynchronizer
{
    protected final boolean syncMemberships;

    protected final boolean syncMembers;

    protected final UserStoreEntity userStore;

    protected UserDao userDao;

    protected GroupDao groupDao;

    protected RemoteUserStore remoteUserStorePlugin;

    protected final boolean syncGroup;

    protected SynchronizeStatus status = null;

    void setStatusCollector( final SynchronizeStatus value )
    {
        status = value;
    }

    public AbstractBaseGroupSynchronizer( final UserStoreEntity userStore, final boolean syncGroup, final boolean syncMemberships,
                                          final boolean syncMembers )
    {
        this.userStore = userStore;
        this.syncMemberships = syncMemberships;
        this.syncMembers = syncMembers;
        this.syncGroup = syncGroup;
    }

    protected UserStoreKey getUserStoreKey()
    {
        return userStore.getKey();
    }

    protected boolean resurrectGroup( final GroupEntity group )
    {
        final boolean resurrected = group.isDeleted();
        group.setDeleted( false );
        return resurrected;
    }

    protected void syncGroupMemberships( final GroupEntity localGroupToSync, final RemoteGroup remoteGroup, final MemberCache memberCache )
    {
        final List<RemoteGroup> remoteMemberships = remoteUserStorePlugin.getMemberships( remoteGroup );

        removeLocalGroupMembershipsNotExistingRemote( localGroupToSync, remoteMemberships );

        for ( final RemoteGroup remoteMembership : remoteMemberships )
        {
            syncGroupMembershipOfTypeGroup( localGroupToSync, remoteMembership, memberCache );
        }
    }

    protected void syncGroupMembers( final GroupEntity localGroupToSync, final RemoteGroup remoteGroup, final MemberCache memberCache )
    {
        // TODO: retrieval of groups in remoteUserStorePlugin.getMembers should be batched due to timeout caused by large number of members.
        final List<RemotePrincipal> remoteMembers = remoteUserStorePlugin.getMembers( remoteGroup );
        removeLocalGroupMembersNotExistingRemote( localGroupToSync, remoteMembers );

        for ( final RemotePrincipal remoteMember : remoteMembers )
        {
            if ( remoteMember instanceof RemoteUser )
            {
                final RemoteUser remoteUserMember = (RemoteUser) remoteMember;

                syncGroupMemberOfTypeUser( localGroupToSync, remoteUserMember, memberCache );
            }
            else
            {
                final RemoteGroup remoteGroupMember = (RemoteGroup) remoteMember;

                syncGroupMemberOfTypeGroup( localGroupToSync, remoteGroupMember, memberCache );
            }
        }
    }

    private void removeLocalGroupMembersNotExistingRemote( final GroupEntity localGroupToSync, final List<RemotePrincipal> remoteMembers )
    {
        // Gather remote groups in a map for fast and easy access
        final Map<String, RemoteGroup> remoteMemberMapOfTypeGroup = new HashMap<String, RemoteGroup>();
        final Map<String, RemoteUser> remoteMemberMapOfTypeUser = new HashMap<String, RemoteUser>();
        for ( final RemotePrincipal remoteMember : remoteMembers )
        {
            if ( remoteMember instanceof RemoteUser )
            {
                final RemoteUser remoteMemberOfTypeUser = (RemoteUser) remoteMember;
                remoteMemberMapOfTypeUser.put( remoteMemberOfTypeUser.getId() + "-" + remoteMemberOfTypeUser.getSync(),
                                               remoteMemberOfTypeUser );
            }
            else
            {
                final RemoteGroup remoteMemberOfTypeGroup = (RemoteGroup) remoteMember;
                remoteMemberMapOfTypeGroup.put( remoteMemberOfTypeGroup.getId() + "-" + remoteMemberOfTypeGroup.getSync(),
                                                remoteMemberOfTypeGroup );
            }
        }

        // Gather local members that does not exist remote
        final Set<GroupEntity> localMembersToRemove = new HashSet<GroupEntity>();
        for ( final GroupEntity localMember : localGroupToSync.getMembers( false ) )
        {
            if ( localMember.isOfType( GroupType.USER, false ) )
            {
                final boolean notExistsRemote =
                    !remoteMemberMapOfTypeUser.containsKey( localMember.getUser().getName() + "-" + localMember.getSyncValue() );
                if ( notExistsRemote )
                {
                    localMembersToRemove.add( localMember );
                }
            }
            else
            {
                final boolean notExistsRemote =
                    !remoteMemberMapOfTypeGroup.containsKey( localMember.getName() + "-" + localMember.getSyncValue() );
                if ( notExistsRemote )
                {
                    localMembersToRemove.add( localMember );
                }
            }
        }

        // Remove local members that does not exist remote
        for ( final GroupEntity localMemberToRemove : localMembersToRemove )
        {
            localMemberToRemove.removeMembership( localGroupToSync );
        }
    }

    private void syncGroupMemberOfTypeUser( final GroupEntity localGroup, final RemoteUser remoteUserMember, final MemberCache memberCache )
    {
        final UserSpecification spec = new UserSpecification();
        spec.setUserStoreKey( getUserStoreKey() );
        spec.setSyncValue( remoteUserMember.getSync() );
        spec.setDeletedState( UserSpecification.DeletedState.ANY );

        UserEntity existingMember = memberCache.getMemberOfTypeUser( spec );
        if ( existingMember == null )
        {
            existingMember = userDao.findSingleBySpecification( spec );
            if ( existingMember != null )
            {
                memberCache.addMemeberOfTypeUser( existingMember );
            }
        }

        if ( existingMember == null )
        {
            // skip creation - only supported in full sync
        }
        else
        {
            final GroupEntity existingUserGroup = existingMember.getUserGroup();
            if ( localGroup.hasMember( existingUserGroup ) )
            {
                // all is fine
            }
            else
            {
                existingUserGroup.addMembership( localGroup );
            }

        }
    }

    private void syncGroupMemberOfTypeGroup( final GroupEntity localGroup, final RemoteGroup remoteGroupMember,
                                             final MemberCache memberCache )
    {
        GroupEntity existingMember = findGroupBySyncValue( remoteGroupMember.getSync(), memberCache );

        if ( existingMember == null )
        {
            // skip creation - only supported in full sync
        }
        else
        {
            if ( localGroup.hasMember( existingMember ) )
            {
                // all is fine
            }
            else
            {
                existingMember.addMembership( localGroup );
            }
        }
    }

    private void syncGroupMembershipOfTypeGroup( final GroupEntity localGroup, final RemoteGroup remoteGroupMember,
                                                 final MemberCache memberCache )
    {
        GroupEntity existingMember = findGroupBySyncValue( remoteGroupMember.getSync(), memberCache );

        if ( existingMember == null )
        {
            // skip creation - only supported in full sync
        }
        else
        {
            if ( localGroup.hasMembership( localGroup ) )
            {
                // all is fine
                if ( status != null )
                {
                    status.groupMembershipVerified();
                }
            }
            else
            {
                localGroup.addMembership( existingMember );
                if ( status != null )
                {
                    status.groupMembershipCreated();
                }
            }
        }
    }

    protected void removeLocalGroupMembershipsNotExistingRemote( final GroupEntity localGroup, final List<RemoteGroup> remoteMemberships )
    {
        // Gather remote users in a map for fast and easy access
        final Map<String, RemoteGroup> remoteMembershipsMap = new HashMap<String, RemoteGroup>();
        for ( final RemoteGroup remoteMembership : remoteMemberships )
        {
            remoteMembershipsMap.put( remoteMembership.getId() + "-" + remoteMembership.getSync(), remoteMembership );
        }

        // Gather local memberships that does not exist remote
        final Set<GroupEntity> localMembershipsToRemove = new HashSet<GroupEntity>();
        for ( final GroupEntity localMembership : localGroup.getMemberships( false ) )
        {
            // We're not removing memberships in built-in or global groups...
            if ( !localMembership.isBuiltIn() && !localMembership.isGlobal() )
            {
                final RemoteGroup remoteMembership =
                    remoteMembershipsMap.get( localMembership.getName() + "-" + localMembership.getSyncValue() );
                if ( remoteMembership == null )
                {
                    localMembershipsToRemove.add( localMembership );
                }
            }
        }

        // Remove local memberships that does not exist remote
        for ( final GroupEntity localMembershipToRemove : localMembershipsToRemove )
        {
            localGroup.removeMembership( localMembershipToRemove );
            if ( status != null )
            {
                status.groupMembershipDeleted();
            }
        }
    }

    private GroupEntity findGroupBySyncValue( final String syncValue, final MemberCache memberCache )
    {
        final GroupSpecification spec = new GroupSpecification();
        spec.setUserStoreKey( getUserStoreKey() );
        spec.setSyncValue( syncValue );

        GroupEntity existingMember = memberCache.getMemberOfTypeGroup( spec );
        if ( existingMember == null )
        {
            existingMember = groupDao.findSingleBySpecification( spec );
            if ( existingMember != null )
            {
                memberCache.addMemeberOfTypeGroup( existingMember );
            }
        }
        return existingMember;
    }

    public void setGroupDao( final GroupDao value )
    {
        this.groupDao = value;
    }

    public void setUserDao( final UserDao value )
    {
        this.userDao = value;
    }

    public void setRemoteUserStorePlugin( final RemoteUserStore value )
    {
        this.remoteUserStorePlugin = value;
    }
}
