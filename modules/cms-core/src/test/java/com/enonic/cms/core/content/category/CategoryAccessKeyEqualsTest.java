/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.content.category;

import org.junit.Test;

import com.enonic.cms.core.AbstractEqualsTest;
import com.enonic.cms.core.security.group.GroupKey;


public class CategoryAccessKeyEqualsTest
    extends AbstractEqualsTest
{

    @Test
    public void testEquals()
    {
        assertEqualsContract();
    }

    public Object getObjectX()
    {
        return new CategoryAccessKey( new CategoryKey( 1 ), new GroupKey( "ABC" ) );
    }

    public Object[] getObjectsThatNotEqualsX()
    {
        return new Object[]{new CategoryAccessKey( new CategoryKey( 1 ), new GroupKey( "123" ) ),
            new CategoryAccessKey( new CategoryKey( 2 ), new GroupKey( "ABC" ) )};
    }

    public Object getObjectThatEqualsXButNotTheSame()
    {
        return new CategoryAccessKey( new CategoryKey( 1 ), new GroupKey( "ABC" ) );
    }

    public Object getObjectThatEqualsXButNotTheSame2()
    {
        return new CategoryAccessKey( new CategoryKey( 1 ), new GroupKey( "ABC" ) );
    }
}
