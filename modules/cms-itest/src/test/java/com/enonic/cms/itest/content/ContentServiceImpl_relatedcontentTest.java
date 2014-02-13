/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.itest.content;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.hibernate3.HibernateTemplate;

import com.enonic.cms.framework.util.JDOMUtil;
import com.enonic.cms.framework.xml.XMLDocumentFactory;

import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.ContentService;
import com.enonic.cms.core.content.ContentVersionEntity;
import com.enonic.cms.core.content.binary.BinaryDataAndBinary;
import com.enonic.cms.core.content.command.CreateContentCommand;
import com.enonic.cms.core.content.command.UpdateContentCommand;
import com.enonic.cms.core.content.contentdata.custom.CustomContentData;
import com.enonic.cms.core.content.contentdata.custom.contentkeybased.RelatedContentDataEntry;
import com.enonic.cms.core.content.contentdata.custom.relationdataentrylistbased.RelatedContentsDataEntry;
import com.enonic.cms.core.content.contentdata.custom.stringbased.TextDataEntry;
import com.enonic.cms.core.content.contenttype.ContentHandlerName;
import com.enonic.cms.core.content.contenttype.ContentTypeConfig;
import com.enonic.cms.core.content.contenttype.ContentTypeConfigParser;
import com.enonic.cms.core.content.contenttype.dataentryconfig.RelatedContentDataEntryConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.TextDataEntryConfig;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.servlet.ServletRequestAccessor;
import com.enonic.cms.itest.AbstractSpringTest;
import com.enonic.cms.itest.util.AssertTool;
import com.enonic.cms.itest.util.DomainFactory;
import com.enonic.cms.itest.util.DomainFixture;
import com.enonic.cms.store.dao.ContentDao;
import com.enonic.cms.store.dao.ContentVersionDao;

import static org.junit.Assert.*;

public class ContentServiceImpl_relatedcontentTest
    extends AbstractSpringTest
{
    @Autowired
    private HibernateTemplate hibernateTemplate;

    @Autowired
    private ContentService contentService;

    @Autowired
    private ContentDao contentDao;

    @Autowired
    private ContentVersionDao contentVersionDao;

    private Element configEl;

    private Document config;

    private DomainFactory factory;

    @Autowired
    private DomainFixture fixture;

    @Before
    public void before()
        throws IOException, JDOMException
    {

        factory = fixture.getFactory();

        fixture.initSystemData();

        StringBuffer configXml = new StringBuffer();
        configXml.append( "<config name=\"MyContentType\" version=\"1.0\">" );
        configXml.append( "     <form>" );

        configXml.append( "         <title name=\"myTitle\"/>" );

        configXml.append( "         <block name=\"General\">" );

        configXml.append( "             <input name=\"myTitle\" required=\"true\" type=\"text\">" );
        configXml.append( "                 <display>My title</display>" );
        configXml.append( "                 <xpath>contentdata/mytitle</xpath>" );
        configXml.append( "             </input>" );

        configXml.append( "         </block>" );

        configXml.append( "         <block name=\"Related content\">" );

        configXml.append( "             <input name=\"myMultipleRelatedContent\" type=\"relatedcontent\" multiple=\"true\">" );
        configXml.append( "                 <display>My related content</display>" );
        configXml.append( "                 <xpath>contentdata/myrelatedcontents</xpath>" );
        configXml.append( "             </input>" );

        configXml.append( "             <input name=\"mySoleRelatedContent\" type=\"relatedcontent\" multiple=\"false\">" );
        configXml.append( "                 <display>My sole related content</display>" );
        configXml.append( "                 <xpath>contentdata/mysolerelatedcontent</xpath>" );
        configXml.append( "                 <contenttype name=\"MyContentType\"/>" );
        configXml.append( "             </input>" );

        configXml.append( "         </block>" );
        configXml.append( "     </form>" );
        configXml.append( "</config>" );
        configEl = JDOMUtil.parseDocument( configXml.toString() ).getRootElement();
        config = XMLDocumentFactory.create( configXml.toString() ).getAsJDOMDocument();

        fixture.createAndStoreNormalUserWithUserGroup( "testuser", "testuser fullname", "testuserstore" );
        hibernateTemplate.save( factory.createContentHandler( "Custom content", ContentHandlerName.CUSTOM.getHandlerClassShortName() ) );
        hibernateTemplate.save(
            factory.createContentType( "MyContentType", ContentHandlerName.CUSTOM.getHandlerClassShortName(), config ) );
        hibernateTemplate.save( factory.createUnit( "MyUnit" ) );
        hibernateTemplate.save( factory.createCategory( "MyCategory", null, "MyContentType", "MyUnit", "testuser", "testuser" ) );
        hibernateTemplate.save(
            factory.createCategoryAccess( "MyCategory", fixture.findUserByName( "testuser" ), "read, create, approve" ) );

        hibernateTemplate.flush();
        hibernateTemplate.clear();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr( "127.0.0.1" );
        ServletRequestAccessor.setRequest( request );

    }


    @Test
    public void testCreateContentWithRelatedContent()
    {

        ContentKey relatedContentKey1 = storeSimpleContent( "rel1" );
        ContentKey relatedContentKey2 = storeSimpleContent( "rel2" );
        ContentKey relatedContentKey3 = storeSimpleContent( "rel3" );

        ContentEntity content = factory.createContent( "MyCategory", "en", "testuser", "0", new Date() );
        ContentVersionEntity version = factory.createContentVersion( "0", "testuser" );

        ContentTypeConfig contentTypeConfig = ContentTypeConfigParser.parse( ContentHandlerName.CUSTOM, configEl );
        CustomContentData contentData = new CustomContentData( contentTypeConfig );

        TextDataEntryConfig titleConfig = new TextDataEntryConfig( "myTitle", true, "Tittel", "contentdata/mytitle" );
        contentData.add( new TextDataEntry( titleConfig, "test title" ) );

        RelatedContentDataEntryConfig multipleRelatedContentsConfig =
            (RelatedContentDataEntryConfig) contentTypeConfig.getInputConfig( "myMultipleRelatedContent" );

        contentData.add( new RelatedContentsDataEntry( multipleRelatedContentsConfig ).add(
            new RelatedContentDataEntry( multipleRelatedContentsConfig, relatedContentKey1 ) ).add(
            new RelatedContentDataEntry( multipleRelatedContentsConfig, relatedContentKey2 ) ) );

        RelatedContentDataEntryConfig soleRelatedConfig =
            (RelatedContentDataEntryConfig) contentTypeConfig.getInputConfig( "mySoleRelatedContent" );

        contentData.add( new RelatedContentDataEntry( soleRelatedConfig, relatedContentKey3 ) );

        version.setContentData( contentData );

        UserEntity runningUser = fixture.findUserByName( "testuser" );

        CreateContentCommand createCommand = new CreateContentCommand();
        createCommand.setCreator( runningUser );

        createCommand.populateCommandWithContentValues( content );
        createCommand.populateCommandWithContentVersionValues( version );

        createCommand.setBinaryDatas( new ArrayList<BinaryDataAndBinary>() );
        createCommand.setUseCommandsBinaryDataToAdd( true );

        ContentKey contenKey = contentService.createContent( createCommand );

        hibernateTemplate.flush();
        hibernateTemplate.clear();

        ContentEntity persistedContent = contentDao.findByKey( contenKey );
        assertNotNull( persistedContent );

        ContentVersionEntity persistedVersion = persistedContent.getMainVersion();

        assertEquals( 3, persistedVersion.getRelatedChildren( true ).size() );

        Document contentDataXml = persistedVersion.getContentDataAsJDomDocument();
        AssertTool.assertXPathEquals( "contentdata/myrelatedcontents/content/@key", contentDataXml,
                                      new String[]{relatedContentKey1.toString(), relatedContentKey2.toString()} );
        AssertTool.assertXPathEquals( "contentdata/mysolerelatedcontent/@key", contentDataXml, relatedContentKey3.toString() );
    }

    @Test
    public void testUpdateCurrentVersion()
    {
        ContentKey relatedContentKey1 = storeSimpleContent( "rel1" );
        ContentKey relatedContentKey2 = storeSimpleContent( "rel2" );
        ContentKey relatedContentKey3 = storeSimpleContent( "rel3" );
        ContentKey relatedContentKey4 = storeSimpleContent( "rel4" );
        ContentKey relatedContentKey5 = storeSimpleContent( "rel5" );

        ContentEntity content = factory.createContent( "MyCategory", "en", "testuser", "0", new Date() );
        ContentVersionEntity version = factory.createContentVersion( "0", "testuser" );

        ContentTypeConfig contentTypeConfig = ContentTypeConfigParser.parse( ContentHandlerName.CUSTOM, configEl );
        CustomContentData contentData = new CustomContentData( contentTypeConfig );
        TextDataEntryConfig titleConfig = new TextDataEntryConfig( "myTitle", true, "Tittel", "contentdata/mytitle" );
        contentData.add( new TextDataEntry( titleConfig, "test title" ) );

        RelatedContentDataEntryConfig multipleRelatedContentsConfig =
            (RelatedContentDataEntryConfig) contentTypeConfig.getInputConfig( "myMultipleRelatedContent" );

        contentData.add( new RelatedContentsDataEntry( multipleRelatedContentsConfig ).add(
            new RelatedContentDataEntry( multipleRelatedContentsConfig, relatedContentKey1 ) ).add(
            new RelatedContentDataEntry( multipleRelatedContentsConfig, relatedContentKey2 ) ) );

        RelatedContentDataEntryConfig soleRelatedConfig =
            (RelatedContentDataEntryConfig) contentTypeConfig.getInputConfig( "mySoleRelatedContent" );

        contentData.add( new RelatedContentDataEntry( soleRelatedConfig, relatedContentKey3 ) );

        version.setContentData( contentData );

        UserEntity runningUser = fixture.findUserByName( "testuser" );

        CreateContentCommand createContentCommand = new CreateContentCommand();
        createContentCommand.setCreator( runningUser );

        createContentCommand.populateCommandWithContentValues( content );
        createContentCommand.populateCommandWithContentVersionValues( version );

        createContentCommand.setBinaryDatas( new ArrayList<BinaryDataAndBinary>() );
        createContentCommand.setUseCommandsBinaryDataToAdd( true );

        ContentKey contentKey = contentService.createContent( createContentCommand );

        hibernateTemplate.flush();
        hibernateTemplate.clear();

        ContentEntity persistedContent = contentDao.findByKey( contentKey );
        assertNotNull( persistedContent );

        ContentVersionEntity persistedVersion = persistedContent.getMainVersion();
        assertNotNull( persistedVersion );

        assertEquals( 3, persistedVersion.getRelatedChildren( true ).size() );

        ContentEntity changedContent = factory.createContent( "MyCategory", "en", "testuser", "0", new Date() );
        changedContent.setKey( contentKey );
        ContentVersionEntity changedVersion = factory.createContentVersion( "0", "testuser" );
        changedVersion.setKey( persistedVersion.getKey() );

        CustomContentData changedCD = new CustomContentData( contentTypeConfig );

        TextDataEntryConfig changedTitleConfig = new TextDataEntryConfig( "myTitle", true, "Tittel", "contentdata/mytitle" );
        changedCD.add( new TextDataEntry( changedTitleConfig, "changed title" ) );

        changedCD.add( new RelatedContentsDataEntry( multipleRelatedContentsConfig ).add(
            new RelatedContentDataEntry( multipleRelatedContentsConfig, relatedContentKey3 ) ).add(
            new RelatedContentDataEntry( multipleRelatedContentsConfig, relatedContentKey5 ) ) );

        changedCD.add( new RelatedContentDataEntry( soleRelatedConfig, relatedContentKey4 ) );

        changedVersion.setContentData( changedCD );

        UpdateContentCommand updateContentCommand = UpdateContentCommand.updateExistingVersion2( persistedVersion.getKey() );
        updateContentCommand.setModifier( runningUser );
        updateContentCommand.setUpdateAsMainVersion( false );

        updateContentCommand.populateContentValuesFromContent( persistedContent );
        updateContentCommand.populateContentVersionValuesFromContentVersion( changedVersion );

        contentService.updateContent( updateContentCommand );

        hibernateTemplate.flush();
        hibernateTemplate.clear();

        ContentEntity contentAfterUpdate = contentDao.findByKey( contentKey );
        ContentVersionEntity versionAfterUpdate = contentVersionDao.findByKey( persistedVersion.getKey() );

        Document contentDataXmlAfterUpdate = versionAfterUpdate.getContentDataAsJDomDocument();

        AssertTool.assertXPathEquals( "/contentdata/mysolerelatedcontent/@key", contentDataXmlAfterUpdate, relatedContentKey4.toString() );
        AssertTool.assertXPathEquals( "/contentdata/myrelatedcontents/content[1]/@key", contentDataXmlAfterUpdate,
                                      relatedContentKey3.toString() );
        AssertTool.assertXPathEquals( "/contentdata/myrelatedcontents/content[2]/@key", contentDataXmlAfterUpdate,
                                      relatedContentKey5.toString() );

        assertEquals( 3, versionAfterUpdate.getRelatedChildren( true ).size() );
    }


    private Element createSimpleContentTypeConfig()
    {

        StringBuffer configXml = new StringBuffer();
        configXml.append( "<config name=\"MyContentType\" version=\"1.0\">" );
        configXml.append( "     <form>" );

        configXml.append( "         <title name=\"myTitle\"/>" );

        configXml.append( "         <block name=\"General\">" );

        configXml.append( "             <input name=\"myTitle\" required=\"true\" type=\"text\">" );
        configXml.append( "                 <display>My title</display>" );
        configXml.append( "                 <xpath>contentdata/mytitle</xpath>" );
        configXml.append( "             </input>" );

        configXml.append( "         </block>" );
        configXml.append( "     </form>" );
        configXml.append( "</config>" );

        try
        {
            return JDOMUtil.parseDocument( configXml.toString() ).getRootElement();
        }
        catch ( IOException e )
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        catch ( JDOMException e )
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return null;
    }


    private ContentKey storeSimpleContent( String title )
    {

        ContentEntity content = factory.createContent( "MyCategory", "en", "testuser", "0", new Date() );
        ContentVersionEntity version = factory.createContentVersion( "0", "testuser" );

        ContentTypeConfig contentTypeConfig = ContentTypeConfigParser.parse( ContentHandlerName.CUSTOM, createSimpleContentTypeConfig() );

        CustomContentData contentData = new CustomContentData( contentTypeConfig );

        TextDataEntryConfig titleConfig = new TextDataEntryConfig( "myTitle", true, title, "contentdata/mytitle" );
        contentData.add( new TextDataEntry( titleConfig, "relatedconfig" ) );

        version.setContentData( contentData );

        UserEntity runningUser = fixture.findUserByName( "testuser" );

        CreateContentCommand createContentCommand = new CreateContentCommand();
        createContentCommand.setCreator( runningUser );

        createContentCommand.populateCommandWithContentValues( content );
        createContentCommand.populateCommandWithContentVersionValues( version );

        createContentCommand.setBinaryDatas( new ArrayList<BinaryDataAndBinary>() );
        createContentCommand.setUseCommandsBinaryDataToAdd( true );

        ContentKey contentKey = contentService.createContent( createContentCommand );

        hibernateTemplate.flush();
        hibernateTemplate.clear();

        return contentKey;
    }

}
