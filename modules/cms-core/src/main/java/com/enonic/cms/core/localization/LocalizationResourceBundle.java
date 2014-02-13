/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.localization;

import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by rmy - Date: Apr 23, 2009
 */
public class LocalizationResourceBundle
    extends ResourceBundle
{
    private final Properties props;

    private static final String UTF_8_ENCODING = "UTF-8";

    private static final String LATIN_1_ENCODING = "ISO-8859-1";

    private static final Logger LOG = LoggerFactory.getLogger( LocalizationResourceBundle.class );

    public LocalizationResourceBundle( Properties props )
    {
        this.props = props;
    }

    protected Object handleGetObject( String key )
    {
        return createUTF8EncodedPhrase( (String) this.props.get( key ) );
    }

    public Enumeration<String> getKeys()
    {
        HashSet<String> set = new HashSet<String>();
        for ( Object o : this.props.keySet() )
        {
            set.add( (String) o );
        }

        return Collections.enumeration( set );
    }

    public String getLocalizedPhrase( String s )
    {
        return (String) handleGetObject( s );
    }

    public String getLocalizedPhrase( String s, Object[] arguments )
    {
        String message = (String) handleGetObject( s );

        return StringUtils.isNotEmpty( message ) ? MessageFormat.format( message, arguments ) : null;
    }

    private String createUTF8EncodedPhrase( String localizedPhrase )
    {
        if ( StringUtils.isBlank( localizedPhrase ) )
        {
            return null;
        }

        try
        {
            return new String( localizedPhrase.getBytes( LATIN_1_ENCODING ), UTF_8_ENCODING );
        }
        catch ( UnsupportedEncodingException e )
        {
            LOG.error( "Parsing localized phrase: " + localizedPhrase + " failed", e );
            return null;
        }
    }
}


