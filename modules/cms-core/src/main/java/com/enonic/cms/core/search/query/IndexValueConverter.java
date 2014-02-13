/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.search.query;

import java.util.Date;

import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.ReadableDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * This class implements the index value.
 */
public final class IndexValueConverter
{

    private static final long SIGN_MASK = 0x8000000000000000L;

    private static final int STRING_DOUBLE_LEN = Long.toString( Long.MAX_VALUE, Character.MAX_RADIX ).length() + 1;

    private final static DateTimeFormatter FULL_DATE_FORMAT_WITH_TIME_ZONE = ISODateTimeFormat.dateTimeParser();

    private final static DateTimeFormatter FULL_DATE_FORMAT = DateTimeFormat.forPattern( "yyyy-MM-dd'T'HH:mm:ss" );

    private final static DateTimeFormatter DATE_FORMAT = DateTimeFormat.forPattern( "yyyy-MM-dd" );

    private final static DateTimeFormatter DATETIME_WITH_SECS_FORMAT = DateTimeFormat.forPattern( "yyyy-MM-dd HH:mm:ss" );

    private final static DateTimeFormatter DATETIME_WITHOUT_SECS_FORMAT = DateTimeFormat.forPattern( "yyyy-MM-dd HH:mm" );

    // new format from CMS-2545
    private final static DateTimeFormatter DATETIME_WITH_SECS_FORMAT_Z = DateTimeFormat.forPattern( "yyyy-MM-dd HH:mm:ssZ" );

    // new format from CMS-2545
    private final static DateTimeFormatter DATETIME_WITHOUT_SECS_FORMAT_Z = DateTimeFormat.forPattern( "yyyy-MM-dd HH:mmZ" );

    /**
     * Private constructor.
     */
    private IndexValueConverter()
    {
    }

    public static String toString( long value )
    {
        return Long.toString( value );
    }

    public static String toString( double value )
    {
        return Double.toString( value );
    }

    public static String toString( Date value )
    {
        return value != null ? FULL_DATE_FORMAT.print( value.getTime() ) : null;
    }

    public static String toTypedString( Date value )
    {
        return value != null ? toTypedString( value.getTime() ) : null;
    }

    public static String toTypedString( double value )
    {
        long longValue = Double.doubleToLongBits( value );
        StringBuffer sb = new StringBuffer( STRING_DOUBLE_LEN );
        if ( ( longValue & SIGN_MASK ) == 0 )
        {
            String s = Long.toString( longValue, Character.MAX_RADIX );
            sb.append( '1' );
            while ( ( sb.length() + s.length() ) < STRING_DOUBLE_LEN )
            {
                sb.append( '0' );
            }
            sb.append( s );
        }
        else
        {
            longValue = -longValue;
            String s = Long.toString( longValue, Character.MAX_RADIX );
            while ( ( sb.length() + s.length() ) < STRING_DOUBLE_LEN )
            {
                sb.append( '0' );
            }
            sb.append( s );
        }

        return sb.toString().toLowerCase();
    }

    public static Double toDouble( String value )
    {
        try
        {
            Double num = new Double( value );
            if ( num.isNaN() || num.isInfinite() )
            {
                return null;
            }
            else
            {
                return num;
            }
        }
        catch ( NumberFormatException e )
        {
            return null;
        }
    }

    public static ReadableDateTime toDate( String value )
    {

        value = value.toUpperCase();

        // used in Queries as indicator for expressions like "myDate = '2013-05-05'"
        // TODO move this hack outside.
        final DateTime dateByDateFormat = toDateTime( value, DATE_FORMAT );
        if ( dateByDateFormat != null )
        {
            // We use DateMidnight to later recognise that user have not specified time
            return new DateMidnight( dateByDateFormat );
        }

        // hza: i do not know why we test for FULL_DATE_FORMAT but then parse using FULL_DATE_FORMAT_WITH_TIME_ZONE
        final DateTime dateTimeByFullFormat = toDateTime( value, FULL_DATE_FORMAT );
        if ( dateTimeByFullFormat != null )
        {
            final DateTime dateTimeByFullFormatTimeZone = toDateTime( value, FULL_DATE_FORMAT_WITH_TIME_ZONE );
            if ( dateTimeByFullFormatTimeZone != null )
            {
                return dateTimeByFullFormatTimeZone;
            }

            return dateTimeByFullFormat;
        }

        final DateTimeFormatter[] dateTimeFormatterArray =
            new DateTimeFormatter[]{DATETIME_WITH_SECS_FORMAT, DATETIME_WITHOUT_SECS_FORMAT, FULL_DATE_FORMAT_WITH_TIME_ZONE, DATETIME_WITH_SECS_FORMAT_Z,
                            DATETIME_WITHOUT_SECS_FORMAT_Z};

        for ( final DateTimeFormatter dateTimeFormatter : dateTimeFormatterArray )
        {
            final DateTime dateTime = toDateTime( value, dateTimeFormatter );

            if ( dateTime != null )
            {
                return dateTime;
            }
        }

        return null;
    }

    private static DateTime toDateTime( String value, DateTimeFormatter format )
    {
        try
        {
            return format.parseDateTime( value );
        }
        catch ( IllegalArgumentException e )
        {
            return null;
        }
    }

}
