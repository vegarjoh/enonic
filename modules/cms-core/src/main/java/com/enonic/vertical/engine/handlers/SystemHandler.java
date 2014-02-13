/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.vertical.engine.handlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.enonic.esl.sql.model.DatabaseSchemaTool;
import com.enonic.vertical.engine.dbmodel.VerticalDatabase;

import com.enonic.cms.core.jdbc.DatabaseValuesInitializer;
import com.enonic.cms.store.DatabaseAccessor;

/**
 * This class implements the system handler that takes care of creating database schema and populating version numbers.
 */
@Component
public final class SystemHandler
    extends BaseHandler
{
    /**
     * Logger.
     */
    private final static Logger LOG = LoggerFactory.getLogger( SystemHandler.class );

    /**
     * Return true if it has the table.
     */
    private boolean hasTable( Connection conn, String tableName )
    {
        Statement stmt = null;

        try
        {
            stmt = conn.createStatement();
            close( stmt.executeQuery( "SELECT * FROM " + tableName ) );
            return true;
        }
        catch ( SQLException e )
        {
            try
            {
                conn.rollback();
            }
            catch ( Exception e2 )
            {
                // Do nothing
            }

            return false;
        }
        finally
        {
            close( stmt );
        }
    }

    /**
     * Return the model version.
     */
    private int getModelNumber( Connection conn )
        throws Exception
    {
        if ( hasTable( conn, this.db.tModelVersion.getName() ) )
        {
            return selectModelNumber( conn );
        }
        else
        {
            return -1;
        }
    }

    /**
     * Select the model version.
     */
    private int selectModelNumber( Connection conn )
        throws Exception
    {
        PreparedStatement stmt =
            conn.prepareStatement( "SELECT " + this.db.tModelVersion.mve_lVersion.getName() + " FROM " + this.db.tModelVersion.getName() +
                                       " WHERE " + this.db.tModelVersion.mve_sKey.getName() + " = ?" );
        ResultSet result = null;

        try
        {
            stmt.setString( 1, "model" );
            result = stmt.executeQuery();

            if ( result.next() )
            {
                return result.getInt( 1 );
            }
            else
            {
                return 0;
            }
        }
        finally
        {
            close( result );
            close( stmt );
        }
    }

    /**
     * Return version number.
     */
    private String getVerticalVersion( Connection conn )
        throws Exception
    {
        PreparedStatement stmt = conn.prepareStatement( "SELECT vve_sVersionName FROM tVerticalVersion" );
        ResultSet result = null;

        try
        {
            result = stmt.executeQuery();
            if ( result.next() )
            {
                return result.getString( 1 );
            }
            else
            {
                return "";
            }
        }
        finally
        {
            close( result );
            close( stmt );
        }
    }

    /**
     * Set model version.
     */
    private void setModelNumber( int version )
        throws Exception
    {
        Connection conn = getConnection();

        if ( !updateModelNumber( conn, version ) )
        {
            insertModelNumber( conn, version );
        }
    }

    /**
     * Update model version.
     */
    private boolean updateModelNumber( Connection conn, int version )
        throws Exception
    {
        PreparedStatement stmt =
            conn.prepareStatement( "UPDATE " + this.db.tModelVersion.getName() + " SET " + this.db.tModelVersion.mve_lVersion.getName() +
                                       " = ? WHERE " + this.db.tModelVersion.mve_sKey.getName() + " = ?" );

        try
        {
            stmt.setInt( 1, version );
            stmt.setString( 2, "model" );
            return stmt.executeUpdate() > 0;
        }
        finally
        {
            close( stmt );
        }
    }

    /**
     * Insert model version.
     */
    private void insertModelNumber( Connection conn, int version )
        throws Exception
    {
        PreparedStatement stmt = conn.prepareStatement( "INSERT INTO " + this.db.tModelVersion.getName() + " VALUES (?, ?)" );

        try
        {
            stmt.setString( 1, "model" );
            stmt.setInt( 2, version );
            stmt.executeUpdate();
        }
        finally
        {
            close( stmt );
        }
    }

    /**
     * Initialize the schema.
     */
    public boolean initializeDatabaseSchema()
        throws Exception
    {
        Connection conn = getConnection();
        return initializeDatabaseSchema( conn );
    }

    /**
     * Initialize the database.
     */
    public boolean initializeDatabaseValues()
        throws Exception
    {
        Connection conn = getConnection();
        return initializeDatabaseValues( conn );
    }

    /**
     * Creating schema if not created.
     */
    private boolean initializeDatabaseSchema( Connection conn )
        throws Exception
    {
        if ( isSchemaCreated( conn ) )
        {
            return false;
        }
        else
        {
            long tm = System.currentTimeMillis();
            LOG.info( "Database schema does not exist. Creating schema..." );
            List<?> sqlList = DatabaseSchemaTool.generateDatabaseSchema( DatabaseAccessor.getLatestDatabase() );
            initializeDatabaseSchema( conn, sqlList );
            LOG.info( "Database schema created in " + ( System.currentTimeMillis() - tm ) + " ms" );
            return true;
        }
    }

    /**
     * Return true if schema is created.
     */
    private boolean isSchemaCreated( Connection conn )
    {
        return hasTable( conn, this.db.tModelVersion.getName() ) || hasTable( conn, "tVerticalVersion" );
    }

    /**
     * Execute database schema sqls.
     */
    private void initializeDatabaseSchema( Connection conn, List<?> schema )
        throws Exception
    {
        Statement stmt = null;
        String currentSql = null;

        try
        {
            stmt = conn.createStatement();
            for ( Object sql : schema )
            {
                currentSql = sql.toString();
                LOG.debug( "Executing statement: " + currentSql );
                stmt.execute( currentSql );
            }
        }
        catch ( SQLException e )
        {
            if ( currentSql != null )
            {
                LOG.debug( "Failed to execute: " + currentSql );
            }

            throw e;
        }
        finally
        {
            close( stmt );
        }
    }

    /**
     * Initialize the database.
     */
    private boolean initializeDatabaseValues( Connection conn )
        throws Exception
    {
        if ( !isDatabaseValuesInitialized( conn ) )
        {
            LOG.info( "Populating database with initial values..." );

            final int modelNumber = VerticalDatabase.getInstance().getVersion();
            new DatabaseValuesInitializer().initializeDatabaseValues( conn );

            setModelNumber( modelNumber );

            return true;
        }
        else
        {
            return false;
        }
    }


    /**
     * Return true if database values is initialized.
     */
    private boolean isDatabaseValuesInitialized( Connection conn )
        throws Exception
    {
        if ( hasTable( conn, this.db.tModelVersion.getName() ) )
        {
            return getModelNumber( conn ) > 0;
        }
        else if ( hasTable( conn, "tVerticalVersion" ) )
        {
            return getVerticalVersion( conn ) != null;
        }
        else
        {
            return true;
        }
    }
}
