/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.structure.menuitem;

public interface MenuItemService
{

    void execute( MenuItemServiceCommand... commands );
}
