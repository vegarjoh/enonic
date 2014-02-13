/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.log;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.servlet.ServletRequestAccessor;
import com.enonic.cms.core.time.TimeService;
import com.enonic.cms.store.dao.LogEntryDao;
import com.enonic.cms.store.dao.UserDao;

@Service("logService")
public class LogServiceImpl
    implements LogService
{
    @Autowired
    private LogEntryDao logEntryDao;

    @Autowired
    private TimeService timeService;

    @Autowired
    private UserDao userDao;

    private static final int PATH_FIELD_MAX_LENGTH = 256;

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public LogEntryResultSet getLogEntries( LogEntrySpecification spec, String orderBy, final int count, final int index )
    {
        if ( spec == null )
        {
            throw new IllegalArgumentException( "Given LogEntrySpecification cannot be null" );
        }

        List<LogEntryKey> keys = logEntryDao.findBySpecification( spec, orderBy );

        final int queryResultTotalSize = keys.size();

        if ( index > queryResultTotalSize )
        {
            return (LogEntryResultSet) new LogEntryResultSetNonLazy( index ).addError(
                "Index greater than result count: " + index + " greater than " + queryResultTotalSize );
        }

        int toIndex = Math.min( queryResultTotalSize, count + index );

        LogEntryResultSet resultSet =
            new LogEntryResultSetLazyFetcher( new LogEntryEntityFetcherImpl( logEntryDao ), keys.subList( index, toIndex ), index,
                                              queryResultTotalSize );
        return resultSet;

    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public LogEntryKey storeNew( StoreNewLogEntryCommand command )
    {
        Assert.notNull( command.getType(), "type cannot be nul" );
        Assert.notNull( command.getUser(), "user cannot be nul" );
        Assert.notNull( command.getTitle(), "title cannot be nul" );

        HttpServletRequest httpRequest = ServletRequestAccessor.getRequest();
        String clientInetAddress = null;
        if ( httpRequest != null )
        {
            clientInetAddress = httpRequest.getRemoteAddr();
        }

        UserEntity user = userDao.findByKey( command.getUser() );

        LogEntryEntity logEntry = new LogEntryEntity();
        logEntry.setType( command.getType().asInteger() );
        logEntry.setTimestamp( timeService.getNowAsDateTime().toDate() );
        logEntry.setInetAddress( clientInetAddress );
        logEntry.setUser( user );

        if ( command.getTable() != null )
        {
            logEntry.setTableKey( command.getTable().asInteger() );
            logEntry.setKeyValue( command.getTableKeyValue() );
        }

        if ( command.getSite() != null )
        {
            logEntry.setSite( command.getSite() );
        }

        logEntry.setTitle( command.getTitle() );

        if ( command.getPath() != null )
        {
            logEntry.setPath( enshurePathWithinBoundary( command.getPath() ) );
        }

        if ( command.getXmlData() != null )
        {
            logEntry.setXmlData( command.getXmlData() );
        }

        else

        {
            logEntry.setXmlData( createEmptyXmlData() );
        }

        logEntryDao.storeNew( logEntry );
        return logEntry.getKey();
    }

    private String enshurePathWithinBoundary( String suggestedPath )
    {
        String path = suggestedPath;

        if ( StringUtils.isNotEmpty( path ) && path.length() > PATH_FIELD_MAX_LENGTH )
        {
            String pathTooLongEnding = " ...";

            path = path.substring( 0, PATH_FIELD_MAX_LENGTH - pathTooLongEnding.length() );
            path = path + pathTooLongEnding;
        }
        return path;
    }

    private Document createEmptyXmlData()
    {

        Element root = new Element( "data" );
        return new Document( root );
    }


}
