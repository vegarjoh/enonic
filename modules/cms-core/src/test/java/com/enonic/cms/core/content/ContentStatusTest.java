/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.content;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Dec 15, 2009
 */
public class ContentStatusTest
{
    @Test
    public void test()
    {
        assertTrue( ContentStatus.APPROVED == ContentStatus.get( 2 ) );
        assertTrue( ContentStatus.APPROVED == ContentStatus.APPROVED );
        assertFalse( ContentStatus.APPROVED == ContentStatus.get( 1 ) );
        assertFalse( ContentStatus.APPROVED == ContentStatus.SNAPSHOT );
    }
}
