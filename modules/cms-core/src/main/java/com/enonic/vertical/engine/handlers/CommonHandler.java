/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.vertical.engine.handlers;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import com.enonic.esl.containers.MultiValueMap;
import com.enonic.esl.sql.model.Column;
import com.enonic.esl.sql.model.ForeignKeyColumn;
import com.enonic.esl.sql.model.Table;
import com.enonic.esl.sql.model.datatypes.DataType;
import com.enonic.esl.xml.XMLTool;
import com.enonic.vertical.engine.Types;
import com.enonic.vertical.engine.VerticalEngineLogger;
import com.enonic.vertical.engine.XDG;
import com.enonic.vertical.engine.processors.ElementProcessor;

import com.enonic.cms.framework.util.TIntArrayList;

@Component
public class CommonHandler
    extends BaseHandler
{
    private final static int FETCH_SIZE = 20;

    public int executeSQL( String sql, int paramValue )
    {
        return executeSQL( sql, new Object[]{paramValue} );
    }

    private int executeSQL( String sql, Object[] paramValues )
    {
        PreparedStatement preparedStmt = null;
        int result = 0;

        try
        {
            Connection con = getConnection();
            preparedStmt = con.prepareStatement( sql );

            if ( paramValues != null )
            {
                for ( int i = 0; i < paramValues.length; i++ )
                {
                    final int at = i + 1;
                    final Object value = paramValues[i];
                    if ( value instanceof String )
                    {
                        preparedStmt.setString( at, (String) value );
                    }
                    else
                    {
                        preparedStmt.setObject( at, value );
                    }
                }
            }
            result = preparedStmt.executeUpdate();
        }
        catch ( SQLException sqle )
        {
            String message = "Failed to execute sql: %t";
            VerticalEngineLogger.error( message, sqle );
        }
        finally
        {
            close( preparedStmt );
        }
        return result;
    }

    public int executeSQL( String sql, int[] paramValues )
    {
        Connection con;
        PreparedStatement preparedStmt = null;
        int result = 0;

        try
        {
            con = getConnection();
            preparedStmt = con.prepareStatement( sql );

            if ( paramValues != null )
            {
                for ( int i = 0; i < paramValues.length; i++ )
                {
                    preparedStmt.setInt( i + 1, paramValues[i] );
                }
            }
            result = preparedStmt.executeUpdate();
        }
        catch ( SQLException sqle )
        {
            String message = "Failed to execute sql: %t";
            VerticalEngineLogger.error( message, sqle );
            result = 0;
        }
        finally
        {
            close( preparedStmt );
        }
        return result;
    }

    public int executeSQL( String sql, Integer[] paramValues )
    {
        Connection con;
        PreparedStatement preparedStmt = null;
        int result = 0;

        try
        {
            con = getConnection();
            preparedStmt = con.prepareStatement( sql );

            if ( paramValues != null )
            {
                for ( int i = 0; i < paramValues.length; i++ )
                {
                    if ( paramValues[i] == null )
                    {
                        preparedStmt.setNull( i + 1, java.sql.Types.INTEGER );
                    }
                    else
                    {
                        preparedStmt.setInt( i + 1, paramValues[i] );
                    }
                }
            }
            result = preparedStmt.executeUpdate();
        }
        catch ( SQLException sqle )
        {
            String message = "Failed to execute sql: %t";
            VerticalEngineLogger.error( message, sqle );
        }
        finally
        {
            close( preparedStmt );
        }
        return result;
    }

    public int getInt( String sql, Object paramValue )
    {
        if ( paramValue != null )
        {
            return getInt( sql, new Object[]{paramValue} );
        }
        else
        {
            return getInt( sql, (Object[]) null );
        }
    }

    private Date getTimestamp( String sql, int paramValue )
    {
        Connection con;
        PreparedStatement preparedStmt = null;
        ResultSet resultSet = null;
        Date timestamp = null;

        try
        {
            con = getConnection();
            preparedStmt = con.prepareStatement( sql );
            preparedStmt.setInt( 1, paramValue );
            resultSet = preparedStmt.executeQuery();

            if ( resultSet.next() )
            {
                timestamp = resultSet.getTimestamp( 1 );
            }
        }
        catch ( SQLException sqle )
        {
            String message = "Failed to get date: %t";
            VerticalEngineLogger.error( message, sqle );
        }
        finally
        {
            close( resultSet );
            close( preparedStmt );
        }
        return timestamp;
    }

    public int getInt( String sql, int paramValue )
    {
        return getInt( sql, new int[]{paramValue} );
    }

    public Object[][] getObjectArray( String sql, Object[] paramValues )
    {
        PreparedStatement preparedStmt = null;
        ResultSet resultSet = null;
        Object[][] values = null;

        try
        {
            Connection con = getConnection();

            preparedStmt = con.prepareStatement( sql );

            if ( paramValues != null )
            {
                for ( int i = 0; i < paramValues.length; i++ )
                {
                    if ( paramValues[i] instanceof String )
                    {
                        preparedStmt.setString( i + 1, paramValues[i].toString() );
                    }
                    else
                    {
                        preparedStmt.setObject( i + 1, paramValues[i] );
                    }
                }
            }

            resultSet = preparedStmt.executeQuery();

            ArrayList<Object[]> rows = new ArrayList<Object[]>();

            while ( resultSet.next() )
            {
                int columnCount = resultSet.getMetaData().getColumnCount();
                Object[] columnValues = new Object[columnCount];

                for ( int columnCounter = 0; columnCounter < columnCount; columnCounter++ )
                {
                    columnValues[columnCounter] = resultSet.getObject( columnCounter + 1 );
                    if ( resultSet.wasNull() )
                    {
                        columnValues[columnCounter] = null;
                    }
                }

                rows.add( columnValues );
            }

            values = new Object[rows.size()][];
            for ( int i = 0; i < rows.size(); i++ )
            {
                values[i] = rows.get( i );
            }
        }
        catch ( SQLException sqle )
        {
            String message = "Failed to get object[][]: %t";
            VerticalEngineLogger.error( message, sqle );
        }
        finally
        {
            close( resultSet );
            close( preparedStmt );
        }
        return values;
    }

    public Object[][] getObjectArray( String sql, int[] paramValues )
    {
        PreparedStatement preparedStmt = null;
        ResultSet resultSet = null;
        Object[][] values = null;

        try
        {
            Connection con = getConnection();

            preparedStmt = con.prepareStatement( sql );

            if ( paramValues != null )
            {
                for ( int i = 0; i < paramValues.length; i++ )
                {
                    preparedStmt.setInt( i + 1, paramValues[i] );
                }
            }

            resultSet = preparedStmt.executeQuery();

            ArrayList<Object[]> rows = new ArrayList<Object[]>();

            while ( resultSet.next() )
            {
                int columnCount = resultSet.getMetaData().getColumnCount();
                Object[] columnValues = new Object[columnCount];

                for ( int columnCounter = 0; columnCounter < columnCount; columnCounter++ )
                {
                    columnValues[columnCounter] = resultSet.getObject( columnCounter + 1 );
                    if ( resultSet.wasNull() )
                    {
                        columnValues[columnCounter] = null;
                    }
                }

                rows.add( columnValues );
            }

            values = new Object[rows.size()][];
            for ( int i = 0; i < rows.size(); i++ )
            {
                values[i] = rows.get( i );
            }
        }
        catch ( SQLException sqle )
        {
            String message = "Failed to get object[][]: %t";
            VerticalEngineLogger.error( message, sqle );
        }
        finally
        {
            close( resultSet );
            close( preparedStmt );
        }
        return values;
    }

    public int getInt( String sql, Object[] paramValues )
    {
        PreparedStatement preparedStmt = null;
        ResultSet resultSet = null;
        int value;

        try
        {
            Connection con = getConnection();

            preparedStmt = con.prepareStatement( sql );

            if ( paramValues != null )
            {
                for ( int i = 0; i < paramValues.length; i++ )
                {
                    if ( paramValues[i] instanceof String )
                    {
                        preparedStmt.setString( i + 1, paramValues[i].toString() );
                    }
                    else
                    {
                        preparedStmt.setObject( i + 1, paramValues[i] );
                    }
                }
            }

            resultSet = preparedStmt.executeQuery();

            if ( resultSet.next() )
            {
                value = resultSet.getInt( 1 );
                if ( resultSet.wasNull() )
                {
                    value = -1;
                }
            }
            else
            {
                value = -1;
            }
        }
        catch ( SQLException sqle )
        {
            String message = "Failed to get int: %t";
            VerticalEngineLogger.error( message, sqle );
            value = -1;
        }
        finally
        {
            close( resultSet );
            close( preparedStmt );
        }
        return value;
    }

    public int getInt( String sql, int[] paramValues )
    {
        PreparedStatement preparedStmt = null;
        ResultSet resultSet = null;
        int value;

        try
        {
            Connection con = getConnection();
            preparedStmt = con.prepareStatement( sql );
            if ( paramValues != null )
            {
                for ( int i = 0; i < paramValues.length; i++ )
                {
                    preparedStmt.setInt( i + 1, paramValues[i] );
                }
            }
            resultSet = preparedStmt.executeQuery();

            if ( resultSet.next() )
            {
                value = resultSet.getInt( 1 );
                if ( resultSet.wasNull() )
                {
                    value = -1;
                }
            }
            else
            {
                value = -1;
            }
        }
        catch ( SQLException sqle )
        {
            String message = "Failed to get int: %t";
            VerticalEngineLogger.error( message, sqle );
            value = -1;
        }
        finally
        {
            close( resultSet );
            close( preparedStmt );
        }
        return value;
    }

    public boolean getBoolean( String sql )
    {
        PreparedStatement preparedStmt = null;
        ResultSet resultSet = null;
        boolean value;

        try
        {
            Connection con = getConnection();
            preparedStmt = con.prepareStatement( sql );
            resultSet = preparedStmt.executeQuery();

            if ( resultSet.next() )
            {
                value = resultSet.getInt( 1 ) != 0;
                if ( resultSet.wasNull() )
                {
                    value = false;
                }
            }
            else
            {
                value = false;
            }
        }
        catch ( SQLException sqle )
        {
            String message = "Failed to get int: %t";
            VerticalEngineLogger.error( message, sqle );
            value = false;
        }
        finally
        {
            close( resultSet );
            close( preparedStmt );
        }
        return value;
    }

    private byte[] getByteArray( String sql, Object[] paramValues )
    {
        PreparedStatement preparedStmt = null;
        ResultSet resultSet = null;
        byte[] byteArray = null;

        try
        {
            Connection con = getConnection();
            preparedStmt = con.prepareStatement( sql );
            if ( paramValues != null )
            {
                for ( int i = 0; i < paramValues.length; i++ )
                {
                    preparedStmt.setObject( i + 1, paramValues[i] );
                }
            }
            resultSet = preparedStmt.executeQuery();

            if ( resultSet.next() )
            {
                byteArray = resultSet.getBytes( 1 );
            }
        }
        catch ( SQLException sqle )
        {
            String message = "Failed to get byte array: %t";
            VerticalEngineLogger.error( message, sqle );
        }
        finally
        {
            close( resultSet );
            close( preparedStmt );
        }
        return byteArray;
    }

    public int[] getIntArray( String sql, int paramValue )
    {
        return getIntArray( sql, new int[]{paramValue} );
    }

    public int[] getIntArray( String sql, int[] paramValues )
    {
        PreparedStatement preparedStmt = null;
        ResultSet resultSet = null;
        TIntArrayList keys = new TIntArrayList();

        try
        {
            Connection con = getConnection();

            preparedStmt = con.prepareStatement( sql );

            if ( paramValues != null )
            {
                for ( int i = 0; i < paramValues.length; i++ )
                {
                    preparedStmt.setInt( i + 1, paramValues[i] );
                }
            }

            resultSet = preparedStmt.executeQuery();

            while ( resultSet.next() )
            {
                keys.add( resultSet.getInt( 1 ) );
            }
        }
        catch ( SQLException sqle )
        {
            String message = "Failed to get integer array: %t";
            VerticalEngineLogger.error( message, sqle );
        }
        finally
        {
            close( resultSet );
            close( preparedStmt );
        }
        return keys.toArray();
    }

    public int[] getIntArray( String sql )
    {
        return getIntArray( sql, (Object[]) null );
    }

    public int[] getIntArray( String sql, Object[] paramValues )
    {
        PreparedStatement preparedStmt = null;
        ResultSet resultSet = null;
        TIntArrayList keys = new TIntArrayList();

        try
        {
            Connection con = getConnection();
            preparedStmt = con.prepareStatement( sql );

            if ( paramValues != null )
            {
                for ( int i = 0; i < paramValues.length; i++ )
                {
                    if ( paramValues[i] instanceof Boolean )
                    {
                        preparedStmt.setBoolean( i + 1, (Boolean) paramValues[i] );
                    }
                    else
                    {
                        preparedStmt.setObject( i + 1, paramValues[i] );
                    }
                }
            }

            resultSet = preparedStmt.executeQuery();

            while ( resultSet.next() )
            {
                keys.add( resultSet.getInt( 1 ) );
            }
        }
        catch ( SQLException sqle )
        {
            String message = "Failed to get integer array: %t";
            VerticalEngineLogger.error( message, sqle );
        }
        finally
        {
            close( resultSet );
            close( preparedStmt );
        }
        return keys.toArray();
    }

    public String getString( String sql, Object[] paramValues )
    {
        PreparedStatement preparedStmt = null;
        ResultSet resultSet = null;
        String string;

        try
        {
            Connection con = getConnection();

            preparedStmt = con.prepareStatement( sql );

            if ( paramValues != null )
            {
                for ( int i = 0; i < paramValues.length; i++ )
                {
                    preparedStmt.setObject( i + 1, paramValues[i] );
                }
            }

            resultSet = preparedStmt.executeQuery();
            if ( resultSet.next() )
            {
                ResultSetMetaData metaData = resultSet.getMetaData();
                String columnName = metaData.getColumnName( 1 );
                Table table = db.getTableByColumnName( columnName );
                if ( table != null )
                {
                    Column column = table.getColumn( metaData.getColumnName( 1 ) );
                    DataType dataType = column.getType();
                    string = dataType.getDataAsString( resultSet, 1 );
                }
                else
                {
                    string = resultSet.getString( 1 );
                    if ( resultSet.wasNull() )
                    {
                        string = null;
                    }
                }
            }
            else
            {
                string = null;
            }
        }
        catch ( SQLException sqle )
        {
            String message = "Failed to get string: %t";
            VerticalEngineLogger.error( message, sqle );
            string = null;
        }
        finally
        {
            close( resultSet );
            close( preparedStmt );
        }

        return string;
    }

    public Date getTimestamp( Table table, Column selectColumn, Column whereColumn, int paramValue )
    {
        String sql = XDG.generateSelectSQL( table, selectColumn, false, whereColumn ).toString();
        return getTimestamp( sql, paramValue );
    }

    public String[] getStringArray( String sql, int paramValue )
    {
        return getStringArray( sql, new int[]{paramValue} );
    }

    public String[] getStringArray( String sql, int[] paramValues )
    {
        Connection con;
        PreparedStatement preparedStmt = null;
        ResultSet resultSet = null;
        ArrayList<String> strings = new ArrayList<String>();
        try
        {
            con = getConnection();
            preparedStmt = con.prepareStatement( sql );
            if ( paramValues != null )
            {
                for ( int i = 0; i < paramValues.length; i++ )
                {
                    preparedStmt.setInt( i + 1, paramValues[i] );
                }
            }
            resultSet = preparedStmt.executeQuery();
            while ( resultSet.next() )
            {
                strings.add( resultSet.getString( 1 ) );
            }
        }
        catch ( SQLException sqle )
        {
            String message = "Failed to get string: %t";
            VerticalEngineLogger.error( message, sqle );
        }
        finally
        {
            close( resultSet );
            close( preparedStmt );
        }

        return strings.toArray( new String[strings.size()] );
    }

    public String getString( String sql, int paramValue )
    {
        return getString( sql, new Object[]{paramValue} );
    }

    public Object[] getObjects( String sql, int paramValue )
    {
        return getObjects( sql, new Integer( paramValue ) );
    }

    private Object[] getObjects( String sql, Object paramValue )
    {
        if ( paramValue == null )
        {
            return getObjects( sql, null );
        }
        else
        {
            return getObjects( sql, new Object[]{paramValue} );
        }
    }

    public String[] getStrings( String sql, Object[] paramValues )
    {
        Connection con;
        PreparedStatement preparedStmt = null;
        ResultSet resultSet = null;
        String[] strings;

        try
        {
            con = getConnection();
            preparedStmt = con.prepareStatement( sql );
            if ( paramValues != null )
            {
                for ( int i = 0; i < paramValues.length; i++ )
                {
                    preparedStmt.setObject( i + 1, paramValues[i] );
                }
            }
            resultSet = preparedStmt.executeQuery();
            if ( resultSet.next() )
            {
                int columnCount = resultSet.getMetaData().getColumnCount();
                strings = new String[columnCount];
                for ( int i = 1; i <= columnCount; i++ )
                {
                    strings[i - 1] = resultSet.getString( i );
                }
            }
            else
            {
                strings = new String[0];
            }
        }
        catch ( SQLException sqle )
        {
            String message = "Failed to get string: %t";
            VerticalEngineLogger.error( message, sqle );
            strings = new String[0];
        }
        finally
        {
            close( resultSet );
            close( preparedStmt );
        }

        return strings;
    }

    private Object[] getObjects( String sql, Object[] paramValues )
    {
        Connection con;
        PreparedStatement preparedStmt = null;
        ResultSet resultSet = null;
        Object[] objects;

        try
        {
            con = getConnection();
            preparedStmt = con.prepareStatement( sql );
            if ( paramValues != null )
            {
                for ( int i = 0; i < paramValues.length; i++ )
                {
                    preparedStmt.setObject( i + 1, paramValues[i] );
                }
            }
            resultSet = preparedStmt.executeQuery();
            if ( resultSet.next() )
            {
                int columnCount = resultSet.getMetaData().getColumnCount();
                objects = new Object[columnCount];
                for ( int i = 1; i <= columnCount; i++ )
                {
                    objects[i - 1] = resultSet.getObject( i );
                }
            }
            else
            {
                objects = new Object[0];
            }
        }
        catch ( SQLException sqle )
        {
            String message = "Failed to get string: %t";
            VerticalEngineLogger.error( message, sqle );
            objects = new Object[0];
        }
        finally
        {
            close( resultSet );
            close( preparedStmt );
        }

        return objects;
    }

    public boolean hasRows( String sql )
    {
        return hasRows( sql, null );
    }

    public boolean hasRows( String sql, int[] paramValues )
    {
        Connection con;
        PreparedStatement preparedStmt = null;
        ResultSet resultSet = null;
        boolean hasRows = false;

        try
        {
            con = getConnection();
            preparedStmt = con.prepareStatement( sql );

            if ( paramValues != null )
            {
                for ( int i = 0; i < paramValues.length; i++ )
                {
                    preparedStmt.setInt( i + 1, paramValues[i] );
                }
            }

            resultSet = preparedStmt.executeQuery();

            if ( resultSet.next() )
            {
                hasRows = true;
            }
        }
        catch ( SQLException sqle )
        {
            String message = "Failed to execute sql: %t";
            VerticalEngineLogger.error( message, sqle );
        }
        finally
        {
            close( resultSet );
            close( preparedStmt );
        }
        return hasRows;
    }

    public Document getSingleData( int type, int key )
    {
        Connection con;
        PreparedStatement preparedStmt = null;
        ResultSet resultSet = null;
        Document doc = null;
        Table table = Types.getTable( type );

        try
        {
            con = getConnection();

            StringBuffer sql = XDG.generateSelectWherePrimaryKeySQL( table );
            preparedStmt = con.prepareStatement( sql.toString() );
            preparedStmt.setInt( 1, key );
            resultSet = preparedStmt.executeQuery();

            doc = XDG.resultSetToXML( table, resultSet, null, null, null, -1 );
        }
        catch ( SQLException sqle )
        {
            String message = "SQL error: %t";
            VerticalEngineLogger.error( message, sqle );
        }
        finally
        {
            close( resultSet );
            close( preparedStmt );
        }

        return doc;
    }

    public Document getData( int type, int[] keys )
    {
        Table table = Types.getTable( type );

        Column pkColumn = table.getPrimaryKeys()[0];

        MultiValueMap parameters = new MultiValueMap();
        if ( keys != null && keys.length > 0 )
        {
            for ( int key : keys )
            {
                parameters.put( pkColumn.getXPath(), new Integer( key ) );
            }
        }
        else
        {
            parameters.put( pkColumn.getXPath(), new Integer( -1 ) );
        }

        return getData( type, parameters );
    }

    private Document getData( int type, MultiValueMap parameters )
    {
        return getData( type, null, parameters, null, -1, -1, null, false );
    }

    private ResultSet getResultSet( PreparedStatement preparedStmt, List<DataType> dataTypes, List<String> paramValues, int fromIndex )
        throws SQLException
    {

        ResultSet resultSet;

        // Set parameter values
        for ( int i = 0; i < paramValues.size(); i++ )
        {
            if ( dataTypes != null )
            {
                DataType dataType = dataTypes.get( i );
                dataType.setData( preparedStmt, i + 1, paramValues.get( i ) );
            }
            else
            {
                preparedStmt.setObject( i + 1, paramValues.get( i ) );
            }
        }
        resultSet = preparedStmt.executeQuery();

        if ( fromIndex != -1 )
        {
            int resultSetPosition = 0;
            boolean moreResults = true;

            // do manual skip
            // NOTE! resultSet.relative does not work correctly on PostgreSQL database
            while ( resultSetPosition < fromIndex && moreResults )
            {
                moreResults = resultSet.next();
                resultSetPosition++;
            }
        }

        return resultSet;
    }

    private PreparedStatement getPreparedStatement( List<DataType> dataTypes, List<String> paramValues, Connection con, int type,
                                                    Column[] selectColumns, MultiValueMap parameters, String orderBy, boolean descending )
        throws SQLException
    {
        PreparedStatement preparedStmt;
        Table table = Types.getTable( type );
        StringBuffer sql;
        if ( selectColumns == null )
        {
            sql = XDG.generateSelectSQL( table );
        }
        else
        {
            sql = XDG.generateSelectSQL( table, selectColumns, false, null );
        }

        if ( parameters != null && parameters.size() > 0 )
        {
            sql.append( " WHERE " );

            Iterator iter = parameters.keySet().iterator();
            for ( int paramCount = 0; iter.hasNext(); paramCount++ )
            {
                String xpath = iter.next().toString();

                if ( paramCount > 0 )
                {
                    sql.append( " AND " );
                }

                Column column = table.getColumnByXPath( xpath );
                List values = parameters.getValueList( xpath );

                if ( values.size() == 0 )
                {
                    sql.append( column ).append( " IS NULL" );
                }
                else if ( values.size() == 1 )
                {
                    if ( XDG.OPERATOR_LIKE.equals( parameters.getAttribute( xpath ) ) )
                    {
                        sql.append( column ).append( XDG.OPERATOR_LIKE + " ?" );
                    }
                    else if ( XDG.OPERATOR_LESS.equals( parameters.getAttribute( xpath ) ) )
                    {
                        sql.append( column ).append( XDG.OPERATOR_LESS + " ?" );
                    }
                    else if ( XDG.OPERATOR_LESS_OR_EQUAL.equals( parameters.getAttribute( xpath ) ) )
                    {
                        sql.append( column ).append( XDG.OPERATOR_LESS_OR_EQUAL + " ?" );
                    }
                    else if ( XDG.OPERATOR_GREATER.equals( parameters.getAttribute( xpath ) ) )
                    {
                        sql.append( column ).append( XDG.OPERATOR_GREATER + " ?" );
                    }
                    else if ( XDG.OPERATOR_GREATER_OR_EQUAL.equals( parameters.getAttribute( xpath ) ) )
                    {
                        sql.append( column ).append( XDG.OPERATOR_GREATER_OR_EQUAL + " ?" );
                    }
                    else
                    {
                        sql.append( column ).append( " = ?" );
                    }
                    paramValues.add( column.getColumnValue( values.get( 0 ) ) );
                    dataTypes.add( column.getType() );
                }
                else if ( values.size() == 2 && XDG.OPERATOR_RANGE.equals( parameters.getAttribute( xpath ) ) )
                {
                    sql.append( column );
                    sql.append( XDG.OPERATOR_GREATER_OR_EQUAL );
                    sql.append( " ? AND " );
                    sql.append( column );
                    sql.append( XDG.OPERATOR_LESS );
                    sql.append( " ?" );
                    paramValues.add( column.getColumnValue( values.get( 0 ) ) );
                    dataTypes.add( column.getType() );
                    paramValues.add( column.getColumnValue( values.get( 1 ) ) );
                    dataTypes.add( column.getType() );
                }
                else
                {
                    sql.append( column ).append( " IN (" );
                    for ( int i = 0; i < values.size(); i++ )
                    {
                        sql.append( "?" );
                        if ( i < values.size() - 1 )
                        {
                            sql.append( ", " );
                        }
                        paramValues.add( column.getColumnValue( values.get( i ) ) );
                        dataTypes.add( column.getType() );
                    }
                    sql.append( ")" );
                }
            }
        }

        if ( orderBy != null )
        {
            sql.append( " ORDER BY " ).append( table.getColumnByXPath( orderBy ) );
            if ( descending )
            {
                sql.append( " DESC" );
            }
        }

        preparedStmt = con.prepareStatement( sql.toString() );
        preparedStmt.setFetchSize( FETCH_SIZE );

        return preparedStmt;
    }

    public Document getData( int type, Column[] selectColumns, MultiValueMap parameters, ElementProcessor[] elementProcessors,
                             int fromIndex, int count, String orderBy, boolean descending )
    {

        Connection con;
        PreparedStatement preparedStmt = null;
        ResultSet resultSet = null;
        Document doc = null;

        Table table = Types.getTable( type );

        try
        {
            con = getConnection();
            List<DataType> dataTypes = new ArrayList<DataType>();
            List<String> paramValues = new ArrayList<String>();
            preparedStmt = getPreparedStatement( dataTypes, paramValues, con, type, selectColumns, parameters, orderBy, descending );

            resultSet = getResultSet( preparedStmt, dataTypes, paramValues, fromIndex );
            doc = XDG.resultSetToXML( table, resultSet, null, elementProcessors, null, count );
            count = XMLTool.getElements( doc.getDocumentElement() ).length;

            int totalCount = getDataCount( type, parameters );
            doc.getDocumentElement().setAttribute( "totalcount", String.valueOf( totalCount ) );
            doc.getDocumentElement().setAttribute( "count", String.valueOf( count ) );
        }
        catch ( SQLException sqle )
        {
            String message = "SQL error: %t";
            VerticalEngineLogger.error( message, sqle );
        }
        finally
        {
            close( resultSet );
            close( preparedStmt );
        }

        return doc;
    }


    private int getDataCount( int type, MultiValueMap parameters )
    {

        Connection con;
        PreparedStatement preparedStmt = null;
        ResultSet resultSet = null;
        Table table = Types.getTable( type );
        int result;

        try
        {
            con = getConnection();

            StringBuffer sql = XDG.generateCountSQL( table );
            List<DataType> dataTypes = new ArrayList<DataType>();
            List<String> paramValues = new ArrayList<String>();

            if ( parameters != null && parameters.size() > 0 )
            {
                sql.append( " WHERE " );
                int paramCount = 0;
                for ( Object o : parameters.keySet() )
                {
                    String xpath = o.toString();
                    if ( paramCount > 0 )
                    {
                        sql.append( " AND " );
                    }

                    Column column = table.getColumnByXPath( xpath );
                    List values = parameters.getValueList( xpath );
                    if ( values.size() == 0 )
                    {
                        sql.append( column ).append( " IS NULL" );
                    }
                    else if ( values.size() == 1 )
                    {
                        if ( XDG.OPERATOR_LIKE.equals( parameters.getAttribute( xpath ) ) )
                        {
                            sql.append( column ).append( XDG.OPERATOR_LIKE + " ?" );
                        }
                        else if ( XDG.OPERATOR_LESS.equals( parameters.getAttribute( xpath ) ) )
                        {
                            sql.append( column ).append( XDG.OPERATOR_LESS + " ?" );
                        }
                        else if ( XDG.OPERATOR_LESS_OR_EQUAL.equals( parameters.getAttribute( xpath ) ) )
                        {
                            sql.append( column ).append( XDG.OPERATOR_LESS_OR_EQUAL + " ?" );
                        }
                        else if ( XDG.OPERATOR_GREATER.equals( parameters.getAttribute( xpath ) ) )
                        {
                            sql.append( column ).append( XDG.OPERATOR_GREATER + " ?" );
                        }
                        else if ( XDG.OPERATOR_GREATER_OR_EQUAL.equals( parameters.getAttribute( xpath ) ) )
                        {
                            sql.append( column ).append( XDG.OPERATOR_GREATER_OR_EQUAL + " ?" );
                        }
                        else
                        {
                            sql.append( column ).append( " = ?" );
                        }
                        paramValues.add( column.getColumnValue( values.get( 0 ) ) );
                        dataTypes.add( column.getType() );
                    }
                    else if ( values.size() == 2 && XDG.OPERATOR_RANGE.equals( parameters.getAttribute( xpath ) ) )
                    {
                        sql.append( column );
                        sql.append( XDG.OPERATOR_GREATER_OR_EQUAL );
                        sql.append( " ? AND " );
                        sql.append( column );
                        sql.append( XDG.OPERATOR_LESS );
                        sql.append( " ?" );
                        paramValues.add( column.getColumnValue( values.get( 0 ) ) );
                        dataTypes.add( column.getType() );
                        paramValues.add( column.getColumnValue( values.get( 1 ) ) );
                        dataTypes.add( column.getType() );
                    }
                    else if ( values.size() > 1 )
                    {
                        sql.append( column ).append( " IN (" );
                        for ( int i = 0; i < values.size(); i++ )
                        {
                            sql.append( "?" );
                            if ( i < values.size() - 1 )
                            {
                                sql.append( ", " );
                            }
                            paramValues.add( column.getColumnValue( values.get( i ) ) );
                            dataTypes.add( column.getType() );
                        }
                        sql.append( ")" );
                    }
                    paramCount++;
                }
            }

            preparedStmt = con.prepareStatement( sql.toString() );

            // Set parameter values
            for ( int i = 0; i < paramValues.size(); i++ )
            {
                DataType dataType = dataTypes.get( i );
                dataType.setData( preparedStmt, i + 1, paramValues.get( i ) );
            }

            resultSet = preparedStmt.executeQuery();
            if ( resultSet.next() )
            {
                result = resultSet.getInt( 1 );
            }
            else
            {
                String message = "Failed to count data.";
                VerticalEngineLogger.error( message );
                result = 0;
            }
        }
        catch ( SQLException sqle )
        {
            String message = "Failed to count data: %t";
            VerticalEngineLogger.error( message, sqle );
            result = 0;
        }
        finally
        {
            close( resultSet );
            close( preparedStmt );
        }

        return result;
    }

    public StringBuffer getPathString( Table table, Column keyColumn, Column parentKeyColumn, Column nameColumn, int key )
    {

        Column[] selectColumns = new Column[]{parentKeyColumn, nameColumn};
        Column[] whereColumns = new Column[]{keyColumn};
        StringBuffer sql = XDG.generateSelectSQL( table, selectColumns, false, whereColumns );

        Connection con = null;
        PreparedStatement preparedStmt = null;
        ResultSet resultSet = null;
        StringBuffer result = new StringBuffer();

        try
        {
            con = getConnection();
            preparedStmt = con.prepareStatement( sql.toString() );

            while ( key >= 0 )
            {
                preparedStmt.setInt( 1, key );
                resultSet = preparedStmt.executeQuery();
                if ( resultSet.next() )
                {
                    String name;
                    key = resultSet.getInt( 1 );
                    if ( resultSet.wasNull() )
                    {
                        key = -1;
                        name = resultSet.getString( 2 );
                    }
                    else
                    {
                        name = resultSet.getString( 2 );
                    }

                    if ( result.length() > 0 )
                    {
                        result.insert( 0, " / " );
                        result.insert( 0, name );
                    }
                    else
                    {
                        result.append( name );
                    }
                }
                else
                {
                    key = -1;
                }
                close( resultSet );
                resultSet = null;
            }
        }
        catch ( SQLException sqle )
        {
            String message = "Failed to get path string: %t";
            VerticalEngineLogger.error( message, sqle );
            result.setLength( 0 );
        }
        finally
        {
            close( resultSet );
            close( preparedStmt );
        }

        return result;
    }

    private Document getDocument( StringBuffer sql, int paramValue )
    {
        byte[] bytes = getByteArray( sql.toString(), new Object[]{paramValue} );
        if ( bytes != null )
        {
            return XMLTool.domparse( new ByteArrayInputStream( bytes ) );
        }
        else
        {
            return null;
        }
    }

    public Document getDocument( Table table, int key )
    {
        StringBuffer sql = XDG.generateSelectSQL( table, table.getXMLColumn(), false, table.getPrimaryKeys()[0] );
        return getDocument( sql, key );
    }

    public Document getData( Table table, String sql, int paramValue, ElementProcessor[] elementProcessors )
    {
        return getData( table, sql, new int[]{paramValue}, elementProcessors );
    }

    public Document getData( Table table, String sql, int[] paramValues, ElementProcessor[] elementProcessors )
    {
        Document doc = null;
        Connection con = null;
        PreparedStatement preparedStmt = null;
        ResultSet resultSet = null;

        try
        {
            con = getConnection();
            preparedStmt = con.prepareStatement( sql );

            if ( paramValues != null )
            {
                for ( int i = 0; i < paramValues.length; i++ )
                {
                    preparedStmt.setInt( i + 1, paramValues[i] );
                }
            }
            resultSet = preparedStmt.executeQuery();
            doc = XDG.resultSetToXML( table, resultSet, null, elementProcessors, null, -1 );
        }
        catch ( SQLException sqle )
        {
            String message = "Failed to get data: %t";
            VerticalEngineLogger.error( message, sqle );
        }
        finally
        {
            close( resultSet );
            close( preparedStmt );
        }
        return doc;
    }

    public void cascadeDelete( Table table, int key )
    {
        ForeignKeyColumn[] deleteForeignKeys = table.getReferencedKeys( true );
        if ( deleteForeignKeys != null && deleteForeignKeys.length > 0 )
        {
            for ( ForeignKeyColumn deleteForeignKey : deleteForeignKeys )
            {
                Table referrerTable = deleteForeignKey.getTable();
                StringBuffer sql = XDG.generateRemoveSQL( referrerTable, deleteForeignKey );
                executeSQL( sql.toString(), key );
            }
        }
    }


}
