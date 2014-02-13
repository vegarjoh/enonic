/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.itest.client;

import java.io.IOException;
import java.util.Date;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mock.web.MockHttpServletRequest;

import com.enonic.cms.framework.xml.XMLDocumentFactory;

import com.enonic.cms.api.client.model.CreateFileContentParams;
import com.enonic.cms.api.client.model.UpdateFileContentParams;
import com.enonic.cms.api.client.model.content.ContentStatus;
import com.enonic.cms.api.client.model.content.file.FileBinaryInput;
import com.enonic.cms.api.client.model.content.file.FileContentDataInput;
import com.enonic.cms.api.client.model.content.file.FileDescriptionInput;
import com.enonic.cms.api.client.model.content.file.FileKeywordsInput;
import com.enonic.cms.api.client.model.content.file.FileNameInput;
import com.enonic.cms.core.client.InternalClient;
import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.ContentVersionEntity;
import com.enonic.cms.core.content.binary.BinaryDataEntity;
import com.enonic.cms.core.content.binary.ContentBinaryDataEntity;
import com.enonic.cms.core.content.contentdata.legacy.LegacyFileContentData;
import com.enonic.cms.core.content.contenttype.ContentHandlerName;
import com.enonic.cms.core.security.PortalSecurityHolder;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.security.user.UserType;
import com.enonic.cms.core.servlet.ServletRequestAccessor;
import com.enonic.cms.itest.AbstractSpringTest;
import com.enonic.cms.itest.util.AssertTool;
import com.enonic.cms.itest.util.DomainFactory;
import com.enonic.cms.itest.util.DomainFixture;
import com.enonic.cms.store.dao.ContentDao;

import static org.junit.Assert.*;

public class InternalClientImpl_UpdateFileContentTest
    extends AbstractSpringTest
{

    @Autowired
    private DomainFixture fixture;

    @Autowired
    private ContentDao contentDao;

    @Autowired
    @Qualifier("localClient")
    private InternalClient internalClient;

    private byte[] dummyBinary1 = new byte[]{1, 2, 3};

    private byte[] dummyBinary2 = new byte[]{1, 2, 3, 4, 5, 6};

    @Before
    public void before()
        throws IOException, JDOMException
    {

        final DomainFactory factory = fixture.getFactory();

        fixture.initSystemData();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr( "127.0.0.1" );
        ServletRequestAccessor.setRequest( request );

        StringBuilder contentTypeConfigXml = new StringBuilder();
        contentTypeConfigXml.append( "<moduledata/>" );
        final Document contentTypeConfig = XMLDocumentFactory.create( contentTypeConfigXml.toString() ).getAsJDOMDocument();

        fixture.createAndStoreUserAndUserGroup( "testuser", "testuser fullname", UserType.NORMAL, "testuserstore" );
        fixture.save( factory.createContentHandler( "File content", ContentHandlerName.FILE.getHandlerClassShortName() ) );
        fixture.save( factory.createContentType( "MyContentType", ContentHandlerName.FILE.getHandlerClassShortName(), contentTypeConfig ) );
        fixture.save( factory.createUnit( "MyUnit", "en" ) );
        fixture.save( factory.createCategory( "MyCategory", null, "MyContentType", "MyUnit", "testuser", "testuser" ) );
        fixture.save( factory.createCategoryAccessForUser( "MyCategory", "testuser", "read, create, approve" ) );
        fixture.flushAndClearHibernateSession();
    }


    @Test
    public void testUpdateCurrentVersion()
    {
        UserEntity runningUser = fixture.findUserByName( "testuser" );
        PortalSecurityHolder.setImpersonatedUser( runningUser.getKey() );

        int contentKey = storeNewFileContent();

        FileContentDataInput fileContentData = new FileContentDataInput();
        fileContentData.binary = new FileBinaryInput( dummyBinary2, "Dummy Name 2" );
        fileContentData.description = new FileDescriptionInput( "Dummy description 2." );
        fileContentData.keywords = new FileKeywordsInput().addKeyword( "keyworda" ).addKeyword( "keywordb" );
        fileContentData.name = new FileNameInput( "test binary 2" );

        UpdateFileContentParams params = new UpdateFileContentParams();
        params.contentKey = contentKey;
        params.createNewVersion = false;
        params.setAsCurrentVersion = true;
        params.status = ContentStatus.STATUS_DRAFT;
        params.fileContentData = fileContentData;

        int versionKey = internalClient.updateFileContent( params );
        assertTrue( versionKey > -1 );

        ContentEntity actualContent = contentDao.findByKey( new ContentKey( contentKey ) );
        ContentVersionEntity actualVersion = actualContent.getMainVersion();

        assertEquals( "test binary 2", actualVersion.getTitle() );

        assertEquals( 1, actualVersion.getContentBinaryData().size() );

        ContentBinaryDataEntity contentBinaryData = actualVersion.getContentBinaryData().iterator().next();
        BinaryDataEntity binaryDataResolvedFromContentBinaryData = contentBinaryData.getBinaryData();
        LegacyFileContentData contentData = (LegacyFileContentData) actualVersion.getContentData();

        assertEquals( "test binary 2", contentData.getTitle() );
        assertEquals( binaryDataResolvedFromContentBinaryData.getBinaryDataKey(), contentData.resolveBinaryDataKey() );

        Document contentDataXml = contentData.getContentDataXml();
        AssertTool.assertSingleXPathValueEquals( "/contentdata/name", contentDataXml, "test binary 2" );
        AssertTool.assertSingleXPathValueEquals( "/contentdata/description", contentDataXml, "Dummy description 2." );
        AssertTool.assertSingleXPathValueEquals( "/contentdata/keywords/keyword[1]", contentDataXml, "keyworda" );
        AssertTool.assertSingleXPathValueEquals( "/contentdata/keywords/keyword[2]", contentDataXml, "keywordb" );
        AssertTool.assertSingleXPathValueEquals( "/contentdata/filesize", contentDataXml, String.valueOf( dummyBinary2.length ) );
        AssertTool.assertSingleXPathValueEquals( "/contentdata/binarydata/@key", contentDataXml,
                                                 binaryDataResolvedFromContentBinaryData.getBinaryDataKey().toString() );

    }

    private int storeNewFileContent()
    {
        FileContentDataInput fileContentData = new FileContentDataInput();
        fileContentData.binary = new FileBinaryInput( dummyBinary1, "Dummy Name" );
        fileContentData.description = new FileDescriptionInput( "Dummy description." );
        fileContentData.keywords = new FileKeywordsInput().addKeyword( "keyword1" ).addKeyword( "keyword2" );
        fileContentData.name = new FileNameInput( "test binary" );

        CreateFileContentParams params = new CreateFileContentParams();
        params.categoryKey = fixture.findCategoryByName( "MyCategory" ).getKey().toInt();
        params.publishFrom = new Date();
        params.publishTo = null;
        params.status = ContentStatus.STATUS_DRAFT;
        params.fileContentData = fileContentData;
        int contentKey = internalClient.createFileContent( params );

        fixture.flushAndClearHibernateSession();

        ContentEntity persistedContent = contentDao.findByKey( new ContentKey( contentKey ) );
        assertNotNull( persistedContent );

        fixture.flushAndClearHibernateSession();

        return contentKey;
    }


}