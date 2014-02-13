/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.esl.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public class URLUtil
{

    private static BitSet dontNeedEncoding;

    static
    {
        dontNeedEncoding = new BitSet( 256 );
        int i;
        for ( i = 'a'; i <= 'z'; i++ )
        {
            dontNeedEncoding.set( i );
        }
        for ( i = 'A'; i <= 'Z'; i++ )
        {
            dontNeedEncoding.set( i );
        }
        for ( i = '0'; i <= '9'; i++ )
        {
            dontNeedEncoding.set( i );
        }
        dontNeedEncoding.set( ' ' ); /*
                                     * encoding a space to a + is done in the
                                     * encode() method
                                     */
        dontNeedEncoding.set( '-' );
        dontNeedEncoding.set( '_' );
        dontNeedEncoding.set( '.' );
        dontNeedEncoding.set( '*' );
    }

    private final static int caseDiff = ( 'a' - 'A' );

    /**
     * Private constructor. No instantiation allowed.
     */
    private URLUtil()
    {
    }

    public static String encode( String s )
    {

        boolean needToChange = false;
        boolean wroteUnencodedChar = false;
        int maxBytesPerChar = 10; // rather arbitrary limit, but safe for now
        StringBuffer out = new StringBuffer( s.length() );
        ByteArrayOutputStream buf = new ByteArrayOutputStream( maxBytesPerChar );
        OutputStreamWriter writer;
        try
        {
            writer = new OutputStreamWriter( buf, "UTF-8" );
        }
        catch ( UnsupportedEncodingException uee )
        {
            return null;
        }
        for ( int i = 0; i < s.length(); i++ )
        {
            int c = (int) s.charAt( i );
            if ( dontNeedEncoding.get( c ) )
            {
                if ( c == ' ' )
                {
                    c = '+';
                    needToChange = true;
                }
                out.append( (char) c );
                wroteUnencodedChar = true;
            }
            else
            {
                // convert to external encoding before hex conversion
                try
                {
                    if ( wroteUnencodedChar )
                    { // Fix for 4407610
                        writer = new OutputStreamWriter( buf, "UTF-8" );
                        wroteUnencodedChar = false;
                    }
                    writer.write( c );
                    /*
                     * If this character represents the start of a Unicode
                     * surrogate pair, then pass in two characters. It's not
                     * clear what should be done if a bytes reserved in the
                     * surrogate pairs range occurs outside of a legal surrogate
                     * pair. For now, just treat it as if it were any other
                     * character.
                     */
                    if ( c >= 0xD800 && c <= 0xDBFF )
                    {
                        /*
                         * (Integer.toHexString(c) + " is high
                         * surrogate");
                         */
                        if ( ( i + 1 ) < s.length() )
                        {
                            int d = (int) s.charAt( i + 1 );
                            /*
                             * ("\tExamining " +
                             * Integer.toHexString(d));
                             */
                            if ( d >= 0xDC00 && d <= 0xDFFF )
                            {
                                /*
                                 * ("\t" +
                                 * Integer.toHexString(d) + " is low
                                 * surrogate");
                                 */
                                writer.write( d );
                                i++;
                            }
                        }
                    }
                    writer.flush();
                }
                catch ( IOException e )
                {
                    buf.reset();
                    continue;
                }
                byte[] bufferBytes = buf.toByteArray();
                for ( byte bufferByte : bufferBytes )
                {
                    out.append( '%' );
                    char ch = Character.forDigit( ( bufferByte >> 4 ) & 0xF, 16 );
                    // converting to use uppercase letter as part of
                    // the hex value if ch is a letter.
                    if ( Character.isLetter( ch ) )
                    {
                        ch -= caseDiff;
                    }
                    out.append( ch );
                    ch = Character.forDigit( bufferByte & 0xF, 16 );
                    if ( Character.isLetter( ch ) )
                    {
                        ch -= caseDiff;
                    }
                    out.append( ch );
                }
                buf.reset();
                needToChange = true;
            }
        }

        return ( needToChange ? out.toString() : s );
    }

    public static String decode( String s )
    {

        boolean needToChange = false;
        StringBuffer sb = new StringBuffer();
        int numChars = s.length();
        int i = 0;

        while ( i < numChars )
        {
            char c = s.charAt( i );
            switch ( c )
            {
                case '+':
                    sb.append( ' ' );
                    i++;
                    needToChange = true;
                    break;
                case '%':
                    /*
                     * Starting with this instance of %, process all consecutive
                     * substrings of the form %xy. Each substring %xy will yield
                     * a byte. Convert all consecutive bytes obtained this way
                     * to whatever character(s) they represent in the provided
                     * encoding.
                     */

                    try
                    {

                        // (numChars-i)/3 is an upper bound for the number
                        // of remaining bytes
                        byte[] bytes = new byte[( numChars - i ) / 3];
                        int pos = 0;

                        while ( ( ( i + 2 ) < numChars ) && ( c == '%' ) )
                        {
                            bytes[pos++] = (byte) Integer.parseInt( s.substring( i + 1, i + 3 ), 16 );
                            i += 3;
                            if ( i < numChars )
                            {
                                c = s.charAt( i );
                            }
                        }

                        // A trailing, incomplete byte encoding such as
                        // "%x" will cause an exception to be thrown

                        if ( ( i < numChars ) && ( c == '%' ) )
                        {
                            throw new IllegalArgumentException( "URLDecoder: Incomplete trailing escape (%) pattern" );
                        }

                        sb.append( new String( bytes, 0, pos, "UTF-8" ) );
                    }
                    catch ( UnsupportedEncodingException uee )
                    {
                        return null;
                    }
                    catch ( NumberFormatException e )
                    {
                        throw new IllegalArgumentException(
                            "URLDecoder: Illegal hex characters in escape (%) pattern - " + e.getMessage() );
                    }
                    needToChange = true;
                    break;
                default:
                    sb.append( c );
                    i++;
                    break;
            }
        }

        return ( needToChange ? sb.toString() : s );
    }

    public static Map<String, String[]> decodeParameterMap( Map<String, String[]> parameterMap )
    {
        return decodeParameterMap( parameterMap, "ISO-8859-1" );
    }

    public static Map<String, String[]> decodeParameterMap( Map<String, String[]> parameterMap, String inputEncoding )
    {
        Map<String, String[]> decodedParameterMap = new HashMap<String, String[]>( parameterMap.size() );
        try
        {
            for ( Map.Entry<String, String[]> entry : parameterMap.entrySet() )
            {
                // decode key
                String key = entry.getKey();
                key = new String( key.getBytes( inputEncoding ), "UTF-8" );

                // decode value(s)
                String[] values = entry.getValue();
                for ( int i = 0; i < values.length; i++ )
                {
                    values[i] = new String( values[i].getBytes( inputEncoding ), "UTF-8" );
                }

                decodedParameterMap.put( key, values );
            }
        }
        catch ( UnsupportedEncodingException uee )
        {
            throw new IllegalStateException( uee.getMessage() );
        }

        return decodedParameterMap;
    }
}
