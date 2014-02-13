/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.structure.page;

import org.junit.Test;

import com.enonic.cms.core.AbstractEqualsTest;

/**
 * Aug 26, 2010
 */
public class PageEntityEqualsTest
    extends AbstractEqualsTest
{

    @Test
    public void testEquals()
    {
        assertEqualsContract();
    }

    public Object getObjectX()
    {
        return createPage( 1 );
    }

    public Object[] getObjectsThatNotEqualsX()
    {
        return new Object[]{createPage( 2 )};
    }

    public Object getObjectThatEqualsXButNotTheSame()
    {
        return createPage( 1 );
    }

    public Object getObjectThatEqualsXButNotTheSame2()
    {
        return createPage( 1 );
    }

    private PageEntity createPage( int key )
    {
        PageEntity pageEntity = new PageEntity();
        pageEntity.setKey( key );
        return pageEntity;
    }
}