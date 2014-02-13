/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.itest.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.hibernate3.HibernateTemplate;

import junit.framework.Assert;

import com.enonic.cms.framework.xml.XMLDocumentFactory;

import com.enonic.cms.api.client.model.CreateImageContentParams;
import com.enonic.cms.api.client.model.content.ContentStatus;
import com.enonic.cms.api.client.model.content.image.ImageBinaryInput;
import com.enonic.cms.api.client.model.content.image.ImageContentDataInput;
import com.enonic.cms.api.client.model.content.image.ImageDescriptionInput;
import com.enonic.cms.api.client.model.content.image.ImageKeywordsInput;
import com.enonic.cms.api.client.model.content.image.ImageNameInput;
import com.enonic.cms.core.client.InternalClient;
import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.ContentVersionEntity;
import com.enonic.cms.core.content.binary.BinaryDataEntity;
import com.enonic.cms.core.content.binary.ContentBinaryDataEntity;
import com.enonic.cms.core.content.contentdata.legacy.LegacyImageContentData;
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

public class InternalClientImpl_CreateImageContentTest
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
    public void testCreateImageContent()
        throws Exception
    {
        setUpContentAndCategory();

        setRunningUser();

        CreateImageContentParams params = new CreateImageContentParams();
        params.categoryKey = fixture.findCategoryByName( "MyCategory" ).getKey().toInt();
        params.publishFrom = new Date();
        params.publishTo = null;
        params.status = ContentStatus.STATUS_DRAFT;
        params.contentData = createImageContentData( "200" );

        int contentKey = internalClient.createImageContent( params );

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

        LegacyImageContentData contentData = (LegacyImageContentData) persistedVersion.getContentData();
        assertNotNull( contentData );

        Document contentDataXml = contentData.getContentDataXml();
        AssertTool.assertSingleXPathValueEquals( "/contentdata/name", contentDataXml, "test binary" );
        AssertTool.assertSingleXPathValueEquals( "/contentdata/description", contentDataXml, "Dummy description." );
        AssertTool.assertSingleXPathValueEquals( "/contentdata/keywords", contentDataXml, "keyword1 keyword2" );
        AssertTool.assertSingleXPathValueEquals( "/contentdata/sourceimage/binarydata/@key", contentDataXml,
                                                 binaryDataResolvedFromContentBinaryData.getBinaryDataKey().toString() );
        AssertTool.assertSingleXPathValueEquals( "/contentdata/sourceimage/@width", contentDataXml, "200" );
        AssertTool.assertSingleXPathValueEquals( "/contentdata/sourceimage/@height", contentDataXml, "200" );
        AssertTool.assertSingleXPathValueEquals( "/contentdata/images/@border", contentDataXml, "no" );
        AssertTool.assertSingleXPathValueEquals( "/contentdata/images/image/@rotation", contentDataXml, "none" );
        AssertTool.assertSingleXPathValueEquals( "/contentdata/images/image/@type", contentDataXml, "original" );
        AssertTool.assertSingleXPathValueEquals( "/contentdata/images/image/width", contentDataXml, "200" );
        AssertTool.assertSingleXPathValueEquals( "/contentdata/images/image/height", contentDataXml, "200" );
        AssertTool.assertSingleXPathValueEquals( "/contentdata/images/image/binarydata/@key", contentDataXml,
                                                 binaryDataResolvedFromContentBinaryData.getBinaryDataKey().toString() );
    }

    @Test
    public void testNoScaleImageCreated()
        throws Exception
    {
        setUpContentAndCategory();

        setRunningUser();

        CreateImageContentParams params = new CreateImageContentParams();
        params.categoryKey = fixture.findCategoryByName( "MyCategory" ).getKey().toInt();
        params.publishFrom = new Date();
        params.publishTo = null;
        params.status = ContentStatus.STATUS_DRAFT;
        params.contentData = createImageContentData( "1024" );

        int contentKey = internalClient.createImageContent( params );

        fixture.flushAndClearHibernateSession();

        ContentEntity persistedContent = fixture.findContentByKey( new ContentKey( contentKey ) );
        assertNotNull( persistedContent );
        assertEquals( "MyCategory", persistedContent.getCategory().getName() );

        ContentVersionEntity persistedVersion = persistedContent.getMainVersion();
        assertNotNull( persistedVersion );
        assertEquals( "test binary", persistedVersion.getTitle() );
        assertEquals( ContentStatus.STATUS_DRAFT, persistedVersion.getStatus().getKey() );

        Set<ContentBinaryDataEntity> contentBinaryDatas = persistedVersion.getContentBinaryData();

        assertEquals( 4, contentBinaryDatas.size() );

        String[] names = new String[4];
        int i = 0;
        for ( ContentBinaryDataEntity contentBinaryData : contentBinaryDatas )
        {
            names[i++] = contentBinaryData.getBinaryData().getName();
        }

        assertArrayEquals( new String[]{"Dummy Name_small.jpeg", "Dummy Name_medium.jpeg", "Dummy Name_large.jpeg", "Dummy Name"}, names );
    }

    private static void assertArrayEquals( Object[] a1, Object[] a2 )
    {
        Assert.assertEquals( arrayToString( a1 ), arrayToString( a2 ) );
    }

    private static String arrayToString( Object[] a )
    {
        StringBuilder result = new StringBuilder( "[" );

        for ( int i = 0; i < a.length; i++ )
        {
            result.append( i ).append( ": " ).append( a[i] );
            if ( i < a.length - 1 )
            {
                result.append( ", " );
            }
        }

        result.append( "]" );

        return result.toString();
    }

    private void setRunningUser()
    {
        UserEntity runningUser = fixture.findUserByName( "testuser" );
        PortalSecurityHolder.setImpersonatedUser( runningUser.getKey() );
    }

    private void setUpContentAndCategory()
    {
        fixture.createAndStoreUserAndUserGroup( "testuser", "testuser fullname", UserType.NORMAL, "testuserstore" );
        fixture.save( factory.createContentHandler( "File content", ContentHandlerName.IMAGE.getHandlerClassShortName() ) );
        fixture.save(
            factory.createContentType( "MyContentType", ContentHandlerName.IMAGE.getHandlerClassShortName(), contentTypeConfig ) );
        fixture.save( factory.createUnit( "MyUnit", "en" ) );
        fixture.save( factory.createCategory( "MyCategory", null, "MyContentType", "MyUnit", "testuser", "testuser" ) );
        fixture.save( factory.createCategoryAccessForUser( "MyCategory", "testuser", "read,create" ) );
        fixture.flushAndClearHibernateSession();
    }

    private ImageContentDataInput createImageContentData( String fileName )
        throws Exception
    {
        ImageContentDataInput imageContentData = new ImageContentDataInput();

        imageContentData.binary = new ImageBinaryInput( loadImageFile( fileName ), "Dummy Name" );

        imageContentData.description = new ImageDescriptionInput( "Dummy description." );

        imageContentData.keywords = new ImageKeywordsInput().addKeyword( "keyword1" ).addKeyword( "keyword2" );
        imageContentData.name = new ImageNameInput( "test binary" );

        return imageContentData;
    }


    private String createFileName( String fileName )
    {
        return InternalClientImpl_CreateImageContentTest.class.getName().replace( ".", "/" ) + "-" + fileName + "px.jpg";
    }

    private byte[] loadImageFile( String fileName )
        throws IOException
    {
        ClassPathResource resource = new ClassPathResource( createFileName( fileName ) );
        InputStream in = resource.getInputStream();
        return IOUtils.toByteArray( in );
    }

}
