/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.itest.content;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

import com.enonic.cms.framework.util.JDOMUtil;
import com.enonic.cms.framework.xml.XMLDocumentFactory;

import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.ContentNameValidator;
import com.enonic.cms.core.content.ContentService;
import com.enonic.cms.core.content.ContentStatus;
import com.enonic.cms.core.content.CreateContentException;
import com.enonic.cms.core.content.command.CreateContentCommand;
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

public class ContentServiceImpl_createContentTest
    extends AbstractSpringTest
{
    @Autowired
    private ContentDao contentDao;

    @Autowired
    private ContentService contentService;


    private DomainFactory factory;

    @Autowired
    private DomainFixture fixture;

    private Element standardConfigEl;

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
        standardConfigEl = JDOMUtil.parseDocument( standardConfigXml.toString() ).getRootElement();
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


    private CreateContentCommand createCreateContentCommand( ContentStatus status )
    {
        UserEntity runningUser = fixture.findUserByName( "testuser" );

        ContentTypeEntity contentType = fixture.findContentTypeByName( "MyContentType" );
        CustomContentData contentData = new CustomContentData( contentType.getContentTypeConfig() );
        TextDataEntryConfig titleConfig = new TextDataEntryConfig( "myTitle", true, "Tittel", "contentdata/mytitle" );
        TextDataEntryConfig subElementConfig =
            new TextDataEntryConfig( "myTitleInSubElement", false, "My title in sub element", "contentdata/subelement/mytitle" );
        contentData.add( new TextDataEntry( titleConfig, "test title" ) );
        contentData.add( new TextDataEntry( subElementConfig, "test subtitle" ) );

        CreateContentCommand createContentCommand = new CreateContentCommand();
        createContentCommand.setCreator( runningUser );
        createContentCommand.setLanguage( fixture.findLanguageByCode( "en" ) );
        createContentCommand.setCategory( fixture.findCategoryByName( "MyCategory" ) );
        createContentCommand.setPriority( 0 );
        createContentCommand.setStatus( status );
        createContentCommand.setContentData( contentData );
        createContentCommand.setContentName( "testcontent_" + status );

        return createContentCommand;
    }


    @Test
    public void testVersionRelationOnCreateContent()
    {
        CreateContentCommand command = createCreateContentCommand( ContentStatus.DRAFT );

        ContentKey contentKey = contentService.createContent( command );

        ContentEntity persistedContent = contentDao.findByKey( contentKey );

        assertNotNull( persistedContent );
        assertNotNull( persistedContent.getDraftVersion() );
        assertNotNull( persistedContent.getVersions() );
        assertEquals( "Content.versions should have one entry", 1, persistedContent.getVersionCount() );
        assertEquals( persistedContent.getDraftVersion(), persistedContent.getVersions().get( 0 ) );

    }

    @Test
    public void testTimestampSetOnCreatedContent()
    {
        Date startTime = Calendar.getInstance().getTime();

        CreateContentCommand command = createCreateContentCommand( ContentStatus.DRAFT );

        ContentKey contentKey = contentService.createContent( command );

        ContentEntity persistedContent = contentDao.findByKey( contentKey );

        assertNotNull( persistedContent.getTimestamp() );
        assertTrue( startTime.compareTo( persistedContent.getTimestamp() ) < 0 );
    }

    @Test
    public void impossible_to_create_content_as_snapshot()
    {
        CreateContentCommand command = createCreateContentCommand( ContentStatus.SNAPSHOT );

        try
        {
            contentService.createContent( command );
            fail( "Expected exception" );
        }
        catch ( Throwable e )
        {
            assertTrue( e instanceof CreateContentException );
            assertTrue( e.getMessage().contains( "SNAPSHOT" ) );
        }

    }

    @Test
    public void testCreateContentMaximumNameLength()
    {
        Date startTime = Calendar.getInstance().getTime();

        CreateContentCommand command = createCreateContentCommand( ContentStatus.DRAFT );
        command.setContentName( StringUtils.repeat( "x", ContentNameValidator.CONTENT_NAME_MAX_LENGTH ) );

        ContentKey contentKey = contentService.createContent( command );

        ContentEntity persistedContent = contentDao.findByKey( contentKey );

        assertNotNull( persistedContent.getTimestamp() );
        assertTrue( startTime.compareTo( persistedContent.getTimestamp() ) <= 0 );
    }

    @Test
    public void testCreateContentNameTooLong()
    {
        CreateContentCommand command = createCreateContentCommand( ContentStatus.DRAFT );
        command.setContentName( StringUtils.repeat( "x", ContentNameValidator.CONTENT_NAME_MAX_LENGTH + 1 ) );
        try
        {
            contentService.createContent( command );
            fail( "Expected exception" );
        }
        catch ( Throwable e )
        {
            assertTrue( e instanceof CreateContentException );
            assertTrue( e.getMessage().toLowerCase().contains( "too long" ) );
        }
    }

}
