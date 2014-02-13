/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.security.group;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.security.userstore.MemberOfResolver;
import com.enonic.cms.core.security.userstore.UserStoreEntity;

@Component("groupAccessResolver")
public class GroupAccessResolverImpl
    implements GroupAccessResolver
{

    @Autowired
    private MemberOfResolver memberOfResolver;

    @Override
    public boolean hasReadGroupAccess( UserEntity reader, GroupEntity group )
    {
        GroupType groupType = group.getType();

        if ( groupType.equals( GroupType.USERSTORE_GROUP ) || groupType.equals( GroupType.USERSTORE_ADMINS ) ||
            groupType.equals( GroupType.AUTHENTICATED_USERS ) )
        {
            return memberOfResolver.hasUserStoreAdministratorPowers( reader, group.getUserStore().getKey() );
        }

        if ( groupType.equals( GroupType.ENTERPRISE_ADMINS ) )
        {
            return memberOfResolver.hasEnterpriseAdminPowers( reader );
        }

        if ( groupType.equals( GroupType.GLOBAL_GROUP ) || groupType.equals( GroupType.ADMINS ) ||
            groupType.equals( GroupType.CONTRIBUTORS ) || groupType.equals( GroupType.DEVELOPERS ) ||
            groupType.equals( GroupType.EXPERT_CONTRIBUTORS ) )
        {
            return memberOfResolver.hasAdministratorPowers( reader.getKey() );
        }

        // User groups (GroupType.USER) or the Anonymous group (GroupType.ANONYMOUS) are not available for access by anyone:
        return false;
    }

    public boolean hasCreateGroupAccess( UserEntity executor, GroupType groupType, UserStoreEntity userStore )
    {
        if ( !( groupType.equals( GroupType.USERSTORE_GROUP ) || groupType.equals( GroupType.GLOBAL_GROUP ) ) )
        {
            throw new UnsupportedOperationException( "Resolving access of given group type not supported: " + groupType );
        }

        if ( groupType.equals( GroupType.USERSTORE_GROUP ) &&
            memberOfResolver.hasUserStoreAdministratorPowers( executor, userStore.getKey() ) )
        {
            return true;
        }
        else if ( groupType.equals( GroupType.GLOBAL_GROUP ) && memberOfResolver.hasAdministratorPowers( executor.getKey() ) )
        {
            return true;
        }

        return false;
    }

    public boolean hasDeleteGroupAccess( UserEntity executor, GroupEntity subject )
    {
        GroupType groupType = subject.getType();

        if ( !( groupType.equals( GroupType.USERSTORE_GROUP ) || groupType.equals( GroupType.GLOBAL_GROUP ) ) )
        {
            throw new UnsupportedOperationException( "Resolving delete access of given group type not supported: " + groupType );
        }

        if ( groupType.equals( GroupType.USERSTORE_GROUP ) &&
            memberOfResolver.hasUserStoreAdministratorPowers( executor, subject.getUserStore().getKey() ) )
        {
            return true;
        }
        else if ( groupType.equals( GroupType.GLOBAL_GROUP ) && memberOfResolver.hasAdministratorPowers( executor.getKey() ) )
        {
            return true;
        }

        return false;
    }

    public boolean hasUpdateGroupAccess( UserEntity executor, GroupEntity subject )
    {
        final GroupType groupType = subject.getType();

        if ( groupType.equals( GroupType.USER ) || groupType.equals( GroupType.AUTHENTICATED_USERS ) ||
            groupType.equals( GroupType.ANONYMOUS ) )
        {
            throw new UnsupportedOperationException( "Resolving update access of given group type not supported: " + groupType );
        }

        if ( GroupType.ENTERPRISE_ADMINS.equals( groupType ) && memberOfResolver.hasEnterpriseAdminPowers( executor ) )
        {
            return true;
        }
        else if ( GroupType.ADMINS.equals( groupType ) && memberOfResolver.hasAdministratorPowers( executor ) )
        {
            return true;
        }
        else if ( GroupType.DEVELOPERS.equals( groupType ) && memberOfResolver.hasAdministratorPowers( executor ) )
        {
            return true;
        }
        else if ( GroupType.EXPERT_CONTRIBUTORS.equals( groupType ) && memberOfResolver.hasAdministratorPowers( executor ) )
        {
            return true;
        }
        else if ( GroupType.CONTRIBUTORS.equals( groupType ) && memberOfResolver.hasAdministratorPowers( executor ) )
        {
            return true;
        }
        else if ( GroupType.GLOBAL_GROUP.equals( groupType ) && memberOfResolver.hasAdministratorPowers( executor ) )
        {
            return true;
        }
        else if ( GroupType.USERSTORE_ADMINS.equals( groupType ) &&
            memberOfResolver.hasUserStoreAdministratorPowers( executor, subject.getUserStore().getKey() ) )
        {
            return true;
        }
        else if ( groupType.equals( GroupType.USERSTORE_GROUP ) &&
            memberOfResolver.hasUserStoreAdministratorPowers( executor, subject.getUserStore().getKey() ) )
        {
            return true;
        }

        return false;
    }

    public boolean hasRemoveMembershipAccess( UserEntity executor, GroupEntity groupToRemove, GroupEntity groupToRemoveFrom )
    {
        if ( hasUpdateGroupAccess( executor, groupToRemoveFrom ) )
        {
            return true;
        }

        final boolean isExecutorRemovingSelfFromGroup = executor.getUserGroup().equals( groupToRemove );
        if ( isExecutorRemovingSelfFromGroup && !groupToRemoveFrom.isRestricted() )
        {
            return true;
        }

        return false;
    }

    public boolean hasAddMembershipAccess( UserEntity executor, GroupEntity groupToAdd, GroupEntity groupToAddTo )
    {
        if ( hasUpdateGroupAccess( executor, groupToAddTo ) )
        {
            return true;
        }

        final boolean isExecutorAddingSelfToGroup = executor.getUserGroup().equals( groupToAdd );
        if ( isExecutorAddingSelfToGroup && !groupToAddTo.isRestricted() )
        {
            return true;
        }

        return false;
    }

}
