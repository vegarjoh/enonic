/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.content.binary;

import org.junit.Test;

import com.enonic.cms.core.AbstractEqualsTest;


public class BinaryDataKeyEqualsTest
    extends AbstractEqualsTest
{

    @Test
    public void testEquals()
    {
        assertEqualsContract();
    }

    public Object getObjectX()
    {
        return new BinaryDataKey( 1 );
    }

    public Object[] getObjectsThatNotEqualsX()
    {
        return new Object[]{new BinaryDataKey( 2 )};
    }

    public Object getObjectThatEqualsXButNotTheSame()
    {
        return new BinaryDataKey( 1 );
    }

    public Object getObjectThatEqualsXButNotTheSame2()
    {
        return new BinaryDataKey( 1 );
    }
}
