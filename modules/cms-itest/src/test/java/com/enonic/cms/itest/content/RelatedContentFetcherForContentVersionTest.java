/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.itest.content;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.jdom.Document;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.orm.hibernate3.HibernateTemplate;

import com.enonic.cms.framework.cache.CacheManager;
import com.enonic.cms.framework.xml.XMLDocumentFactory;

import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.ContentService;
import com.enonic.cms.core.content.ContentStatus;
import com.enonic.cms.core.content.ContentVersionEntity;
import com.enonic.cms.core.content.RelatedContentFetcherForContentVersion;
import com.enonic.cms.core.content.command.CreateContentCommand;
import com.enonic.cms.core.content.command.UpdateContentCommand;
import com.enonic.cms.core.content.contentdata.custom.CustomContentData;
import com.enonic.cms.core.content.contentdata.custom.contentkeybased.RelatedContentDataEntry;
import com.enonic.cms.core.content.contentdata.custom.relationdataentrylistbased.RelatedContentsDataEntry;
import com.enonic.cms.core.content.contentdata.custom.stringbased.TextDataEntry;
import com.enonic.cms.core.content.contenttype.ContentHandlerName;
import com.enonic.cms.core.content.contenttype.ContentTypeConfigBuilder;
import com.enonic.cms.core.content.resultset.RelatedChildContent;
import com.enonic.cms.core.content.resultset.RelatedContentResultSet;
import com.enonic.cms.core.security.PortalSecurityHolder;
import com.enonic.cms.core.security.user.User;
import com.enonic.cms.core.security.user.UserType;
import com.enonic.cms.itest.AbstractSpringTest;
import com.enonic.cms.itest.util.DomainFactory;
import com.enonic.cms.itest.util.DomainFixture;
import com.enonic.cms.store.dao.ContentEntityDao;
import com.enonic.cms.store.dao.RelatedChildContentQuery;

import static org.junit.Assert.*;

public class RelatedContentFetcherForContentVersionTest
    extends AbstractSpringTest
{
    @Autowired
    private HibernateTemplate hibernateTemplate;

    @Qualifier("cacheFacadeManager")
    @Autowired
    private CacheManager cacheManager;

    private OverridingContentEntityDao contentDao;

    @Autowired
    private ContentService contentService;

    @Autowired
    private DomainFixture fixture;

    @Before
    public void setUp()
    {
        DomainFactory factory = fixture.getFactory();

        // setup needed common data for each test
        fixture.initSystemData();

        fixture.createAndStoreUserAndUserGroup( "testuser", "testuser fullname", UserType.NORMAL, "testuserstore" );

        //SecurityHolder.setUser( findUserByName( User.ANONYMOUS_UID ).getKey() );
        PortalSecurityHolder.setAnonUser( fixture.findUserByName( User.ANONYMOUS_UID ).getKey() );
        fixture.save( factory.createContentHandler( "Custom content", ContentHandlerName.CUSTOM.getHandlerClassShortName() ) );

        fixture.flushAndClearHibernateSession();

        // setup content type
        ContentTypeConfigBuilder ctyconf = new ContentTypeConfigBuilder( "MyRelatingContent", "title" );
        ctyconf.startBlock( "General" );
        ctyconf.addInput( "title", "text", "contentdata/title", "Title", true );
        ctyconf.addRelatedContentInput( "myRelatedContent", "contentdata/myRelatedContent", "My related content", false, true );
        ctyconf.endBlock();
        Document configAsJDOMDocument = XMLDocumentFactory.create( ctyconf.toString() ).getAsJDOMDocument();
        fixture.save(
            factory.createContentType( "MyRelatingContent", ContentHandlerName.CUSTOM.getHandlerClassShortName(), configAsJDOMDocument ) );

        fixture.flushAndClearHibernateSession();

        fixture.save( factory.createUnit( "MyUnit", "en" ) );
        fixture.save( factory.createCategory( "MyCategory", null, "MyRelatingContent", "MyUnit", "testuser", "testuser" ) );
        fixture.save( factory.createCategoryAccessForUser( "MyCategory", "testuser", "read, create, approve" ) );

        fixture.flushAndClearHibernateSession();

        contentDao = new OverridingContentEntityDao();
        contentDao.setHibernateTemplate( hibernateTemplate );
        contentDao.setCacheManager( cacheManager );
    }

    @Test
    public void eternal_loop_is_prevented_for_related_children_with_circular_reference_but_all_other_are_included()
    {
        // setup content to update
        CreateContentCommand createCommand = setupDefaultCreateContentCommandForMyRelatingContent( "Content not relating yet 1", null );
        ContentKey content_1 = contentService.createContent( createCommand );

        createCommand = setupDefaultCreateContentCommandForMyRelatingContent( "Content not relating yet 2", null );
        ContentKey content_2 = contentService.createContent( createCommand );

        createCommand = setupDefaultCreateContentCommandForMyRelatingContent( "Content not relating yet 3", null );
        ContentKey content_3 = contentService.createContent( createCommand );

        createCommand = setupDefaultCreateContentCommandForMyRelatingContent( "Content not relating yet 4", null );
        ContentKey content_4 = contentService.createContent( createCommand );

        createCommand = setupDefaultCreateContentCommandForMyRelatingContent( "Content not relating yet 5", null );
        ContentKey content_5 = contentService.createContent( createCommand );

        createCommand = setupDefaultCreateContentCommandForMyRelatingContent( "Content not relating yet 6", null );
        ContentKey content_6 = contentService.createContent( createCommand );

        UpdateContentCommand updateCommand =
            setupDefaultUpdateContentCommandForMyRelatingContent( content_1, "Relating content 1 to 2 and 6", content_2, content_6 );
        contentService.updateContent( updateCommand );

        updateCommand =
            setupDefaultUpdateContentCommandForMyRelatingContent( content_2, "Relating content 2 to 3 and 5", content_3, content_5 );
        contentService.updateContent( updateCommand );

        updateCommand =
            setupDefaultUpdateContentCommandForMyRelatingContent( content_3, "Relating content 3 to 1 and 4", content_1, content_4 );
        contentService.updateContent( updateCommand );

        contentDao.setMaxExpectedFindRelatedChildrenByKeysAttempts( 6 );

        RelatedContentFetcherForContentVersion relatedContentFetcher = new RelatedContentFetcherForContentVersion( contentDao );
        relatedContentFetcher.setAvailableCheckDate( new Date() );
        relatedContentFetcher.setMaxChildrenLevel( Integer.MAX_VALUE );
        relatedContentFetcher.setIncludeOfflineContent( true );

        List<ContentVersionEntity> versions = new ArrayList<ContentVersionEntity>();
        versions.add( fixture.findContentByKey( content_1 ).getMainVersion() );
        RelatedContentResultSet resultSet = relatedContentFetcher.fetch( versions );

        List<ContentKey> expectedRelatedContentKeys = new ArrayList<ContentKey>();
        expectedRelatedContentKeys.add( content_2 );
        expectedRelatedContentKeys.add( content_3 );
        expectedRelatedContentKeys.add( content_4 );
        expectedRelatedContentKeys.add( content_5 );
        expectedRelatedContentKeys.add( content_6 );
        assertRelatedContent( expectedRelatedContentKeys, resultSet.getContentKeys() );

    }

    @Test
    public void eternal_loop_is_prevented_for_related_children_with_multiple_circular_references()
    {
        // setup content to update
        CreateContentCommand createCommand = setupDefaultCreateContentCommandForMyRelatingContent( "Content not relating yet 1", null );
        ContentKey content_1 = contentService.createContent( createCommand );

        createCommand = setupDefaultCreateContentCommandForMyRelatingContent( "Content not relating yet 2", null );
        ContentKey content_2 = contentService.createContent( createCommand );

        createCommand = setupDefaultCreateContentCommandForMyRelatingContent( "Content not relating yet 3", null );
        ContentKey content_3 = contentService.createContent( createCommand );

        createCommand = setupDefaultCreateContentCommandForMyRelatingContent( "Content not relating yet 4", null );
        ContentKey content_4 = contentService.createContent( createCommand );

        UpdateContentCommand updateCommand =
            setupDefaultUpdateContentCommandForMyRelatingContent( content_1, "Relating content 1 to 2, 3 and 4", content_2, content_3,
                                                                  content_4 );
        contentService.updateContent( updateCommand );

        updateCommand = setupDefaultUpdateContentCommandForMyRelatingContent( content_2, "Relating content 2 to 1", content_1 );
        contentService.updateContent( updateCommand );

        updateCommand = setupDefaultUpdateContentCommandForMyRelatingContent( content_3, "Relating content 3 to 1", content_1 );
        contentService.updateContent( updateCommand );

        updateCommand = setupDefaultUpdateContentCommandForMyRelatingContent( content_4, "Relating content 4 to 1", content_1 );
        contentService.updateContent( updateCommand );

        contentDao.setMaxExpectedFindRelatedChildrenByKeysAttempts( 4 );

        RelatedContentFetcherForContentVersion relatedContentFetcher = new RelatedContentFetcherForContentVersion( contentDao );
        relatedContentFetcher.setAvailableCheckDate( new Date() );
        relatedContentFetcher.setMaxChildrenLevel( Integer.MAX_VALUE );
        relatedContentFetcher.setIncludeOfflineContent( true );

        List<ContentVersionEntity> versions = new ArrayList<ContentVersionEntity>();
        versions.add( fixture.findContentByKey( content_1 ).getMainVersion() );
        RelatedContentResultSet resultSet = relatedContentFetcher.fetch( versions );

        List<ContentKey> expectedRelatedContentKeys = new ArrayList<ContentKey>();
        expectedRelatedContentKeys.add( content_2 );
        expectedRelatedContentKeys.add( content_3 );
        expectedRelatedContentKeys.add( content_4 );
        assertRelatedContent( expectedRelatedContentKeys, resultSet.getContentKeys() );
    }

    @Test
    public void eternal_loop_is_prevented_for_related_children_of_children()
    {
        // setup content to update
        CreateContentCommand createCommand = setupDefaultCreateContentCommandForMyRelatingContent( "Content not relating yet 1", null );
        ContentKey content_1 = contentService.createContent( createCommand );

        createCommand = setupDefaultCreateContentCommandForMyRelatingContent( "Content not relating yet 2", null );
        ContentKey content_2 = contentService.createContent( createCommand );

        createCommand = setupDefaultCreateContentCommandForMyRelatingContent( "Content not relating yet 3", null );
        ContentKey content_3 = contentService.createContent( createCommand );

        createCommand = setupDefaultCreateContentCommandForMyRelatingContent( "Content not relating yet 4", null );
        ContentKey content_4 = contentService.createContent( createCommand );

        UpdateContentCommand updateCommand =
            setupDefaultUpdateContentCommandForMyRelatingContent( content_1, "Relating content 1-2" + content_2, content_2 );
        contentService.updateContent( updateCommand );

        updateCommand = setupDefaultUpdateContentCommandForMyRelatingContent( content_2, "Relating content 2-3" + content_3, content_3 );
        contentService.updateContent( updateCommand );

        updateCommand = setupDefaultUpdateContentCommandForMyRelatingContent( content_3, "Relating content 3-4" + content_4, content_4 );
        contentService.updateContent( updateCommand );

        updateCommand = setupDefaultUpdateContentCommandForMyRelatingContent( content_4, "Relating content 4-3" + content_3, content_3 );
        contentService.updateContent( updateCommand );

        fixture.flushAndClearHibernateSession();

        contentDao.setMaxExpectedFindRelatedChildrenByKeysAttempts( 4 );

        RelatedContentFetcherForContentVersion relatedContentFetcher = new RelatedContentFetcherForContentVersion( contentDao );
        relatedContentFetcher.setAvailableCheckDate( new Date() );
        relatedContentFetcher.setMaxChildrenLevel( Integer.MAX_VALUE );
        relatedContentFetcher.setIncludeOfflineContent( true );

        List<ContentVersionEntity> versions = new ArrayList<ContentVersionEntity>();
        versions.add( fixture.findContentByKey( content_1 ).getMainVersion() );
        RelatedContentResultSet resultSet = relatedContentFetcher.fetch( versions );

        List<ContentKey> expectedRelatedContentKeys = new ArrayList<ContentKey>();
        expectedRelatedContentKeys.add( content_2 );
        expectedRelatedContentKeys.add( content_3 );
        expectedRelatedContentKeys.add( content_4 );
        assertRelatedContent( expectedRelatedContentKeys, resultSet.getContentKeys() );
    }

    @Test
    public void including_visited_returns_all_when_true_but_not_when_false()
    {
        // setup content to update
        CreateContentCommand createCommand = setupDefaultCreateContentCommandForMyRelatingContent( "Main content 1", null );
        ContentKey content_1 = contentService.createContent( createCommand );

        createCommand = setupDefaultCreateContentCommandForMyRelatingContent( "Main content 2", null );
        ContentKey content_2 = contentService.createContent( createCommand );

        createCommand = setupDefaultCreateContentCommandForMyRelatingContent( "Related Content A", null );
        ContentKey content_A = contentService.createContent( createCommand );

        createCommand = setupDefaultCreateContentCommandForMyRelatingContent( "Related Content B", null );
        ContentKey content_B = contentService.createContent( createCommand );

        createCommand = setupDefaultCreateContentCommandForMyRelatingContent( "Related Content C", null );
        ContentKey content_C = contentService.createContent( createCommand );

        UpdateContentCommand updateCommand =
            setupDefaultUpdateContentCommandForMyRelatingContent( content_1, "Relating content 1 to A and B", content_A, content_B );
        contentService.updateContent( updateCommand );

        updateCommand =
            setupDefaultUpdateContentCommandForMyRelatingContent( content_2, "Relating content 2 to A and C", content_A, content_C );
        contentService.updateContent( updateCommand );
        RelatedContentFetcherForContentVersion relatedContentFetcher = new RelatedContentFetcherForContentVersion( contentDao );
        relatedContentFetcher.setAvailableCheckDate( new Date() );
        relatedContentFetcher.setMaxChildrenLevel( Integer.MAX_VALUE );
        relatedContentFetcher.setIncludeOfflineContent( true );

        // First attempt.  Regular retrieval: A and B are related to 1.
        List<ContentVersionEntity> versions = new ArrayList<ContentVersionEntity>();
        versions.add( fixture.findContentByKey( content_1 ).getMainVersion() );
        RelatedContentResultSet resultSet = relatedContentFetcher.fetch( versions );

        List<ContentKey> expectedRelatedContentKeys = new ArrayList<ContentKey>();
        expectedRelatedContentKeys.add( content_A );
        expectedRelatedContentKeys.add( content_B );
        assertRelatedContent( expectedRelatedContentKeys, resultSet.getContentKeys() );

        // Second attempt.  Regular retrieval: Only C that has not been retrieved before is related to 2.
        versions = new ArrayList<ContentVersionEntity>();
        versions.add( fixture.findContentByKey( content_2 ).getMainVersion() );
        resultSet = relatedContentFetcher.fetch( versions );

        expectedRelatedContentKeys = new ArrayList<ContentKey>();
        expectedRelatedContentKeys.add( content_C );
        assertRelatedContent( expectedRelatedContentKeys, resultSet.getContentKeys() );

        // Third attempt.  Regular retrieval: All related items have been retrieved before.  Empty result.
        versions = new ArrayList<ContentVersionEntity>();
        versions.add( fixture.findContentByKey( content_1 ).getMainVersion() );
        resultSet = relatedContentFetcher.fetch( versions );

        expectedRelatedContentKeys = new ArrayList<ContentKey>();
        assertRelatedContent( expectedRelatedContentKeys, resultSet.getContentKeys() );

        // Fourth attempt.  RequireAll retrieval: A and C are related to 2.
        versions = new ArrayList<ContentVersionEntity>();
        versions.add( fixture.findContentByKey( content_2 ).getMainVersion() );
        resultSet = relatedContentFetcher.fetch( versions, true );

        expectedRelatedContentKeys = new ArrayList<ContentKey>();
        expectedRelatedContentKeys.add( content_A );
        expectedRelatedContentKeys.add( content_C );
        assertRelatedContent( expectedRelatedContentKeys, resultSet.getContentKeys() );

    }

    private CreateContentCommand setupDefaultCreateContentCommandForMyRelatingContent( String title, ContentKey contentToRelateTo )
    {
        CreateContentCommand createCommand = new CreateContentCommand();
        createCommand.setAccessRightsStrategy( CreateContentCommand.AccessRightsStrategy.INHERIT_FROM_CATEGORY );
        createCommand.setCategory( fixture.findCategoryByName( "MyCategory" ).getKey() );
        createCommand.setCreator( fixture.findUserByName( "testuser" ).getKey() );
        createCommand.setPriority( 0 );
        createCommand.setLanguage( fixture.findLanguageByCode( "en" ) );
        createCommand.setStatus( ContentStatus.APPROVED );
        createCommand.setContentName( "testcontent" );

        CustomContentData contentData =
            new CustomContentData( fixture.findContentTypeByName( "MyRelatingContent" ).getContentTypeConfig() );
        contentData.add( new TextDataEntry( contentData.getInputConfig( "title" ), title ) );
        if ( contentToRelateTo != null )
        {
            contentData.add( new RelatedContentDataEntry( contentData.getInputConfig( "myRelatedContent" ), contentToRelateTo ) );
        }
        createCommand.setContentData( contentData );

        return createCommand;
    }

    private UpdateContentCommand setupDefaultUpdateContentCommandForMyRelatingContent( ContentKey contentKeyToUpdate, String title,
                                                                                       ContentKey... contentToRelateTo )
    {
        ContentEntity contentToUpdate = fixture.findContentByKey( contentKeyToUpdate );

        UpdateContentCommand command = UpdateContentCommand.updateExistingVersion2( contentToUpdate.getMainVersion().getKey() );
        command.setContentKey( contentToUpdate.getKey() );
        command.setUpdateStrategy( UpdateContentCommand.UpdateStrategy.MODIFY );
        command.setModifier( fixture.findUserByName( "testuser" ).getKey() );
        command.setPriority( 0 );
        command.setLanguage( fixture.findLanguageByCode( "en" ) );
        command.setStatus( ContentStatus.APPROVED );

        CustomContentData contentData =
            new CustomContentData( fixture.findContentTypeByName( "MyRelatingContent" ).getContentTypeConfig() );
        contentData.add( new TextDataEntry( contentData.getInputConfig( "title" ), title ) );
        if ( contentToRelateTo != null )
        {
            RelatedContentsDataEntry relatedContentsDataEntry =
                new RelatedContentsDataEntry( contentData.getInputConfig( "myRelatedContent" ) );
            for ( ContentKey singleContentToRelateTo : contentToRelateTo )
            {
                relatedContentsDataEntry.add(
                    new RelatedContentDataEntry( contentData.getInputConfig( "myRelatedContent" ), singleContentToRelateTo ) );
            }
            contentData.add( relatedContentsDataEntry );
        }
        command.setContentData( contentData );

        return command;
    }

    private void assertRelatedContent( Collection<ContentKey> expectedRelatedContentKeys, Collection<ContentKey> actual )
    {
        for ( ContentKey expectedContentKey : expectedRelatedContentKeys )
        {
            assertTrue( "expected related content with key: " + expectedContentKey, actual.contains( expectedContentKey ) );
        }

        assertEquals( "unexpected number of related content", expectedRelatedContentKeys.size(), actual.size() );
    }

    class OverridingContentEntityDao
        extends ContentEntityDao
    {
        private int numberOfFindRelatedChildrenByKeysAttempts = 0;

        private int maxExpectedFindRelatedChildrenByKeysAttempts = Integer.MAX_VALUE;

        public void setMaxExpectedFindRelatedChildrenByKeysAttempts( int maxExpectedFindRelatedChildrenByKeysAttempts )
        {
            this.maxExpectedFindRelatedChildrenByKeysAttempts = maxExpectedFindRelatedChildrenByKeysAttempts;
        }

        @Override
        public Collection<RelatedChildContent> findRelatedChildrenByKeys( RelatedChildContentQuery relatedChildContentQuery )
        {
            numberOfFindRelatedChildrenByKeysAttempts++;
            if ( numberOfFindRelatedChildrenByKeysAttempts > maxExpectedFindRelatedChildrenByKeysAttempts )
            {
                fail( "max expected findRelatedChildrenByKeys attempts exceeded : " + numberOfFindRelatedChildrenByKeysAttempts );
            }
            return super.findRelatedChildrenByKeys( relatedChildContentQuery );
        }
    }

}