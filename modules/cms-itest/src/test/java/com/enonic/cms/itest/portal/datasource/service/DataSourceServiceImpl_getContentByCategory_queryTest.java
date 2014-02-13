/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.itest.portal.datasource.service;

import org.jdom.Document;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

import com.enonic.cms.framework.xml.XMLDocument;
import com.enonic.cms.framework.xml.XMLDocumentFactory;

import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.ContentService;
import com.enonic.cms.core.content.ContentStatus;
import com.enonic.cms.core.content.command.AssignContentCommand;
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
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.servlet.ServletRequestAccessor;
import com.enonic.cms.core.time.MockTimeService;
import com.enonic.cms.itest.AbstractSpringTest;
import com.enonic.cms.itest.util.AssertTool;
import com.enonic.cms.itest.util.DomainFactory;
import com.enonic.cms.itest.util.DomainFixture;
import com.enonic.cms.store.dao.UserDao;

import static org.junit.Assert.*;

public class DataSourceServiceImpl_getContentByCategory_queryTest
    extends AbstractSpringTest
{
    private DomainFactory factory;

    @Autowired
    private DomainFixture fixture;

    @Autowired
    private UserDao userDao;

    private DataSourceServiceImpl dataSourceService;

    @Autowired
    private ContentService contentService;


    @Before
    public void setUp()
    {

        factory = fixture.getFactory();

        // setup needed common data for each test
        fixture.initSystemData();

        fixture.save( factory.createContentHandler( "Custom content", ContentHandlerName.CUSTOM.getHandlerClassShortName() ) );

        MockHttpServletRequest httpRequest = new MockHttpServletRequest( "GET", "/" );
        ServletRequestAccessor.setRequest( httpRequest );

        dataSourceService = new DataSourceServiceImpl();
        dataSourceService.setContentService( contentService );
        dataSourceService.setTimeService( new MockTimeService( new DateTime( 2010, 7, 1, 12, 0, 0, 0 ) ) );
        dataSourceService.setUserDao( userDao );
    }

    @Test
    public void query_content_on_qualifiedName()
    {
        // setup content type
        ContentTypeConfigBuilder ctyconf = new ContentTypeConfigBuilder( "Person", "name" );
        ctyconf.startBlock( "Person" );
        ctyconf.addInput( "name", "text", "contentdata/name", "Name", true );
        ctyconf.endBlock();
        Document configAsXmlBytes = XMLDocumentFactory.create( ctyconf.toString() ).getAsJDOMDocument();

        // setup content type, unit category, users, and rights
        fixture.save(
            factory.createContentType( "MyContentType", ContentHandlerName.CUSTOM.getHandlerClassShortName(), configAsXmlBytes ) );
        fixture.save( factory.createUnit( "MyUnit", "en" ) );
        fixture.save(
            factory.createCategory( "MyCategory", null, "MyContentType", "MyUnit", User.ANONYMOUS_UID, User.ANONYMOUS_UID, false ) );
        fixture.createAndStoreNormalUserWithUserGroup( "content-creator", "Creator", "testuserstore" );
        fixture.createAndStoreNormalUserWithUserGroup( "content-querier", "Creator", "testuserstore" );
        fixture.save( factory.createCategoryAccessForUser( "MyCategory", "content-creator", "read, create, approve, admin_browse" ) );
        fixture.save( factory.createCategoryAccessForUser( "MyCategory", "content-querier", "read, admin_browse" ) );

        fixture.flushAndClearHibernateSession();
        fixture.flushIndexTransaction();

        // setup content assigned to content-creator
        CustomContentData contentData = new CustomContentData( fixture.findContentTypeByName( "MyContentType" ).getContentTypeConfig() );
        contentData.add( new TextDataEntry( contentData.getInputConfig( "name" ), "Test Dummy" ) );
        ContentKey expectedContentKey = contentService.createContent(
            createCreateContentCommand( "MyCategory", "content-creator", ContentStatus.APPROVED, new DateTime( 2020, 1, 1, 0, 0, 0, 0 ),
                                        "content-creator", contentData, new DateTime( 2010, 1, 1, 0, 0, 0, 0 ), null ) );
        fixture.flushIndexTransaction();

        UserEntity contentCreator = fixture.findUserByName( "content-creator" );

        AssignContentCommand assignCommand = new AssignContentCommand();
        assignCommand.setAssigneeKey( contentCreator.getKey() );
        assignCommand.setAssignerKey( contentCreator.getKey() );
        assignCommand.setContentKey( expectedContentKey );

        contentService.assignContent( assignCommand );
        fixture.flushIndexTransaction();

        // setup another content assigned to some one else
        contentService.createContent(
            createCreateContentCommand( "MyCategory", User.ROOT_UID, ContentStatus.APPROVED, new DateTime( 2020, 1, 1, 0, 0, 0, 0 ),
                                        User.ROOT_UID, contentData, new DateTime( 2010, 1, 1, 0, 0, 0, 0 ), null ) );
        fixture.flushIndexTransaction();

        // setup: verify that 2 content is created
        assertEquals( 2, fixture.countAllContent() );

        // exercise
        DataSourceContext context = new DataSourceContext();
        context.setUser( fixture.findUserByName( "content-querier" ) );
        int[] categoryKeys = new int[]{fixture.findCategoryByName( "MyCategory" ).getKey().toInt()};
        int levels = 1;
        String query = "assignee/qualifiedName = '" + fixture.findUserByName( "content-creator" ).getQualifiedName().toString() + "'";
        String orderyBy = "";
        int index = 0;
        int count = 100;
        boolean includeData = false;
        int childrenLevel = 0;
        int parentLevel = 0;

        XMLDocument xmlDocResult =
            dataSourceService.getContentByCategory( context, categoryKeys, levels, query, orderyBy, index, count, includeData,
                                                    childrenLevel, parentLevel, false, null );

        // verify
        AssertTool.assertXPathEquals( "/contents/content/@key", xmlDocResult.getAsJDOMDocument(),
                                      new String[]{expectedContentKey.toString()} );
    }


    @Test
    public void query_content_with_filterOnUser()
    {
        // setup content type
        ContentTypeConfigBuilder ctyconf = new ContentTypeConfigBuilder( "Person", "name" );
        ctyconf.startBlock( "Person" );
        ctyconf.addInput( "name", "text", "contentdata/name", "Name", true );
        ctyconf.endBlock();
        Document configAsXmlBytes = XMLDocumentFactory.create( ctyconf.toString() ).getAsJDOMDocument();

        // setup content type, unit category, users, and rights
        fixture.save(
            factory.createContentType( "MyContentType", ContentHandlerName.CUSTOM.getHandlerClassShortName(), configAsXmlBytes ) );
        fixture.save( factory.createUnit( "MyUnit", "en" ) );
        fixture.save(
            factory.createCategory( "MyCategory", null, "MyContentType", "MyUnit", User.ANONYMOUS_UID, User.ANONYMOUS_UID, false ) );
        fixture.createAndStoreNormalUserWithUserGroup( "content-creator", "Creator", "testuserstore" );
        fixture.createAndStoreNormalUserWithUserGroup( "content-querier", "Creator", "testuserstore" );
        fixture.save( factory.createCategoryAccessForUser( "MyCategory", "content-creator", "read, create, approve, admin_browse" ) );
        fixture.save( factory.createCategoryAccessForUser( "MyCategory", "content-querier", "read, create, approve, admin_browse" ) );

        fixture.flushAndClearHibernateSession();
        fixture.flushIndexTransaction();

        // setup content assigned to content-creator
        CustomContentData contentData = new CustomContentData( fixture.findContentTypeByName( "MyContentType" ).getContentTypeConfig() );
        contentData.add( new TextDataEntry( contentData.getInputConfig( "name" ), "Test Dummy" ) );
        contentService.createContent(
            createCreateContentCommand( "MyCategory", "content-creator", ContentStatus.APPROVED, new DateTime( 2020, 1, 1, 0, 0, 0, 0 ),
                                        "content-creator", contentData, new DateTime( 2010, 1, 1, 0, 0, 0, 0 ), null ) );

        // setup another content assigned to some one else
        contentService.createContent(
            createCreateContentCommand( "MyCategory", User.ROOT_UID, ContentStatus.APPROVED, new DateTime( 2020, 1, 1, 0, 0, 0, 0 ),
                                        User.ROOT_UID, contentData, new DateTime( 2010, 1, 1, 0, 0, 0, 0 ), null ) );

        // Add content with owner = content-querier
        ContentKey expectedContentKey = contentService.createContent(
            createCreateContentCommand( "MyCategory", "content-querier", ContentStatus.APPROVED, new DateTime( 2020, 1, 1, 0, 0, 0, 0 ),
                                        User.ROOT_UID, contentData, new DateTime( 2010, 1, 1, 0, 0, 0, 0 ), null ) );

        fixture.flushIndexTransaction();

        // setup: verify that 2 content is created
        assertEquals( 3, fixture.countAllContent() );

        // exercise
        DataSourceContext context = new DataSourceContext();
        context.setUser( fixture.findUserByName( "content-querier" ) );
        int[] categoryKeys = new int[]{fixture.findCategoryByName( "MyCategory" ).getKey().toInt()};
        int levels = 1;
        String query = "status = 2";
        String orderyBy = "";
        int index = 0;
        int count = 100;
        boolean includeData = false;
        int childrenLevel = 0;
        int parentLevel = 0;
        boolean filterOnUser = true;

        XMLDocument xmlDocResult =
            dataSourceService.getContentByCategory( context, categoryKeys, levels, query, orderyBy, index, count, includeData,
                                                    childrenLevel, parentLevel, filterOnUser, null );

        // verify
        AssertTool.assertXPathEquals( "/contents/content/@key", xmlDocResult.getAsJDOMDocument(),
                                      new String[]{expectedContentKey.toString()} );
    }


    @Test(expected = DataSourceException.class)
    public void testIndexGreaterThanZeroRequirement()
    {

        // setup content type
        ContentTypeConfigBuilder ctyconf = new ContentTypeConfigBuilder( "Person", "name" );
        ctyconf.startBlock( "Person" );
        ctyconf.addInput( "name", "text", "contentdata/name", "Name", true );
        ctyconf.endBlock();
        Document configAsXmlBytes = XMLDocumentFactory.create( ctyconf.toString() ).getAsJDOMDocument();

        // setup content type, unit category, users, and rights
        fixture.save(
            factory.createContentType( "MyContentType", ContentHandlerName.CUSTOM.getHandlerClassShortName(), configAsXmlBytes ) );
        fixture.save( factory.createUnit( "MyUnit", "en" ) );
        fixture.save(
            factory.createCategory( "MyCategory", null, "MyContentType", "MyUnit", User.ANONYMOUS_UID, User.ANONYMOUS_UID, false ) );
        fixture.createAndStoreNormalUserWithUserGroup( "content-creator", "Creator", "testuserstore" );
        fixture.createAndStoreNormalUserWithUserGroup( "content-querier", "Creator", "testuserstore" );
        fixture.save( factory.createCategoryAccessForUser( "MyCategory", "content-creator", "read, create, approve, admin_browse" ) );
        fixture.save( factory.createCategoryAccessForUser( "MyCategory", "content-querier", "read, create, approve, admin_browse" ) );

        fixture.flushAndClearHibernateSession();
        fixture.flushIndexTransaction();

        // setup content assigned to content-creator
        CustomContentData contentData = new CustomContentData( fixture.findContentTypeByName( "MyContentType" ).getContentTypeConfig() );
        contentData.add( new TextDataEntry( contentData.getInputConfig( "name" ), "Test Dummy" ) );
        contentService.createContent(
            createCreateContentCommand( "MyCategory", "content-creator", ContentStatus.APPROVED, new DateTime( 2020, 1, 1, 0, 0, 0, 0 ),
                                        "content-creator", contentData, new DateTime( 2010, 1, 1, 0, 0, 0, 0 ), null ) );

        // setup another content assigned to some one else
        contentService.createContent(
            createCreateContentCommand( "MyCategory", User.ROOT_UID, ContentStatus.APPROVED, new DateTime( 2020, 1, 1, 0, 0, 0, 0 ),
                                        User.ROOT_UID, contentData, new DateTime( 2010, 1, 1, 0, 0, 0, 0 ), null ) );

        // Add content with owner = content-querier
        ContentKey expectedContentKey = contentService.createContent(
            createCreateContentCommand( "MyCategory", "content-querier", ContentStatus.APPROVED, new DateTime( 2020, 1, 1, 0, 0, 0, 0 ),
                                        User.ROOT_UID, contentData, new DateTime( 2010, 1, 1, 0, 0, 0, 0 ), null ) );

        fixture.flushIndexTransaction();

        DataSourceContext context = new DataSourceContext();
        context.setUser( fixture.findUserByName( "content-querier" ) );
        int[] categoryKeys = new int[]{fixture.findCategoryByName( "MyCategory" ).getKey().toInt()};
        int levels = 1;
        String query = "status = 2";
        String orderyBy = "";
        int index = -1;
        int count = 100;
        boolean includeData = false;
        int childrenLevel = 0;
        int parentLevel = 0;
        boolean filterOnUser = true;

        XMLDocument xmlDocResult =
            dataSourceService.getContentByCategory( context, categoryKeys, levels, query, orderyBy, index, count, includeData,
                                                    childrenLevel, parentLevel, filterOnUser, null );
    }


    private CreateContentCommand createCreateContentCommand( String categoryName, String creatorUid, ContentStatus contentStatus,
                                                             DateTime dueDate, String assigneeUserName, ContentData contentData,
                                                             DateTime availableFrom, DateTime availableTo )
    {
        CreateContentCommand createContentCommand = new CreateContentCommand();
        createContentCommand.setCategory( fixture.findCategoryByName( categoryName ) );
        createContentCommand.setCreator( fixture.findUserByName( creatorUid ).getKey() );
        createContentCommand.setLanguage( fixture.findLanguageByCode( "en" ) );
        createContentCommand.setStatus( contentStatus );
        createContentCommand.setPriority( 0 );
        createContentCommand.setAccessRightsStrategy( CreateContentCommand.AccessRightsStrategy.INHERIT_FROM_CATEGORY );
        createContentCommand.setContentData( contentData );
        createContentCommand.setContentName( "testcontent" );

        if ( availableFrom != null )
        {
            createContentCommand.setAvailableFrom( availableFrom.toDate() );
        }
        if ( availableTo != null )
        {
            createContentCommand.setAvailableTo( availableTo.toDate() );
        }
        return createContentCommand;
    }
}
