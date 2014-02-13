/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Sets;

import junit.framework.TestCase;

import com.enonic.cms.core.Path;
import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.structure.SiteEntity;
import com.enonic.cms.core.structure.SiteKey;
import com.enonic.cms.core.structure.SitePath;
import com.enonic.cms.core.structure.menuitem.ContentHomeEntity;
import com.enonic.cms.core.structure.menuitem.ContentHomeKey;
import com.enonic.cms.core.structure.menuitem.MenuItemEntity;
import com.enonic.cms.core.structure.menuitem.MenuItemKey;
import com.enonic.cms.core.structure.menuitem.MenuItemType;
import com.enonic.cms.core.structure.menuitem.section.SectionContentEntity;
import com.enonic.cms.core.structure.menuitem.section.SectionContentKey;
import com.enonic.cms.store.dao.SectionContentDao;

import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by IntelliJ IDEA.
 * User: rmh
 * Date: Nov 2, 2010
 * Time: 2:09:08 PM
 */
public class PathToContentResolverTest
    extends TestCase
{

    SectionContentDao sectionContentDao = mock( SectionContentDao.class );

    PathToContentResolver pathToContentResolver;


    @Before
    public void setUp()
    {
        pathToContentResolver = new PathToContentResolver( sectionContentDao );
    }

    @Test
    public void testResolvePathToContent_noHome()
    {
        ContentEntity content = new ContentEntity();
        content.setName( "contentName" );
        content.setKey( new ContentKey( "123" ) );

        SiteKey siteKey = new SiteKey( 1 );

        Path resolvedContentPath = pathToContentResolver.resolveContentUrlLocalPath( content, siteKey );

        assertEquals( "/123/contentName", resolvedContentPath.getPathAsString() );
    }

    @Test
    public void testResolvePathToContent_hasSetHome_HomeButNoSection()
    {
        SiteEntity site = new SiteEntity();
        site.setKey( 1 );

        when( sectionContentDao.getCountNamedContentsInSection( isA( MenuItemKey.class ), isA( String.class ) ) ).thenReturn( 1 );

        ContentEntity content = new ContentEntity();
        content.setName( "contentName" );
        content.setKey( new ContentKey( "123" ) );

        MenuItemEntity menuItem = new MenuItemEntity();
        menuItem.setKey( new MenuItemKey( 1 ) );
        menuItem.setName( "test" );
        menuItem.setSite( site );

        ContentHomeEntity contentHome = new ContentHomeEntity();
        contentHome.setContent( content );
        contentHome.setMenuItem( menuItem );
        contentHome.setSite( site );
        contentHome.setKey( new ContentHomeKey( new SiteKey( 1 ), content.getKey() ) );

        content.addContentHome( contentHome );

        SectionContentEntity sectionContentEntity = new SectionContentEntity();
        sectionContentEntity.setMenuItem( menuItem );
        sectionContentEntity.setContent( content );

        menuItem.setSectionContent( Sets.<SectionContentEntity>newHashSet( sectionContentEntity ) );

        SiteKey siteKey = new SiteKey( 1 );

        Path resolvedContentPath = pathToContentResolver.resolveContentUrlLocalPath( content, siteKey );

        assertEquals( "/test/contentName" + PathToContentResolver.CONTENT_PATH_SEPARATOR + content.getKey().toString(),
                      resolvedContentPath.getPathAsString() );
    }

    @Test
    public void testResolvePathToContent_uniqueInSection()
    {
        when( sectionContentDao.getCountNamedContentsInSection( isA( MenuItemKey.class ), isA( String.class ) ) ).thenReturn( 1 );

        SiteKey siteKey = new SiteKey( 1 );
        ContentEntity content = createContentWithContentHomeAndInSection();

        Path resolvedContentPath = pathToContentResolver.resolveContentUrlLocalPath( content, siteKey );

        assertEquals( "/test/contentName", resolvedContentPath.getPathAsString() );
    }

    @Test
    public void testResolvePathToContent_notUniqueInSection()
    {
        when( sectionContentDao.getCountNamedContentsInSection( isA( MenuItemKey.class ), isA( String.class ) ) ).thenReturn( 2 );

        SiteKey siteKey = new SiteKey( 1 );
        ContentEntity content = createContentWithContentHomeAndInSection();

        Path resolvedContentPath = pathToContentResolver.resolveContentUrlLocalPath( content, siteKey );

        assertEquals( "/test/contentName--123", resolvedContentPath.getPathAsString() );
    }


    @Test
    public void testResolvePathToContent_permalink()
    {
        ContentEntity content = new ContentEntity();
        content.setName( "contentName" );
        content.setKey( new ContentKey( "123" ) );

        SiteKey siteKey = new SiteKey( 1 );
        SitePath sitePath = new SitePath( siteKey, "234/blabla" );

        Path resolvedContentPath = pathToContentResolver.resolveContentUrlLocalPathForPermalink( content, sitePath );

        assertEquals( "/123/contentName", resolvedContentPath.getPathAsString() );
    }

    @Test
    public void testResolvePathToContent_permalink_with_windowurl()
    {
        ContentEntity content = new ContentEntity();
        content.setName( "contentName" );
        content.setKey( new ContentKey( "123" ) );

        SiteKey siteKey = new SiteKey( 1 );
        SitePath sitePath = new SitePath( siteKey, "234/blabla/_window/features%3A+xslt+functions%3A+createwindowurl+-+content+page" );

        Path resolvedContentPath = pathToContentResolver.resolveContentUrlLocalPathForPermalink( content, sitePath );

        assertEquals( "/123/contentName/_window/features%3A+xslt+functions%3A+createwindowurl+-+content+page",
                      resolvedContentPath.getPathAsString() );
    }

    private ContentEntity createContentWithContentHomeAndInSection()
    {
        SiteEntity site = new SiteEntity();
        site.setKey( 1 );
        site.setName( "testSite" );

        ContentEntity content = new ContentEntity();
        content.setName( "contentName" );
        content.setKey( new ContentKey( "123" ) );

        MenuItemEntity menuItem = new MenuItemEntity();
        menuItem.setKey( new MenuItemKey( 1 ) );
        menuItem.setName( "test" );
        menuItem.setSite( site );
        menuItem.setType( MenuItemType.SECTION );

        ContentHomeEntity contentHome = new ContentHomeEntity();
        contentHome.setContent( content );
        contentHome.setMenuItem( menuItem );
        contentHome.setSite( site );
        contentHome.setKey( new ContentHomeKey( new SiteKey( 1 ), content.getKey() ) );

        content.addContentHome( contentHome );

        SectionContentEntity sectionContent = new SectionContentEntity();
        sectionContent.setMenuItem( menuItem );
        sectionContent.setContent( content );
        sectionContent.setKey( new SectionContentKey( 1 ) );
        sectionContent.setApproved( true );

        content.addSectionContent( sectionContent );
        menuItem.setSectionContent( Sets.<SectionContentEntity>newHashSet( sectionContent ) );
        return content;
    }


}
