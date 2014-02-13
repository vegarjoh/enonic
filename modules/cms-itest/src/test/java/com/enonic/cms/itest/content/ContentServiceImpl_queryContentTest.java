/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.itest.content;

import org.jdom.Document;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.enonic.cms.framework.xml.XMLDocumentFactory;

import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.ContentService;
import com.enonic.cms.core.content.ContentStatus;
import com.enonic.cms.core.content.category.CategoryKey;
import com.enonic.cms.core.content.command.AssignContentCommand;
import com.enonic.cms.core.content.command.CreateContentCommand;
import com.enonic.cms.core.content.contentdata.ContentData;
import com.enonic.cms.core.content.contentdata.custom.CustomContentData;
import com.enonic.cms.core.content.contentdata.custom.stringbased.TextDataEntry;
import com.enonic.cms.core.content.contenttype.ContentHandlerName;
import com.enonic.cms.core.content.contenttype.ContentTypeConfigBuilder;
import com.enonic.cms.core.content.query.ContentByCategoryQuery;
import com.enonic.cms.core.content.resultset.ContentResultSet;
import com.enonic.cms.core.security.user.User;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.itest.AbstractSpringTest;
import com.enonic.cms.itest.util.DomainFactory;
import com.enonic.cms.itest.util.DomainFixture;

import static org.junit.Assert.*;

public class ContentServiceImpl_queryContentTest
    extends AbstractSpringTest
{

    private DomainFactory factory;

    @Autowired
    private DomainFixture fixture;

    @Autowired
    protected ContentService contentService;


    @Before
    public void setUp()
    {
        factory = fixture.getFactory();

        // setup needed common data for each test
        fixture.initSystemData();

        fixture.save( factory.createContentHandler( "Custom content", ContentHandlerName.CUSTOM.getHandlerClassShortName() ) );
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

        fixture.save(
            factory.createContentType( "MyContentType", ContentHandlerName.CUSTOM.getHandlerClassShortName(), configAsXmlBytes ) );
        fixture.save( factory.createUnit( "MyUnit", "en" ) );
        fixture.save(
            factory.createCategory( "MyCategory", null, "MyContentType", "MyUnit", User.ANONYMOUS_UID, User.ANONYMOUS_UID, false ) );
        fixture.createAndStoreNormalUserWithUserGroup( "content-creator", "Creator", "testuserstore" );
        fixture.save( factory.createCategoryAccessForUser( "MyCategory", "content-creator", "read, create, approve, admin_browse" ) );
        fixture.createAndStoreNormalUserWithUserGroup( "content-querier", "Creator", "testuserstore" );
        fixture.save( factory.createCategoryAccessForUser( "MyCategory", "content-querier", "read, admin_browse" ) );

        fixture.flushAndClearHibernateSession();
        fixture.flushIndexTransaction();

        // setup content assigned to content-creator
        CustomContentData contentData = new CustomContentData( fixture.findContentTypeByName( "MyContentType" ).getContentTypeConfig() );
        contentData.add( new TextDataEntry( contentData.getInputConfig( "name" ), "Test Dummy" ) );
        ContentKey expectedContentKey = contentService.createContent(
            createCreateContentCommand( "MyCategory", "content-creator", ContentStatus.APPROVED, new DateTime( 2020, 1, 1, 0, 0, 0, 0 ),
                                        "content-creator", contentData ) );
        fixture.flushIndexTransaction();

        // setup another content assigned to some one else
        contentService.createContent(
            createCreateContentCommand( "MyCategory", User.ROOT_UID, ContentStatus.APPROVED, new DateTime( 2020, 1, 1, 0, 0, 0, 0 ),
                                        User.ROOT_UID, contentData ) );

        // setup: verify that 2 content is created 
        assertEquals( 2, fixture.countAllContent() );

        UserEntity contentCreator = fixture.findUserByName( "content-creator" );

        AssignContentCommand assignCommand = new AssignContentCommand();
        assignCommand.setAssigneeKey( contentCreator.getKey() );
        assignCommand.setAssignerKey( contentCreator.getKey() );
        assignCommand.setContentKey( expectedContentKey );

        contentService.assignContent( assignCommand );
        fixture.flushIndexTransaction();

        // exercise
        ContentByCategoryQuery contentByCategoryQuery = new ContentByCategoryQuery();
        contentByCategoryQuery.setUser( fixture.findUserByName( "content-querier" ) );
        contentByCategoryQuery.setCategoryKeyFilter( CategoryKey.convertToList( fixture.findCategoryByName( "MyCategory" ).getKey() ), 1 );
        contentByCategoryQuery.setQuery( "assignee/qualifiedName = '" + contentCreator.getQualifiedName().toString() + "'" );

        ContentResultSet resultSet = contentService.queryContent( contentByCategoryQuery );

        // verify
        assertEquals( 1, resultSet.getLength() );
        assertEquals( expectedContentKey, resultSet.getKey( 0 ) );
    }

    private CreateContentCommand createCreateContentCommand( String categoryName, String creatorUid, ContentStatus contentStatus,
                                                             DateTime dueDate, String assigneeUserName, ContentData contentData )
    {
        CreateContentCommand createContentCommand = new CreateContentCommand();
        createContentCommand.setCategory( fixture.findCategoryByName( categoryName ) );
        createContentCommand.setCreator( fixture.findUserByName( creatorUid ).getKey() );
        createContentCommand.setLanguage( fixture.findLanguageByCode( "en" ) );
        createContentCommand.setStatus( contentStatus );
        createContentCommand.setPriority( 0 );
        createContentCommand.setAccessRightsStrategy( CreateContentCommand.AccessRightsStrategy.INHERIT_FROM_CATEGORY );
        createContentCommand.setContentData( contentData );
        createContentCommand.setContentName( "testcontent_" + categoryName + "_" + contentStatus.getName() );
        return createContentCommand;
    }


}