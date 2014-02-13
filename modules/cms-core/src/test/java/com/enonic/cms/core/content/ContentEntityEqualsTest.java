/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.content;

import org.junit.Test;

import com.enonic.cms.core.AbstractEqualsTest;


public class ContentEntityEqualsTest
    extends AbstractEqualsTest
{
    @Test
    public void testEquals()
    {
        assertEqualsContract();
    }

    public Object getObjectX()
    {
        ContentEntity c = new ContentEntity();
        c.setKey( new ContentKey( 1 ) );
        return c;
    }

    public Object[] getObjectsThatNotEqualsX()
    {
        ContentEntity c = new ContentEntity();
        c.setKey( new ContentKey( 2 ) );
        return new Object[]{c};
    }

    public Object getObjectThatEqualsXButNotTheSame()
    {
        ContentEntity c = new ContentEntity();
        c.setKey( new ContentKey( 1 ) );
        return c;
    }

    public Object getObjectThatEqualsXButNotTheSame2()
    {
        ContentEntity c = new ContentEntity();
        c.setKey( new ContentKey( 1 ) );
        return c;
    }
}
