/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.resolver.locale;

import java.util.Locale;

import com.google.common.base.Preconditions;

public abstract class LocaleParser
{

    public static Locale parseLocale( final String value )
    {
        Preconditions.checkArgument( value != null, "locale string cannot be null" );

        String normalized = supportBothUnderscoreAndDash( value );

        final String[] parts = normalized.split( "_" );

        if ( parts.length == 1 )
        {
            return new Locale( parts[0] );
        }
        else if ( parts.length == 2 )
        {
            return new Locale( parts[0], parts[1] );
        }
        else if ( parts.length == 3 )
        {
            return new Locale( parts[0], parts[1], parts[2] );
        }

        throw new IllegalArgumentException( "Could not parse string: '" + value + "' to a valid locale" );
    }

    private static String supportBothUnderscoreAndDash( String value )
    {
        String normalized = value.replace( "-", "_" );
        return normalized;
    }

}

