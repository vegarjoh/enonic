/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.structure.menuitem.section;

import java.io.Serializable;
import java.util.Date;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.structure.SiteEntity;
import com.enonic.cms.core.structure.menuitem.MenuItemEntity;

public class SectionContentEntity
    implements Serializable, Comparable<SectionContentEntity>
{
    private SectionContentKey key;

    private int order;

    private Date timestamp;

    private Integer approved;

    private MenuItemEntity menuItem;

    private ContentEntity content;

    public SectionContentKey getKey()
    {
        return key;
    }

    public int getOrder()
    {
        return order;
    }

    public Date getTimestamp()
    {
        return timestamp;
    }

    public boolean isApproved()
    {
        return approved != null && approved != 0;
    }

    public Integer getApproved()
    {
        return approved;
    }

    public MenuItemEntity getMenuItem()
    {
        return menuItem;
    }

    public ContentEntity getContent()
    {
        return content;
    }

    public void setKey( SectionContentKey key )
    {
        this.key = key;
    }

    public void setOrder( int order )
    {
        this.order = order;
    }

    public void setTimestamp( Date timestamp )
    {
        this.timestamp = timestamp;
    }

    public void setApproved( boolean approved )
    {
        if ( approved )
        {
            this.approved = 1;
        }
        else
        {
            this.approved = 0;
        }
    }

    public void setMenuItem( MenuItemEntity menuItem )
    {
        this.menuItem = menuItem;
    }

    public void setContent( ContentEntity content )
    {
        this.content = content;
    }

    public SiteEntity getSite()
    {
        return getMenuItem().getSite();
    }

    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof SectionContentEntity ) )
        {
            return false;
        }

        SectionContentEntity that = (SectionContentEntity) o;

        if ( key != null ? !key.equals( that.key ) : that.key != null )
        {
            return false;
        }

        return true;
    }

    public int hashCode()
    {
        return new HashCodeBuilder( 689, 781 ).append( key ).toHashCode();
    }

    public int compareTo( SectionContentEntity other )
    {
        if ( this.getOrder() < other.getOrder() )
        {
            return -1;
        }
        else if ( this.getOrder() == other.getOrder() )
        {
            return 0;
        }
        else
        {
            return 1;
        }
    }

    public String toString()
    {
        return ", menuItem=" + menuItem + ", content=" + content + ", order=" + order + ", approved=" + approved;
    }
}
