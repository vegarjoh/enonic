/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.search;

import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.MutableDateTime;
import org.joda.time.ReadableDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class ElasticSearchFormatter
{

    public final static DateTimeFormatter ELASTICSEARCH_SIMPLE_DATE_FORMAT = DateTimeFormat.forPattern( "yyyy-MM-dd HH:mm" );

    public final static DateTimeFormatter ELASTICSEARCH_FULL_DATE_FORMAT = DateTimeFormat.forPattern( "yyyy-MM-dd HH:mm:ss.SSS" );

    public final static DateTimeFormatter ELASTICSEARCH_DATE_OPTIONAL_TIME_FORMAT = ISODateTimeFormat.dateOptionalTimeParser();

    public static String formatDateAsStringIgnoreTimezone( final Date date )
    {
        return ELASTICSEARCH_SIMPLE_DATE_FORMAT.print( date.getTime() );
    }

    public static String formatDateAsStringIgnoreTimezone( final ReadableDateTime date )
    {
        return ELASTICSEARCH_SIMPLE_DATE_FORMAT.print( date );
    }

    public static String formatDateAsStringFull( final Date date )
    {
        return ELASTICSEARCH_FULL_DATE_FORMAT.print( date.getTime() );
    }

    public static ReadableDateTime toUTCTimeZone( final ReadableDateTime dateTime )
    {
        if ( DateTimeZone.UTC.equals( dateTime.getZone() ) )
        {
            return dateTime;
        }
        final MutableDateTime dateInUTC = dateTime.toMutableDateTime();
        dateInUTC.setZone( DateTimeZone.UTC );
        return dateInUTC.toDateTime();
    }

    public static DateTime parseStringAsElasticsearchDateOptionalTimeFormat( final String dateString )
    {
        try
        {
            return ELASTICSEARCH_DATE_OPTIONAL_TIME_FORMAT.parseDateTime( dateString );
        }
        catch ( IllegalArgumentException e )
        {
            return null;
        }
    }
}
