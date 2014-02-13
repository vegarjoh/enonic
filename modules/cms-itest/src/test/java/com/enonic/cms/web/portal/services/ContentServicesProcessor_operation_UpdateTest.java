/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.web.portal.services;

import java.rmi.RemoteException;

import org.jdom.Document;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.orm.hibernate3.HibernateTemplate;

import junitx.framework.Assert;

import com.enonic.esl.containers.ExtendedMap;

import com.enonic.cms.framework.xml.XMLDocumentFactory;

import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentService;
import com.enonic.cms.core.content.ContentVersionEntity;
import com.enonic.cms.core.content.category.CategoryEntity;
import com.enonic.cms.core.content.contentdata.custom.BinaryDataEntry;
import com.enonic.cms.core.content.contentdata.custom.BooleanDataEntry;
import com.enonic.cms.core.content.contentdata.custom.CustomContentData;
import com.enonic.cms.core.content.contentdata.custom.stringbased.TextDataEntry;
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
import com.enonic.cms.store.dao.ContentDao;
import com.enonic.cms.web.portal.SiteRedirectHelper;

import static org.easymock.classextension.EasyMock.createMock;
import static org.junit.Assert.*;

public class ContentServicesProcessor_operation_UpdateTest
    extends AbstractSpringTest
{
    @Autowired
    private ContentService contenService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private CategoryDao categoryDao;

    @Autowired
    private ContentDao contentDao;

    @Autowired
    private HibernateTemplate hibernateTemplate;

    private SiteRedirectHelper siteRedirectHelper;

    private ContentServicesProcessor customContentHandlerController;

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
        customContentHandlerController.setContentService( contenService );
        customContentHandlerController.setSecurityService( securityService );
        customContentHandlerController.setCategoryDao( categoryDao );
        customContentHandlerController.setContentDao( contentDao );
        customContentHandlerController.setUserServicesRedirectHelper( new UserServicesRedirectUrlResolver() );

        // just need a dummy of the SiteRedirectHelper
        siteRedirectHelper = createMock( SiteRedirectHelper.class );
        customContentHandlerController.setSiteRedirectHelper( siteRedirectHelper );

        // setup
        fixture.initSystemData();
        fixture.save( factory.createContentHandler( "Custom content", ContentHandlerName.CUSTOM.getHandlerClassShortName() ) );
        fixture.createAndStoreNormalUserWithUserGroup( "testuser", "Test user", "testuserstore" );
        PortalSecurityHolder.setAnonUser( fixture.findUserByName( "anonymous" ).getKey() );
        PortalSecurityHolder.setImpersonatedUser( fixture.findUserByName( "testuser" ).getKey() );
        PortalSecurityHolder.setLoggedInUser( fixture.findUserByName( "testuser" ).getKey() );

        fixture.flushAndClearHibernateSession();
    }

    @Test
    public void update_with_text_input()
        throws RemoteException
    {
        // setup content type
        ContentTypeConfigBuilder ctyconf = new ContentTypeConfigBuilder( "MyContentType", "myTitle" );
        ctyconf.startBlock( "General" );
        ctyconf.addInput( "myTitle", "text", "contentdata/mytitle", "Mandantory", true );
        ctyconf.addInput( "tochange", "text", "contentdata/toupdate", "To be changed", false );
        ctyconf.addInput( "tobeempty", "text", "contentdata/tobeempty", "To be set to empty", false );
        ctyconf.addInput( "tobenull", "text", "contentdata/tobenull", "To be set to null", false );
        ctyconf.addInput( "toalsobeempty", "text", "contentdata/toalsobeempty", "To also be set to empty", false );
        ctyconf.endBlock();
        Document configAsXmlBytes = XMLDocumentFactory.create( ctyconf.toString() ).getAsJDOMDocument();
        fixture.save(
            factory.createContentType( "MyContentType", ContentHandlerName.CUSTOM.getHandlerClassShortName(), configAsXmlBytes ) );

        // setup content repository
        fixture.save( factory.createUnit( "MyUnit" ) );
        final CategoryEntity categoryEntity =
            factory.createCategory( "MyCategory", null, "MyContentType", "MyUnit", "testuser", "testuser", true );
        fixture.save( categoryEntity );
        fixture.save( factory.createCategoryAccessForUser( "MyCategory", "testuser", "read, create" ) );

        hibernateTemplate.flush();
        hibernateTemplate.clear();

        // setup: create the content to update
        ExtendedMap formItems = new ExtendedMap( true );
        formItems.putString( "categorykey", fixture.findCategoryByName( "MyCategory" ).getKey().toString() );
        formItems.putString( "myTitle", "Mandantory" );
        formItems.putString( "tochange", "Initial" );
        formItems.putString( "tobeempty", "Not empty" );
        formItems.putString( "tobenull", "Not empty" );
        formItems.putString( "toalsobeempty", "Not empty" );
        customContentHandlerController.handlerCreate( request, response, session, formItems, null, siteKey_1 );

        fixture.flushAndClearHibernateSession();

        // execise: update the content
        formItems = new ExtendedMap( true );
        formItems.putString( "key", fixture.findFirstContentByCategory( fixture.findCategoryByName( "MyCategory" ) ).getKey().toString() );
        formItems.putString( "myTitle", "Mandantory" );
        formItems.putString( "tochange", "Changed" );
        formItems.putString( "tobeempty", "" );
        formItems.putString( "toalsobeempty", null );
        customContentHandlerController.handlerUpdate( request, response, session, formItems, null, siteKey_1 );

        fixture.flushAndClearHibernateSession();

        // verify
        ContentEntity content = fixture.findFirstContentByCategory( fixture.findCategoryByName( "MyCategory" ) );
        assertNotNull( content );
        ContentVersionEntity version = content.getMainVersion();
        CustomContentData contentData = (CustomContentData) version.getContentData();

        assertEquals( "Changed", ( (TextDataEntry) contentData.getEntry( "tochange" ) ).getValue() );

        assertTrue( contentData.getEntry( "tobeempty" ).hasValue() );
        assertEquals( "", ( (TextDataEntry) contentData.getEntry( "tobeempty" ) ).getValue() );

        assertFalse( contentData.getEntry( "tobenull" ).hasValue() );
        assertEquals( null, ( (TextDataEntry) contentData.getEntry( "tobenull" ) ).getValue() );

        assertEquals( "", ( (TextDataEntry) contentData.getEntry( "toalsobeempty" ) ).getValue() );
        assertTrue( contentData.getEntry( "toalsobeempty" ).hasValue() );
    }

    @Test
    public void update_with_checkbox_input_with_no_value_becomes_false()
        throws RemoteException
    {
        // setup content type
        ContentTypeConfigBuilder ctyconf = new ContentTypeConfigBuilder( "MyContentType", "myTitle" );
        ctyconf.startBlock( "General" );
        ctyconf.addInput( "myTitle", "text", "contentdata/mytitle", "Mandantory", true );
        ctyconf.addInput( "myCheckbox", "checkbox", "contentdata/mycheckbox", "My checkbox", false );
        ctyconf.endBlock();
        Document configAsXmlBytes = XMLDocumentFactory.create( ctyconf.toString() ).getAsJDOMDocument();
        fixture.save(
            factory.createContentType( "MyContentType", ContentHandlerName.CUSTOM.getHandlerClassShortName(), configAsXmlBytes ) );

        // setup content repository
        fixture.save( factory.createUnit( "MyUnit" ) );
        final CategoryEntity categoryEntity =
            factory.createCategory( "MyCategory", null, "MyContentType", "MyUnit", User.ANONYMOUS_UID, User.ANONYMOUS_UID, true );
        fixture.save( categoryEntity );
        fixture.save( factory.createCategoryAccessForUser( "MyCategory", "testuser", "read, create" ) );

        fixture.flushAndClearHibernateSession();

        // setup: create the content to update
        ExtendedMap formItems = new ExtendedMap( true );
        formItems.putString( "categorykey", fixture.findCategoryByName( "MyCategory" ).getKey().toString() );
        formItems.putString( "myTitle", "Mandantory" );
        formItems.putString( "myCheckbox", "true" );
        customContentHandlerController.handlerCreate( request, response, session, formItems, null, siteKey_1 );

        fixture.flushAndClearHibernateSession();

        // execise: update the content
        formItems = new ExtendedMap( true );
        formItems.putString( "key", fixture.findFirstContentByCategory( fixture.findCategoryByName( "MyCategory" ) ).getKey().toString() );
        formItems.putString( "myTitle", "Mandantory" );
        customContentHandlerController.handlerUpdate( request, response, session, formItems, null, siteKey_1 );

        fixture.flushAndClearHibernateSession();

        // verify
        ContentEntity content = fixture.findFirstContentByCategory( fixture.findCategoryByName( "MyCategory" ) );
        assertNotNull( content );
        ContentVersionEntity version = content.getMainVersion();
        CustomContentData contentData = (CustomContentData) version.getContentData();

        assertEquals( "false", ( (BooleanDataEntry) contentData.getEntry( "myCheckbox" ) ).getValueAsString() );
    }


    @Test
    public void handlerUpdate_uploadfile_input_keep_existing_binary()
        throws RemoteException
    {
        String myCategoryName = "MyCategory3";
        String myContentTypeName = "MyContentType3";

        ContentTypeConfigBuilder ctyconf = setUpUploadFileTestContent( myContentTypeName, false );

        createAndSaveContentTypeAndCategory( myContentTypeName, myCategoryName, ctyconf );
        fixture.save( factory.createCategoryAccessForUser( "MyCategory3", "testuser", "read, create" ) );

        fixture.flushAndClearHibernateSession();

        // execise: create the content
        String categoryName = myCategoryName;
        ExtendedMap formItems = new ExtendedMap( true );
        formItems.putString( "categorykey", fixture.findCategoryByName( categoryName ).getKey().toString() );
        formItems.putString( "title", "Title" );
        formItems.put( "unrequired_new", new MockFileItem( "thisIsATestFile".getBytes() ) );

        customContentHandlerController.handlerCreate( request, response, session, formItems, null, siteKey_1 );

        fixture.flushAndClearHibernateSession();

        CustomContentData contentData = getCustomContentDataResult( categoryName );
        Integer existingBinaryKey = getExistingBinaryKey( contentData, "unrequired" );

        // execise: modify the content
        formItems = new ExtendedMap( true );
        formItems.putString( "key",
                             fixture.findFirstContentByCategory( fixture.findCategoryByName( myCategoryName ) ).getKey().toString() );
        formItems.putString( "title", "Title2" );
        formItems.put( "unrequired", existingBinaryKey );

        customContentHandlerController.handlerUpdate( request, response, session, formItems, null, siteKey_1 );

        fixture.flushAndClearHibernateSession();

        contentData = getCustomContentDataResult( categoryName );

        assertEquals( getExistingBinaryKey( contentData, "unrequired" ), existingBinaryKey );
        assertEquals( "Title2", ( (TextDataEntry) contentData.getEntry( "title" ) ).getValue() );
    }

    @Test
    public void handlerUpdate_uploadfile_input_overwrite_existing_binary()
        throws RemoteException
    {
        String myContentTypeName = "MyContentType3";

        ContentTypeConfigBuilder ctyconf = setUpUploadFileTestContent( myContentTypeName, false );

        createAndSaveContentTypeAndCategory( myContentTypeName, "MyCategory3", ctyconf );
        fixture.save( factory.createCategoryAccessForUser( "MyCategory3", "testuser", "read, create" ) );

        fixture.flushAndClearHibernateSession();

        // execise: create the content
        String categoryName = "MyCategory3";
        ExtendedMap formItems = new ExtendedMap( true );
        formItems.putString( "categorykey", fixture.findCategoryByName( categoryName ).getKey().toString() );
        formItems.putString( "title", "Title" );
        formItems.put( "unrequired_new", new MockFileItem( "thisIsATestFile".getBytes() ) );

        customContentHandlerController.handlerCreate( request, response, session, formItems, null, siteKey_1 );

        fixture.flushAndClearHibernateSession();

        CustomContentData contentData = getCustomContentDataResult( categoryName );
        Integer existingBinaryKey = getExistingBinaryKey( contentData, "unrequired" );

        // execise: update the content
        formItems = new ExtendedMap( true );
        formItems.putString( "key", fixture.findFirstContentByCategory( fixture.findCategoryByName( "MyCategory3" ) ).getKey().toString() );
        formItems.putString( "title", "Title2" );
        formItems.put( "unrequired", existingBinaryKey );
        formItems.put( "unrequired_new", new MockFileItem( "thisIsATestFile".getBytes() ) );

        customContentHandlerController.handlerUpdate( request, response, session, formItems, null, siteKey_1 );

        fixture.flushAndClearHibernateSession();

        contentData = getCustomContentDataResult( categoryName );

        Assert.assertFalse( getExistingBinaryKey( contentData, "unrequired" ).equals( existingBinaryKey ) );
        assertEquals( "Title2", ( (TextDataEntry) contentData.getEntry( "title" ) ).getValue() );
    }

    @Test
    public void handlerUpdate_uploadfile_input_remove_existing_binary()
        throws RemoteException
    {
        String myContentTypeName = "MyContentType3";

        ContentTypeConfigBuilder ctyconf = setUpUploadFileTestContent( myContentTypeName, false );

        createAndSaveContentTypeAndCategory( myContentTypeName, "MyCategory3", ctyconf );
        fixture.save( factory.createCategoryAccessForUser( "MyCategory3", "testuser", "read, create" ) );

        fixture.flushAndClearHibernateSession();

        // execise: create the content
        String categoryName = "MyCategory3";
        ExtendedMap formItems = new ExtendedMap( true );
        formItems.putString( "categorykey", fixture.findCategoryByName( categoryName ).getKey().toString() );
        formItems.putString( "title", "Title" );
        formItems.put( "unrequired_new", new MockFileItem( "thisIsATestFile".getBytes() ) );

        customContentHandlerController.handlerCreate( request, response, session, formItems, null, siteKey_1 );

        fixture.flushAndClearHibernateSession();

        // execise: update the content
        formItems = new ExtendedMap( true );
        formItems.putString( "key", fixture.findFirstContentByCategory( fixture.findCategoryByName( "MyCategory3" ) ).getKey().toString() );
        formItems.putString( "title", "Title2" );
        formItems.put( "unrequired", "" );

        customContentHandlerController.handlerUpdate( request, response, session, formItems, null, siteKey_1 );

        fixture.flushAndClearHibernateSession();

        CustomContentData contentData = getCustomContentDataResult( categoryName );

        // The binary should have been removed
        assertBinaryDataEntryHasNoValue( contentData, "unrequired" );

        assertEquals( "Title2", ( (TextDataEntry) contentData.getEntry( "title" ) ).getValue() );
    }


    private ContentTypeConfigBuilder setUpUploadFileTestContent( String myContentTypeName, boolean requiredUploadFile )
    {
        // setup content type
        ContentTypeConfigBuilder ctyconf = new ContentTypeConfigBuilder( myContentTypeName, "title" );
        ctyconf.startBlock( "General" );
        ctyconf.addInput( "title", "text", "contentdata/title", "Title", true );
        ctyconf.addInput( "unrequired", "uploadfile", "contentdata/unrequired", "Unrequired", requiredUploadFile );
        ctyconf.endBlock();
        return ctyconf;
    }

    private Integer getExistingBinaryKey( CustomContentData contentData, String entryName )
    {
        return ( (BinaryDataEntry) contentData.getEntry( entryName ) ).getExistingBinaryKey();
    }

    private void assertBinaryDataEntryHasNoValue( CustomContentData contentData, String entryName )
    {
        assertTrue( ( (BinaryDataEntry) contentData.getEntry( entryName ) ).hasNullBinaryKey() );
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

    private CustomContentData getCustomContentDataResult( String categoryName )
    {
        ContentEntity content = fixture.findFirstContentByCategory( fixture.findCategoryByName( categoryName ) );
        assertNotNull( content );
        ContentVersionEntity version = content.getMainVersion();
        CustomContentData contentData = (CustomContentData) version.getContentData();
        return contentData;
    }

}