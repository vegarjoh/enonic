/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.security.user;

import com.enonic.cms.api.plugin.ext.userstore.UserFieldType;


public class ReadOnlyUserFieldPolicyException
    extends RuntimeException
{
    public ReadOnlyUserFieldPolicyException( UserFieldType type )
    {
        super( buildMessage( type ) );

    }

    private static String buildMessage( UserFieldType type )
    {
        return "Read only user field not expected: " + type.getName();
    }


}
