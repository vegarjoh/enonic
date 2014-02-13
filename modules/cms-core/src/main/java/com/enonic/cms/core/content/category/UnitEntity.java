/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.content.category;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.enonic.cms.core.content.contenttype.ContentTypeEntity;
import com.enonic.cms.core.language.LanguageEntity;

public class UnitEntity
    implements Serializable
{
    private UnitKey key;

    private String name;

    private String description;

    private Date timestamp;

    private Integer deleted;

    private UnitEntity parent;

    private LanguageEntity language;

    private Set<ContentTypeEntity> contentTypes = new LinkedHashSet<ContentTypeEntity>();

    public UnitKey getKey()
    {
        return key;
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public Date getTimestamp()
    {
        return timestamp;
    }

    public boolean isDeleted()
    {
        return deleted != 0;
    }

    public LanguageEntity getLanguage()
    {
        return language;
    }

    public void addContentType( ContentTypeEntity contentType )
    {
        contentTypes.add( contentType );
    }

    public Set<ContentTypeEntity> getContentTypes()
    {
        return contentTypes;
    }

    public void removeContentTypes()
    {
        contentTypes.clear();
    }

    public void setKey( UnitKey key )
    {
        this.key = key;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public void setTimestamp( Date timestamp )
    {
        this.timestamp = timestamp;
    }

    public void setDeleted( boolean deleted )
    {
        this.deleted = deleted ? 1 : 0;
    }

    public void setLanguage( LanguageEntity language )
    {
        this.language = language;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof UnitEntity ) )
        {
            return false;
        }

        UnitEntity that = (UnitEntity) o;
        return getKey().equals( that.getKey() );
    }

    public boolean synchronizeContentTypes( final Collection<ContentTypeEntity> allowedContentTypes )
    {
        boolean removed = contentTypes.retainAll( allowedContentTypes );
        boolean added = contentTypes.addAll( allowedContentTypes );
        // TODO return
        return removed || added;
    }

    public int hashCode()
    {
        return new HashCodeBuilder( 549, 363 ).append( key ).toHashCode();
    }
}
