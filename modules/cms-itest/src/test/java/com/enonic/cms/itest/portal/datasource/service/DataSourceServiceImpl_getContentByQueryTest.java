/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.itest.portal.datasource.service;

import org.jdom.Document;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

import com.enonic.cms.framework.xml.XMLDocument;
import com.enonic.cms.framework.xml.XMLDocumentFactory;

import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.ContentService;
import com.enonic.cms.core.content.ContentStatus;
import com.enonic.cms.core.content.command.CreateContentCommand;
import com.enonic.cms.core.content.contentdata.ContentData;
import com.enonic.cms.core.content.contentdata.custom.CustomContentData;
import com.enonic.cms.core.content.contentdata.custom.stringbased.TextDataEntry;
import com.enonic.cms.core.content.contenttype.ContentHandlerName;
import com.enonic.cms.core.content.contenttype.ContentTypeConfigBuilder;
import com.enonic.cms.core.portal.datasource.DataSourceContext;
import com.enonic.cms.core.portal.datasource.DataSourceException;
import com.enonic.cms.core.portal.datasource.service.DataSourceServiceImpl;
import com.enonic.cms.core.security.user.User;
import com.enonic.cms.core.servlet.ServletRequestAccessor;
import com.enonic.cms.core.time.MockTimeService;
import com.enonic.cms.itest.AbstractSpringTest;
import com.enonic.cms.itest.util.AssertTool;
import com.enonic.cms.itest.util.DomainFactory;
import com.enonic.cms.itest.util.DomainFixture;
import com.enonic.cms.store.dao.UserDao;

import static org.junit.Assert.*;

public class DataSourceServiceImpl_getContentByQueryTest
    extends AbstractSpringTest
{

    @Autowired
    private DomainFixture fixture;

    @Autowired
    private UserDao userDao;

    private DataSourceServiceImpl dataSourceService;

    @Autowired
    private ContentService contentService;

    private static final DateTime DATE_TIME_2010_01_01 = new DateTime( 2010, 1, 1, 0, 0, 0, 0 );


    @Before
    public void setUp()
    {

        DomainFactory factory = fixture.getFactory();

        // setup needed common data for each test
        fixture.initSystemData();

        fixture.save( factory.createContentHandler( "Custom content", ContentHandlerName.CUSTOM.getHandlerClassShortName() ) );

        MockHttpServletRequest httpRequest = new MockHttpServletRequest( "GET", "/" );
        ServletRequestAccessor.setRequest( httpRequest );

        dataSourceService = new DataSourceServiceImpl();
        dataSourceService.setContentService( contentService );
        dataSourceService.setTimeService( new MockTimeService( new DateTime( 2010, 7, 1, 12, 0, 0, 0 ) ) );
        dataSourceService.setUserDao( userDao );

        fixture.createAndStoreNormalUserWithUserGroup( "content-creator", "Creator", "testuserstore" );
        fixture.createAndStoreNormalUserWithUserGroup( "content-querier", "Querier", "testuserstore" );

        // setup content type
        ContentTypeConfigBuilder ctyconf = new ContentTypeConfigBuilder( "MyContent", "title" );
        ctyconf.startBlock( "MyContent" );
        ctyconf.addInput( "title", "text", "contentdata/title", "Title", true );
        ctyconf.endBlock();
        Document configAsXmlBytes = XMLDocumentFactory.create( ctyconf.toString() ).getAsJDOMDocument();

        fixture.save(
            factory.createContentType( "MyContentType", ContentHandlerName.CUSTOM.getHandlerClassShortName(), configAsXmlBytes ) );
        fixture.save( factory.createUnit( "MyUnit", "en" ) );
        fixture.save(
            factory.createCategory( "MyCategory", null, "MyContentType", "MyUnit", User.ANONYMOUS_UID, User.ANONYMOUS_UID, false ) );
        fixture.save(
            factory.createCategory( "MyOtherCategory", null, "MyContentType", "MyUnit", User.ANONYMOUS_UID, User.ANONYMOUS_UID, false ) );

        fixture.save( factory.createCategoryAccessForUser( "MyCategory", "content-creator", "read, create, approve, admin_browse" ) );
        fixture.save( factory.createCategoryAccessForUser( "MyCategory", "content-querier", "read, admin_browse" ) );
        fixture.save( factory.createCategoryAccessForUser( "MyOtherCategory", "content-creator", "read, create, approve, admin_browse" ) );
        fixture.save( factory.createCategoryAccessForUser( "MyOtherCategory", "content-querier", "read, admin_browse" ) );

        fixture.flushAndClearHibernateSession();
        fixture.flushIndexTransaction();
    }

    @Ignore
    @Test
    public void content_from_two_different_categories()
    {
        // setup: create same content in two different categories
        CustomContentData contentData = new CustomContentData( fixture.findContentTypeByName( "MyContentType" ).getContentTypeConfig() );
        contentData.add( new TextDataEntry( contentData.getInputConfig( "title" ), "Test title" ) );
        ContentKey content_1 = contentService.createContent( createCreateContentCommand( "MyCategory", contentData, "content-creator" ) );

        ContentKey content_2 =
            contentService.createContent( createCreateContentCommand( "MyOtherCategory", contentData, "content-creator" ) );

        fixture.flushAndClearHibernateSession();
        fixture.flushIndexTransaction();

        // setup: verify that 2 content is created
        assertEquals( 2, fixture.countAllContent() );

        // exercise
        DataSourceContext context = new DataSourceContext();
        context.setUser( fixture.findUserByName( "content-querier" ) );

        String query = "title = 'Test title'";
        String orderyBy = "@key asc";
        int index = 0;
        int count = 10;
        boolean includeData = true;
        int childrenLevel = 0;
        int parentLevel = 0;

        XMLDocument xmlDocResult =
            dataSourceService.getContentByQuery( context, query, orderyBy, index, count, includeData, childrenLevel, parentLevel, null );

        // verify
        Document jdomDocResult = xmlDocResult.getAsJDOMDocument();
        AssertTool.assertSingleXPathValueEquals( "/contents/@totalcount", jdomDocResult, "2" );
        AssertTool.assertXPathEquals( "/contents/content/@key", jdomDocResult, content_1, content_2 );
    }

    @Test(expected = DataSourceException.class)
    public void testIndexGreaterThanZeroRequirement()
    {
        DataSourceContext context = new DataSourceContext();
        context.setUser( fixture.findUserByName( "content-querier" ) );

        String query = "title = 'Test title'";
        String orderyBy = "@key asc";
        int index = -1;
        int count = 10;
        boolean includeData = true;
        int childrenLevel = 0;
        int parentLevel = 0;

        XMLDocument xmlDocResult =
            dataSourceService.getContentByQuery( context, query, orderyBy, index, count, includeData, childrenLevel, parentLevel, null );
    }

    private CreateContentCommand createCreateContentCommand( String categoryName, ContentData contentData, String creatorUid )
    {
        CreateContentCommand createContentCommand = new CreateContentCommand();
        createContentCommand.setCategory( fixture.findCategoryByName( categoryName ) );
        createContentCommand.setCreator( fixture.findUserByName( creatorUid ).getKey() );
        createContentCommand.setLanguage( fixture.findLanguageByCode( "en" ) );
        createContentCommand.setStatus( ContentStatus.APPROVED );
        createContentCommand.setPriority( 0 );
        createContentCommand.setAccessRightsStrategy( CreateContentCommand.AccessRightsStrategy.INHERIT_FROM_CATEGORY );
        createContentCommand.setContentData( contentData );
        createContentCommand.setAvailableFrom( DATE_TIME_2010_01_01.toDate() );
        createContentCommand.setAvailableTo( null );
        createContentCommand.setContentName( "testcontent" );
        return createContentCommand;
    }
}
