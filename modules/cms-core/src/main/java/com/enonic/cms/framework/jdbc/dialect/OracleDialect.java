/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.framework.jdbc.dialect;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * This class implements the Oracle dialect.
 */
public final class OracleDialect
    extends Dialect
{
    /**
     * Vendor ids.
     */
    private final static String[] VENDOR_IDS = {"oracle"};

    /**
     * Construct the dialect.
     */
    public OracleDialect()
    {
        super( "oracle", VENDOR_IDS );
        setSeparatorValue( ";" );
        setNullableValue( "" );
        setNotNullableValue( "not null" );
        setUpdateRestrictValue( "" );
        setDeleteRestrictValue( "" );
        setUpdateCascadeValue( "on update cascade" );
        setDeleteCascadeValue( "on delete cascade" );
        setIntegerTypeValue( "integer" );
        setFloatTypeValue( "float" );
        setBigintTypeValue( "decimal(28,0)" );
        setCharTypeValue( "char(?)" );
        setVarcharTypeValue( "varchar(?)" );
        setBlobTypeValue( "blob" );
        setTimestampTypeValue( "date" );
        setSubstringFunctionName( "substr" );
    }

    public Object getObject( ResultSet result, int columnIndex )
        throws SQLException
    {

        ResultSetMetaData metaData = result.getMetaData();
        if ( "java.sql.Timestamp".equals( metaData.getColumnClassName( columnIndex ) ) )
        {
            return getTimestamp( result, columnIndex );
        }
        else if ( "java.sql.Date".equals( metaData.getColumnClassName( columnIndex ) ) )
        {
            return getDate( result, columnIndex );
        }
        else if ( "java.sql.Time".equals( metaData.getColumnClassName( columnIndex ) ) )
        {
            return getTime( result, columnIndex );
        }

        return super.getObject( result, columnIndex );
    }

    public String formatTimestamp( long time )
    {
        return "timestamp" + super.formatTimestamp( time );
    }
}
