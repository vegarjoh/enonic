/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.itest.client;

import java.io.IOException;
import java.util.Date;
import java.util.Set;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.hibernate3.HibernateTemplate;

import com.enonic.cms.framework.xml.XMLDocumentFactory;

import com.enonic.cms.api.client.model.CreateFileContentParams;
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

import static org.junit.Assert.*;

public class InternalClientImpl_CreateFileContentTest
    extends AbstractSpringTest
{
    @Autowired
    private HibernateTemplate hibernateTemplate;

    private DomainFactory factory;

    @Autowired
    private DomainFixture fixture;

    @Autowired
    @Qualifier("localClient")
    private InternalClient internalClient;

    private byte[] dummyBinary = new byte[]{1, 2, 3};

    private Document contentTypeConfig;

    @Before
    public void before()
        throws IOException, JDOMException
    {

        factory = fixture.getFactory();
        fixture.initSystemData();

        StringBuilder contentTypeConfigXml = new StringBuilder();
        contentTypeConfigXml.append( "<moduledata/>" );
        contentTypeConfig = XMLDocumentFactory.create( contentTypeConfigXml.toString() ).getAsJDOMDocument();

        hibernateTemplate.flush();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr( "127.0.0.1" );
        ServletRequestAccessor.setRequest( request );

    }


    @Test
    public void testCreateFileContent()
    {
        fixture.createAndStoreUserAndUserGroup( "testuser", "testuser fullname", UserType.NORMAL, "testuserstore" );
        fixture.save( factory.createContentHandler( "File content", ContentHandlerName.FILE.getHandlerClassShortName() ) );
        fixture.save( factory.createContentType( "MyContentType", ContentHandlerName.FILE.getHandlerClassShortName(), contentTypeConfig ) );
        fixture.save( factory.createUnit( "MyUnit", "en" ) );
        fixture.save( factory.createCategory( "MyCategory", null, "MyContentType", "MyUnit", "testuser", "testuser" ) );
        fixture.save( factory.createCategoryAccessForUser( "MyCategory", "testuser", "read,create" ) );
        fixture.flushAndClearHibernateSession();

        UserEntity runningUser = fixture.findUserByName( "testuser" );
        PortalSecurityHolder.setImpersonatedUser( runningUser.getKey() );

        FileContentDataInput fileContentData = new FileContentDataInput();
        fileContentData.binary = new FileBinaryInput( dummyBinary, "Dummy Name" );
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

        ContentEntity persistedContent = fixture.findContentByKey( new ContentKey( contentKey ) );
        assertNotNull( persistedContent );
        assertEquals( "MyCategory", persistedContent.getCategory().getName() );

        ContentVersionEntity persistedVersion = persistedContent.getMainVersion();
        assertNotNull( persistedVersion );
        assertEquals( "test binary", persistedVersion.getTitle() );
        assertEquals( com.enonic.cms.core.content.ContentStatus.DRAFT.getKey(), persistedVersion.getStatus().getKey() );

        Set<ContentBinaryDataEntity> contentBinaryDatas = persistedVersion.getContentBinaryData();
        assertEquals( 1, contentBinaryDatas.size() );
        assertEquals( "source", contentBinaryDatas.iterator().next().getLabel() );

        BinaryDataEntity binaryDataResolvedFromContentBinaryData = contentBinaryDatas.iterator().next().getBinaryData();
        assertEquals( "Dummy Name", binaryDataResolvedFromContentBinaryData.getName() );

        LegacyFileContentData contentData = (LegacyFileContentData) persistedVersion.getContentData();
        assertNotNull( contentData );

        Document contentDataXml = contentData.getContentDataXml();
        AssertTool.assertSingleXPathValueEquals( "/contentdata/name", contentDataXml, "test binary" );
        AssertTool.assertSingleXPathValueEquals( "/contentdata/description", contentDataXml, "Dummy description." );
        AssertTool.assertSingleXPathValueEquals( "/contentdata/keywords/keyword[1]", contentDataXml, "keyword1" );
        AssertTool.assertSingleXPathValueEquals( "/contentdata/keywords/keyword[2]", contentDataXml, "keyword2" );
        AssertTool.assertSingleXPathValueEquals( "/contentdata/filesize", contentDataXml, String.valueOf( dummyBinary.length ) );
        AssertTool.assertSingleXPathValueEquals( "/contentdata/binarydata/@key", contentDataXml,
                                                 binaryDataResolvedFromContentBinaryData.getBinaryDataKey().toString() );
    }


}