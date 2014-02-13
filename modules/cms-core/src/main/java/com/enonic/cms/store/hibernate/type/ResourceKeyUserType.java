/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.store.hibernate.type;

import com.enonic.cms.core.resource.ResourceKey;

public class ResourceKeyUserType
    extends AbstractStringBasedUserType<ResourceKey>
{
    public ResourceKeyUserType()
    {
        super( ResourceKey.class );
    }

    public boolean isMutable()
    {
        return false;
    }

    public ResourceKey get( final String value )
    {
        return ResourceKey.from( value );
    }

    public String getStringValue( final ResourceKey value )
    {
        return value.toString();
    }
}
