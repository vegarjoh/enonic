/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.security.userstore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.enonic.cms.core.security.user.UserEntity;

@Component
public class UserStoreAccessResolver
{
    private MemberOfResolver memberOfResolver;

    public boolean hasReadUserAccess( UserEntity user, UserStoreEntity userstore )
    {
        return memberOfResolver.hasUserStoreAdministratorPowers( user, userstore.getKey() );
    }

    public boolean hasDeleteUserAccess( UserEntity user, UserStoreEntity userstore )
    {
        return memberOfResolver.hasUserStoreAdministratorPowers( user, userstore.getKey() );
    }

    public boolean hasCreateUserAccess( UserEntity user, UserStoreEntity userstore )
    {
        return memberOfResolver.hasUserStoreAdministratorPowers( user, userstore.getKey() );
    }

    public boolean hasUpdateUserAccess( UserEntity updater, UserStoreEntity userstore, boolean allowedToUpdateSelf,
                                        UserEntity userToUpdate )
    {
        if ( allowedToUpdateSelf && updater.equals( userToUpdate ) )
        {
            return true;
        }

        return memberOfResolver.hasUserStoreAdministratorPowers( updater, userstore.getKey() );
    }

    public boolean hasCreateUserStoreAccess( UserEntity user )
    {
        return memberOfResolver.hasEnterpriseAdminPowers( user );
    }

    public boolean hasUpdateUserStoreAccess( UserEntity user )
    {
        return memberOfResolver.hasEnterpriseAdminPowers( user );
    }

    public boolean hasDeleteUserStoreAccess( UserEntity user )
    {
        return memberOfResolver.hasEnterpriseAdminPowers( user );
    }

    @Autowired
    public void setMemberOfResolver( MemberOfResolver memberOfResolver )
    {
        this.memberOfResolver = memberOfResolver;
    }
}
