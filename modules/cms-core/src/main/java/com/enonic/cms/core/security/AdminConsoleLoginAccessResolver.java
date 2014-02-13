/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.security;

import org.springframework.stereotype.Component;

import com.enonic.cms.core.security.group.GroupEntity;
import com.enonic.cms.core.security.user.UserEntity;

@Component
public class AdminConsoleLoginAccessResolver
{
    public boolean hasAccess( UserEntity user )
    {
        if ( user.isEnterpriseAdmin() )
        {
            return true;
        }

        for ( GroupEntity group : user.getAllMembershipsGroups() )
        {
            switch ( group.getType() )
            {
                case USERSTORE_ADMINS:
                case ADMINS:
                case CONTRIBUTORS:
                case EXPERT_CONTRIBUTORS:
                case DEVELOPERS:
                    return true;
            }
        }
        return false;
    }
}
