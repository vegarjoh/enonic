/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.content.category;

import org.junit.Test;

import com.enonic.cms.core.AbstractEqualsTest;


public class CategoryKeyEqualsTest
    extends AbstractEqualsTest
{
    @Test
    public void testEquals()
    {
        assertEqualsContract();
    }

    public Object getObjectX()
    {
        return new CategoryKey( 1 );
    }

    public Object[] getObjectsThatNotEqualsX()
    {
        return new Object[]{new CategoryKey( 2 )};
    }

    public Object getObjectThatEqualsXButNotTheSame()
    {
        return new CategoryKey( 1 );
    }

    public Object getObjectThatEqualsXButNotTheSame2()
    {
        return new CategoryKey( 1 );
    }
}
