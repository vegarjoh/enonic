/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.content;

import org.junit.Test;

import com.enonic.cms.core.AbstractEqualsTest;


public class ContentVersionEntityEqualsTest
    extends AbstractEqualsTest
{
    @Test
    public void testEquals()
    {
        assertEqualsContract();
    }

    public Object getObjectX()
    {
        ContentVersionEntity instance = new ContentVersionEntity();
        instance.setKey( new ContentVersionKey( 1 ) );
        return instance;
    }

    public Object[] getObjectsThatNotEqualsX()
    {
        ContentVersionEntity instance = new ContentVersionEntity();
        instance.setKey( new ContentVersionKey( 2 ) );
        return new Object[]{instance};
    }

    public Object getObjectThatEqualsXButNotTheSame()
    {
        ContentVersionEntity instance = new ContentVersionEntity();
        instance.setKey( new ContentVersionKey( 1 ) );
        return instance;
    }

    public Object getObjectThatEqualsXButNotTheSame2()
    {
        ContentVersionEntity instance = new ContentVersionEntity();
        instance.setKey( new ContentVersionKey( 1 ) );
        return instance;
    }
}
