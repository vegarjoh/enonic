/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.security.group;

import org.junit.Test;

import com.enonic.cms.core.AbstractEqualsTest;


public class GroupKeyEqualsTest
    extends AbstractEqualsTest
{
    @Test
    public void testEquals()
    {
        assertEqualsContract();
    }

    public Object getObjectX()
    {
        return new GroupKey( "ABC" );
    }

    public Object[] getObjectsThatNotEqualsX()
    {
        return new Object[]{new GroupKey( "CBA" )};
    }

    public Object getObjectThatEqualsXButNotTheSame()
    {
        return new GroupKey( "ABC" );
    }

    public Object getObjectThatEqualsXButNotTheSame2()
    {
        return new GroupKey( "ABC" );
    }
}
