/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.esl.sql.model.datatypes;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;

import org.w3c.dom.Node;

import com.enonic.esl.util.DateUtil;
import com.enonic.esl.xml.XMLTool;

public class TimestampType
    extends DataType
{
    private static final TimestampType type = new TimestampType();

    public int getSQLType()
    {
        return Types.TIMESTAMP;
    }

    public Object getData( ResultSet resultSet, int columnIndex )
        throws SQLException
    {
        Timestamp timestamp = resultSet.getTimestamp( columnIndex );
        if ( resultSet.wasNull() )
        {
            return null;
        }
        else
        {
            return new Date( timestamp.getTime() );
        }
    }

    public String getDataAsString( ResultSet resultSet, int columnIndex )
        throws SQLException
    {
        Timestamp timestamp = resultSet.getTimestamp( columnIndex );
        if ( resultSet.wasNull() )
        {
            return null;
        }
        else
        {
            return DateUtil.formatISODateTime( timestamp );
        }
    }

    public Object getDataForXML( Object obj )
    {
        if ( obj == null )
        {
            return null;
        }
        else
        {
            return DateUtil.formatISODateTime( (Date) obj );
        }
    }

    public String getDataString( Object obj )
    {
        if ( obj == null )
        {
            return "null";
        }
        else
        {
            return DateUtil.formatISODateTime( (Date) obj );
        }
    }

    public Object getDataFromXML( Node node )
    {
        String text = XMLTool.getNodeText( node );

        if ( text == null || text.length() == 0 )
        {
            return null;
        }

        Date date = null;
        try
        {
            date = DateUtil.parseISODateTime( text );
        }
        catch ( final Exception pe )
        {
            pe.printStackTrace();
        }

        return date;
    }

    public void setData( PreparedStatement preparedStmt, int columnIndex, Object obj )
        throws SQLException
    {
        Date date;
        if ( obj instanceof String )
        {
            try
            {
                date = DateUtil.parseISODateTime( (String) obj );
            }
            catch ( Exception pe )
            {
                throw new IllegalArgumentException( "Invalid date: " + obj );
            }
        }
        else
        {
            date = (Date) obj;
        }
        preparedStmt.setTimestamp( columnIndex, new Timestamp( date.getTime() ) );
    }

    /**
     * @see com.enonic.esl.sql.model.datatypes.DataType#getSQLValue(java.lang.Object)
     */
    public String getSQLValue( Object xpathValue )
    {
        if ( xpathValue instanceof Date )
        {
            return DateUtil.formatISODateTime( (Date) xpathValue );
        }
        else
        {
            return xpathValue.toString();
        }
    }

    public String getTypeString()
    {
        return "TIMESTAMP";
    }

    public static DataType getInstance()
    {
        return type;
    }

}