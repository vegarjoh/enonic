/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.xslt.functions.portal;

import java.util.TimeZone;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

import com.enonic.cms.core.search.query.IndexValueConverter;

public class DateTimeFunctionTest
    extends AbstractPortalFunctionTest
{
    @Before
    public void setUp()
    {
        super.setUp();

        TimeZone.setDefault( TimeZone.getTimeZone( "Europe/Oslo" ) );
    }

    @Test
    public void testFunction()
        throws Exception
    {
        processTemplate( "dateTime" );
    }
}
