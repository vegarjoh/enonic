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

import com.enonic.cms.framework.xml.XMLDocumentFactory;

import com.enonic.cms.api.client.model.CreateContentParams;
import com.enonic.cms.api.client.model.content.BinaryInput;
import com.enonic.cms.api.client.model.content.ContentDataInput;
import com.enonic.cms.api.client.model.content.ContentStatus;
import com.enonic.cms.api.client.model.content.GroupInput;
import com.enonic.cms.api.client.model.content.TextInput;
import com.enonic.cms.core.client.InternalClient;
import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.ContentVersionEntity;
import com.enonic.cms.core.content.binary.BinaryDataEntity;
import com.enonic.cms.core.content.binary.ContentBinaryDataEntity;
import com.enonic.cms.core.content.contentdata.custom.BlockGroupDataEntries;
import com.enonic.cms.core.content.contentdata.custom.CustomContentData;
import com.enonic.cms.core.content.contentdata.custom.GroupDataEntry;
import com.enonic.cms.core.content.contentdata.custom.stringbased.TextDataEntry;
import com.enonic.cms.core.content.contenttype.ContentHandlerName;
import com.enonic.cms.core.content.contenttype.ContentTypeConfigBuilder;
import com.enonic.cms.core.security.PortalSecurityHolder;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.security.user.UserType;
import com.enonic.cms.core.servlet.ServletRequestAccessor;
import com.enonic.cms.itest.AbstractSpringTest;
import com.enonic.cms.itest.util.DomainFactory;
import com.enonic.cms.itest.util.DomainFixture;

import static org.junit.Assert.*;

public class InternalClientImpl_CreateContentTest
    extends AbstractSpringTest
{
    private DomainFactory factory;

    @Autowired
    private DomainFixture fixture;

    @Autowired
    @Qualifier("localClient")
    private InternalClient internalClient;

    private Document standardConfig;

    private byte[] dummyBinary = new byte[]{1, 2, 3};

    @Before
    public void before()
        throws IOException, JDOMException
    {

        factory = fixture.getFactory();
        fixture.initSystemData();

        StringBuilder standardConfigXml = new StringBuilder();
        standardConfigXml.append( "<config name=\"MyContentType\" version=\"1.0\">" );
        standardConfigXml.append( "     <form>" );

        standardConfigXml.append( "         <title name=\"myTitle\"/>" );

        standardConfigXml.append( "         <block name=\"TestBlock1\">" );

        standardConfigXml.append( "             <input name=\"myTitle\" required=\"true\" type=\"text\">" );
        standardConfigXml.append( "                 <display>My title</display>" );
        standardConfigXml.append( "                 <xpath>contentdata/mytitle</xpath>" );
        standardConfigXml.append( "             </input>" );

        standardConfigXml.append( "             <input name=\"myBinaryfile\" type=\"uploadfile\">" );
        standardConfigXml.append( "                 <display>My binaryfile</display>" );
        standardConfigXml.append( "                 <xpath>contentdata/mybinaryfile</xpath>" );
        standardConfigXml.append( "             </input>" );

        standardConfigXml.append( "         </block>" );
        standardConfigXml.append( "     </form>" );
        standardConfigXml.append( "</config>" );
        standardConfig = XMLDocumentFactory.create( standardConfigXml.toString() ).getAsJDOMDocument();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr( "127.0.0.1" );
        ServletRequestAccessor.setRequest( request );
    }

    @Test
    public void testCreateContentWithBinary()
    {
        fixture.createAndStoreNormalUserWithUserGroup( "testuser", "Test user", "testuserstore" );

        fixture.save( factory.createContentHandler( "Custom content", ContentHandlerName.CUSTOM.getHandlerClassShortName() ) );
        fixture.save( factory.createContentType( "MyContentType", ContentHandlerName.CUSTOM.getHandlerClassShortName(), standardConfig ) );
        fixture.save( factory.createUnit( "MyUnit", "en" ) );
        fixture.save( factory.createCategory( "MyCategory", null, "MyContentType", "MyUnit", "testuser", "testuser" ) );
        fixture.save( factory.createCategoryAccessForUser( "MyCategory", "testuser", "read,create" ) );

        fixture.flushAndClearHibernateSession();

        UserEntity runningUser = fixture.findUserByName( "testuser" );
        PortalSecurityHolder.setImpersonatedUser( runningUser.getKey() );

        ContentDataInput contentData = new ContentDataInput( "MyContentType" );
        contentData.add( new TextInput( "myTitle", "testtitle" ) );
        contentData.add( new BinaryInput( "myBinaryfile", dummyBinary, "dummyBinary" ) );

        CreateContentParams params = new CreateContentParams();
        params.categoryKey = fixture.findCategoryByName( "MyCategory" ).getKey().toInt();
        params.contentData = contentData;
        params.publishFrom = new Date();
        params.publishTo = null;
        params.status = ContentStatus.STATUS_DRAFT;
        int contentKey = internalClient.createContent( params );

        fixture.flushAndClearHibernateSession();

        ContentEntity persistedContent = fixture.findContentByKey( new ContentKey( contentKey ) );
        assertNotNull( persistedContent );
        assertEquals( "MyCategory", persistedContent.getCategory().getName() );
        ContentVersionEntity persistedVersion = persistedContent.getMainVersion();
        assertNotNull( persistedVersion );
        assertEquals( "testtitle", persistedVersion.getTitle() );
        assertEquals( com.enonic.cms.core.content.ContentStatus.DRAFT.getKey(), persistedVersion.getStatus().getKey() );

        // verify binary was saved
        Set<ContentBinaryDataEntity> contentBinaryDatas = persistedVersion.getContentBinaryData();
        assertEquals( 1, contentBinaryDatas.size() );
        ContentBinaryDataEntity contentBinaryData = contentBinaryDatas.iterator().next();
        BinaryDataEntity binaryData = contentBinaryData.getBinaryData();
        assertEquals( "dummyBinary", binaryData.getName() );

        CustomContentData customContentData = (CustomContentData) persistedVersion.getContentData();
        assertNotNull( customContentData );
    }

    @Test
    public void testCreateContentWithBlockGroup()
    {
        fixture.createAndStoreUserAndUserGroup( "testuser", "testuser fullname", UserType.NORMAL, "testuserstore" );

        fixture.save( factory.createContentHandler( "Custom content", ContentHandlerName.CUSTOM.getHandlerClassShortName() ) );

        // setup content type
        ContentTypeConfigBuilder ctyconf = new ContentTypeConfigBuilder( "Skole", "tittel" );
        ctyconf.startBlock( "Skole" );
        ctyconf.addInput( "tittel", "text", "contentdata/tittel", "Tittel", true );
        ctyconf.endBlock();
        ctyconf.startBlock( "Elever", "contentdata/elever" );
        ctyconf.addInput( "elev-navn", "text", "navn", "Navn" );
        ctyconf.addInput( "elev-karakter", "text", "karakter", "Karakter" );
        ctyconf.endBlock();
        ctyconf.startBlock( "Laerere", "contentdata/laerere" );
        ctyconf.addInput( "laerer-navn", "text", "navn", "Navn" );
        ctyconf.addInput( "laerer-karakter", "text", "karakter", "Karakter" );
        ctyconf.endBlock();
        Document configAsXmlBytes = XMLDocumentFactory.create( ctyconf.toString() ).getAsJDOMDocument();
        fixture.save( factory.createContentType( "Skole", ContentHandlerName.CUSTOM.getHandlerClassShortName(), configAsXmlBytes ) );

        fixture.save( factory.createUnit( "MyUnit", "en" ) );
        fixture.save( factory.createCategory( "Skole", null, "Skole", "MyUnit", "testuser", "testuser" ) );
        fixture.save( factory.createCategoryAccessForUser( "Skole", "testuser", "read,create,approve" ) );

        UserEntity runningUser = fixture.findUserByName( "testuser" );
        PortalSecurityHolder.setImpersonatedUser( runningUser.getKey() );

        CreateContentParams content = new CreateContentParams();
        content.categoryKey = fixture.findCategoryByName( "Skole" ).getKey().toInt();
        content.publishFrom = new Date();
        content.status = ContentStatus.STATUS_APPROVED;

        ContentDataInput contentData = new ContentDataInput( "Skole" );
        contentData.add( new TextInput( "tittel", "St. Olav Videregaende skole" ) );

        GroupInput groupInputElev1 = contentData.addGroup( "Elever" );
        groupInputElev1.add( new TextInput( "elev-navn", "Vegar Jansen" ) );
        groupInputElev1.add( new TextInput( "elev-karakter", "S" ) );

        GroupInput groupInputElev2 = contentData.addGroup( "Elever" );
        groupInputElev2.add( new TextInput( "elev-navn", "Thomas Sigdestad" ) );
        groupInputElev2.add( new TextInput( "elev-karakter", "M" ) );

        GroupInput groupInputLaerer1 = contentData.addGroup( "Laerere" );
        groupInputLaerer1.add( new TextInput( "laerer-navn", "Mutt Hansen" ) );
        groupInputLaerer1.add( new TextInput( "laerer-karakter", "LG" ) );

        GroupInput groupInputLaerer2 = contentData.addGroup( "Laerere" );
        groupInputLaerer2.add( new TextInput( "laerer-navn", "Striks Jansen" ) );
        groupInputLaerer2.add( new TextInput( "laerer-karakter", "M" ) );

        content.contentData = contentData;
        ContentKey contentKey = new ContentKey( internalClient.createContent( content ) );

        ContentEntity createdContent = fixture.findContentByKey( contentKey );
        ContentVersionEntity createdVersion = createdContent.getMainVersion();
        CustomContentData createdContentData = (CustomContentData) createdVersion.getContentData();
        BlockGroupDataEntries elever = createdContentData.getBlockGroupDataEntries( "Elever" );

        GroupDataEntry elev1 = elever.getGroupDataEntry( 1 );
        assertEquals( "Vegar Jansen", ( (TextDataEntry) elev1.getEntry( "elev-navn" ) ).getValue() );
        assertEquals( "S", ( (TextDataEntry) elev1.getEntry( "elev-karakter" ) ).getValue() );

        GroupDataEntry elev2 = elever.getGroupDataEntry( 2 );
        assertEquals( "Thomas Sigdestad", ( (TextDataEntry) elev2.getEntry( "elev-navn" ) ).getValue() );
        assertEquals( "M", ( (TextDataEntry) elev2.getEntry( "elev-karakter" ) ).getValue() );

        BlockGroupDataEntries laerere = createdContentData.getBlockGroupDataEntries( "Laerere" );

        GroupDataEntry laerer1 = laerere.getGroupDataEntry( 1 );
        assertEquals( "Mutt Hansen", ( (TextDataEntry) laerer1.getEntry( "laerer-navn" ) ).getValue() );
        assertEquals( "LG", ( (TextDataEntry) laerer1.getEntry( "laerer-karakter" ) ).getValue() );

        GroupDataEntry laerer2 = laerere.getGroupDataEntry( 2 );
        assertEquals( "Striks Jansen", ( (TextDataEntry) laerer2.getEntry( "laerer-navn" ) ).getValue() );
        assertEquals( "M", ( (TextDataEntry) laerer2.getEntry( "laerer-karakter" ) ).getValue() );
    }
}
