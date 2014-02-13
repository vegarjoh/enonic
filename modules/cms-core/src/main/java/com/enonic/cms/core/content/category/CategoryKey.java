/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.content.category;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.enonic.cms.core.AbstractIntegerBasedKey;
import com.enonic.cms.core.InvalidKeyException;

public class CategoryKey
    extends AbstractIntegerBasedKey
{

    public CategoryKey( String key )
    {
        init( key );
    }

    public CategoryKey( int key )
    {
        init( key );
    }

    public CategoryKey( Integer key )
    {
        init( key );
    }

    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        CategoryKey key = (CategoryKey) o;

        return intValue == key.intValue;
    }

    public int hashCode()
    {
        return new HashCodeBuilder( 483, 547 ).append( intValue ).toHashCode();
    }

    public static List<CategoryKey> convertToList( int[] array )
        throws InvalidKeyException
    {

        if ( array == null || array.length == 0 )
        {
            return null;
        }

        List<CategoryKey> list = new ArrayList<CategoryKey>( array.length );
        for ( int value : array )
        {
            list.add( new CategoryKey( value ) );

        }
        return list;
    }

    public static List<CategoryKey> convertToList( CategoryKey categoryKey )
        throws InvalidKeyException
    {
        List<CategoryKey> list = new ArrayList<CategoryKey>();
        if ( categoryKey != null )
        {
            list.add( categoryKey );
        }
        return list;
    }

    public static CategoryKey parse( int intKey )
    {
        if ( intKey > -1 )
        {
            return new CategoryKey( intKey );
        }
        return null;
    }
}