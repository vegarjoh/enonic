/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.itest.client;

import java.io.IOException;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mock.web.MockHttpServletRequest;

import com.enonic.cms.framework.xml.XMLDocumentFactory;

import com.enonic.cms.api.client.ClientException;
import com.enonic.cms.api.client.model.SnapshotContentParams;
import com.enonic.cms.core.client.InternalClient;
import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.ContentService;
import com.enonic.cms.core.content.ContentStatus;
import com.enonic.cms.core.content.ContentVersionEntity;
import com.enonic.cms.core.content.command.CreateContentCommand;
import com.enonic.cms.core.content.command.CreateContentCommand.AccessRightsStrategy;
import com.enonic.cms.core.content.contentdata.custom.CustomContentData;
import com.enonic.cms.core.content.contentdata.custom.stringbased.TextDataEntry;
import com.enonic.cms.core.content.contenttype.ContentHandlerName;
import com.enonic.cms.core.content.contenttype.dataentryconfig.TextDataEntryConfig;
import com.enonic.cms.core.security.PortalSecurityHolder;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.security.user.UserType;
import com.enonic.cms.core.servlet.ServletRequestAccessor;
import com.enonic.cms.itest.AbstractSpringTest;
import com.enonic.cms.itest.util.DomainFactory;
import com.enonic.cms.itest.util.DomainFixture;
import com.enonic.cms.store.dao.ContentDao;

import static org.junit.Assert.*;

public class InternalClientImpl_SnapshotContentTest
    extends AbstractSpringTest
{

    private DomainFactory factory;

    @Autowired
    private DomainFixture fixture;

    @Autowired
    private ContentDao contentDao;

    @Autowired
    private ContentService contentService;

    @Autowired
    @Qualifier("localClient")
    private InternalClient internalClient;

    private Document standardConfig;


    @Before
    public void before()
        throws IOException, JDOMException
    {

        factory = fixture.getFactory();

        fixture.initSystemData();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr( "127.0.0.1" );
        ServletRequestAccessor.setRequest( request );

        createContentTypeXml();

        saveNeededEntities();

        UserEntity runningUser = fixture.findUserByName( "testuser" );
        PortalSecurityHolder.setImpersonatedUser( runningUser.getKey() );

    }


    private void saveNeededEntities()
    {
        // prepare: save needed entities
        fixture.createAndStoreUserAndUserGroup( "testuser", "testuser fullname", UserType.NORMAL, "testuserstore" );
        fixture.save( factory.createContentHandler( "Custom content", ContentHandlerName.CUSTOM.getHandlerClassShortName() ) );
        fixture.save( factory.createContentType( "MyContentType", ContentHandlerName.CUSTOM.getHandlerClassShortName(), standardConfig ) );
        fixture.save( factory.createUnit( "MyUnit", "en" ) );
        fixture.save( factory.createCategory( "MyCategory", null, "MyContentType", "MyUnit", "testuser", "testuser" ) );
        fixture.save( factory.createCategoryAccessForUser( "MyCategory", "testuser", "read, create, approve" ) );
        fixture.flushAndClearHibernateSession();
    }

    @Test
    public void testSnapshotContent()
    {
        ContentKey contentKey = createUpdateContent( ContentStatus.DRAFT );

        SnapshotContentParams params = new SnapshotContentParams();
        params.contentKey = contentKey.toInt();
        params.clearCommentInDraft = false;
        params.snapshotComment = "this is a snapshot";

        internalClient.snapshotContent( params );

        ContentEntity persistedContent = contentDao.findByKey( contentKey );

        assertEquals( "Should have two versions, draft and snapshot", 2, persistedContent.getVersionCount() );

        ContentVersionEntity snapshot = null;
        ContentVersionEntity draft = null;

        for ( ContentVersionEntity version : persistedContent.getVersions() )
        {
            if ( version.isDraft() )
            {
                draft = version;
            }
            else if ( version.isSnapshot() )
            {
                snapshot = version;
            }
        }

        assertNotNull( draft );
        assertNotNull( snapshot );
        assertEquals( "initial version", draft.getChangeComment() );
        assertEquals( "this is a snapshot", snapshot.getChangeComment() );
        assertEquals( draft, snapshot.getSnapshotSource() );

        params.contentKey = contentKey.toInt();
        params.clearCommentInDraft = true;
        params.snapshotComment = "this is a snapshot";

        internalClient.snapshotContent( params );

        persistedContent = contentDao.findByKey( contentKey );

        assertEquals( "Should have three versions, draft and 2*snapshot", 3, persistedContent.getVersionCount() );
        assertNull( "Draft comment should be cleared", persistedContent.getDraftVersion().getChangeComment() );
    }

    @Test(expected = ClientException.class)
    public void testSnapshotContentNotDraft()
    {
        ContentKey contentKey = createUpdateContent( ContentStatus.APPROVED );

        SnapshotContentParams params = new SnapshotContentParams();
        params.contentKey = contentKey.toInt();
        params.clearCommentInDraft = false;
        params.snapshotComment = "this is a snapshot";

        internalClient.snapshotContent( params );
    }


    private ContentKey createUpdateContent( ContentStatus status )
    {
        UserEntity runningUser = fixture.findUserByName( "testuser" );

        // prepare: save a new content

        CustomContentData contentData = new CustomContentData( fixture.findContentTypeByName( "MyContentType" ).getContentTypeConfig() );
        TextDataEntryConfig titleConfig = (TextDataEntryConfig) contentData.getContentTypeConfig().getForm().getInputConfig( "myTitle" );
        contentData.add( new TextDataEntry( titleConfig, "testitle" ) );

        CreateContentCommand createContentCommand = new CreateContentCommand();
        createContentCommand.setCreator( runningUser );
        createContentCommand.setAvailableFrom( new DateTime( 2010, 1, 1, 0, 0, 0, 0 ).toDate() );
        createContentCommand.setAccessRightsStrategy( AccessRightsStrategy.USE_GIVEN );
        createContentCommand.setPriority( 0 );
        createContentCommand.setStatus( status );
        createContentCommand.setLanguage( fixture.findLanguageByCode( "en" ).getKey() );
        createContentCommand.setCategory( fixture.findCategoryByName( "MyCategory" ).getKey() );
        createContentCommand.setContentData( contentData );
        createContentCommand.setChangeComment( "initial version" );
        createContentCommand.setContentName( "testcontent" );

        ContentKey contentKey = contentService.createContent( createContentCommand );

        fixture.flushAndClearHibernateSession();

        return contentKey;
    }

    private void createContentTypeXml()
    {
        StringBuffer standardConfigXml = new StringBuffer();
        standardConfigXml.append( "<config name=\"MyContentType\" version=\"1.0\">" );
        standardConfigXml.append( "     <form>" );

        standardConfigXml.append( "         <title name=\"myTitle\"/>" );

        standardConfigXml.append( "         <block name=\"TestBlock1\">" );

        standardConfigXml.append( "             <input name=\"myTitle\" required=\"true\" type=\"text\">" );
        standardConfigXml.append( "                 <display>My title</display>" );
        standardConfigXml.append( "                 <xpath>contentdata/mytitle</xpath>" );
        standardConfigXml.append( "             </input>" );

        standardConfigXml.append( "         </block>" );
        standardConfigXml.append( "     </form>" );
        standardConfigXml.append( "</config>" );
        standardConfig = XMLDocumentFactory.create( standardConfigXml.toString() ).getAsJDOMDocument();
    }

    private String createClientUserQualifiedName( UserEntity user )
    {
        return user.getUserStoreKey().toString() + ":" + user.getUserGroup().getName();
    }


}