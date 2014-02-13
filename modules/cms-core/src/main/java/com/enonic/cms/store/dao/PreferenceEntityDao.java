/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.store.dao;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.enonic.cms.core.preference.PreferenceEntity;
import com.enonic.cms.core.preference.PreferenceKey;
import com.enonic.cms.core.preference.PreferenceSpecification;

@Repository("preferenceDao")
public final class PreferenceEntityDao
    extends AbstractBaseEntityDao<PreferenceEntity>
    implements PreferenceDao
{

    public PreferenceEntity findByKey( PreferenceKey key )
    {
        return get( PreferenceEntity.class, key );
    }

    public List<PreferenceEntity> findBy( PreferenceSpecification spec )
    {

        final List<PreferenceEntity> list = new ArrayList<PreferenceEntity>();

        List<String> prefixes = spec.getPrefixes();
        for ( String prefix : prefixes )
        {
            String str = translateWildcardExpressionToSqlExpression( prefix );
            list.addAll( findByNamedQuery( PreferenceEntity.class, "PreferenceEntity.findByKeyLike", "key", str ) );
        }

        return list;
    }

    public void removeBy( PreferenceSpecification spec )
    {

        List<String> prefixes = spec.getPrefixes();
        for ( String prefix : prefixes )
        {
            String str = translateWildcardExpressionToSqlExpression( prefix );
            deleteByNamedQuery( "PreferenceEntity.deleteByLike", "key", str );
        }
    }

    private String translateWildcardExpressionToSqlExpression( String s )
    {
        return s.replaceAll( "\\*", "%" );
    }
}
