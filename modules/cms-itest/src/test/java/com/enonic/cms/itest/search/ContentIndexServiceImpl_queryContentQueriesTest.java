/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.itest.search;

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.Lists;

import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.category.CategoryKey;
import com.enonic.cms.core.content.contenttype.ContentTypeKey;
import com.enonic.cms.core.content.index.ContentIndexQuery;
import com.enonic.cms.core.content.index.UserDefinedField;
import com.enonic.cms.core.content.resultset.ContentResultSet;
import com.enonic.cms.core.search.query.ContentDocument;
import com.enonic.cms.core.search.query.SimpleText;

import static org.junit.Assert.*;

/**
 * Created by IntelliJ IDEA.
 * User: rmh
 * Date: 1/13/12
 * Time: 12:20 PM
 */
@Ignore("Tests is hanging on team city")
public class ContentIndexServiceImpl_queryContentQueriesTest
    extends ContentIndexServiceTestBase
{
    @Test
    public void testContentQueryWithCategoryFilter()
    {

        ContentDocument doc1 = createContentDocument( new ContentKey( 1 ), new CategoryKey( 101 ), new ContentTypeKey( 10 ), "title1",
                                                      Lists.newArrayList(
                                                          new UserDefinedField( "data/heading", new SimpleText( "title1" ) ) ) );
        contentIndexService.index( doc1 );

        ContentDocument doc2 = createContentDocument( new ContentKey( 2 ), new CategoryKey( 101 ), new ContentTypeKey( 10 ), "title2",
                                                      Lists.newArrayList(
                                                          new UserDefinedField( "data/heading", new SimpleText( "title2" ) ) ) );
        contentIndexService.index( doc2 );

        ContentDocument doc3 = createContentDocument( new ContentKey( 3 ), new CategoryKey( 102 ), new ContentTypeKey( 10 ), "title3",
                                                      Lists.newArrayList(
                                                          new UserDefinedField( "data/heading", new SimpleText( "title3" ) ) ) );
        contentIndexService.index( doc3 );

        flushIndex();

        ContentIndexQuery query = new ContentIndexQuery( "data/heading CONTAINS 'title'", "" );
        query.setCategoryFilter( createCategoryKeyList( 101 ) );
        assertContentResultSetEquals( new int[]{1, 2}, contentIndexService.query( query ) );

        query = new ContentIndexQuery( "data/heading CONTAINS 'title'", "" );
        query.setCategoryFilter( createCategoryKeyList( 102 ) );
        assertContentResultSetEquals( new int[]{3}, contentIndexService.query( query ) );
    }

    @Test
    public void testContentQueryWithContentTypeFilter()
    {

        ContentDocument doc1 = createContentDocument( new ContentKey( 1 ), new CategoryKey( 101 ), new ContentTypeKey( 10 ), "title1",
                                                      Lists.newArrayList(
                                                          new UserDefinedField( "data/heading", new SimpleText( "title1" ) ) ) );
        contentIndexService.index( doc1 );

        ContentDocument doc2 = createContentDocument( new ContentKey( 2 ), new CategoryKey( 101 ), new ContentTypeKey( 10 ), "title2",
                                                      Lists.newArrayList(
                                                          new UserDefinedField( "data/heading", new SimpleText( "title2" ) ) ) );
        contentIndexService.index( doc2 );

        ContentDocument doc3 = createContentDocument( new ContentKey( 3 ), new CategoryKey( 101 ), new ContentTypeKey( 11 ), "title3",
                                                      Lists.newArrayList(
                                                          new UserDefinedField( "data/heading", new SimpleText( "title3" ) ) ) );
        contentIndexService.index( doc3 );

        flushIndex();

        ContentIndexQuery query = new ContentIndexQuery( "data/heading CONTAINS 'title'", "" );
        query.setContentTypeFilter( createContentTypeList( 10 ) );
        assertContentResultSetEquals( new int[]{1, 2}, contentIndexService.query( query ) );

        query = new ContentIndexQuery( "data/heading CONTAINS 'title'", "" );
        query.setContentTypeFilter( createContentTypeList( 11 ) );
        assertContentResultSetEquals( new int[]{3}, contentIndexService.query( query ) );
    }

    @Test
    public void testContentQueryWithCategoryFilterAndComplexLogicalExpression()
    {
        ContentDocument doc1 = createContentDocument( new ContentKey( 1 ), new CategoryKey( 101 ), new ContentTypeKey( 10 ), "title1",
                                                      Lists.newArrayList( new UserDefinedField( "data/a", new SimpleText( "1" ) ),
                                                                          new UserDefinedField( "data/b", new SimpleText( "2" ) ),
                                                                          new UserDefinedField( "data/c", new SimpleText( "3" ) ) ) );

        contentIndexService.index( doc1 );

        ContentDocument doc2 = createContentDocument( new ContentKey( 2 ), new CategoryKey( 101 ), new ContentTypeKey( 11 ), "title2",
                                                      Lists.newArrayList( new UserDefinedField( "data/a", new SimpleText( "2" ) ),
                                                                          new UserDefinedField( "data/b", new SimpleText( "2" ) ),
                                                                          new UserDefinedField( "data/c", new SimpleText( "1" ) ) ) );
        contentIndexService.index( doc2 );

        ContentDocument doc3 = createContentDocument( new ContentKey( 3 ), new CategoryKey( 101 ), new ContentTypeKey( 10 ), "title3",
                                                      Lists.newArrayList( new UserDefinedField( "data/a", new SimpleText( "2" ) ),
                                                                          new UserDefinedField( "data/b", new SimpleText( "1" ) ),
                                                                          new UserDefinedField( "data/c", new SimpleText( "3" ) ) ) );
        contentIndexService.index( doc3 );

        flushIndex();

        ContentIndexQuery query = new ContentIndexQuery( "(data/a = 1 AND data/b = 2)", "" );
        query.setCategoryFilter( createCategoryKeyList( 101 ) );
        assertContentResultSetEquals( new int[]{1}, contentIndexService.query( query ) );

        query = new ContentIndexQuery( "(data/a = 1 AND data/b = 2) OR data/c = 1", "" );
        query.setCategoryFilter( createCategoryKeyList( 101 ) );
        assertContentResultSetEquals( new int[]{1, 2}, contentIndexService.query( query ) );

        query = new ContentIndexQuery( "(data/a = 1 OR data/b = 2) AND data/c = 1", "" );
        query.setCategoryFilter( createCategoryKeyList( 101 ) );
        assertContentResultSetEquals( new int[]{2}, contentIndexService.query( query ) );

        query = new ContentIndexQuery( "data/a = 3 OR data/b = 1 OR data/c = 1", "" );
        query.setCategoryFilter( createCategoryKeyList( 101 ) );
        assertContentResultSetEquals( new int[]{2, 3}, contentIndexService.query( query ) );

        query = new ContentIndexQuery( "(data/a = 2 OR data/b = 2) AND (data/a = 1 OR data/c = 3)", "" );
        query.setCategoryFilter( createCategoryKeyList( 101 ) );
        assertContentResultSetEquals( new int[]{1, 3}, contentIndexService.query( query ) );

        query = new ContentIndexQuery( "(data/a = 2 AND data/b = 2) OR (data/a = 1 AND data/c = 3)", "" );
        query.setCategoryFilter( createCategoryKeyList( 101 ) );
        assertContentResultSetEquals( new int[]{1, 2}, contentIndexService.query( query ) );
    }

    @Test
    public void testContentQueryWithCategoryFilterAndContentTypeNameSearch()
    {
        ContentDocument doc1 = createContentDocument( new ContentKey( 1 ), new CategoryKey( 101 ), new ContentTypeKey( 10 ), "title1",
                                                      Lists.newArrayList(
                                                          new UserDefinedField( "data/heading", new SimpleText( "title1" ) ) ) );
        ;
        doc1.setContentTypeName( "Article3" );
        contentIndexService.index( doc1 );

        ContentDocument doc2 = createContentDocument( new ContentKey( 2 ), new CategoryKey( 101 ), new ContentTypeKey( 10 ), "title2",
                                                      Lists.newArrayList(
                                                          new UserDefinedField( "data/heading", new SimpleText( "title2" ) ) ) );
        doc2.setContentTypeName( "Article3" );
        contentIndexService.index( doc2 );

        ContentDocument doc3 = createContentDocument( new ContentKey( 3 ), new CategoryKey( 101 ), new ContentTypeKey( 11 ), "title3",
                                                      Lists.newArrayList(
                                                          new UserDefinedField( "data/heading", new SimpleText( "title3" ) ) ) );
        doc3.setContentTypeName( "Loooooooooooooong-content-type-name-it-is" );
        contentIndexService.index( doc3 );

        flushIndex();

        ContentIndexQuery query = new ContentIndexQuery( "contenttype = 'Article3'", "" );
        query.setContentTypeFilter( createContentTypeList( 10 ) );
        assertContentResultSetEquals( new int[]{1, 2}, contentIndexService.query( query ) );

        query = new ContentIndexQuery( "contenttype = 'Loooooooooooooong-content-type-name-it-is'", "" );
        query.setContentTypeFilter( createContentTypeList( 11 ) );
        assertContentResultSetEquals( new int[]{3}, contentIndexService.query( query ) );
    }

    @Test
    public void testQueriesOnUserDefinedData()
    {
        setUpStandardTestValues();

        ContentIndexQuery query1 = new ContentIndexQuery( "data/person/age > 9" );
        ContentResultSet res1 = contentIndexService.query( query1 );
        assertEquals( 3, res1.getLength() );

        ContentIndexQuery query2 = new ContentIndexQuery( "data/person/gender = 'male'" );
        ContentResultSet res2 = contentIndexService.query( query2 );
        assertEquals( 3, res2.getLength() );

        ContentIndexQuery query3 = new ContentIndexQuery( "data/person/description LIKE '%description%'" );
        ContentResultSet res3 = contentIndexService.query( query3 );
        assertEquals( 4, res3.getLength() );

        ContentIndexQuery query4 = new ContentIndexQuery( "data/person/gender = 'male' AND data/person/description LIKE '%description%'" );
        ContentResultSet res4 = contentIndexService.query( query4 );
        assertEquals( 3, res4.getLength() );

    }


    @Test
    public void testQueriesOnUserDefinedDataWithDot()
    {
        setUpStandardTestValues();

        ContentIndexQuery query1 = new ContentIndexQuery( "data.person.age > 9" );
        ContentResultSet res1 = contentIndexService.query( query1 );
        assertEquals( 3, res1.getLength() );

        ContentIndexQuery query2 = new ContentIndexQuery( "data.person.gender = 'male'" );
        ContentResultSet res2 = contentIndexService.query( query2 );
        assertEquals( 3, res2.getLength() );
    }

    @Test
    public void testQueriesWithLanguageCode()
    {
        setUpStandardTestValues();

        ContentIndexQuery query1 = new ContentIndexQuery( "language = 'en'" );
        ContentResultSet res1 = contentIndexService.query( query1 );
        assertEquals( 3, res1.getLength() );

        ContentIndexQuery query2 = new ContentIndexQuery( "language = 'fr'" );
        ContentResultSet res2 = contentIndexService.query( query2 );
        assertEquals( 1, res2.getLength() );
    }

    private List<CategoryKey> createCategoryKeyList( Integer... array )
    {
        List<CategoryKey> keys = new ArrayList<CategoryKey>();
        for ( int x : array )
        {
            keys.add( new CategoryKey( x ) );
        }
        return keys;
    }

    private List<ContentTypeKey> createContentTypeList( Integer... array )
    {
        List<ContentTypeKey> keys = new ArrayList<ContentTypeKey>();
        for ( int x : array )
        {
            keys.add( new ContentTypeKey( x ) );
        }
        return keys;
    }


}
