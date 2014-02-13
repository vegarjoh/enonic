/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.store.dao;

import java.util.List;

import com.enonic.cms.core.log.LogEntryEntity;
import com.enonic.cms.core.log.LogEntryKey;
import com.enonic.cms.core.log.LogEntrySpecification;


public interface LogEntryDao
    extends EntityDao<LogEntryEntity>
{

    List<LogEntryKey> findBySpecification( LogEntrySpecification specification, String orderBy );

    LogEntryEntity findByKey( LogEntryKey key );


}
