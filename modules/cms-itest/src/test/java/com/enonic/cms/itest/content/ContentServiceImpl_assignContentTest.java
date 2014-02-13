/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.itest.content;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

import com.enonic.cms.framework.xml.XMLDocumentFactory;

import com.enonic.cms.core.content.AssignContentException;
import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.ContentService;
import com.enonic.cms.core.content.ContentStatus;
import com.enonic.cms.core.content.ContentVersionKey;
import com.enonic.cms.core.content.command.AssignContentCommand;
import com.enonic.cms.core.content.command.CreateContentCommand;
import com.enonic.cms.core.content.command.UnassignContentCommand;
import com.enonic.cms.core.content.command.UpdateContentCommand;
import com.enonic.cms.core.content.contentdata.custom.CustomContentData;
import com.enonic.cms.core.content.contentdata.custom.stringbased.TextDataEntry;
import com.enonic.cms.core.content.contenttype.ContentHandlerName;
import com.enonic.cms.core.content.contenttype.ContentTypeEntity;
import com.enonic.cms.core.content.contenttype.dataentryconfig.TextDataEntryConfig;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.security.user.UserType;
import com.enonic.cms.core.servlet.ServletRequestAccessor;
import com.enonic.cms.itest.AbstractSpringTest;
import com.enonic.cms.itest.util.DomainFactory;
import com.enonic.cms.itest.util.DomainFixture;
import com.enonic.cms.store.dao.ContentDao;

import static org.junit.Assert.*;

public class ContentServiceImpl_assignContentTest
    extends AbstractSpringTest
{
    @Autowired
    private ContentDao contentDao;

    @Autowired
    private ContentService contentService;


    private DomainFactory factory;

    @Autowired
    private DomainFixture fixture;

    private Document standardConfig;

    @Before
    public void setUp()
        throws IOException, JDOMException
    {

        factory = fixture.getFactory();

        fixture.initSystemData();

        fixture.createAndStoreUserAndUserGroup( "testuser", "testuser fullname", UserType.NORMAL, "testuserstore" );

        StringBuffer standardConfigXml = new StringBuffer();
        standardConfigXml.append( "<config name=\"MyContentType\" version=\"1.0\">" );
        standardConfigXml.append( "     <form>" );

        standardConfigXml.append( "         <title name=\"myTitle\"/>" );

        standardConfigXml.append( "         <block name=\"TestBlock1\">" );

        standardConfigXml.append( "             <input name=\"myTitle\" required=\"true\" type=\"text\">" );
        standardConfigXml.append( "                 <display>My title</display>" );
        standardConfigXml.append( "                 <xpath>contentdata/mytitle</xpath>" );
        standardConfigXml.append( "             </input>" );

        standardConfigXml.append( "             <input name=\"myTitleInSubElement\" required=\"false\" type=\"text\">" );
        standardConfigXml.append( "                 <display>My title in sub element</display>" );
        standardConfigXml.append( "                 <xpath>contentdata/subelement/mytitle</xpath>" );
        standardConfigXml.append( "             </input>" );

        standardConfigXml.append( "         </block>" );
        standardConfigXml.append( "     </form>" );
        standardConfigXml.append( "</config>" );
        standardConfig = XMLDocumentFactory.create( standardConfigXml.toString() ).getAsJDOMDocument();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr( "127.0.0.1" );
        ServletRequestAccessor.setRequest( request );

        fixture.save( factory.createContentHandler( "Custom content", ContentHandlerName.CUSTOM.getHandlerClassShortName() ) );
        fixture.save( factory.createContentType( "MyContentType", ContentHandlerName.CUSTOM.getHandlerClassShortName(), standardConfig ) );
        fixture.save( factory.createUnit( "MyUnit" ) );
        fixture.save( factory.createCategory( "MyCategory", null, "MyContentType", "MyUnit", "testuser", "testuser" ) );

        fixture.save( factory.createCategoryAccessForUser( "MyCategory", "testuser", "read, create, approve" ) );

        fixture.flushAndClearHibernateSession();
    }

    private CreateContentCommand createCreateContentCommand( Integer status, UserEntity creator )
    {
        ContentTypeEntity contentType = fixture.findContentTypeByName( "MyContentType" );
        CustomContentData contentData = new CustomContentData( contentType.getContentTypeConfig() );
        TextDataEntryConfig titleConfig = new TextDataEntryConfig( "myTitle", true, "Tittel", "contentdata/mytitle" );
        TextDataEntryConfig subElementConfig =
            new TextDataEntryConfig( "myTitleInSubElement", false, "My title in sub element", "contentdata/subelement/mytitle" );
        contentData.add( new TextDataEntry( titleConfig, "test title" ) );
        contentData.add( new TextDataEntry( subElementConfig, "test subtitle" ) );

        CreateContentCommand createContentCommand = new CreateContentCommand();
        createContentCommand.setCreator( creator );
        createContentCommand.setLanguage( fixture.findLanguageByCode( "en" ) );
        createContentCommand.setCategory( fixture.findCategoryByName( "MyCategory" ) );
        createContentCommand.setPriority( 0 );
        createContentCommand.setStatus( ContentStatus.get( status ) );
        createContentCommand.setContentData( contentData );
        createContentCommand.setContentName( "content_test" );

        return createContentCommand;
    }

    private UpdateContentCommand createUpdateContentCommand( ContentKey contentKey, ContentVersionKey versionKey, Integer status,
                                                             boolean asMainVersion, boolean asNewVersion )
    {
        UpdateContentCommand command;
        if ( asNewVersion )
        {
            command = UpdateContentCommand.storeNewVersionEvenIfUnchanged( versionKey );
        }
        else
        {
            command = UpdateContentCommand.updateExistingVersion2( versionKey );
        }

        command.setModifier( fixture.findUserByName( "testuser" ) );
        command.setUpdateAsMainVersion( asMainVersion );

        // Populate command with contentEntity data
        command.setLanguage( fixture.findLanguageByCode( "en" ) );
        command.setStatus( ContentStatus.get( status ) );
        command.setContentKey( contentKey );
        return command;
    }

    @Test
    public void testAssignContent()
    {
        addUser( "testuser2", "read, create, update" );

        UserEntity testUser = fixture.findUserByName( "testuser" );
        UserEntity testUser2 = fixture.findUserByName( "testuser2" );
        Date now = Calendar.getInstance().getTime();

        CreateContentCommand createCommand = createCreateContentCommand( ContentStatus.APPROVED.getKey(), testUser );
        ContentKey contentKey = contentService.createContent( createCommand );

        fixture.flushAndClearHibernateSession();

        ContentEntity persistedContent = contentDao.findByKey( contentKey );

        assertNull( persistedContent.getAssignee() );

        AssignContentCommand assignCommand = new AssignContentCommand();

        assignCommand.setAssigneeKey( testUser2.getKey() );
        assignCommand.setAssignmentDueDate( now );
        assignCommand.setContentKey( persistedContent.getKey() );
        assignCommand.setAssignerKey( testUser.getKey() );

        String assignmentDescription = "This is the assignment comment";
        assignCommand.setAssignmentDescription( assignmentDescription );

        contentService.assignContent( assignCommand );

        fixture.flushAndClearHibernateSession();

        persistedContent = contentDao.findByKey( contentKey );

        assertNotNull( persistedContent );
        assertEquals( assignmentDescription, persistedContent.getAssignmentDescription() );
        assertEquals( testUser2, persistedContent.getAssignee() );
        assertEquals( testUser, persistedContent.getAssigner() );
        assertEquals( now, persistedContent.getAssignmentDueDate() );
    }

    @Test
    public void testNewAssignmentOnCopy()
    {
        addUser( "testuser2", "read, create" );

        UserEntity testUser = fixture.findUserByName( "testuser" );
        UserEntity testUser2 = fixture.findUserByName( "testuser2" );

        Date now = Calendar.getInstance().getTime();

        CreateContentCommand createCommand = createCreateContentCommand( ContentStatus.APPROVED.getKey(), testUser );

        ContentKey contentKey = contentService.createContent( createCommand );

        ContentEntity persistedContent = contentDao.findByKey( contentKey );

        ContentKey contentCopyKey = contentService.copyContent( testUser2, persistedContent, persistedContent.getCategory() );

        fixture.flushAndClearHibernateSession();

        ContentEntity contentCopy = contentDao.findByKey( contentCopyKey );

        assertNotNull( contentCopy );
        assertEquals( testUser2, contentCopy.getAssignee() );
        assertEquals( testUser2, contentCopy.getAssigner() );
        assertNull( contentCopy.getAssignmentDescription() );
        assertNull( contentCopy.getAssignmentDueDate() );
    }

    @Test
    public void testContentTimestampUpdatedOnAssignContent()
    {
        // setup
        UserEntity testUser = fixture.findUserByName( "testuser" );

        CreateContentCommand createCommand = createCreateContentCommand( ContentStatus.APPROVED.getKey(), testUser );
        ContentKey contentKey = contentService.createContent( createCommand );

        ContentEntity persistedContent = contentDao.findByKey( contentKey );
        ContentVersionKey versionKey = persistedContent.getMainVersion().getKey();

        // excersise
        fixture.flushAndClearHibernateSession();

        persistedContent = contentDao.findByKey( contentKey );

        Calendar now = Calendar.getInstance();

        AssignContentCommand assignCommand = new AssignContentCommand();

        assignCommand.setAssigneeKey( testUser.getKey() );
        assignCommand.setAssignmentDueDate( now.getTime() );
        assignCommand.setContentKey( persistedContent.getKey() );
        assignCommand.setAssignerKey( testUser.getKey() );

        String assignmentDescription = "This is the assignment comment";
        assignCommand.setAssignmentDescription( assignmentDescription );

        Date startTime = Calendar.getInstance().getTime();

        contentService.assignContent( assignCommand );

        fixture.flushAndClearHibernateSession();

        persistedContent = contentDao.findByKey( contentKey );

        assertNotNull( persistedContent.getTimestamp() );
        Assert.assertTrue( persistedContent.getTimestamp().compareTo( startTime ) > 0 );
    }

    @Test
    public void testUnassignContent()
    {
        UserEntity testUser = fixture.findUserByName( "testuser" );

        CreateContentCommand createCommand = createCreateContentCommand( ContentStatus.DRAFT.getKey(), testUser );
        ContentKey contentKey = contentService.createContent( createCommand );

        ContentEntity persistedContent = contentDao.findByKey( contentKey );

        assertNull( "No assigne should be set", persistedContent.getAssignee() );

        Calendar now = Calendar.getInstance();

        AssignContentCommand assignCommand = new AssignContentCommand();

        assignCommand.setAssigneeKey( testUser.getKey() );
        assignCommand.setContentKey( persistedContent.getKey() );
        assignCommand.setAssignerKey( testUser.getKey() );
        assignCommand.setAssignmentDueDate( now.getTime() );
        assignCommand.setAssignmentDescription( "This is the assignment comment" );
        assignCommand.setAssignmentDescription( "AssignmentDescription" );

        contentService.assignContent( assignCommand );

        assertNotNull( persistedContent );
        assertEquals( "AssignmentDescription", persistedContent.getAssignmentDescription() );
        assertNotNull( "Assignee should be set", persistedContent.getAssignee() );
        assertNotNull( "DueDate should be set", persistedContent.getAssignmentDueDate() );
        assertEquals( testUser, persistedContent.getAssigner() );

        fixture.flushAndClearHibernateSession();

        persistedContent = contentDao.findByKey( contentKey );

        UnassignContentCommand unassignContentCommand = new UnassignContentCommand();
        unassignContentCommand.setContentKey( persistedContent.getKey() );
        unassignContentCommand.setUnassigner( testUser.getKey() );

        contentService.unassignContent( unassignContentCommand );

        fixture.flushAndClearHibernateSession();

        persistedContent = contentDao.findByKey( contentKey );

        assertNull( "assignmentDescription should not be touched", persistedContent.getAssignmentDescription() );
        assertNull( "Assignee should not be touched", persistedContent.getAssignee() );
        assertNull( "DueDate should not be touched", persistedContent.getAssignmentDueDate() );
        assertNull( "Assigner should not be touched", persistedContent.getAssigner() );
    }

    @Test
    public void testAssignmentValuesKeptForUpdateDraft()
    {
        UserEntity testUser = fixture.findUserByName( "testuser" );

        CreateContentCommand createCommand = createCreateContentCommand( ContentStatus.DRAFT.getKey(), testUser );
        ContentKey contentKey = contentService.createContent( createCommand );

        ContentEntity persistedContent = contentDao.findByKey( contentKey );

        Calendar now = Calendar.getInstance();

        AssignContentCommand assignCommand = new AssignContentCommand();

        assignCommand.setAssigneeKey( testUser.getKey() );
        assignCommand.setContentKey( persistedContent.getKey() );
        assignCommand.setAssignerKey( testUser.getKey() );
        assignCommand.setAssignmentDueDate( now.getTime() );
        assignCommand.setAssignmentDescription( "AssignmentDescription" );

        contentService.assignContent( assignCommand );

        assertNotNull( persistedContent );
        assertEquals( "AssignmentDescription", persistedContent.getAssignmentDescription() );
        assertNotNull( "Assignee should be set", persistedContent.getAssignee() );
        assertNotNull( "DueDate should be set", persistedContent.getAssignmentDueDate() );
        assertEquals( testUser, persistedContent.getAssigner() );

        fixture.flushAndClearHibernateSession();

        persistedContent = contentDao.findByKey( contentKey );

        UpdateContentCommand updateContentCommand =
            createUpdateContentCommand( persistedContent.getKey(), persistedContent.getAssignedVersion().getKey(),
                                        ContentStatus.DRAFT.getKey(), true, false );

        updateContentCommand.populateContentValuesFromContent( persistedContent );

        contentService.updateContent( updateContentCommand );

        fixture.flushAndClearHibernateSession();

        persistedContent = contentDao.findByKey( contentKey );

        assertNotNull( persistedContent );
        assertEquals( "AssignmentDescription", persistedContent.getAssignmentDescription() );
        assertNotNull( "Assignee should be set", persistedContent.getAssignee() );
        assertNotNull( "DueDate should be set", persistedContent.getAssignmentDueDate() );
        assertEquals( testUser, persistedContent.getAssigner() );

    }

    @Test
    public void testDontTouchAssignmentIfNotSpecified()
    {
        UserEntity testUser = fixture.findUserByName( "testuser" );

        CreateContentCommand createCommand = createCreateContentCommand( ContentStatus.DRAFT.getKey(), testUser );
        ContentKey contentKey = contentService.createContent( createCommand );

        ContentEntity persistedContent = contentDao.findByKey( contentKey );

        assertNull( "No assigne should be set", persistedContent.getAssignee() );

        Calendar now = Calendar.getInstance();

        AssignContentCommand assignCommand = new AssignContentCommand();

        assignCommand.setAssigneeKey( testUser.getKey() );
        assignCommand.setContentKey( persistedContent.getKey() );
        assignCommand.setAssignerKey( testUser.getKey() );
        assignCommand.setAssignmentDueDate( now.getTime() );
        assignCommand.setAssignmentDescription( "This is the assignment comment" );
        assignCommand.setAssignmentDescription( "AssignmentDescription" );

        contentService.assignContent( assignCommand );

        assertNotNull( persistedContent );
        assertEquals( "AssignmentDescription", persistedContent.getAssignmentDescription() );
        assertNotNull( "Assignee should be set", persistedContent.getAssignee() );
        assertNotNull( "DueDate should be set", persistedContent.getAssignmentDueDate() );
        assertEquals( testUser, persistedContent.getAssigner() );

        fixture.flushAndClearHibernateSession();

        persistedContent = contentDao.findByKey( contentKey );

        UpdateContentCommand updateContentCommand =
            createUpdateContentCommand( persistedContent.getKey(), persistedContent.getAssignedVersion().getKey(),
                                        ContentStatus.APPROVED.getKey(), true, true );

        contentService.updateContent( updateContentCommand );

        fixture.flushAndClearHibernateSession();

        persistedContent = contentDao.findByKey( contentKey );

        assertNotNull( "assignmentDescription should not be touched", persistedContent.getAssignmentDescription() );
        assertNotNull( "Assignee should not be touched", persistedContent.getAssignee() );
        assertNotNull( "DueDate should not be touched", persistedContent.getAssignmentDueDate() );
        assertNotNull( "Assigner should not be touched", persistedContent.getAssigner() );
    }


    @Test(expected = AssignContentException.class)
    public void testNotAllowedToSetAnonymousAsAssignee()
    {
        fixture.save( factory.createCategoryAccessForUser( "MyCategory", "anonymous", "read, create" ) );

        UserEntity testUser = fixture.findUserByName( "testuser" );

        CreateContentCommand createCommand = createCreateContentCommand( ContentStatus.DRAFT.getKey(), testUser );
        ContentKey contentKey = contentService.createContent( createCommand );

        ContentEntity persistedContent = contentDao.findByKey( contentKey );
        persistedContent.getMainVersion().getKey();

        persistedContent = contentDao.findByKey( contentKey );

        Calendar now = Calendar.getInstance();

        AssignContentCommand assignCommand = new AssignContentCommand();

        UserEntity anonymousUser = fixture.findUserByName( "anonymous" );

        assignCommand.setAssigneeKey( anonymousUser.getKey() );
        assignCommand.setAssignmentDueDate( now.getTime() );
        assignCommand.setContentKey( persistedContent.getKey() );
        assignCommand.setAssignerKey( anonymousUser.getKey() );

        String assignmentDescription = "This is the assignment comment";
        assignCommand.setAssignmentDescription( assignmentDescription );

        contentService.assignContent( assignCommand );
    }


    public void testSetAnonymousAsAssigner()
    {
        fixture.save( factory.createCategoryAccessForUser( "MyCategory", "anonymous", "read, create" ) );

        UserEntity testUser = fixture.findUserByName( "testuser" );

        CreateContentCommand createCommand = createCreateContentCommand( ContentStatus.DRAFT.getKey(), testUser );
        ContentKey contentKey = contentService.createContent( createCommand );

        ContentEntity persistedContent = contentDao.findByKey( contentKey );
        ContentVersionKey versionKey = persistedContent.getMainVersion().getKey();

        persistedContent = contentDao.findByKey( contentKey );

        Calendar now = Calendar.getInstance();

        AssignContentCommand assignCommand = new AssignContentCommand();

        UserEntity anonymousUser = fixture.findUserByName( "anonymous" );

        assignCommand.setAssigneeKey( fixture.findUserByName( "testUser" ).getKey() );
        assignCommand.setAssignmentDueDate( now.getTime() );
        assignCommand.setContentKey( persistedContent.getKey() );
        assignCommand.setAssignerKey( anonymousUser.getKey() );

        String assignmentDescription = "This is the assignment comment";
        assignCommand.setAssignmentDescription( assignmentDescription );

        contentService.assignContent( assignCommand );

        assertEquals( anonymousUser, persistedContent.getAssigner() );
        assertEquals( fixture.findUserByName( "testuser" ), persistedContent.getAssignee() );
    }

    private void addUser( String uid, String accessRightsString )
    {
        fixture.createAndStoreNormalUserWithUserGroup( uid, uid, "testuserstore" );
        fixture.save( factory.createCategoryAccessForUser( "MyCategory", uid, accessRightsString ) );
        fixture.flushAndClearHibernateSession();
    }


}
