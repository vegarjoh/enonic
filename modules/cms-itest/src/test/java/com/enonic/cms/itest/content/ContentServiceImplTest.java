/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.itest.content;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
import com.enonic.cms.core.content.ContentService;
import com.enonic.cms.core.content.ContentStatus;
import com.enonic.cms.core.content.ContentVersionEntity;
import com.enonic.cms.core.content.binary.BinaryDataAndBinary;
import com.enonic.cms.core.content.binary.BinaryDataEntity;
import com.enonic.cms.core.content.binary.ContentBinaryDataEntity;
import com.enonic.cms.core.content.command.CreateContentCommand;
import com.enonic.cms.core.content.contentdata.custom.BinaryDataEntry;
import com.enonic.cms.core.content.contentdata.custom.CustomContentData;
import com.enonic.cms.core.content.contentdata.custom.stringbased.TextDataEntry;
import com.enonic.cms.core.content.contenttype.ContentHandlerName;
import com.enonic.cms.core.content.contenttype.ContentTypeConfig;
import com.enonic.cms.core.content.contenttype.ContentTypeConfigParser;
import com.enonic.cms.core.content.contenttype.dataentryconfig.BinaryDataEntryConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.TextDataEntryConfig;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.security.user.UserType;
import com.enonic.cms.core.servlet.ServletRequestAccessor;
import com.enonic.cms.itest.AbstractSpringTest;
import com.enonic.cms.itest.util.AssertTool;
import com.enonic.cms.itest.util.DomainFactory;
import com.enonic.cms.itest.util.DomainFixture;
import com.enonic.cms.store.dao.ContentDao;

import static org.junit.Assert.*;

public class ContentServiceImplTest
    extends AbstractSpringTest
{
    private DomainFactory factory;

    @Autowired
    private DomainFixture fixture;

    @Autowired
    protected ContentDao contentDao;

    @Autowired
    protected ContentService contentService;

    private Element standardConfigEl;

    private Document standardConfig;

    private byte[] dummyBinary = new byte[]{1, 2, 3};

    @Before
    public void before()
        throws IOException, JDOMException
    {

        factory = fixture.getFactory();

        // setup needed common data for each test
        fixture.initSystemData();

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

        standardConfigXml.append( "             <input name=\"myBinaryfile\" type=\"uploadfile\">" );
        standardConfigXml.append( "                 <display>My binaryfile</display>" );
        standardConfigXml.append( "                 <xpath>contentdata/mybinaryfile</xpath>" );
        standardConfigXml.append( "             </input>" );

        standardConfigXml.append( "         </block>" );
        standardConfigXml.append( "     </form>" );
        standardConfigXml.append( "</config>" );
        standardConfigEl = JDOMUtil.parseDocument( standardConfigXml.toString() ).getRootElement();
        standardConfig = XMLDocumentFactory.create( standardConfigXml.toString() ).getAsJDOMDocument();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr( "127.0.0.1" );
        ServletRequestAccessor.setRequest( request );

    }

    @Test
    public void testCreateContentWithBinary()
    {
        fixture.createAndStoreUserAndUserGroup( "testuser", "testuser fullname", UserType.NORMAL, "testuserstore" );
        fixture.save( factory.createContentHandler( "Custom content", ContentHandlerName.CUSTOM.getHandlerClassShortName() ) );
        fixture.save( factory.createContentType( "MyContentType", ContentHandlerName.CUSTOM.getHandlerClassShortName(), standardConfig ) );
        fixture.save( factory.createUnit( "MyUnit" ) );
        fixture.save( factory.createCategory( "MyCategory", null, "MyContentType", "MyUnit", "testuser", "testuser" ) );
        fixture.save( factory.createCategoryAccessForUser( "MyCategory", "testuser", "read, create, approve" ) );
        fixture.flushAndClearHibernateSession();

        UserEntity runningUser = fixture.findUserByName( "testuser" );

        ContentEntity content = new ContentEntity();
        content.setLanguage( fixture.findLanguageByCode( "en" ) );
        content.setCategory( fixture.findCategoryByName( "MyCategory" ) );
        content.setOwner( fixture.findUserByName( "testuser" ) );
        content.setPriority( 0 );
        content.setName( "testcontet" );

        ContentVersionEntity version = new ContentVersionEntity();
        version.setModifiedBy( fixture.findUserByName( "testuser" ) );
        version.setStatus( ContentStatus.DRAFT );
        version.setContent( content );

        ContentTypeConfig contentTypeConfig = ContentTypeConfigParser.parse( ContentHandlerName.CUSTOM, standardConfigEl );
        CustomContentData contentData = new CustomContentData( contentTypeConfig );

        TextDataEntryConfig titleConfig = (TextDataEntryConfig) contentData.getInputConfig( "myTitle" );
        BinaryDataEntryConfig binaryConfig = (BinaryDataEntryConfig) contentData.getInputConfig( "myBinaryfile" );
        contentData.add( new TextDataEntry( titleConfig, "title" ) );
        contentData.add( new BinaryDataEntry( binaryConfig, "%0" ) );

        version.setContentData( contentData );
        version.setTitle( contentData.getTitle() );

        CreateContentCommand createContentCommand = new CreateContentCommand();
        createContentCommand.setCreator( runningUser.getKey() );

        createContentCommand.populateCommandWithContentValues( content );
        createContentCommand.populateCommandWithContentVersionValues( version );

        List<BinaryDataAndBinary> binaryDatas = new ArrayList<BinaryDataAndBinary>();
        binaryDatas.add( factory.createBinaryDataAndBinary( "dummyBinary", dummyBinary ) );
        createContentCommand.setBinaryDatas( binaryDatas );
        createContentCommand.setUseCommandsBinaryDataToAdd( true );

        ContentKey contenKey = contentService.createContent( createContentCommand );

        fixture.flushAndClearHibernateSession();

        ContentEntity persistedContent = contentDao.findByKey( contenKey );
        assertNotNull( persistedContent );
        ContentVersionEntity persistedVersion = persistedContent.getMainVersion();

        // verify content binary data
        Set<ContentBinaryDataEntity> contentBinaryDatas = persistedVersion.getContentBinaryData();
        assertEquals( 1, contentBinaryDatas.size() );
        ContentBinaryDataEntity contentBinaryData = contentBinaryDatas.iterator().next();
        assertNull( contentBinaryData.getLabel() );

        // verify binary data
        BinaryDataEntity binaryData = contentBinaryData.getBinaryData();
        assertEquals( "dummyBinary", binaryData.getName() );

        // verify binary
        // BinaryEntity binary = binaryDao.findByKey( binaryData.getBinaryDataKey() );
        // assertArrayEquals( dummyBinary, binary.getData() );

        CustomContentData peristedContentData = (CustomContentData) persistedVersion.getContentData();

        // verify binary data entry in content data
        List<BinaryDataEntry> binaryDataEntryList = peristedContentData.getBinaryDataEntryList();
        assertEquals( 1, binaryDataEntryList.size() );
        BinaryDataEntry binaryDataEntry = binaryDataEntryList.get( 0 );
        assertEquals( binaryData.getBinaryDataKey().toInt(), binaryDataEntry.getExistingBinaryKey().intValue() );
    }


    @Test
    public void testCreateContent_TitleIsSaved()
    {
        fixture.createAndStoreUserAndUserGroup( "testuser", "testuser fullname", UserType.NORMAL, "testuserstore" );
        fixture.save( factory.createContentHandler( "Custom content", ContentHandlerName.CUSTOM.getHandlerClassShortName() ) );
        fixture.save( factory.createContentType( "MyContentType", ContentHandlerName.CUSTOM.getHandlerClassShortName(), standardConfig ) );
        fixture.save( factory.createUnit( "MyUnit" ) );
        fixture.save( factory.createCategory( "MyCategory", null, "MyContentType", "MyUnit", "testuser", "testuser" ) );
        fixture.save( factory.createCategoryAccessForUser( "MyCategory", "testuser", "read, create, approve" ) );
        fixture.flushAndClearHibernateSession();

        UserEntity runningUser = fixture.findUserByName( "testuser" );

        ContentEntity content = new ContentEntity();
        content.setLanguage( fixture.findLanguageByCode( "en" ) );
        content.setCategory( fixture.findCategoryByName( "MyCategory" ) );
        content.setOwner( fixture.findUserByName( "testuser" ) );
        content.setPriority( 0 );
        content.setName( "testcontent" );

        ContentVersionEntity version = new ContentVersionEntity();
        version.setModifiedBy( fixture.findUserByName( "testuser" ) );
        version.setStatus( ContentStatus.DRAFT );
        version.setContent( content );

        ContentTypeConfig contentTypeConfig = ContentTypeConfigParser.parse( ContentHandlerName.CUSTOM, standardConfigEl );
        CustomContentData contentData = new CustomContentData( contentTypeConfig );

        TextDataEntryConfig titleConfig = (TextDataEntryConfig) contentData.getInputConfig( "myTitle" );
        TextDataEntryConfig subElementConfig = (TextDataEntryConfig) contentData.getInputConfig( "myTitleInSubElement" );
        contentData.add( new TextDataEntry( titleConfig, "test title" ) );
        contentData.add( new TextDataEntry( subElementConfig, "test subtitle" ) );

        version.setContentData( contentData );
        version.setTitle( contentData.getTitle() );

        CreateContentCommand createContentCommand = new CreateContentCommand();
        createContentCommand.setCreator( runningUser.getKey() );

        createContentCommand.populateCommandWithContentValues( content );
        createContentCommand.populateCommandWithContentVersionValues( version );

        List<BinaryDataAndBinary> binaryDatas = new ArrayList<BinaryDataAndBinary>();

        createContentCommand.setBinaryDatas( binaryDatas );
        createContentCommand.setUseCommandsBinaryDataToAdd( true );

        ContentKey contenKey = contentService.createContent( createContentCommand );

        fixture.flushAndClearHibernateSession();

        ContentEntity persistedContent = contentDao.findByKey( contenKey );
        assertNotNull( persistedContent );

        ContentVersionEntity persistedVersion = persistedContent.getMainVersion();
        assertEquals( "test title", persistedVersion.getTitle() );

        Document contentDataXml = persistedVersion.getContentDataAsJDomDocument();
        AssertTool.assertSingleXPathValueEquals( "/contentdata/mytitle", contentDataXml, "test title" );
        AssertTool.assertSingleXPathValueEquals( "/contentdata/subelement/mytitle", contentDataXml, "test subtitle" );

        CustomContentData peristedContentData = (CustomContentData) persistedVersion.getContentData();
        assertEquals( "test title", peristedContentData.getTitle() );
    }

}
