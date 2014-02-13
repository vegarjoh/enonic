/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.user.field;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.enonic.cms.api.client.model.user.Gender;
import com.enonic.cms.api.plugin.ext.userstore.UserField;
import com.enonic.cms.api.plugin.ext.userstore.UserFieldType;
import com.enonic.cms.core.resolver.locale.LocaleParser;

public final class UserFieldHelper
{
    private final DateTimeFormatter DATE_FORMAT = DateTimeFormat.forPattern( "yyyyMMdd" );

    private final DateTimeFormatter dateFormat;

    private DateTimeFormatter[] getSupportedDateFormats()
    {
        final DateTimeFormatter iso = DateTimeFormat.forPattern( "yyyy-MM-dd" );
        final DateTimeFormatter old = DateTimeFormat.forPattern( "dd.MM.yyyy" );
        final DateTimeFormatter standard = DATE_FORMAT;

        return new DateTimeFormatter[]{iso, old, standard};
    }

    public UserFieldHelper()
    {
        this( null );
    }

    public UserFieldHelper( String format )
    {
        this.dateFormat = format != null ? DateTimeFormat.forPattern( format ) : DATE_FORMAT;
    }

    public String toString( UserField field )
    {
        if ( field == null )
        {
            return null;
        }

        Object value = field.getValue();
        if ( value == null )
        {
            return null;
        }

        if ( value instanceof Date )
        {
            return formatDate( (Date) value );
        }

        if ( value instanceof Boolean )
        {
            return formatBoolean( (Boolean) value );
        }

        if ( value instanceof Gender )
        {
            return formatGender( (Gender) value );
        }

        if ( value instanceof Locale )
        {
            return formatLocale( (Locale) value );
        }

        if ( value instanceof TimeZone )
        {
            return formatTimezone( (TimeZone) value );
        }

        return value.toString();
    }

    public Object fromString( UserFieldType type, String value )
    {
        if ( value == null )
        {
            return null;
        }

        if ( type.isOfType( String.class ) )
        {
            return value;
        }

        if ( type.isOfType( Date.class ) )
        {
            return parseDate( value );
        }

        if ( type.isOfType( Boolean.class ) )
        {
            return parseBoolean( value );
        }

        if ( type.isOfType( Gender.class ) )
        {
            return parseGender( value );
        }

        if ( type.isOfType( Locale.class ) )
        {
            return parseLocale( value );
        }

        if ( type.isOfType( TimeZone.class ) )
        {
            return parseTimeZone( value );
        }

        throw new IllegalArgumentException( "Convertion of type [" + type.getTypeClass().getName() + "] not supported" );
    }

    private String formatDate( Date value )
    {
        return value == null ? null : this.dateFormat.print( value.getTime() );
    }

    private String formatBoolean( Boolean value )
    {
        return value.toString();
    }

    private String formatGender( Gender value )
    {
        return value.toString().toLowerCase();
    }

    private String formatLocale( Locale value )
    {
        return value.toString();
    }

    private String formatTimezone( TimeZone value )
    {
        return value.getID();
    }

    private Boolean parseBoolean( String value )
    {
        if ( StringUtils.isBlank( value ) )
        {
            return null;
        }

        return "|1|true|on".indexOf( value ) > 0;
    }

    private Gender parseGender( String value )
    {
        if ( StringUtils.isBlank( value ) )
        {
            return null;
        }

        if ( value.equalsIgnoreCase( "m" ) || value.equalsIgnoreCase( "male" ) )
        {
            return Gender.MALE;
        }

        if ( value.equalsIgnoreCase( "f" ) || value.equalsIgnoreCase( "female" ) )
        {
            return Gender.FEMALE;
        }

        return null;
    }

    private Locale parseLocale( final String value )
    {
        if ( StringUtils.isBlank( value ) )
        {
            return null;
        }

        return LocaleParser.parseLocale( value );
    }

    private TimeZone parseTimeZone( String value )
    {
        if ( StringUtils.isBlank( value ) )
        {
            return null;
        }

        return TimeZone.getTimeZone( value );
    }

    private Date parseDate( String value )
    {
        if ( StringUtils.isEmpty( value ) )
        {
            return null;
        }

        for ( final DateTimeFormatter format : getSupportedDateFormats() )
        {
            Date date = parseDate( value, format );
            if ( date != null )
            {
                return date;
            }
        }
        throw new IllegalArgumentException( "Could not parse date " + value );
    }

    private Date parseDate( String value, DateTimeFormatter format )
    {
        try
        {
            return format.parseDateTime( value ).toDate();
        }
        catch ( Exception e )
        {
            return null;
        }
    }
}
