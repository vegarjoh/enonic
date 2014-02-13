/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.esl.util;

import java.util.Date;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class DateUtil
{

    private final static DateTimeFormatter dateFormat = DateTimeFormat.forPattern( "dd.MM.yyyy" );

    private final static DateTimeFormatter dateFormatWithTime = DateTimeFormat.forPattern( "dd.MM.yyyy HH:mm" );

    private final static DateTimeFormatter dateFormatWithTimeSeconds = DateTimeFormat.forPattern( "dd.MM.yyyy HH:mm:ss" );

    private final static DateTimeFormatter isoDateFormatNoTime = DateTimeFormat.forPattern( "yyyy-MM-dd" );

    private final static DateTimeFormatter isoDateFormatWithTime = DateTimeFormat.forPattern( "yyyy-MM-dd HH:mm" );

    public static Date parseDate( String date )
        throws IllegalArgumentException
    {
        return dateFormat.parseDateTime( date ).toDate();
    }

    public static Date parseDateTime( String date )
        throws IllegalArgumentException
    {
        return parseDateTime( date, false );
    }

    public static Date parseDateTime( String date, boolean includeSeconds )
        throws IllegalArgumentException
    {
        if ( includeSeconds )
        {
            return dateFormatWithTimeSeconds.parseDateTime( date ).toDate();
        }
        else
        {
            return dateFormatWithTime.parseDateTime( date ).toDate();
        }
    }

    public static Date parseISODate( String date )
        throws IllegalArgumentException
    {
        return isoDateFormatNoTime.parseDateTime( date ).toDate();
    }

    public static Date parseISODateTime( String date )
        throws IllegalArgumentException
    {
        return isoDateFormatWithTime.parseDateTime( date ).toDate();
    }

    public static String formatDate( Date date )
    {
        return dateFormat.print( date.getTime() );
    }

    public static String formatDateTime( Date date )
    {
        return dateFormatWithTime.print( date.getTime() );
    }

    public static String formatISODate( Date date )
    {
        return isoDateFormatNoTime.print( date.getTime() );
    }

    public static String formatISODateTime( Date date )
    {
        return isoDateFormatWithTime.print( date.getTime() );
    }
}
