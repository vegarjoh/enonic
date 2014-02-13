/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.store.dao;

import org.springframework.stereotype.Repository;

import com.enonic.cms.core.security.group.GroupKey;
import com.enonic.cms.core.structure.DefaultSiteAccessEntity;

@Repository("defaultSiteAccessDao")
public final class DefaultSiteAccessEntityDao
    extends AbstractBaseEntityDao<DefaultSiteAccessEntity>
    implements DefaultSiteAccessDao
{
    public void deleteByGroupKey( GroupKey groupKey )
    {
        deleteByNamedQuery( "DefaultSiteAccessEntity.deleteByGroupKey", "groupKey", groupKey );
    }
}
