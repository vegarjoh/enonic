/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.search.query;

import org.junit.Assert;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.*;

public class QueryValueTest
{

    @Test
    public void testInt()
    {
        QueryValue queryValue = new QueryValue( 1 );

        assertEquals( 1.0, queryValue.getNumericValue() );
        assertEquals( "1", queryValue.getStringValueNormalized() );

        assertFalse( queryValue.isEmpty() );
        assertFalse( queryValue.isDateTime() );
        assertTrue( queryValue.isNumeric() );
    }

    @Test
    public void testIntAsString()
    {
        QueryValue queryValue = new QueryValue( "1" );

        assertFalse( queryValue.isEmpty() );
        assertFalse( queryValue.isDateTime() );
        assertFalse( queryValue.isNumeric() );

        assertEquals( "1", queryValue.getStringValueNormalized() );
    }

    @Test
    public void testStringWithDateFormat()
    {
        QueryValue queryValue = new QueryValue( "1975-08-01" );

        assertFalse( queryValue.isEmpty() );
        assertFalse( queryValue.isDateTime() );
        assertFalse( queryValue.isNumeric() );
    }

    @Test
    public void testNormalizedString()
    {
        QueryValue queryValue = new QueryValue( "AbCdE" );

        Assert.assertEquals( "abcde", queryValue.getStringValueNormalized() );

    }

    @Test
    public void testWildcardValue()
    {
        QueryValue queryValue = new QueryValue( "%Content%" );

        Assert.assertEquals( "*content*", queryValue.getStringValueNormalized() );

    }

}
