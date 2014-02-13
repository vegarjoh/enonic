/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal;

import com.enonic.cms.core.structure.menuitem.MenuItemKey;

public class PortalAccessDeniedException
    extends RuntimeException
    implements ForbiddenErrorType
{

    private String message;

    public PortalAccessDeniedException( MenuItemKey menuItemKey )
    {
        this.message = "Access denied to menu item: " + menuItemKey.toString();
    }

    public String getMessage()
    {
        return message;
    }

}
