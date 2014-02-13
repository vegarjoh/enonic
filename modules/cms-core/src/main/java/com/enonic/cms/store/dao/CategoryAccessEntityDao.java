/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.store.dao;

import org.springframework.stereotype.Repository;

import com.enonic.cms.core.content.category.CategoryAccessEntity;
import com.enonic.cms.core.security.group.GroupKey;

@Repository("categoryAccessDao")
public final class CategoryAccessEntityDao
    extends AbstractBaseEntityDao<CategoryAccessEntity>
    implements CategoryAccessDao
{
    public void deleteByGroupKey( GroupKey groupKey )
    {
        deleteByNamedQuery( "CategoryAccessEntity.deleteByGroupKey", "groupKey", groupKey );
    }
}
