/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.vacuum;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.enonic.cms.framework.blob.gc.GarbageCollector;

import com.enonic.cms.core.security.SecurityService;
import com.enonic.cms.core.security.user.User;
import com.enonic.cms.core.security.userstore.MemberOfResolver;
import com.enonic.cms.store.support.ConnectionFactory;

@Component
public class VacuumServiceImpl
    implements VacuumService
{
    private static final int BATCH_SIZE = 10;

    private static final String VACUUM_READ_LOGS_SQL = "DELETE FROM tLogEntry WHERE len_lTypeKey = 7";

    @Autowired
    protected GarbageCollector garbageCollector;

    @Autowired
    protected ConnectionFactory connectionFactory;

    @Autowired
    protected SecurityService securityService;

    @Autowired
    protected MemberOfResolver memberOfResolver;


    private ProgressInfo progressInfo = new ProgressInfo();

    private final Map<String, List<Integer>> queryCache = new HashMap<String, List<Integer>>();

    /**
     * Clean read logs.
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void cleanReadLogs()
    {
        if ( progressInfo.isInProgress() || !isAdmin() )
        {
            return;
        }

        try
        {
            startProgress( "Cleaning read logs..." );

            final Connection conn = connectionFactory.getConnection( true );

            setProgress( "Vacuum read logs...", 5 );

            vacuumReadLogs( conn );
        }
        catch ( final Exception e )
        {
            setProgress( "Failed to clean read logs: " + e.getMessage(), 100 );
            progressInfo.setInProgress( false );

            throw new RuntimeException( "Failed to clean read logs", e );
        }
        finally
        {
            finishProgress();
        }
    }

    /**
     * Clean unused content.
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class, timeout = 3600)
    public void cleanUnusedContent()
    {
        if ( progressInfo.isInProgress() || !isAdmin() )
        {
            return;
        }

        try
        {
            startProgress( "Cleaning unused content..." );

            final Connection conn = connectionFactory.getConnection( true );

            setProgress( "Vacuum binaries...", 5 );
            vacuumBinaries( conn );

            setProgress( "Vacuum contents...", 20 );
            vacuumContents( conn );

            setProgress( "Vacuum categories...", 40 );
            vacuumCategories( conn );

            setProgress( "Vacuum archives...", 60 );
            vacuumArchives( conn );

            setProgress( "Vacuum blob store...", 80 );
            vacuumBlobStore();

        }
        catch ( final Exception e )
        {
            setProgress( "Failed to clean unused content: " + e.getMessage(), 100 );
            progressInfo.setInProgress( false );

            throw new RuntimeException( "Failed to clean unused content", e );
        }
        finally
        {
            finishProgress();

            queryCache.clear();
        }
    }

    /**
     * returns progress info about either Clean unused content or Clean read logs.
     */
    public ProgressInfo getProgressInfo()
    {
        if ( !isAdmin() )
        {
            return ProgressInfo.NONE;
        }

        return progressInfo;
    }

    private void startProgress( final String logLine )
    {
        setProgress( logLine, 0 );
        progressInfo.setInProgress( true );
    }

    private void setProgress( final String logLine, final int percent )
    {
        progressInfo.setLogLine( logLine );
        progressInfo.setPercent( percent );
    }

    private void finishProgress()
    {
        if ( progressInfo.isInProgress() )
        {
            setProgress( "Finished. Last job was executed at " + new Date().toString(), 100 );
            progressInfo.setInProgress( false );
        }
    }

    private boolean isAdmin()
    {
        final User user = securityService.getLoggedInAdminConsoleUser();
        return memberOfResolver.hasEnterpriseAdminPowers( user.getKey() );
    }


    /**
     * Vacuum binaries.
     */
    private void vacuumBinaries( final Connection conn )
        throws Exception
    {
        executeStatements( conn, VacuumContentSQL.VACUUM_BINARIES_STATEMENTS );
    }

    /**
     * Vacuum contents.
     */
    private void vacuumContents( final Connection conn )
        throws Exception
    {
        executeStatements( conn, VacuumContentSQL.VACUUM_CONTENT_STATEMENTS );
    }

    /**
     * Vacuum categories.
     */
    private void vacuumCategories( final Connection conn )
        throws Exception
    {
        executeStatements( conn, VacuumContentSQL.VACUUM_CATEGORIES_STATEMENTS );
    }

    /**
     * Vacuum arvhives.
     */
    private void vacuumArchives( final Connection conn )
        throws Exception
    {
        executeStatements( conn, VacuumContentSQL.VACUUM_ARCHIVES_STATEMENTS );
    }

    /**
     * Vacuum read logs.
     */
    private void vacuumReadLogs( final Connection conn )
        throws Exception
    {
        executeStatements( conn, new String[]{VACUUM_READ_LOGS_SQL} );
    }

    private void vacuumBlobStore()
    {
        this.garbageCollector.process();
    }

    /**
     * Execute a list of statements.
     */
    private void executeStatements( final Connection conn, final String[] sqlList )
        throws Exception
    {
        for ( final String sql : sqlList )
        {
            executeStatementBatch( conn, sql );
        }
    }

    /**
     * Execute statement.
     */
    private void executeStatement( final Connection conn, final String sql )
        throws Exception
    {
        Statement stmt = null;

        try
        {
            stmt = conn.createStatement();
            stmt.execute( sql );
        }
        finally
        {
            JdbcUtils.closeStatement( stmt );
        }
    }

    /**
     * Execute statement trying do it in batch.
     */
    private void executeStatementBatch( final Connection conn, final String sql )
        throws Exception
    {
        final Pattern pattern = Pattern.compile( "DELETE FROM (\\w+) WHERE (\\w+) IN \\((.*)\\)" );
        final Matcher matcher = pattern.matcher( sql );

        if ( matcher.matches() )
        {
            final String table = matcher.group( 1 );
            final String column = matcher.group( 2 );
            final String select = matcher.group( 3 );

            final List<Integer> ids = readIds( conn, select );
            deleteIdsInTableBatch( conn, ids, column, table );
        }
        else
        {
            // WHERE NOT IN query or some unknown ... -> just execute SQL.
            executeStatement( conn, sql );
        }
    }

    /**
     * gets array of primary keys that are to delete. used query cache.
     */
    private List<Integer> readIds( final Connection conn, final String select )
        throws SQLException
    {
        List<Integer> ids = queryCache.get( select );

        if ( ids == null )
        {
            ids = readIdsFromDB( conn, select );

            queryCache.put( select, ids );
        }

        return ids;
    }

    /**
     * gets array of primary keys that are to delete. read from database
     */
    private List<Integer> readIdsFromDB( final Connection conn, final String select )
        throws SQLException
    {
        final List<Integer> ids = new ArrayList<Integer>();

        Statement stmt = null;
        ResultSet rs = null;

        try
        {
            stmt = conn.createStatement();

            rs = stmt.executeQuery( select );

            while ( rs.next() )
            {
                ids.add( rs.getInt( 1 ) );
            }

        }
        finally
        {
            JdbcUtils.closeResultSet( rs );
            JdbcUtils.closeStatement( stmt );
        }

        return ids;
    }

    /**
     * splits delete to batch
     */
    private void deleteIdsInTableBatch( final Connection conn, final List<Integer> ids, final String column, final String table )
        throws Exception
    {
        int fromIndex, length;

        for ( fromIndex = 0, length = ids.size(); fromIndex < length; fromIndex += BATCH_SIZE )
        {
            final int toIndex = fromIndex + BATCH_SIZE < length ? fromIndex + BATCH_SIZE : length;
            deleteIdsInTable( conn, ids.subList( fromIndex, toIndex ), column, table );
        }

    }

    /**
     * deletes from table by idsds
     */
    private void deleteIdsInTable( final Connection conn, final List<Integer> ids, final String column, final String table )
        throws Exception
    {
        final String join = StringUtils.join( ids, "," );

        final String sql = "DELETE FROM " + table + " WHERE " + column + " IN (" + join + ")";

        executeStatement( conn, sql );
    }
}
