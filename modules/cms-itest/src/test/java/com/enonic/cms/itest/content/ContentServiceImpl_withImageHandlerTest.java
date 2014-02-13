/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.itest.content;

import java.util.ArrayList;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.ContentService;
import com.enonic.cms.core.content.ContentStatus;
import com.enonic.cms.core.content.ContentVersionEntity;
import com.enonic.cms.core.content.binary.BinaryDataAndBinary;
import com.enonic.cms.core.content.command.CreateContentCommand;
import com.enonic.cms.core.content.contentdata.legacy.LegacyImageContentData;
import com.enonic.cms.core.content.contenttype.ContentHandlerName;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.security.user.UserType;
import com.enonic.cms.core.servlet.ServletRequestAccessor;
import com.enonic.cms.itest.AbstractSpringTest;
import com.enonic.cms.itest.util.AssertTool;
import com.enonic.cms.itest.util.DomainFactory;
import com.enonic.cms.itest.util.DomainFixture;
import com.enonic.cms.store.dao.ContentDao;

import static org.junit.Assert.*;

public class ContentServiceImpl_withImageHandlerTest
    extends AbstractSpringTest
{

    private DomainFactory factory;

    @Autowired
    private DomainFixture fixture;

    @Autowired
    private ContentService contentService;

    @Autowired
    private ContentDao contentDao;

    @Test
    public void testCreateContent()
    {

        factory = fixture.getFactory();

        fixture.initSystemData();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr( "127.0.0.1" );
        ServletRequestAccessor.setRequest( request );

        fixture.createAndStoreUserAndUserGroup( "testuser", "testuser fullname", UserType.NORMAL, "testuserstore" );
        fixture.save( factory.createContentHandler( "Image content", ContentHandlerName.IMAGE.getHandlerClassShortName() ) );
        fixture.save( factory.createContentType( "MyContentType", ContentHandlerName.IMAGE.getHandlerClassShortName() ) );
        fixture.save( factory.createUnit( "MyUnit" ) );
        fixture.save( factory.createCategory( "MyCategory", null, "MyContentType", "MyUnit", "testuser", "testuser" ) );
        fixture.save( factory.createCategoryAccessForUser( "MyCategory", "testuser", "read, create" ) );
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

        Document contentDataDoc = createImageContentDataDoc( "my image", 2 );
        LegacyImageContentData contentData = new LegacyImageContentData( contentDataDoc );
        version.setContentData( contentData );

        List<BinaryDataAndBinary> binaryDatas = new ArrayList<BinaryDataAndBinary>();
        binaryDatas.add( factory.createBinaryDataAndBinary( "bilde_scaled_1", new byte[]{1, 0, 1} ) );
        binaryDatas.add( factory.createBinaryDataAndBinary( "bilde_scaled_2", new byte[]{1, 0, 2} ) );
        binaryDatas.add( factory.createBinaryDataAndBinary( "bilde_original", new byte[]{1, 0, 3} ) );

        CreateContentCommand command = new CreateContentCommand();
        command.setCreator( runningUser );

        command.populateCommandWithContentValues( content );
        command.populateCommandWithContentVersionValues( version );

        command.setBinaryDatas( binaryDatas );
        command.setUseCommandsBinaryDataToAdd( true );

        ContentKey contenKey = contentService.createContent( command );

        fixture.flushAndClearHibernateSession();

        ContentEntity actualContent = contentDao.findByKey( contenKey );
        assertNotNull( actualContent );
        ContentVersionEntity actualVersion = actualContent.getMainVersion();
        assertNotNull( actualVersion );

        assertNotNull( actualVersion.getContentBinaryData() );
        assertEquals( 3, actualVersion.getContentBinaryData().size() );
        Document actualContentDataXml = actualVersion.getContentDataAsJDomDocument();

        AssertTool.assertSingleXPathValueEquals( "count(/contentdata/images/image/binarydata/@key)", actualContentDataXml, "2" );
        AssertTool.assertSingleXPathValueEquals( "count(/contentdata/sourceimage/binarydata/@key)", actualContentDataXml, "1" );

    }

    private Document createImageContentDataDoc( String name, int imageCountIncludingSourceImage )
    {
        Element contentdataEl = new Element( "contentdata" );
        contentdataEl.addContent( new Element( "name" ).setText( name ) );
        Element imagesEl = new Element( "images" );
        contentdataEl.addContent( imagesEl );
        int nextPlaceHolderIndex = 0;
        if ( imageCountIncludingSourceImage > 0 )
        {
            for ( int i = 0; i < imageCountIncludingSourceImage; i++ )
            {
                Element imageEl = new Element( "image" );
                imagesEl.addContent( imageEl );
                Element binarydataEl = new Element( "binarydata" );
                binarydataEl.setAttribute( "key", "%" + String.valueOf( nextPlaceHolderIndex++ ) );
                imageEl.addContent( binarydataEl );
            }
            Element sourceimageEl = new Element( "sourceimage" );
            Element binarydataEl = new Element( "binarydata" );
            binarydataEl.setAttribute( "key", "%" + String.valueOf( nextPlaceHolderIndex++ ) );
            sourceimageEl.addContent( binarydataEl );
            contentdataEl.addContent( sourceimageEl );
        }
        return new Document( contentdataEl );
    }


}