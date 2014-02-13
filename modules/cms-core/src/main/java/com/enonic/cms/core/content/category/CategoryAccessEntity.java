/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.content.category;

import java.io.Serializable;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.enonic.cms.core.security.group.GroupEntity;

public class CategoryAccessEntity
    implements Serializable
{
    private CategoryAccessKey key;

    private GroupEntity group;

    private int readAccess = 0;

    private int adminBrowseAccess = 0;

    private int publishAccess = 0;

    private int createAccess = 0;

    private int adminAccess = 0;


    public CategoryAccessKey getKey()
    {
        return key;
    }

    public GroupEntity getGroup()
    {
        return group;
    }

    public void setGroup( GroupEntity group )
    {
        this.group = group;
    }

    public boolean isReadAccess()
    {
        return readAccess != 0;
    }

    public boolean isCreateAccess()
    {
        return createAccess != 0;
    }

    public boolean isPublishAccess()
    {
        return publishAccess != 0;
    }

    public boolean isAdminAccess()
    {
        return adminAccess != 0;
    }

    public boolean isAdminBrowseAccess()
    {
        return adminBrowseAccess != 0;
    }

    public void setKey( CategoryAccessKey key )
    {
        this.key = key;
    }

    public void setReadAccess( boolean readAccess )
    {
        this.readAccess = readAccess ? 1 : 0;
    }

    public void setCreateAccess( boolean createAccess )
    {
        this.createAccess = createAccess ? 1 : 0;
    }

    public void setPublishAccess( boolean publishAccess )
    {
        this.publishAccess = publishAccess ? 1 : 0;
    }

    public void setAdminAccess( boolean adminAccess )
    {
        this.adminAccess = adminAccess ? 1 : 0;
    }

    public void setAdminBrowseAccess( boolean adminReadAccess )
    {
        this.adminBrowseAccess = adminReadAccess ? 1 : 0;
    }

    public void setAll( boolean value )
    {
        this.readAccess = value ? 1 : 0;
        this.adminBrowseAccess = value ? 1 : 0;
        this.createAccess = value ? 1 : 0;
        this.publishAccess = value ? 1 : 0;
        this.adminAccess = value ? 1 : 0;
    }

    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof CategoryAccessEntity ) )
        {
            return false;
        }

        CategoryAccessEntity that = (CategoryAccessEntity) o;

        return key.equals( that.getKey() );

    }

    public int hashCode()
    {
        return new HashCodeBuilder( 741, 173 ).append( key ).toHashCode();
    }

    public boolean givesRead()
    {
        return isReadAccess() || isAdminAccess();
    }

    public boolean givesAdminBrowse()
    {
        return isAdminBrowseAccess() || isAdminAccess();
    }

    public boolean givesCreate()
    {
        return isCreateAccess() || isAdminAccess();
    }

    public boolean givesApprove()
    {
        return isPublishAccess() || isAdminAccess();
    }

    public boolean givesAdministrate()
    {
        return isAdminAccess();
    }

    public boolean givesContentReadAccess()
    {
        return isReadAccess() || isAdminBrowseAccess() || givesApprove();
    }

    public boolean givesContentUpdateAccess()
    {
        return givesApprove();
    }

    public boolean givesContentDeleteAccess()
    {
        return givesApprove();
    }

    public void setAccess( CategoryAccessControl bluePrint )
    {
        this.readAccess = bluePrint.isReadAccess() ? 1 : 0;
        this.adminBrowseAccess = bluePrint.isAdminBrowseAccess() ? 1 : 0;
        this.publishAccess = bluePrint.isPublishAccess() ? 1 : 0;
        this.createAccess = bluePrint.isCreateAccess() ? 1 : 0;
        this.adminAccess = bluePrint.isAdminAccess() ? 1 : 0;
    }

    public static CategoryAccessEntity create( CategoryKey category, GroupEntity group, CategoryAccessControl car )
    {
        final CategoryAccessEntity ca = new CategoryAccessEntity();
        ca.setKey( new CategoryAccessKey( category, group.getGroupKey() ) );
        ca.setGroup( group );
        ca.setReadAccess( car.isReadAccess() );
        ca.setAdminBrowseAccess( car.isAdminBrowseAccess() );
        ca.setPublishAccess( car.isPublishAccess() );
        ca.setCreateAccess( car.isCreateAccess() );
        ca.setAdminAccess( car.isAdminAccess() );
        return ca;
    }

    public CategoryAccessControl toAccessRights()
    {
        CategoryAccessControl car = new CategoryAccessControl();
        car.setGroupKey( this.group.getGroupKey() );
        car.setReadAccess( this.isReadAccess() );
        car.setAdminBrowseAccess( this.isAdminBrowseAccess() );
        car.setPublishAccess( this.isPublishAccess() );
        car.setCreateAccess( this.isCreateAccess() );
        car.setAdminAccess( this.isAdminAccess() );
        return car;
    }
}
