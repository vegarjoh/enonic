/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal;

import org.junit.Test;

import junit.framework.TestCase;

import com.enonic.cms.core.Path;

public class ContentPathResolverTest
    extends TestCase
{

    @Test
    public void testPathsWithoutContentKey()
    {
        ContentPath resolvedPath = resolvePath( "/This/is/a/test/without/content-key/content-name" );
        assertNull( resolvedPath );

        resolvedPath = resolvePath( "/This/is/a/test/without/content-key/content-name/content-title--" );
        assertNull( resolvedPath );

        resolvedPath = resolvePath( "/This/is/a/test/without/content-key/content-name/content-title-1234" );
        assertNull( resolvedPath );

        resolvedPath = resolvePath( "/This/is/a/test/with/content-key/content-name--1234anything" );
        assertNull( resolvedPath );

        resolvedPath = resolvePath( "/This/is/a/test/with/content-key/content-name--1234/anything" );
        assertNull( resolvedPath );

        resolvedPath = resolvePath( "/This/is/a/test/with/content-key/content-name--1234/_windows/mywindow" );
        assertNull( resolvedPath );

        resolvedPath = resolvePath( "/This/is/a/test/with/content-key/content-name--1234/_window" );
        assertNull( resolvedPath );
    }

    @Test
    public void testPathsWithContentKey()
    {
        ContentPath resolvedPath = resolvePath( "/content-name--1234" );
        verifyContentPath( resolvedPath, "1234", "content-name", "/" );

        resolvedPath = resolvePath( "content-name--1234" );
        verifyContentPath( resolvedPath, "1234", "content-name", "/" );

        resolvedPath = resolvePath( "/This/is/a/test/with/content-key/content--1234name--1234" );
        verifyContentPath( resolvedPath, "1234", "content--1234name", "/This/is/a/test/with/content-key" );

        resolvedPath = resolvePath( "/This/is/a/test/with/content-key/content-name--1234" );
        verifyContentPath( resolvedPath, "1234", "content-name", "/This/is/a/test/with/content-key" );

        resolvedPath = resolvePath( "This/is/a/test/with/content-key/content-name--1234" );
        verifyContentPath( resolvedPath, "1234", "content-name", "/This/is/a/test/with/content-key" );

        resolvedPath = resolvePath( "/This/is/a/test/with/content-key/content-name--1234#withfragment" );
        verifyContentPath( resolvedPath, "1234", "content-name", "/This/is/a/test/with/content-key" );

        resolvedPath = resolvePath( "/This/is/a/test/with/content-key/content-name--1234/_window/mywindow" );
        verifyContentPath( resolvedPath, "1234", "content-name", "/This/is/a/test/with/content-key" );

        resolvedPath = resolvePath( "/This/is/a/test/with/content-key/content-name--1234/_window/mywindow?param=value" );
        verifyContentPath( resolvedPath, "1234", "content-name", "/This/is/a/test/with/content-key" );

        resolvedPath = resolvePath( "/This/is/a/test/with/content-key/content-name--1234/_window/mywindow?param=value#fragment" );
        verifyContentPath( resolvedPath, "1234", "content-name", "/This/is/a/test/with/content-key" );
    }

    @Test
    public void testOldTypeContentPath()
    {
        ContentPath resolvedPath = resolvePath( "/This/is/a/test/with/content-key/content-name.1234.cms" );
        verifyContentPath( resolvedPath, "1234", "content-name", "/This/is/a/test/with/content-key" );

        resolvedPath = resolvePath( "/content-name.1234.cms" );
        verifyContentPath( resolvedPath, "1234", "content-name", "/" );

        resolvedPath = resolvePath( "content-name.1234.cms" );
        verifyContentPath( resolvedPath, "1234", "content-name", "/" );
    }

    @Test
    public void testInvalidOldTypeContentPath()
    {
        ContentPath resolvedPath = resolvePath( "/This/is/a/test/with/content-key/content-name.xxxx.cms" );
        assertNull( resolvedPath );

        resolvedPath = resolvePath( "/This/is/a/test/with/content-key/aaa--1234.cms" );
        assertNull( resolvedPath );

        resolvedPath = resolvePath( "/This/is/a/test/with/content-key/aaa.cms" );
        assertNull( resolvedPath );
    }

    private ContentPath resolvePath( String pathAsString )
    {
        return ContentPathResolver.resolveContentPath( new Path( pathAsString ) );
    }

    private void verifyContentPath( ContentPath resolvedPath, String contentKey, String contentName, String pathToMenuItem )
    {
        assertNotNull( resolvedPath );
        assertEquals( contentKey, resolvedPath.getContentKey().toString() );
        assertEquals( contentName, resolvedPath.getContentName() );
        assertEquals( pathToMenuItem, resolvedPath.getPathToMenuItem().toString() );
    }


}
