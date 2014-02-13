/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.admin;

import com.enonic.cms.core.security.user.QualifiedUsername;

public class AdminConsoleAccessDeniedException
    extends RuntimeException
{
    public AdminConsoleAccessDeniedException( QualifiedUsername qname )
    {
        super( "Access denied to Admin Console for user: " + qname.toString() );
    }

}