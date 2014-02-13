/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.locale;

import java.io.IOException;
import java.util.Locale;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLAssert;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import static org.junit.Assert.*;

public class LocaleXmlCreatorTest
{
    private LocaleService localeService;

    private LocaleXmlCreator localeXmlCreator;

    @Before
    public void setUp()
    {
        this.localeService = new LocaleServiceImpl();
        this.localeXmlCreator = new LocaleXmlCreator();
    }

    @Test
    public void createDocument()
    {
        Locale[] locales = localeService.getLocales();
        assertNotNull( locales );

        Document localesDoc = localeXmlCreator.createLocalesDocument( locales );
        assertNotNull( localesDoc );
    }

    @Test
    public void createSingleLocaleDocument()
        throws SAXException, IOException
    {
        Locale.setDefault( Locale.ENGLISH );
        Locale locale = new Locale( "NO", "NO", "NB" );

        Document localeDoc = localeXmlCreator.createLocalesDocument( locale );
        assertNotNull( localeDoc );

        String controlXml = "<locales><locale><name>no_NO_NB</name><country>NO</country><display-country>Norway</display-country>" +
            "<display-language>Norwegian</display-language><display-name>Norwegian (Norway,NB)</display-name><display-variant>NB</display-variant>" +
            "<iso3country>NOR</iso3country><iso3language>nor</iso3language><language>no</language><variant>NB</variant></locale></locales>";

        XMLOutputter outputter = new XMLOutputter();
        Diff myDiff = new Diff( controlXml, outputter.outputString( localeDoc ) );

        XMLAssert.assertXMLEqual( myDiff, true );

    }

    @Test
    public void createSingleLocaleDocumentWithInLocaleNO()
        throws SAXException, IOException
    {

        Locale.setDefault( Locale.ENGLISH );
        Locale locale = new Locale( "NO", "NO", "NB" );
        Document localeDoc = localeXmlCreator.createLocaleDocument( locale, locale );
        assertNotNull( localeDoc );

        String controlXml = "<locales><locale><name>no_NO_NB</name><country>NO</country><display-country>Norway</display-country>" +
            "<display-language>Norwegian</display-language><display-name>Norwegian (Norway,NB)</display-name><display-variant>NB</display-variant>" +
            "<iso3country>NOR</iso3country><iso3language>nor</iso3language><language>no</language><variant>NB</variant>" +
            "<display-country-in-locale language=\"no\">Norge</display-country-in-locale>" +
            "<display-language-in-locale language=\"no\">norsk</display-language-in-locale>" +
            "<display-name-in-locale language=\"no\">norsk (Norge,NB)</display-name-in-locale>" +
            "<display-variant-in-locale language=\"no\">NB</display-variant-in-locale></locale></locales>";

        XMLOutputter outputter = new XMLOutputter();
        Diff myDiff = new Diff( controlXml, outputter.outputString( localeDoc ) );
        XMLAssert.assertXMLEqual( myDiff, true );

    }

    @Test
    public void createSingleLocaleDocumentWithInLocaleSE()
        throws SAXException, IOException
    {

        Locale.setDefault( Locale.ENGLISH );
        Locale locale = new Locale( "NO", "NO", "NB" );
        Locale inLocale = new Locale( "DE" );
        Document localeDoc = localeXmlCreator.createLocaleDocument( locale, inLocale );
        assertNotNull( localeDoc );

        String controlXml = "<locales><locale><name>no_NO_NB</name><country>NO</country><display-country>Norway</display-country>" +
            "<display-language>Norwegian</display-language><display-name>Norwegian (Norway,NB)</display-name><display-variant>NB</display-variant>" +
            "<iso3country>NOR</iso3country><iso3language>nor</iso3language><language>no</language><variant>NB</variant>" +
            "<display-country-in-locale language=\"de\">Norwegen</display-country-in-locale>" +
            "<display-language-in-locale language=\"de\">Norwegisch</display-language-in-locale>" +
            "<display-name-in-locale language=\"de\">Norwegisch (Norwegen,NB)</display-name-in-locale>" +
            "<display-variant-in-locale language=\"de\">NB</display-variant-in-locale></locale></locales>";

        XMLOutputter outputter = new XMLOutputter();
        Diff myDiff = new Diff( controlXml, outputter.outputString( localeDoc ) );
        XMLAssert.assertXMLEqual( myDiff, true );

    }

}
