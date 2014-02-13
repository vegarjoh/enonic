/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.preference;

import com.enonic.cms.core.security.user.UserEntity;


public class PreferenceAccessResolver
{


    public boolean hasReadAccess( UserEntity user )
    {

        // anon is not allowed to have preferences
        if ( user.isAnonymous() )
        {
            return false;
        }

        return true;
    }

    public boolean hasWriteAccess( UserEntity user )
    {

        // anon is not allowed to have preferences
        if ( user.isAnonymous() )
        {
            return false;
        }

        return true;
    }
}
