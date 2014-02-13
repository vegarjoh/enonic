/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HTTPService
{

    private static final Logger LOG = LoggerFactory.getLogger( HTTPService.class );

    private final static int DEFAULT_CONNECTION_TIMEOUT = 5000;

    private final static int DEFAULT_READ_TIMEOUT = 10000;

    private String userAgent;

    public String getURL( String address, String encoding, int timeoutMs, int readTimeoutMs )
    {
        BufferedReader reader = null;
        try
        {
            URLConnection urlConn = setUpConnection( address, timeoutMs, readTimeoutMs );
            reader = setUpReader( encoding, urlConn );
            StringBuffer sb = new StringBuffer( 1024 );
            char[] line = new char[1024];
            int charCount = reader.read( line );
            while ( charCount > 0 )
            {
                sb.append( line, 0, charCount );
                charCount = reader.read( line );
            }
            return sb.toString();

        }
        catch ( Exception e )
        {
            String message = "Failed to get URL: \"" + address + "\": " + e.getMessage();
            LOG.warn( message );
        }
        finally
        {
            try
            {
                closeReader( reader );
            }
            catch ( IOException ioe )
            {
                String message = "Failed to close reader stream: \"" + address + "\": " + ioe.getMessage();
                LOG.warn( message );
            }
        }

        return null;
    }

    public byte[] getURLAsBytes( String address, int timeoutMs, int readTimeoutMs )
    {
        BufferedReader reader = null;
        try
        {
            URLConnection urlConn = setUpConnection( address, timeoutMs, readTimeoutMs );

            InputStream responseStream = urlConn.getInputStream();
            return IOUtils.toByteArray( responseStream );
        }
        catch ( Exception e )
        {
            String message = "Failed to get URL: \"" + address + "\": " + e.getMessage();
            LOG.warn( message );
        }
        finally
        {
            try
            {
                closeReader( reader );
            }
            catch ( IOException ioe )
            {
                String message = "Failed to close reader stream: \"" + address + "\": " + ioe.getMessage();
                LOG.warn( message );
            }
        }

        return null;
    }

    private URLConnection setUpConnection( String address, int timeoutMs, int readTimeoutMs )
        throws IOException
    {
        URL url = new URL( address );
        URLConnection urlConn = url.openConnection();
        urlConn.setConnectTimeout( timeoutMs > 0 ? timeoutMs : DEFAULT_CONNECTION_TIMEOUT );
        urlConn.setReadTimeout( readTimeoutMs > 0 ? readTimeoutMs : DEFAULT_READ_TIMEOUT );
        urlConn.setRequestProperty( "User-Agent", userAgent );
        String userInfo = url.getUserInfo();
        if ( StringUtils.isNotBlank( userInfo ) )
        {
            String userInfoBase64Encoded = new String( Base64.encodeBase64( userInfo.getBytes() ) );
            urlConn.setRequestProperty( "Authorization", "Basic " + userInfoBase64Encoded );
        }
        return urlConn;

    }

    private BufferedReader setUpReader( String encoding, URLConnection urlConn )
        throws IOException
    {
        InputStream in = urlConn.getInputStream();
        BufferedReader reader;
        if ( encoding == null )
        {
            reader = new BufferedReader( new InputStreamReader( in, "utf8" ) );
        }
        else
        {
            reader = new BufferedReader( new InputStreamReader( in, encoding ) );
        }
        return reader;
    }

    private void closeReader( BufferedReader reader )
        throws IOException
    {
        if ( reader != null )
        {
            reader.close();
        }

    }

    @Value("${cms.enonic.vertical.presentation.dataSource.getUrl.userAgent}")
    public void setUserAgent( final String userAgent )
    {
        this.userAgent = userAgent;
    }
}
