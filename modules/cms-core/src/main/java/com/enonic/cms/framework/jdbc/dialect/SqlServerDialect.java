/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.framework.jdbc.dialect;

/**
 * This class implements the SQLServer dialect.
 */
public final class SqlServerDialect
    extends Dialect
{
    /**
     * Vendor ids.
     */
    private final static String[] VENDOR_IDS = {"microsoft"};

    /**
     * Construct the dialect.
     */
    public SqlServerDialect()
    {
        super( "sqlserver", VENDOR_IDS );
        setSeparatorValue( ";" );
        setNullableValue( "" );
        setNotNullableValue( "not null" );
        setUpdateRestrictValue( "" );
        setDeleteRestrictValue( "" );
        setUpdateCascadeValue( "on update cascade" );
        setDeleteCascadeValue( "on delete cascade" );
        setIntegerTypeValue( "integer" );
        setFloatTypeValue( "float" );
        setBigintTypeValue( "bigint" );
        setCharTypeValue( "char(?)" );
        setVarcharTypeValue( "nvarchar(?)" );
        setBlobTypeValue( "image" );
        setTimestampTypeValue( "datetime" );

        /*
         * len() (not datalength()) might seam like the obviuos choice, but it doesn't work on
         * colums of type image. ...and that's exactly what we need.
         */
        setLengthFunctionName( "datalength" );
    }

    @Override
    public String translateDropIndex( String tableName, String indexName )
    {
        StringBuilder sql = new StringBuilder("DROP INDEX " ).append( tableName ).append( "." ).append( indexName );
        return sql.toString();
    }
}
