/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.framework.hibernate.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public abstract class InClauseBuilder<T>
{

    private int maxValuesPrInClause = 500;

    private String columnName;

    private Collection<T> values;

    private int index = 0;

    public InClauseBuilder( String columnName, Collection<T> values )
    {
        this.columnName = columnName;
        this.values = values;
    }

    public void setMaxValuesPrInClause( int value )
    {
        this.maxValuesPrInClause = value;
    }

    public void appendTo( final StringBuffer sql )
    {

        if ( values == null || values.size() == 0 )
        {
            return;
        }

        sql.append( "(" );
        int i = 0;
        int size = values.size();
        boolean firstInClause = true;
        for ( T value : values )
        {

            boolean newInClause = i == 0 || ( i % maxValuesPrInClause ) == 0;
            if ( newInClause )
            {
                if ( !firstInClause )
                {
                    sql.append( ") OR " );
                }
                sql.append( columnName ).append( " IN (" );
                firstInClause = false;
            }

            if ( !newInClause )
            {
                sql.append( "," );
            }
            appendValue( sql, value );

            if ( i == size - 1 )
            {
                sql.append( ")" );
            }

            i++;
            index = i;
        }
        sql.append( ")" );
    }

    public abstract void appendValue( final StringBuffer sql, final T value );

    public int getIndex()
    {
        return index;
    }

    public String toString()
    {
        StringBuffer str = new StringBuffer();
        appendTo( str );
        return str.toString();
    }

    public static void buildAndAppendTemplateInClause( StringBuffer sql, String columnName, int count )
    {

        List<String> values = new ArrayList<String>( count );
        for ( int i = 1; i <= count; i++ )
        {
            values.add( "?" );
        }
        InClauseBuilder inclause = new InClauseBuilder<String>( columnName, values )
        {
            public void appendValue( StringBuffer sql, String value )
            {
                sql.append( "?" );
            }
        };
        inclause.appendTo( sql );
    }
}
