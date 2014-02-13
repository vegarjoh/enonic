/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.log;

import java.util.Collection;
import java.util.List;

import com.enonic.cms.core.ResultSet;

public interface LogEntryResultSet
    extends ResultSet
{
    LogEntryKey getKey( int index );

    List<LogEntryKey> getKeys();

    LogEntryEntity getLogEntry( int index );

    Collection<LogEntryEntity> getLogEntries();
}
