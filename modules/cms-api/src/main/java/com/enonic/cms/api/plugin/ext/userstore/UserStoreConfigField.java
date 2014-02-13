/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.api.plugin.ext.userstore;

public final class UserStoreConfigField
    implements Comparable<UserStoreConfigField>
{
    private final UserFieldType type;

    private boolean required = false;

    private boolean readOnly = false;

    private boolean remote = false;

    private boolean iso = true;

    public UserStoreConfigField( final UserFieldType type )
    {
        this.type = type;
    }

    public UserFieldType getType()
    {
        return type;
    }

    public boolean isRequired()
    {
        return required;
    }

    public void setRequired( final boolean value )
    {
        required = value;
    }

    public boolean isReadOnly()
    {
        return readOnly;
    }

    public void setReadOnly( final boolean value )
    {
        readOnly = value;
    }

    public boolean isRemote()
    {
        return remote;
    }

    public void setRemote( final boolean value )
    {
        remote = value;
    }

    public boolean useIso()
    {
        return iso;
    }

    public void setIso( final boolean value )
    {
        iso = value;
    }

    public int compareTo( final UserStoreConfigField o )
    {
        return type.getName().compareTo( o.getType().getName() );
    }
}
