/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.esl.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.*;

public final class ArrayUtil
{

    /**
     * Private constructor.
     */
    private ArrayUtil()
    {
    }

    /**
     * <p>Differentiate two integer sequences. Will return a 2-dimensional array containing the values to be removed (index=0) into and
     * inserted (index=1) from the first sequence (a[]).</p>
     * <p/>
     * <strong>NOTE! The arrays will be altered during execution.</strong>
     *
     * @param a int[] first sequence
     * @param b int[] second sequence
     * @return int[][] the 2-dimensional array containing the diff
     */
    public static int[][] diff( int[] a, int[] b )
    {

        if ( a == null || b == null )
        {
            return new int[2][0];
        }
        else if ( a.length == 0 || b.length == 0 )
        {
            return new int[][]{a, b};
        }

        int idx_a = 0, idx_b = 0, i = 0, j = 0;
        while ( i < a.length && j < b.length )
        {
            if ( a[i] > b[j] )
            {
                b[idx_b++] = b[j++];
            }
            else if ( a[i] < b[j] )
            {
                a[idx_a++] = a[i++];
            }
            else
            {
                i++;
                j++;
            }
        }

        int[][] temp = new int[2][];
        if ( i < a.length )
        {
            temp[0] = new int[a.length - i + idx_a];
            System.arraycopy( a, 0, temp[0], 0, idx_a );
            System.arraycopy( a, i, temp[0], idx_a, a.length - i );

            temp[1] = new int[idx_b];
            System.arraycopy( b, 0, temp[1], 0, idx_b );
        }
        else if ( j < b.length )
        {
            temp[0] = new int[idx_a];
            System.arraycopy( a, 0, temp[0], 0, idx_a );

            temp[1] = new int[b.length - j + idx_b];
            System.arraycopy( b, 0, temp[1], 0, idx_b );
            System.arraycopy( b, j, temp[1], idx_b, b.length - j );
        }
        else
        {
            temp[0] = new int[idx_a];
            System.arraycopy( a, 0, temp[0], 0, idx_a );

            temp[1] = new int[idx_b];
            System.arraycopy( b, 0, temp[1], 0, idx_b );
        }

        return temp;
    }

    /**
     * <p>Remove duplicates from an integer sequence.</p>
     *
     * @param a int[] the sequence
     * @return int[] the new sequence without duplicates
     */
    public static int[] removeDuplicates( int[] a )
    {

        if ( a == null )
        {
            return null;
        }
        else if ( a.length <= 1 )
        {
            return a;
        }

        int[] newA = new int[a.length];
        int newSize = 1;
        int lastInt = newA[0] = a[0];

        for ( int i = 1; i < a.length; i++ )
        {
            if ( lastInt != a[i] )
            {
                newA[newSize++] = a[i];
                lastInt = a[i];
            }
        }

        if ( newSize < newA.length )
        {
            int[] temp = new int[newSize];
            System.arraycopy( newA, 0, temp, 0, newSize );
            newA = temp;
        }

        return newA;
    }

    public static boolean arrayContains( Object obj, Object[] array )
    {
        for ( int i = 0; i < array.length; ++i )
        {
            if ( array[i].equals( obj ) )
            {
                return true;
            }
        }

        return false;
    }

    public static int[] toIntArray( String[] strValues )
    {
        int[] intValues = new int[strValues.length];
        for ( int i = 0; i < strValues.length; i++ )
        {
            intValues[i] = Integer.parseInt( strValues[i].trim() );
        }
        return intValues;
    }

    public static ArrayList<Integer> toArrayList( int[] intValues )
    {
        if ( intValues == null )
        {
            return new ArrayList<Integer>();
        }

        ArrayList<Integer> values = new ArrayList<Integer>( intValues.length );
        for ( int i = 0; i < intValues.length; i++ )
        {
            values.add( new Integer( intValues[i] ) );
        }
        return values;
    }

    public static boolean contains( int[] array, int value )
    {
        if ( array == null )
        {
            return false;
        }

        for ( int i = 0; i < array.length; i++ )
        {
            if ( array[i] == value )
            {
                return true;
            }
        }

        return false;
    }

    public static String[] filter( String[] sourceValues, String[] excludeValues )
    {
        if ( sourceValues == null || sourceValues.length == 0 )
        {
            return sourceValues;
        }

        final Set<String> set = Sets.newHashSet();
        set.addAll(ImmutableList.copyOf(sourceValues));
        set.removeAll(ImmutableList.copyOf(excludeValues));

        return set.toArray(new String[set.size()]);
    }


    public static byte[] concat( byte[] values1, byte[] values2 )
    {
        byte[] result = new byte[values1.length + values2.length];

        System.arraycopy( values1, 0, result, 0, values1.length );
        System.arraycopy( values2, 0, result, values1.length, values2.length );

        return result;
    }

    public static String[] concat( String[] values1, String[] values2, boolean removeDuplicates )
    {
        final List<String> list1 = ImmutableList.copyOf(values1);
        final List<String> list2 = ImmutableList.copyOf(values2);
        final Collection<String> result;

        if (removeDuplicates) {
            result = Sets.newHashSet();
        } else {
            result = Lists.newArrayList();
        }

        result.addAll(list1);
        result.addAll(list2);

        return result.toArray(new String[result.size()]);
    }
}
