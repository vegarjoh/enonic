/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.locale;

import java.util.ArrayList;
import java.util.Locale;

import org.springframework.stereotype.Service;

@Service("localeService")
public class LocaleServiceImpl
    implements LocaleService
{
    private final ArrayList<Locale> locales = new ArrayList<Locale>();

    public LocaleServiceImpl()
    {
        for ( final Locale locale : Locale.getAvailableLocales() )
        {
            final String country = locale.getCountry();
            if ( country != null && !country.equals( "" ) )
            {
                this.locales.add( locale );
            }
        }
    }

    public Locale[] getLocales()
    {
        return this.locales.toArray( new Locale[this.locales.size()] );
    }
}
