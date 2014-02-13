/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.esl.util;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import org.junit.Test;

import junit.framework.Assert;

public class PropertiesLoaderTest
{
    @Test
    public void testLoadUtf8File_asUTF8()
        throws Exception
    {
        final Properties properties = new Properties(  );
        final InputStream in = getClass().getResourceAsStream( "/com/enonic/esl/util/PropertiesLoader-site0-utf8.properties" );
        properties.load( new InputStreamReader( in, "UTF8" ) );

        Assert.assertEquals("DG82-\u00e6\u00f8\u00e5-\u00c5\u00d8\u00c6", properties.get("sn.id"));
    }

    @Test
    public void testLoadUtf8File_asDefault()
        throws Exception
    {
        final Properties properties = new Properties(  );
        final InputStream in = getClass().getResourceAsStream( "/com/enonic/esl/util/PropertiesLoader-site0-utf8.properties" );
        properties.load( in );

        // FAIL to load correctly !
        Assert.assertFalse("DG82-\u00e6\u00f8\u00e5-\u00c5\u00d8\u00c6".equals( properties.get("sn.id")));
    }

    @Test
    public void testLoadWin1252File_asUTF8()
        throws Exception
    {
        final Properties properties = new Properties(  );
        final InputStream in = getClass().getResourceAsStream( "/com/enonic/esl/util/PropertiesLoader-site0-win1252.properties" );
        properties.load( new InputStreamReader( in, "UTF8" ) );

        // FAIL to load correctly !
        Assert.assertFalse("DG82-\u00e6\u00f8\u00e5-\u00c5\u00d8\u00c6".equals( properties.get("sn.id")));
    }

    @Test
    public void testLoadWin1252File_asDefault()
        throws Exception
    {
        final Properties properties = new Properties(  );
        final InputStream in = getClass().getResourceAsStream( "/com/enonic/esl/util/PropertiesLoader-site0-win1252.properties" );
        properties.load( in );

        Assert.assertEquals("DG82-\u00e6\u00f8\u00e5-\u00c5\u00d8\u00c6", properties.get("sn.id"));
    }

}
