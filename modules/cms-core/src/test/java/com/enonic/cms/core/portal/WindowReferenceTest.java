/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.portal;

import org.junit.Assert;
import org.junit.Test;

import com.enonic.cms.core.Path;

public class WindowReferenceTest
{
    private WindowReference reference;

    @Test
    public void testParse_without_window()
        throws Exception
    {
        reference = WindowReference.parse( new Path( "/news/products" ) );
        Assert.assertNull( reference );
    }

    @Test
    public void testParse_with_window()
        throws Exception
    {
        reference = WindowReference.parse( new Path( "_window/news/products" ) );
        Assert.assertEquals( reference.getPortletName(), "news" );
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testParse_window_only()
        throws Exception
    {
        reference = WindowReference.parse( new Path( "_window" ) );
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testParse_window_only_withslash()
        throws Exception
    {
        reference = WindowReference.parse( new Path( "_window/" ) );
    }

    @Test
    public void testParse_good_path()
        throws Exception
    {
        reference = WindowReference.parse( new Path( "_window/news/products.txt" ) );
        Assert.assertEquals( reference.getPortletName(), "news" );
    }

    @Test
    public void testParse_path_with_extension()
        throws Exception
    {
        reference = WindowReference.parse( new Path( "_window/my-portlet.json" ) );
        Assert.assertEquals( "my-portlet", reference.getPortletName() );
        Assert.assertEquals( "json", reference.getExtension() );
    }

    @Test
    public void testParse_path_with_spaces()
        throws Exception
    {
        reference = WindowReference.parse( new Path( "/main/internal/test/_window/article show portlet" ) );
        Assert.assertEquals( reference.getPortletName(), "article show portlet" );
    }

    @Test
    public void testParse_path_with_spaces_and_xml_extension()
        throws Exception
    {
        reference = WindowReference.parse( new Path( "/main/internal/test/_window/article show portlet.xml" ) );
        Assert.assertEquals( reference.getPortletName(), "article show portlet" );
    }

    @Test
    public void testParse_long_path_with_spaces()
        throws Exception
    {
        reference = WindowReference.parse( new Path( "/main/internal/test/_window/article show portlet/_window/article show portlet" ) );
        Assert.assertEquals( reference.getPortletName(), "article show portlet" );
    }

    @Test
    public void testParse_long_path_with_spaces_and_extension()
        throws Exception
    {
        reference =
            WindowReference.parse( new Path( "/main/internal/test/_window/article show portlet/_window/article show portlet.xml" ) );
        Assert.assertEquals( reference.getPortletName(), "article show portlet" );
    }

    @Test
    public void getExtension()
        throws Exception
    {
        Assert.assertEquals( "xml", WindowReference.parse( new Path( "/myPage/_window/myWindow.xml" ) ).getExtension() );
        Assert.assertEquals( null, WindowReference.parse( new Path( "/myPage/_window/myWindow" ) ).getExtension() );
        Assert.assertEquals( null, WindowReference.parse( new Path( "/myPage/_window/myWindow." ) ).getExtension() );
        Assert.assertEquals( null, WindowReference.parse( new Path( "/myPage/_window/myWindow. " ) ).getExtension() );
    }

    @Test
    public void hasExtension()
        throws Exception
    {
        Assert.assertEquals( true, WindowReference.parse( new Path( "/myPage/_window/myWindow.xml" ) ).hasExtension() );
        Assert.assertEquals( false, WindowReference.parse( new Path( "/myPage/_window/myWindow" ) ).hasExtension() );
        Assert.assertEquals( false, WindowReference.parse( new Path( "/myPage/_window/myWindow." ) ).hasExtension() );
        Assert.assertEquals( false, WindowReference.parse( new Path( "/myPage/_window/myWindow. " ) ).hasExtension() );
    }

    @Test
    public void testParse_long_path_with_spaces_and_several_extensions()
        throws Exception
    {
        reference = WindowReference.parse( new Path( "/main/internal/test/_window/article show portlet.xml.txt.xml" ) );
        Assert.assertEquals( reference.getPortletName(), "article show portlet.xml.txt" );
    }

    @Test
    public void testParse_long_path_with_spaces_and_several_extensions_and_dot()
        throws Exception
    {
        // removes last dot only !
        reference = WindowReference.parse( new Path( "/main/internal/test/_window/article show portlet.xml.txt.xml." ) );
        Assert.assertEquals( reference.getPortletName(), "article show portlet.xml.txt.xml" );
    }
}
