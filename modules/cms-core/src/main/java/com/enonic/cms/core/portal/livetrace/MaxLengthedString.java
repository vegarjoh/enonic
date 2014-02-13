/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.portal.livetrace;

public class MaxLengthedString
{
    private static int DEFAULT_MAX_LENGTH = 3000;

    public final static String MESSAGE = "...(chopped)...";

    private String string;

    public MaxLengthedString()
    {
        this( "", DEFAULT_MAX_LENGTH );
    }

    public MaxLengthedString( String s )
    {
        this( s, DEFAULT_MAX_LENGTH );
    }

    public MaxLengthedString( String s, int maxLength )
    {
        if ( s != null && s.length() > maxLength )
        {
            this.string = chopString( s, maxLength );
        }
        else
        {
            this.string = s;
        }
    }

    private String chopString( String s, int maxLength )
    {
        int messageStart = maxLength - 10 - MESSAGE.length();

        StringBuilder result = new StringBuilder( maxLength );
        result.append( s.substring( 0, messageStart ) );
        result.append( MESSAGE );

        result.append( s.substring( s.length() - 10, s.length() ) );
        return result.toString();
    }

    public String toString()
    {
        return string;
    }
}
