/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.vertical.engine;

public final class CategoryAccessRight
    extends AccessRight
{

    private boolean create;

    private boolean publish;

    private boolean administrate;

    private boolean adminread;

    public CategoryAccessRight()
    {
    }

    public void setCreate( boolean create )
    {
        this.create = create;
    }

    public void setPublish( boolean publish )
    {
        this.publish = publish;
    }

    public void setAdministrate( boolean administrate )
    {
        this.administrate = administrate;
    }

    public void setAdminRead( boolean adminread )
    {
        this.adminread = adminread;
    }

    public boolean getCreate()
    {
        return create;
    }

    public boolean getPublish()
    {
        return publish;
    }

    public boolean getAdministrate()
    {
        return administrate;
    }

    public boolean getAdminRead()
    {
        return adminread;
    }
}
