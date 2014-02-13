/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal.rendering;

import java.util.Locale;

import org.junit.Test;

import com.enonic.cms.core.AbstractEqualsTest;
import com.enonic.cms.core.structure.menuitem.MenuItemKey;


public class WindowCacheKeyEqualsTest
    extends AbstractEqualsTest
{
    @Test
    public void testEquals()
    {
        assertEqualsContract();
    }


    public Object getObjectX()
    {
        return new WindowCacheKey( "ABC", new MenuItemKey( 1 ), 1, "q", "p", "a", new Locale( "no" ) );
    }

    public Object[] getObjectsThatNotEqualsX()
    {
        return new Object[]{new WindowCacheKey( "ABC", new MenuItemKey( 1 ), 1, "q", "x", "a", new Locale( "no" ) ),
            new WindowCacheKey( "ABC", new MenuItemKey( 1 ), 1, "x", "p", "a", new Locale( "no" ) ),
            new WindowCacheKey( "ABC", new MenuItemKey( 1 ), 2, "q", "p", "a", new Locale( "no" ) ),
            new WindowCacheKey( "ABC", new MenuItemKey( 2 ), 1, "q", "p", "a", new Locale( "no" ) ),
            new WindowCacheKey( "XXX", new MenuItemKey( 1 ), 1, "q", "p", "a", new Locale( "no" ) ),
            new WindowCacheKey( "ABC", new MenuItemKey( 1 ), 1, "q", "p", "b", new Locale( "no" ) ),
            new WindowCacheKey( "ABC", new MenuItemKey( 1 ), 1, "q", "p", null, new Locale( "no" ) ),};
    }

    public Object getObjectThatEqualsXButNotTheSame()
    {
        return new WindowCacheKey( "ABC", new MenuItemKey( 1 ), 1, "q", "p", "a", new Locale( "no" ) );
    }

    public Object getObjectThatEqualsXButNotTheSame2()
    {
        return new WindowCacheKey( "ABC", new MenuItemKey( 1 ), 1, "q", "p", "a", new Locale( "no" ) );
    }
}
