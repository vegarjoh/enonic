/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.itest.content;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jdom.Document;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.enonic.cms.framework.xml.XMLDocumentFactory;

import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.ContentService;
import com.enonic.cms.core.content.ContentStatus;
import com.enonic.cms.core.content.ContentVersionEntity;
import com.enonic.cms.core.content.ContentVersionKey;
import com.enonic.cms.core.content.RelatedContentEntity;
import com.enonic.cms.core.content.command.CreateContentCommand;
import com.enonic.cms.core.content.command.UpdateContentCommand;
import com.enonic.cms.core.content.contentdata.custom.CustomContentData;
import com.enonic.cms.core.content.contentdata.custom.contentkeybased.RelatedContentDataEntry;
import com.enonic.cms.core.content.contentdata.custom.stringbased.TextDataEntry;
import com.enonic.cms.core.content.contenttype.ContentHandlerName;
import com.enonic.cms.core.content.contenttype.ContentTypeConfigBuilder;
import com.enonic.cms.core.security.PortalSecurityHolder;
import com.enonic.cms.core.security.user.User;
import com.enonic.cms.core.security.user.UserType;
import com.enonic.cms.itest.AbstractSpringTest;
import com.enonic.cms.itest.util.DomainFactory;
import com.enonic.cms.itest.util.DomainFixture;
import com.enonic.cms.store.dao.ContentDao;

import static org.junit.Assert.*;

public class ContentServiceImpl_storeRelatedContentTest
    extends AbstractSpringTest
{
    @Autowired
    protected ContentDao contentDao;

    @Autowired
    protected ContentService contentService;


    private DomainFactory factory;

    @Autowired
    private DomainFixture fixture;

    private ContentKey related1;

    private ContentKey related2;

    private ContentKey related3;


    @Before
    public void setUp()
    {

        factory = fixture.getFactory();

        // setup needed common data for each test
        fixture.initSystemData();

        fixture.createAndStoreUserAndUserGroup( "testuser", "testuser fullname", UserType.NORMAL, "testuserstore" );

        //SecurityHolder.setUser( findUserByName( User.ANONYMOUS_UID ).getKey() );
        PortalSecurityHolder.setAnonUser( fixture.findUserByName( User.ANONYMOUS_UID ).getKey() );
        fixture.save( factory.createContentHandler( "Custom content", ContentHandlerName.CUSTOM.getHandlerClassShortName() ) );

        fixture.flushAndClearHibernateSession();

        // setup content type
        ContentTypeConfigBuilder ctyconf = new ContentTypeConfigBuilder( "MyRelatedTypes", "title" );
        ctyconf.startBlock( "MyRelatedTypes" );
        ctyconf.addInput( "title", "text", "contentdata/title", "Title", true );
        ctyconf.addRelatedContentInput( "mySingleRelatedToBeUnmodified", "contentdata/mySingleRelatedToBeUnmodified", "My related1", false,
                                        false );
        ctyconf.addRelatedContentInput( "mySingleRelatedToBeModified", "contentdata/mySingleRelatedToBeModified", "My related2", false,
                                        false );
        ctyconf.endBlock();
        Document configAsXmlBytes = XMLDocumentFactory.create( ctyconf.toString() ).getAsJDOMDocument();
        fixture.save(
            factory.createContentType( "MyRelatedTypes", ContentHandlerName.CUSTOM.getHandlerClassShortName(), configAsXmlBytes ) );

        fixture.flushAndClearHibernateSession();

        fixture.save( factory.createUnit( "UnitForMyRelatedTypes", "en" ) );
        fixture.save( factory.createCategory( "MyCategory", null, "MyRelatedTypes", "UnitForMyRelatedTypes", "testuser", "testuser" ) );
        fixture.save( factory.createCategoryAccessForUser( "MyCategory", "testuser", "read, create, approve" ) );

        fixture.flushAndClearHibernateSession();

        // setup some content to relate to
        related1 = storeContentToRelatedTo( "related-1" );
        related2 = storeContentToRelatedTo( "related-2" );
        related3 = storeContentToRelatedTo( "related-3" );

        fixture.flushAndClearHibernateSession();
    }

    @Test
    public void create_new_content_version_retain_unmodified_related_content_after_modification_of_other_related_content_input()
    {
        // setup content to update
        CreateContentCommand createCommand = new CreateContentCommand();
        createCommand.setLanguage( fixture.findLanguageByCode( "en" ).getKey() );
        createCommand.setCategory( fixture.findCategoryByName( "MyCategory" ).getKey() );
        createCommand.setCreator( fixture.findUserByName( "testuser" ).getKey() );
        createCommand.setStatus( ContentStatus.APPROVED );
        createCommand.setPriority( 0 );
        createCommand.setContentName( "testcontent" );

        CustomContentData contentData = new CustomContentData( fixture.findContentTypeByName( "MyRelatedTypes" ).getContentTypeConfig() );
        contentData.add( new TextDataEntry( contentData.getInputConfig( "title" ), "Title 1" ) );
        contentData.add( new RelatedContentDataEntry( contentData.getInputConfig( "mySingleRelatedToBeUnmodified" ), related1 ) );
        contentData.add( new RelatedContentDataEntry( contentData.getInputConfig( "mySingleRelatedToBeModified" ), related2 ) );
        createCommand.setContentData( contentData );

        ContentKey createdContentKey = contentService.createContent( createCommand );

        fixture.flushAndClearHibernateSession();

        ContentVersionEntity firstVersion = fixture.findContentByKey( createdContentKey ).getMainVersion();
        ContentVersionKey firstVersionKey = firstVersion.getKey();

        // verify that created content has expected contentdata
        assertEquals( contentData, firstVersion.getContentData() );

        // verify that created content has expected contentdata
        CustomContentData actualContentData = (CustomContentData) firstVersion.getContentData();
        assertNotNull( actualContentData.getEntry( "mySingleRelatedToBeUnmodified" ) );
        assertNotNull( actualContentData.getEntry( "mySingleRelatedToBeModified" ) );
        assertEquals( related1,
                      ( (RelatedContentDataEntry) actualContentData.getEntry( "mySingleRelatedToBeUnmodified" ) ).getContentKey() );
        assertEquals( related2, ( (RelatedContentDataEntry) actualContentData.getEntry( "mySingleRelatedToBeModified" ) ).getContentKey() );

        // verify that setup content has expected related content
        List<ContentKey> expectedRelatedContent = ContentKey.convertToList( related1, related2 );
        List<ContentKey> actualRelatedContent = extractContentKeys( fixture.findRelatedContentsByContentVersionKey( firstVersionKey ) );
        assertStoredRelatedContent( expectedRelatedContent, actualRelatedContent );

        fixture.flushAndClearHibernateSession();

        // exercise
        UpdateContentCommand updateCommand = UpdateContentCommand.storeNewVersionEvenIfUnchanged( firstVersionKey );
        updateCommand.setContentKey( fixture.findFirstContentVersionByTitle( "Title 1" ).getContent().getKey() );
        updateCommand.setUpdateStrategy( UpdateContentCommand.UpdateStrategy.MODIFY );
        updateCommand.setStatus( ContentStatus.DRAFT );
        updateCommand.setModifier( fixture.findUserByName( "testuser" ).getKey() );
        contentData = new CustomContentData( fixture.findContentTypeByName( "MyRelatedTypes" ).getContentTypeConfig() );
        contentData.add( new RelatedContentDataEntry( contentData.getInputConfig( "mySingleRelatedToBeModified" ), related3 ) );
        updateCommand.setContentData( contentData );

        ContentVersionKey targedVersion = contentService.updateContent( updateCommand ).getTargetedVersionKey();

        // verify that updated content has expected contentdata
        ContentVersionEntity secondVersion = fixture.findContentVersionByKey( targedVersion );
        actualContentData = (CustomContentData) secondVersion.getContentData();
        assertNotNull( actualContentData.getEntry( "mySingleRelatedToBeUnmodified" ) );
        assertNotNull( actualContentData.getEntry( "mySingleRelatedToBeModified" ) );
        assertEquals( related1,
                      ( (RelatedContentDataEntry) actualContentData.getEntry( "mySingleRelatedToBeUnmodified" ) ).getContentKey() );
        assertEquals( related3, ( (RelatedContentDataEntry) actualContentData.getEntry( "mySingleRelatedToBeModified" ) ).getContentKey() );

        assertEquals( 2, fixture.countContentVersionsByTitle( "Title 1" ) );

        fixture.flushAndClearHibernateSession();

        // verify stored related content
        expectedRelatedContent = ContentKey.convertToList( related1, related3 );
        actualRelatedContent = extractContentKeys( fixture.findRelatedContentsByContentVersionKey( targedVersion ) );
        assertStoredRelatedContent( expectedRelatedContent, actualRelatedContent );
    }


    @Test
    public void given_existing_content_with_related_content_and_the_related_content_does_not_exist_when_modifying_content_then_the_relation_in_contentdata_should_be_marked_as_deleted()
    {
        // setup content to update
        CreateContentCommand createCommand = new CreateContentCommand();
        createCommand.setLanguage( fixture.findLanguageByCode( "en" ).getKey() );
        createCommand.setCategory( fixture.findCategoryByName( "MyCategory" ).getKey() );
        createCommand.setCreator( fixture.findUserByName( "testuser" ).getKey() );
        createCommand.setStatus( ContentStatus.APPROVED );
        createCommand.setPriority( 0 );
        createCommand.setContentName( "testcontent" );

        CustomContentData contentData = new CustomContentData( fixture.findContentTypeByName( "MyRelatedTypes" ).getContentTypeConfig() );
        contentData.add( new TextDataEntry( contentData.getInputConfig( "title" ), "Title 1" ) );
        contentData.add( new RelatedContentDataEntry( contentData.getInputConfig( "mySingleRelatedToBeUnmodified" ), related1 ) );
        contentData.add( new RelatedContentDataEntry( contentData.getInputConfig( "mySingleRelatedToBeModified" ), related2 ) );
        createCommand.setContentData( contentData );

        ContentKey createdContentKey = contentService.createContent( createCommand );

        fixture.flushAndClearHibernateSession();

        ContentVersionEntity firstVersion = fixture.findContentByKey( createdContentKey ).getMainVersion();
        ContentVersionKey firstVersionKey = firstVersion.getKey();

        // verify that created content has expected contentdata
        assertEquals( contentData, firstVersion.getContentData() );

        // verify that created content has expected contentdata
        CustomContentData actualContentData = (CustomContentData) firstVersion.getContentData();
        assertNotNull( actualContentData.getEntry( "mySingleRelatedToBeUnmodified" ) );
        assertNotNull( actualContentData.getEntry( "mySingleRelatedToBeModified" ) );
        assertEquals( related1,
                      ( (RelatedContentDataEntry) actualContentData.getEntry( "mySingleRelatedToBeUnmodified" ) ).getContentKey() );
        assertEquals( related2, ( (RelatedContentDataEntry) actualContentData.getEntry( "mySingleRelatedToBeModified" ) ).getContentKey() );

        // verify that setup content has expected related content
        List<ContentKey> expectedRelatedContent = ContentKey.convertToList( related1, related2 );
        List<ContentKey> actualRelatedContent = extractContentKeys( fixture.findRelatedContentsByContentVersionKey( firstVersionKey ) );
        assertStoredRelatedContent( expectedRelatedContent, actualRelatedContent );

        fixture.flushAndClearHibernateSession();

        //////////////////////////////////// delete related ////////////////////////////////////

        final ContentEntity contentEntity = ContentEntity.class.cast( fixture.findContentByKey( related1 ) );
        contentService.deleteContent( fixture.findUserByName( "testuser" ), contentEntity );

        //////////////////////////////////////// update ////////////////////////////////////////

        // exercise
        UpdateContentCommand updateCommand = UpdateContentCommand.storeNewVersionEvenIfUnchanged( firstVersionKey );
        updateCommand.setContentKey( fixture.findFirstContentVersionByTitle( "Title 1" ).getContent().getKey() );
        updateCommand.setUpdateStrategy( UpdateContentCommand.UpdateStrategy.MODIFY );
        updateCommand.setStatus( ContentStatus.DRAFT );
        updateCommand.setModifier( fixture.findUserByName( "testuser" ).getKey() );
        contentData = new CustomContentData( fixture.findContentTypeByName( "MyRelatedTypes" ).getContentTypeConfig() );
        contentData.add( new RelatedContentDataEntry( contentData.getInputConfig( "mySingleRelatedToBeModified" ), related3 ) );
        updateCommand.setContentData( contentData );

        ContentVersionKey targedVersion = contentService.updateContent( updateCommand ).getTargetedVersionKey();

        // verify that updated content has expected contentdata
        ContentVersionEntity secondVersion = fixture.findContentVersionByKey( targedVersion );
        actualContentData = (CustomContentData) secondVersion.getContentData();
        assertNotNull( actualContentData.getEntry( "mySingleRelatedToBeUnmodified" ) );
        assertNotNull( actualContentData.getEntry( "mySingleRelatedToBeModified" ) );
        assertEquals( related1,
                      ( (RelatedContentDataEntry) actualContentData.getEntry( "mySingleRelatedToBeUnmodified" ) ).getContentKey() );
        assertEquals( related3, ( (RelatedContentDataEntry) actualContentData.getEntry( "mySingleRelatedToBeModified" ) ).getContentKey() );

        assertEquals( 2, fixture.countContentVersionsByTitle( "Title 1" ) );

        fixture.flushAndClearHibernateSession();

        // verify stored related content
        expectedRelatedContent = ContentKey.convertToList( related1, related3 );
        actualRelatedContent = extractContentKeys( fixture.findRelatedContentsByContentVersionKey( targedVersion ) );
        assertStoredRelatedContent( expectedRelatedContent, actualRelatedContent );
    }


    @Test
    public void update_version_retain_unmodified_related_content_after_modification_of_other_related_content_input()
    {
        // setup content to update
        CreateContentCommand createCommand = new CreateContentCommand();
        createCommand.setLanguage( fixture.findLanguageByCode( "en" ).getKey() );
        createCommand.setCategory( fixture.findCategoryByName( "MyCategory" ).getKey() );
        createCommand.setCreator( fixture.findUserByName( "testuser" ).getKey() );
        createCommand.setStatus( ContentStatus.DRAFT );
        createCommand.setContentName( "testcontent" );
        createCommand.setPriority( 0 );

        CustomContentData contentData = new CustomContentData( fixture.findContentTypeByName( "MyRelatedTypes" ).getContentTypeConfig() );
        contentData.add( new TextDataEntry( contentData.getInputConfig( "title" ), "Title 1" ) );
        contentData.add( new RelatedContentDataEntry( contentData.getInputConfig( "mySingleRelatedToBeUnmodified" ), related1 ) );
        contentData.add( new RelatedContentDataEntry( contentData.getInputConfig( "mySingleRelatedToBeModified" ), related2 ) );
        createCommand.setContentData( contentData );

        ContentKey createdContentKey = contentService.createContent( createCommand );

        ContentVersionEntity firstVersion = fixture.findContentByKey( createdContentKey ).getMainVersion();

        assertEquals( contentData, firstVersion.getContentData() );

        ContentVersionKey firstVersionKey = firstVersion.getKey();

        // verify that created content has expected contentdata
        CustomContentData actualContentData = (CustomContentData) firstVersion.getContentData();
        assertNotNull( actualContentData.getEntry( "mySingleRelatedToBeUnmodified" ) );
        assertNotNull( actualContentData.getEntry( "mySingleRelatedToBeModified" ) );
        assertEquals( related1,
                      ( (RelatedContentDataEntry) actualContentData.getEntry( "mySingleRelatedToBeUnmodified" ) ).getContentKey() );
        assertEquals( related2, ( (RelatedContentDataEntry) actualContentData.getEntry( "mySingleRelatedToBeModified" ) ).getContentKey() );

        // verify that setup content has expected related content

        List<ContentKey> expectedRelatedContent = ContentKey.convertToList( related1, related2 );
        List<ContentKey> actualRelatedContent = extractContentKeys( fixture.findRelatedContentsByContentVersionKey( firstVersionKey ) );
        assertStoredRelatedContent( expectedRelatedContent, actualRelatedContent );

        fixture.flushAndClearHibernateSession();

        // exercise
        UpdateContentCommand updateCommand = UpdateContentCommand.updateExistingVersion2( firstVersionKey );
        updateCommand.setContentKey( fixture.findFirstContentVersionByTitle( "Title 1" ).getContent().getKey() );
        updateCommand.setUpdateStrategy( UpdateContentCommand.UpdateStrategy.MODIFY );
        updateCommand.setStatus( ContentStatus.DRAFT );
        updateCommand.setModifier( fixture.findUserByName( "testuser" ).getKey() );
        contentData = new CustomContentData( fixture.findContentTypeByName( "MyRelatedTypes" ).getContentTypeConfig() );
        contentData.add( new RelatedContentDataEntry( contentData.getInputConfig( "mySingleRelatedToBeModified" ), related3 ) );
        updateCommand.setContentData( contentData );

        ContentVersionKey targedVersion = contentService.updateContent( updateCommand ).getTargetedVersionKey();

        // verify that updated content has expected contentdata
        actualContentData = (CustomContentData) fixture.findContentVersionByKey( targedVersion ).getContentData();
        assertNotNull( actualContentData.getEntry( "mySingleRelatedToBeUnmodified" ) );
        assertNotNull( actualContentData.getEntry( "mySingleRelatedToBeModified" ) );
        assertEquals( related1,
                      ( (RelatedContentDataEntry) actualContentData.getEntry( "mySingleRelatedToBeUnmodified" ) ).getContentKey() );
        assertEquals( related3, ( (RelatedContentDataEntry) actualContentData.getEntry( "mySingleRelatedToBeModified" ) ).getContentKey() );

        assertEquals( 1, fixture.countContentVersionsByTitle( "Title 1" ) );

        fixture.flushAndClearHibernateSession();

        // verify stored related content
        expectedRelatedContent = ContentKey.convertToList( related1, related3 );
        actualRelatedContent = extractContentKeys( fixture.findRelatedContentsByContentVersionKey( targedVersion ) );
        assertStoredRelatedContent( expectedRelatedContent, actualRelatedContent );
    }

    private ContentKey storeContentToRelatedTo( String title )
    {
        CreateContentCommand createCommand = new CreateContentCommand();
        createCommand.setLanguage( fixture.findLanguageByCode( "en" ).getKey() );
        createCommand.setCategory( fixture.findCategoryByName( "MyCategory" ).getKey() );
        createCommand.setCreator( fixture.findUserByName( "testuser" ).getKey() );
        createCommand.setStatus( ContentStatus.APPROVED );
        createCommand.setPriority( 0 );
        createCommand.setContentName( "testcontentrelated" );

        CustomContentData contentData = new CustomContentData( fixture.findContentTypeByName( "MyRelatedTypes" ).getContentTypeConfig() );
        contentData.add( new TextDataEntry( contentData.getInputConfig( "title" ), title ) );
        createCommand.setContentData( contentData );
        return contentService.createContent( createCommand );
    }

    private List<ContentKey> extractContentKeys( List<RelatedContentEntity> relatedContentEntities )
    {
        List<ContentKey> contentKeys = new ArrayList<ContentKey>();
        for ( RelatedContentEntity relatedContentEntity : relatedContentEntities )
        {
            contentKeys.add( relatedContentEntity.getKey().getChildContentKey() );
        }
        return contentKeys;
    }

    private void assertStoredRelatedContent( Collection<ContentKey> expectedRelatedContentKeys, Collection<ContentKey> actual )
    {
        for ( ContentKey expectedContentKey : expectedRelatedContentKeys )
        {
            assertTrue( "expected related content with key: " + expectedContentKey, actual.contains( expectedContentKey ) );
        }

        assertEquals( "unexpected number of related content", expectedRelatedContentKeys.size(), actual.size() );
    }


}