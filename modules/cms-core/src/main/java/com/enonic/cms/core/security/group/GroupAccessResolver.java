/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.security.group;

import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.security.userstore.UserStoreEntity;


public interface GroupAccessResolver
{

    boolean hasReadGroupAccess( UserEntity reader, GroupEntity group );

    boolean hasCreateGroupAccess( UserEntity executor, GroupType userstoreGroup, UserStoreEntity userStore );

    boolean hasDeleteGroupAccess( UserEntity executor, GroupEntity group );

    boolean hasUpdateGroupAccess( UserEntity executor, GroupEntity subject );

    boolean hasAddMembershipAccess( UserEntity executor, GroupEntity groupToAdd, GroupEntity groupToAddTo );

    boolean hasRemoveMembershipAccess( UserEntity executor, GroupEntity groupToRemove, GroupEntity groupToRemoveFrom );
}
