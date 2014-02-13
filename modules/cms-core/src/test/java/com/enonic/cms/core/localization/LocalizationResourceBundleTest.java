/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.localization;

import java.util.Properties;

import org.junit.Test;

import static org.junit.Assert.*;

public class LocalizationResourceBundleTest
{
    private static final String NORWEGIAN = "\u00c6\u00d8\u00c5\u00e6\u00f8\u00e5";

    @Test
    public void testNorwegianCharacters()
        throws Exception
    {
        LocalizationResourceBundle resourceBundle = LocalizationTestUtils.create_US_NO_DEFAULT_resourceBundle();
        assertEquals( NORWEGIAN, resourceBundle.getLocalizedPhrase( "norsketegn" ) );
    }

    @Test
    public void testResourceOrdering()
        throws Exception
    {
        LocalizationResourceBundle resourceBundle = LocalizationTestUtils.create_US_NO_DEFAULT_resourceBundle();

        assertEquals( resourceBundle.getLocalizedPhrase( "only_in_en-us" ), "en-us" );
        assertEquals( resourceBundle.getLocalizedPhrase( "in_all" ), "en-us" );
        assertEquals( resourceBundle.getLocalizedPhrase( "no_and_default" ), "no" );
        assertEquals( resourceBundle.getLocalizedPhrase( "only_in_default" ), "default" );
    }

    @Test
    public void testNonExistingKey()
        throws Exception
    {
        LocalizationResourceBundle resourceBundle = LocalizationTestUtils.create_US_NO_DEFAULT_resourceBundle();

        assertNull( resourceBundle.getLocalizedPhrase( "in_all_not" ) );
        assertNotNull( resourceBundle.getLocalizedPhrase( "in_all" ) );
        assertNull( resourceBundle.getLocalizedPhrase( "only_in_en" ) );
        assertNotNull( resourceBundle.getLocalizedPhrase( "only_in_en-us" ) );
    }

    @Test
    public void testEmptyResourceBundle()
    {
        LocalizationResourceBundle resourceBundle = new LocalizationResourceBundle( new Properties() );
        assertNull( resourceBundle.getLocalizedPhrase( "in_all" ) );
    }

    @Test
    public void testParameterizedPhrase()
        throws Exception
    {
        LocalizationResourceBundle resourceBundle = LocalizationTestUtils.create_US_NO_DEFAULT_resourceBundle();

        Object[] testArgs = {"torsk", 8};

        String resolvedPhrase = resourceBundle.getLocalizedPhrase( "fiskmessage", testArgs );

        assertEquals( "det ble fisket 8 fisk av type torsk med musse p\u00e5 stampen", resolvedPhrase );
    }

    @Test
    public void testMissingParametersPhrase()
        throws Exception
    {
        LocalizationResourceBundle resourceBundle = LocalizationTestUtils.create_US_NO_DEFAULT_resourceBundle();

        Object[] testArgs = {"torsk"};

        String resolvedPhrase = resourceBundle.getLocalizedPhrase( "fiskmessage", testArgs );

        assertEquals( "det ble fisket {1} fisk av type torsk med musse p\u00e5 stampen", resolvedPhrase );
    }

    @Test
    public void testNullParametersPhrase()
        throws Exception
    {
        LocalizationResourceBundle resourceBundle = LocalizationTestUtils.create_US_NO_DEFAULT_resourceBundle();

        String resolvedPhrase = resourceBundle.getLocalizedPhrase( "fiskmessage", null );

        assertEquals( "det ble fisket {1} fisk av type {0} med musse p\u00e5 stampen", resolvedPhrase );
    }
}
