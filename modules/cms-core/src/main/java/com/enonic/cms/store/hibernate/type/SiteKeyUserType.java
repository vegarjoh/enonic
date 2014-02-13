/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.store.hibernate.type;

import com.enonic.cms.core.structure.SiteKey;


public class SiteKeyUserType
    extends AbstractIntegerBasedUserType<SiteKey>
{
    public SiteKeyUserType()
    {
        super( SiteKey.class );
    }

    public boolean isMutable()
    {
        return false;
    }

    public SiteKey get( int value )
    {
        return new SiteKey( value );
    }

    public Integer getIntegerValue( SiteKey value )
    {
        return value.toInt();
    }
}
