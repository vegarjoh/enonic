/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.web.portal.services;

import java.rmi.RemoteException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.jdom.Document;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.orm.hibernate3.HibernateTemplate;

import com.enonic.esl.containers.ExtendedMap;
import com.enonic.esl.containers.MultiValueMap;

import com.enonic.cms.framework.xml.XMLDocumentFactory;

import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentService;
import com.enonic.cms.core.content.ContentVersionEntity;
import com.enonic.cms.core.content.category.CategoryEntity;
import com.enonic.cms.core.content.contentdata.custom.BinaryDataEntry;
import com.enonic.cms.core.content.contentdata.custom.BooleanDataEntry;
import com.enonic.cms.core.content.contentdata.custom.CustomContentData;
import com.enonic.cms.core.content.contentdata.custom.GroupDataEntry;
import com.enonic.cms.core.content.contentdata.custom.stringbased.HtmlAreaDataEntry;
import com.enonic.cms.core.content.contentdata.custom.stringbased.TextAreaDataEntry;
import com.enonic.cms.core.content.contentdata.custom.stringbased.TextDataEntry;
import com.enonic.cms.core.content.contentdata.custom.xmlbased.XmlDataEntry;
import com.enonic.cms.core.content.contenttype.ContentHandlerName;
import com.enonic.cms.core.content.contenttype.ContentTypeConfigBuilder;
import com.enonic.cms.core.security.PortalSecurityHolder;
import com.enonic.cms.core.security.SecurityService;
import com.enonic.cms.core.security.user.User;
import com.enonic.cms.core.structure.SiteKey;
import com.enonic.cms.itest.AbstractSpringTest;
import com.enonic.cms.itest.util.DomainFactory;
import com.enonic.cms.itest.util.DomainFixture;
import com.enonic.cms.store.dao.CategoryDao;
import com.enonic.cms.web.portal.SiteRedirectHelper;

import static junit.framework.Assert.assertTrue;
import static junitx.framework.Assert.assertFalse;
import static org.easymock.classextension.EasyMock.createMock;
import static org.junit.Assert.*;

public class ContentServicesProcessor_operation_CreateTest
    extends AbstractSpringTest
{
    @Autowired
    private SecurityService securityService;

    @Autowired
    private CategoryDao categoryDao;

    @Autowired
    protected HibernateTemplate hibernateTemplate;

    @Autowired
    protected ContentService contentService;

    private SiteRedirectHelper siteRedirectHelper;

    private ContentServicesProcessor customContentHandlerController;

    private UserServicesRedirectUrlResolver userServicesRedirectUrlResolver;

    private MockHttpServletRequest request = new MockHttpServletRequest();

    private MockHttpServletResponse response = new MockHttpServletResponse();

    private MockHttpSession session = new MockHttpSession();

    private SiteKey siteKey_1 = new SiteKey( 1 );

    private DomainFactory factory;

    @Autowired
    private DomainFixture fixture;


    @Before
    public void setUp()
    {

        factory = fixture.getFactory();

        customContentHandlerController = new ContentServicesProcessor();
        customContentHandlerController.setContentService( contentService );
        customContentHandlerController.setSecurityService( securityService );
        customContentHandlerController.setCategoryDao( categoryDao );

        userServicesRedirectUrlResolver = Mockito.mock( UserServicesRedirectUrlResolver.class );
        customContentHandlerController.setUserServicesRedirectHelper( userServicesRedirectUrlResolver );

        // just need a dummy of the SiteRedirectHelper 
        siteRedirectHelper = createMock( SiteRedirectHelper.class );
        customContentHandlerController.setSiteRedirectHelper( siteRedirectHelper );

        // setup needed common data for each test
        fixture.initSystemData();

        fixture.save( factory.createContentHandler( "Custom content", ContentHandlerName.CUSTOM.getHandlerClassShortName() ) );
        fixture.createAndStoreNormalUserWithUserGroup( "testuser", "Test user", "testuserstore" );
        PortalSecurityHolder.setAnonUser( fixture.findUserByName( "anonymous" ).getKey() );
        PortalSecurityHolder.setImpersonatedUser( fixture.findUserByName( "testuser" ).getKey() );
        PortalSecurityHolder.setLoggedInUser( fixture.findUserByName( "testuser" ).getKey() );

        fixture.flushAndClearHibernateSession();
    }

    @Test
    public void handlerCreate_generates_content_name()
        throws RemoteException
    {
        // setup content type
        ContentTypeConfigBuilder ctyconf = new ContentTypeConfigBuilder( "Person", "name" );
        ctyconf.startBlock( "Person" );
        ctyconf.addInput( "name", "text", "contentdata/name", "Name", true );
        ctyconf.endBlock();

        createAndSaveContentTypeAndCategory( "Person", "PersonCategory", ctyconf );
        fixture.save( factory.createCategoryAccessForUser( "PersonCategory", "testuser", "read, create" ) );

        fixture.flushAndClearHibernateSession();

        Mockito.when(
            userServicesRedirectUrlResolver.resolveRedirectUrlToPage( Mockito.any( HttpServletRequest.class ), Mockito.anyString(),
                                                                      Mockito.any( MultiValueMap.class ) ) ).thenReturn( "http://anyurl" );

        // execise: create the content
        ExtendedMap formItems = new ExtendedMap( true );
        formItems.putString( "categorykey", fixture.findCategoryByName( "PersonCategory" ).getKey().toString() );
        formItems.putString( "name", "Laverne Veronica Wyatt-Skriubakken" );
        customContentHandlerController.handlerCreate( request, response, session, formItems, null, siteKey_1 );

        fixture.flushAndClearHibernateSession();

        // verify
        ContentEntity content = fixture.findFirstContentByCategory( fixture.findCategoryByName( "MyCategory" ) );
        assertNotNull( content );
        assertEquals( "laverne-veronica-wyatt-skriubakken", content.getName() );
    }

    @Test
    public void handlerCreate_with_group_data()
        throws RemoteException
    {
        // setup content type
        ContentTypeConfigBuilder ctyconf = new ContentTypeConfigBuilder( "Person", "name" );
        ctyconf.startBlock( "Person" );
        ctyconf.addInput( "name", "text", "contentdata/name", "Name", true );
        ctyconf.endBlock();
        ctyconf.startBlock( "Phone", "contentdata/phone" );
        ctyconf.addInput( "phone_label", "text", "label", "Label", true );
        ctyconf.addInput( "phone_number", "text", "number", "Number", false );
        ctyconf.endBlock();

        createAndSaveContentTypeAndCategory( "Person", "PersonCategory", ctyconf );
        fixture.save( factory.createCategoryAccessForUser( "PersonCategory", "testuser", "read, create" ) );

        fixture.flushAndClearHibernateSession();

        Mockito.when(
            userServicesRedirectUrlResolver.resolveRedirectUrlToPage( Mockito.any( HttpServletRequest.class ), Mockito.anyString(),
                                                                      Mockito.any( MultiValueMap.class ) ) ).thenReturn( "http://anyurl" );

        // execise: create the content
        ExtendedMap formItems = new ExtendedMap( true );
        formItems.putString( "categorykey", fixture.findCategoryByName( "PersonCategory" ).getKey().toString() );
        formItems.putString( "name", "Laverne Veronica Wyatt-Skriubakken" );
        formItems.putString( "Phone[1].phone_label", "Mobile" );
        formItems.putString( "Phone[1].phone_number", "99999999" );
        formItems.putString( "Phone[2].phone_label", "Home" );
        formItems.putString( "Phone[2].phone_number", "22222222" );
        formItems.putString( "Phone[3].phone_label", "Fax" );
        formItems.putString( "Phone[3].phone_number", "00000000" );
        customContentHandlerController.handlerCreate( request, response, session, formItems, null, siteKey_1 );

        fixture.flushAndClearHibernateSession();

        // verify
        ContentEntity content = fixture.findFirstContentByCategory( fixture.findCategoryByName( "MyCategory" ) );
        assertNotNull( content );
        ContentVersionEntity version = content.getMainVersion();
        CustomContentData contentData = (CustomContentData) version.getContentData();

        assertEquals( "Laverne Veronica Wyatt-Skriubakken", ( (TextDataEntry) contentData.getEntry( "name" ) ).getValue() );

        GroupDataEntry groupDataEntry1 = contentData.getGroupDataEntry( "Phone", 1 );
        assertEquals( "99999999", ( (TextDataEntry) groupDataEntry1.getEntry( "phone_number" ) ).getValue() );

        GroupDataEntry groupDataEntry2 = contentData.getGroupDataEntry( "Phone", 2 );
        assertEquals( "22222222", ( (TextDataEntry) groupDataEntry2.getEntry( "phone_number" ) ).getValue() );

        GroupDataEntry groupDataEntry3 = contentData.getGroupDataEntry( "Phone", 3 );
        assertEquals( "00000000", ( (TextDataEntry) groupDataEntry3.getEntry( "phone_number" ) ).getValue() );

    }

    /*@Test
    public void handlerCreate_with_input_type_image()
        throws RemoteException
    {
        // setup content type
        ContentTypeConfigBuilder ctyconf = new ContentTypeConfigBuilder( "Person", "title" );
        ctyconf.startBlock( "General" );
        ctyconf.addInput( "title", "text", "contentdata/title", "Title", true );
        ctyconf.addInput( "myImage", "image", "contentdata/myimage", "My image", false );
        ctyconf.endBlock();

        createAndSaveContentTypeAndCategory( "MyContentType", "MyCategory", ctyconf );

        fixture.flushAndClearHibernateSesssion();

        Mockito.when(
            userServicesRedirectUrlResolver.resolveRedirectUrlToPage( Mockito.any( HttpServletRequest.class ), Mockito.anyString(),
                                                                      Mockito.any( MultiValueMap.class ) ) ).thenReturn( "http://anyurl" );

        // execise: create the content
        ExtendedMap formItems = new ExtendedMap( true );
        formItems.putString( "categorykey", findCategoryByName( "MyCategory" ).getKey().toString() );
        formItems.putString( "title", "Title" );
        formItems.putString( "myImage", "123" );
        customContentHandlerController.handlerCreate( request, response, session, formItems, null, siteKey_1 );

        fixture.flushAndClearHibernateSesssion();

        // verify
        ContentEntity content = fixture.findFirstContentByCategory( findCategoryByName( "MyCategory" ) );
        assertNotNull( content );
        ContentVersionEntity version = content.getMainVersion();
        CustomContentData contentData = (CustomContentData) version.getContentData();

        assertEquals( "Title", ( (TextDataEntry) contentData.getEntry( "title" ) ).getValue() );
        assertEquals( new ContentKey( 123 ), ( (ImageDataEntry) contentData.getEntry( "myImage" ) ).getContentKey() );
    }*/

    @Test
    public void handlerCreate_with_input_type_checkbox()
        throws RemoteException
    {
        // setup content type
        ContentTypeConfigBuilder ctyconf = new ContentTypeConfigBuilder( "MyContentType2", "title" );
        ctyconf.startBlock( "General" );
        ctyconf.addInput( "title", "text", "contentdata/title", "Title", true );
        ctyconf.addInput( "myCheckbox_false", "checkbox", "contentdata/mycheckbox_false", "My checkbox to be false", false );
        ctyconf.addInput( "myCheckbox_false2", "checkbox", "contentdata/mycheckbox_false2", "My checkbox to also be false", true );
        ctyconf.addInput( "myCheckbox_true", "checkbox", "contentdata/mycheckbox_true", "My checkbox to be true", false );
        ctyconf.endBlock();

        createAndSaveContentTypeAndCategory( "MyContentType2", "MyCategory2", ctyconf );
        fixture.save( factory.createCategoryAccessForUser( "MyCategory2", "testuser", "read, create" ) );

        fixture.flushAndClearHibernateSession();

        Mockito.when(
            userServicesRedirectUrlResolver.resolveRedirectUrlToPage( Mockito.any( HttpServletRequest.class ), Mockito.anyString(),
                                                                      Mockito.any( MultiValueMap.class ) ) ).thenReturn( "http://anyurl" );

        // execise: create the content
        ExtendedMap formItems = new ExtendedMap( true );
        formItems.putString( "categorykey", fixture.findCategoryByName( "MyCategory2" ).getKey().toString() );
        formItems.putString( "title", "Title" );
        //formItems.putString( "myCheckbox_false", "false" );
        formItems.putString( "myCheckbox_true", "true" );
        customContentHandlerController.handlerCreate( request, response, session, formItems, null, siteKey_1 );

        fixture.flushAndClearHibernateSession();

        // verify no error by checking that correct redirect was done (enough checking that right method was called)
        verifyRedirectOk();
        // verify
        CustomContentData contentData = getCustomContentDataResult( "MyCategory2" );

        assertEquals( "Title", ( (TextDataEntry) contentData.getEntry( "title" ) ).getValue() );
        assertEquals( Boolean.FALSE, ( (BooleanDataEntry) contentData.getEntry( "myCheckbox_false" ) ).getValueAsBoolean() );
        assertEquals( Boolean.TRUE, ( (BooleanDataEntry) contentData.getEntry( "myCheckbox_true" ) ).getValueAsBoolean() );
        assertEquals( Boolean.FALSE, ( (BooleanDataEntry) contentData.getEntry( "myCheckbox_false2" ) ).getValueAsBoolean() );
    }

    @Test
    public void handlerCreate_missing_required_input_of_type_text_is_parsed_as_missing()
        throws RemoteException
    {
        // setup content type
        ContentTypeConfigBuilder ctyconf = new ContentTypeConfigBuilder( "MyContentType3", "title" );
        ctyconf.startBlock( "General" );
        ctyconf.addInput( "title", "text", "contentdata/title", "Title", true );
        ctyconf.addInput( "myText", "text", "contentdata/mytext", "My text", true );
        ctyconf.endBlock();

        createAndSaveContentTypeAndCategory( "MyContentType3", "MyCategory3", ctyconf );
        fixture.save( factory.createCategoryAccessForUser( "MyCategory3", "testuser", "read, create" ) );

        fixture.flushAndClearHibernateSession();

        // execise: create the content
        ExtendedMap formItems = new ExtendedMap( true );
        formItems.putString( "categorykey", fixture.findCategoryByName( "MyCategory3" ).getKey().toString() );
        formItems.putString( "title", "Title" );
        customContentHandlerController.handlerCreate( request, response, session, formItems, null, siteKey_1 );

        fixture.flushAndClearHibernateSession();

        // verify
        Mockito.verify( userServicesRedirectUrlResolver ).resolveRedirectUrlToErrorPage( Mockito.any( HttpServletRequest.class ),
                                                                                         Mockito.any( ExtendedMap.class ),
                                                                                         Mockito.eq( new int[]{400} ),
                                                                                         Mockito.any( MultiValueMap.class ) );
    }

    @Test
    public void handlerCreate_uploadfile_input_no_uploadfile_no_key()
        throws RemoteException
    {

        // setup content type
        ContentTypeConfigBuilder ctyconf = new ContentTypeConfigBuilder( "MyContentType3", "title" );
        ctyconf.startBlock( "General" );
        ctyconf.addInput( "title", "text", "contentdata/title", "Title", true );
        ctyconf.addInput( "unrequired", "uploadfile", "contentdata/unrequired", "Unrequired", false );
        ctyconf.endBlock();

        createAndSaveContentTypeAndCategory( "MyContentType3", "MyCategory3", ctyconf );
        fixture.save( factory.createCategoryAccessForUser( "MyCategory3", "testuser", "read, create" ) );

        fixture.flushAndClearHibernateSession();

        Mockito.when(
            userServicesRedirectUrlResolver.resolveRedirectUrlToPage( Mockito.any( HttpServletRequest.class ), Mockito.anyString(),
                                                                      Mockito.any( MultiValueMap.class ) ) ).thenReturn( "http://anyurl" );

        // execise: create the content
        String categoryName = "MyCategory3";
        ExtendedMap formItems = new ExtendedMap( true );
        formItems.putString( "categorykey", fixture.findCategoryByName( categoryName ).getKey().toString() );
        formItems.putString( "title", "Title" );

        customContentHandlerController.handlerCreate( request, response, session, formItems, null, siteKey_1 );

        fixture.flushAndClearHibernateSession();

        verifyRedirectOk();

        CustomContentData contentData = getCustomContentDataResult( categoryName );

        assertEquals( "Title", ( (TextDataEntry) contentData.getEntry( "title" ) ).getValue() );
        assertTrue( "Unrequired", contentData.getEntry( "unrequired" ) != null );
    }

    @Test
    public void handlerCreate_uploadfile_input_using_basekey_for_backwards_compatability()
        throws RemoteException
    {
        // This test sends the binary in the "unrequired"-field instead of the now recommended "unrequired_new"

        // setup content type
        ContentTypeConfigBuilder ctyconf = new ContentTypeConfigBuilder( "MyContentType3", "title" );
        ctyconf.startBlock( "General" );
        ctyconf.addInput( "title", "text", "contentdata/title", "Title", true );
        ctyconf.addInput( "unrequired", "uploadfile", "contentdata/unrequired", "Unrequired", true );
        ctyconf.endBlock();

        createAndSaveContentTypeAndCategory( "MyContentType3", "MyCategory3", ctyconf );
        fixture.save( factory.createCategoryAccessForUser( "MyCategory3", "testuser", "read, create" ) );

        fixture.flushAndClearHibernateSession();

        Mockito.when(
            userServicesRedirectUrlResolver.resolveRedirectUrlToPage( Mockito.any( HttpServletRequest.class ), Mockito.anyString(),
                                                                      Mockito.any( MultiValueMap.class ) ) ).thenReturn( "http://anyurl" );

        // execise: create the content
        String categoryName = "MyCategory3";
        ExtendedMap formItems = new ExtendedMap( true );
        formItems.putString( "categorykey", fixture.findCategoryByName( categoryName ).getKey().toString() );
        formItems.putString( "title", "Title" );

        FileItem testFile = new MockFileItem( "thisIsATestFile".getBytes() );
        testFile.setFieldName( "testFile" );

        formItems.put( "unrequired", testFile );

        customContentHandlerController.handlerCreate( request, response, session, formItems, null, siteKey_1 );

        fixture.flushAndClearHibernateSession();

        verifyRedirectOk();

        CustomContentData contentData = getCustomContentDataResult( categoryName );

        assertEquals( "Title", ( (TextDataEntry) contentData.getEntry( "title" ) ).getValue() );
        assertTrue( "Unrequired", contentData.getEntry( "unrequired" ) != null );
        assertNotNull( ( (BinaryDataEntry) contentData.getEntry( "unrequired" ) ).getExistingBinaryKey() );
    }


    @Test
    public void handlerCreate_uploadfile_input()
        throws RemoteException
    {

        // This test submits the binary in the "new way" "unrequired"_new key

        // setup content type
        ContentTypeConfigBuilder ctyconf = new ContentTypeConfigBuilder( "MyContentType3", "title" );
        ctyconf.startBlock( "General" );
        ctyconf.addInput( "title", "text", "contentdata/title", "Title", true );
        ctyconf.addInput( "unrequired", "uploadfile", "contentdata/unrequired", "Unrequired", true );
        ctyconf.endBlock();

        createAndSaveContentTypeAndCategory( "MyContentType3", "MyCategory3", ctyconf );
        fixture.save( factory.createCategoryAccessForUser( "MyCategory3", "testuser", "read, create" ) );

        fixture.flushAndClearHibernateSession();

        Mockito.when(
            userServicesRedirectUrlResolver.resolveRedirectUrlToPage( Mockito.any( HttpServletRequest.class ), Mockito.anyString(),
                                                                      Mockito.any( MultiValueMap.class ) ) ).thenReturn( "http://anyurl" );

        // execise: create the content
        String categoryName = "MyCategory3";
        ExtendedMap formItems = new ExtendedMap( true );
        formItems.putString( "categorykey", fixture.findCategoryByName( categoryName ).getKey().toString() );
        formItems.putString( "title", "Title" );

        formItems.put( "unrequired_new", new MockFileItem( "thisIsATestFile".getBytes() ) );

        customContentHandlerController.handlerCreate( request, response, session, formItems, null, siteKey_1 );

        fixture.flushAndClearHibernateSession();

        verifyRedirectOk();

        CustomContentData contentData = getCustomContentDataResult( categoryName );

        assertEquals( "Title", ( (TextDataEntry) contentData.getEntry( "title" ) ).getValue() );
        assertTrue( "Unrequired", contentData.getEntry( "unrequired" ) != null );
        assertNotNull( ( (BinaryDataEntry) contentData.getEntry( "unrequired" ) ).getExistingBinaryKey() );
    }


    @Test
    public void handlerCreate_uploadfile_input_existing_binarykey()
        throws RemoteException
    {
        // This test submits an existing binary-key in the "unrequired" key

        // setup content type
        ContentTypeConfigBuilder ctyconf = new ContentTypeConfigBuilder( "MyContentType3", "title" );
        ctyconf.startBlock( "General" );
        ctyconf.addInput( "title", "text", "contentdata/title", "Title", true );
        ctyconf.addInput( "unrequired", "uploadfile", "contentdata/unrequired", "Unrequired", true );
        ctyconf.endBlock();

        createAndSaveContentTypeAndCategory( "MyContentType3", "MyCategory3", ctyconf );
        fixture.save( factory.createCategoryAccessForUser( "MyCategory3", "testuser", "read, create" ) );

        fixture.flushAndClearHibernateSession();

        Mockito.when(
            userServicesRedirectUrlResolver.resolveRedirectUrlToPage( Mockito.any( HttpServletRequest.class ), Mockito.anyString(),
                                                                      Mockito.any( MultiValueMap.class ) ) ).thenReturn( "http://anyurl" );

        // execise: create the content
        String categoryName = "MyCategory3";
        ExtendedMap formItems = new ExtendedMap( true );
        formItems.putString( "categorykey", fixture.findCategoryByName( categoryName ).getKey().toString() );
        formItems.putString( "title", "Title" );

        formItems.put( "unrequired", "123" );

        customContentHandlerController.handlerCreate( request, response, session, formItems, null, siteKey_1 );

        fixture.flushAndClearHibernateSession();

        verifyRedirectOk();

        CustomContentData contentData = getCustomContentDataResult( categoryName );

        assertEquals( "Title", ( (TextDataEntry) contentData.getEntry( "title" ) ).getValue() );
        assertTrue( "Unrequired", contentData.getEntry( "unrequired" ) != null );
        assertEquals( new Integer( 123 ), ( (BinaryDataEntry) contentData.getEntry( "unrequired" ) ).getExistingBinaryKey() );
    }

    @Test
    public void handlerCreate_uploadfile_input_existing_and_new_binarykey()
        throws RemoteException
    {
        // This test submits an existing binary-key in the "unrequired" key

        // setup content type
        ContentTypeConfigBuilder ctyconf = new ContentTypeConfigBuilder( "MyContentType3", "title" );
        ctyconf.startBlock( "General" );
        ctyconf.addInput( "title", "text", "contentdata/title", "Title", true );
        ctyconf.addInput( "unrequired", "uploadfile", "contentdata/unrequired", "Unrequired", true );
        ctyconf.endBlock();

        createAndSaveContentTypeAndCategory( "MyContentType3", "MyCategory3", ctyconf );
        fixture.save( factory.createCategoryAccessForUser( "MyCategory3", "testuser", "read, create" ) );

        fixture.flushAndClearHibernateSession();

        Mockito.when(
            userServicesRedirectUrlResolver.resolveRedirectUrlToPage( Mockito.any( HttpServletRequest.class ), Mockito.anyString(),
                                                                      Mockito.any( MultiValueMap.class ) ) ).thenReturn( "http://anyurl" );

        // execise: create the content
        String categoryName = "MyCategory3";
        ExtendedMap formItems = new ExtendedMap( true );
        formItems.putString( "categorykey", fixture.findCategoryByName( categoryName ).getKey().toString() );
        formItems.putString( "title", "Title" );

        int existingBinaryKey = Integer.MAX_VALUE;
        formItems.put( "unrequired", existingBinaryKey );
        formItems.put( "unrequired_new", new MockFileItem( "thisIsATestFile".getBytes() ) );

        customContentHandlerController.handlerCreate( request, response, session, formItems, null, siteKey_1 );

        fixture.flushAndClearHibernateSession();

        verifyRedirectOk();

        CustomContentData contentData = getCustomContentDataResult( categoryName );

        assertEquals( "Title", ( (TextDataEntry) contentData.getEntry( "title" ) ).getValue() );
        assertTrue( "Unrequired", contentData.getEntry( "unrequired" ) != null );
        assertNotNull( ( (BinaryDataEntry) contentData.getEntry( "unrequired" ) ).getExistingBinaryKey() );
        assertFalse(
            new Integer( existingBinaryKey ).equals( ( (BinaryDataEntry) contentData.getEntry( "unrequired" ) ).getExistingBinaryKey() ) );
    }


    private CustomContentData getCustomContentDataResult( String categoryName )
    {
        ContentEntity content = fixture.findFirstContentByCategory( fixture.findCategoryByName( categoryName ) );
        assertNotNull( content );
        ContentVersionEntity version = content.getMainVersion();
        CustomContentData contentData = (CustomContentData) version.getContentData();
        return contentData;
    }

    private void verifyRedirectOk()
    {
        Mockito.verify( userServicesRedirectUrlResolver ).resolveRedirectUrlToPage( Mockito.any( HttpServletRequest.class ),
                                                                                    Mockito.anyString(),
                                                                                    Mockito.any( MultiValueMap.class ) );
    }

    @Test
    public void handlerCreate_missing_required_input_of_type_checkbox_is_parsed_as_value_false()
        throws RemoteException
    {
        // setup content type
        ContentTypeConfigBuilder ctyconf = new ContentTypeConfigBuilder( "MyContentType4", "title" );
        ctyconf.startBlock( "General" );
        ctyconf.addInput( "title", "text", "contentdata/title", "Title", true );
        ctyconf.addInput( "myCheckbox_missing", "checkbox", "contentdata/mycheckbox_false2", "My checkbox to be missed", true );
        ctyconf.endBlock();

        createAndSaveContentTypeAndCategory( "MyContentType4", "MyCategory4", ctyconf );
        fixture.save( factory.createCategoryAccessForUser( "MyCategory4", "testuser", "read, create" ) );

        fixture.flushAndClearHibernateSession();

        Mockito.when(
            userServicesRedirectUrlResolver.resolveRedirectUrlToPage( Mockito.any( HttpServletRequest.class ), Mockito.anyString(),
                                                                      Mockito.any( MultiValueMap.class ) ) ).thenReturn( "http://anyurl" );

        // execise: create the content
        ExtendedMap formItems = new ExtendedMap( true );
        formItems.putString( "categorykey", fixture.findCategoryByName( "MyCategory4" ).getKey().toString() );
        formItems.putString( "title", "Title" );
        customContentHandlerController.handlerCreate( request, response, session, formItems, null, siteKey_1 );

        fixture.flushAndClearHibernateSession();

        // verify no error by checking that correct redirect was done (enough checking that right method was called)
        Mockito.verify( userServicesRedirectUrlResolver ).resolveRedirectUrlToPage( Mockito.any( HttpServletRequest.class ),
                                                                                    Mockito.anyString(),
                                                                                    Mockito.any( MultiValueMap.class ) );

        // verify
        ContentEntity content = fixture.findFirstContentByCategory( fixture.findCategoryByName( "MyCategory4" ) );
        assertNotNull( content );
        ContentVersionEntity version = content.getMainVersion();
        CustomContentData contentData = (CustomContentData) version.getContentData();

        assertEquals( "Title", ( (TextDataEntry) contentData.getEntry( "title" ) ).getValue() );
        assertEquals( Boolean.FALSE, ( (BooleanDataEntry) contentData.getEntry( "myCheckbox_missing" ) ).getValueAsBoolean() );
    }

    @Test
    public void handlerCreate_with_string_inputs_that_contains_special_chars_and_encodings()
        throws RemoteException
    {
        // setup content type
        ContentTypeConfigBuilder ctyconf = new ContentTypeConfigBuilder( "MyContentType4", "title" );
        ctyconf.startBlock( "General" );
        ctyconf.addInput( "title", "text", "contentdata/title", "Title", true );
        ctyconf.addInput( "myText_eurosymbol", "text", "contentdata/mytext_eurosymbol", "My text", false );
        ctyconf.addInput( "myText_ampsymbol", "text", "contentdata/mytext_ampsymbol", "My text", false );
        ctyconf.addInput( "myText_ampenc", "text", "contentdata/mytext_ampenc", "My text", false );
        ctyconf.addInput( "myTextarea_ampsymbol", "textarea", "contentdata/mytextarea_ampsymbol", "My textarea", false );
        ctyconf.addInput( "myTextarea_ampenc", "textarea", "contentdata/mytextarea_ampenc", "My textarea", false );
        ctyconf.addInput( "myHtmlarea_ampenc", "htmlarea", "contentdata/myhtmlarea_ampenc", "My htmlarea", false );
        ctyconf.addInput( "myHtmlarea_ampenc_encoded", "htmlarea", "contentdata/myhtmlarea_ampenc_encoded", "My htmlarea", false );
        ctyconf.addInput( "myXml_ampenc", "xml", "contentdata/myxml_ampenc", "My xml", false );
        ctyconf.endBlock();

        createAndSaveContentTypeAndCategory( "MyContentType4", "MyCategory4", ctyconf );
        fixture.save( factory.createCategoryAccessForUser( "MyCategory4", "testuser", "read, create" ) );

        fixture.flushAndClearHibernateSession();

        Mockito.when(
            userServicesRedirectUrlResolver.resolveRedirectUrlToPage( Mockito.any( HttpServletRequest.class ), Mockito.anyString(),
                                                                      Mockito.any( MultiValueMap.class ) ) ).thenReturn( "http://anyurl" );

        // execise: create the content
        ExtendedMap formItems = new ExtendedMap( true );
        formItems.putString( "categorykey", fixture.findCategoryByName( "MyCategory4" ).getKey().toString() );
        formItems.putString( "title", "Title" );
        formItems.putString( "myText_eurosymbol", "€" );
        formItems.putString( "myText_ampsymbol", "&" );
        formItems.putString( "myText_ampenc", "&amp;" );
        formItems.putString( "myTextarea_ampsymbol", "&" );
        formItems.putString( "myTextarea_ampenc", "&amp;" );
        formItems.putString( "myHtmlarea_ampenc", "&amp;" );
        formItems.putString( "myHtmlarea_ampenc_encoded", "&amp;amp;" );
        formItems.putString( "myXml_ampenc", "<data>&amp;</data>" );
        customContentHandlerController.handlerCreate( request, response, session, formItems, null, siteKey_1 );

        fixture.flushAndClearHibernateSession();

        // verify no error by checking that correct redirect was done (enough checking that right method was called)
        verifyRedirectOk();

        // verify
        CustomContentData contentData = getCustomContentDataResult( "MyCategory4" );

        assertEquals( "Title", ( (TextDataEntry) contentData.getEntry( "title" ) ).getValue() );
        assertEquals( "€", ( (TextDataEntry) contentData.getEntry( "myText_eurosymbol" ) ).getValue() );
        assertEquals( "&", ( (TextDataEntry) contentData.getEntry( "myText_ampsymbol" ) ).getValue() );
        assertEquals( "&amp;", ( (TextDataEntry) contentData.getEntry( "myText_ampenc" ) ).getValue() );
        assertEquals( "&", ( (TextAreaDataEntry) contentData.getEntry( "myTextarea_ampsymbol" ) ).getValue() );
        assertEquals( "&amp;", ( (TextAreaDataEntry) contentData.getEntry( "myTextarea_ampenc" ) ).getValue() );
        assertEquals( "&amp;", ( (HtmlAreaDataEntry) contentData.getEntry( "myHtmlarea_ampenc" ) ).getValue() );
        assertEquals( "&amp;amp;", ( (HtmlAreaDataEntry) contentData.getEntry( "myHtmlarea_ampenc_encoded" ) ).getValue() );
        assertEquals( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n<data>&amp;</data>\r\n\r\n",
                      ( (XmlDataEntry) contentData.getEntry( "myXml_ampenc" ) ).getValueAsString() );
        assertEquals( "&", ( (XmlDataEntry) contentData.getEntry( "myXml_ampenc" ) ).getValue().getRootElement().getText() );
    }


    private void createAndSaveContentTypeAndCategory( String contentTypeName, String categoryName, ContentTypeConfigBuilder ctyconf )
    {
        Document configAsXmlBytes = XMLDocumentFactory.create( ctyconf.toString() ).getAsJDOMDocument();
        fixture.save(
            factory.createContentType( contentTypeName, ContentHandlerName.CUSTOM.getHandlerClassShortName(), configAsXmlBytes ) );

        fixture.flushAndClearHibernateSession();

        createAndSaveCategoryOfContentType( categoryName, contentTypeName );
    }

    private void createAndSaveCategoryOfContentType( String categoryName, String contentTypeName )
    {
        String unitName = "UnitFor_" + categoryName;
        fixture.save( factory.createUnit( unitName, "en" ) );

        fixture.flushAndClearHibernateSession();

        final CategoryEntity categoryEntity =
            factory.createCategory( categoryName, null, contentTypeName, unitName, User.ANONYMOUS_UID, User.ANONYMOUS_UID, true );

        fixture.save( categoryEntity );

        fixture.flushAndClearHibernateSession();
    }

}
