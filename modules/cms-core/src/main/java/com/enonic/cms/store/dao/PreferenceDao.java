/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.store.dao;

import java.util.List;

import com.enonic.cms.core.preference.PreferenceEntity;
import com.enonic.cms.core.preference.PreferenceKey;
import com.enonic.cms.core.preference.PreferenceSpecification;


public interface PreferenceDao
    extends EntityDao<PreferenceEntity>
{
    PreferenceEntity findByKey( PreferenceKey key );

    List<PreferenceEntity> findBy( PreferenceSpecification spec );

    void removeBy( PreferenceSpecification spec );
}
