/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.itest.client;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.orm.hibernate3.HibernateTemplate;

import com.enonic.cms.framework.xml.XMLDocumentFactory;

import com.enonic.cms.api.client.model.ImportContentsParams;
import com.enonic.cms.core.client.InternalClient;
import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.ContentService;
import com.enonic.cms.core.content.ContentVersionEntity;
import com.enonic.cms.core.content.binary.BinaryDataAndBinary;
import com.enonic.cms.core.content.command.CreateContentCommand;
import com.enonic.cms.core.content.contentdata.legacy.LegacyImageContentData;
import com.enonic.cms.core.content.contenttype.ContentHandlerName;
import com.enonic.cms.core.content.imports.ImportJobFactory;
import com.enonic.cms.core.security.PortalSecurityHolder;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.security.user.UserType;
import com.enonic.cms.itest.AbstractSpringTest;
import com.enonic.cms.itest.util.DomainFactory;
import com.enonic.cms.itest.util.DomainFixture;
import com.enonic.cms.store.dao.ContentDao;

public abstract class AbstractInternalClientImpl_ImportContentTest
    extends AbstractSpringTest
{
    public static final String NORWEGIAN = "\u00c6\u00d8\u00c5\u00e6\u00f8\u00e5";     // AE, OE, AA, ae, oe, aa

    public static final String CHINESE = "\u306d\u304e\u30de\u30e8\u713c\u304d";

    public static final String AEC_ALL = "\u0082\u0083\u0084\u0085\u0086\u0087\u0089\u008a\u008b\u008c\u0091\u0092\u0093\u0094" +
        "\u0095\u0096\u0097\u0098\u0099\u009a\u009b\u009c\u009f";

    @Autowired
    protected HibernateTemplate hibernateTemplate;

    protected DomainFactory factory;

    @Autowired
    protected DomainFixture fixture;

    @Autowired
    protected ContentService contentService;

    @Autowired
    @Qualifier("localClient")
    protected InternalClient internalClient;

    @Autowired
    protected ContentDao contentDao;

    @Before
    public void before()
        throws IOException, JDOMException
    {
        ImportJobFactory.setExecuteInOneTransaction( true );

        factory = fixture.getFactory();

        fixture.initSystemData();
        fixture.flushAndClearHibernateSession();
        fixture.flushIndexTransaction();
    }

    protected void setupImport( final Document config )
    {
        fixture.createAndStoreUserAndUserGroup( "testuser", "testuser fullname", UserType.NORMAL, "testuserstore" );
        fixture.createAndStoreUserAndUserGroup( "testuser2", "testuser2 fullname", UserType.NORMAL, "testuserstore" );

        fixture.save( factory.createContentHandler( "Custom content", ContentHandlerName.CUSTOM.getHandlerClassShortName() ) );
        fixture.save( factory.createContentType( "MyCustomContentType", ContentHandlerName.CUSTOM.getHandlerClassShortName(), config ) );
        fixture.save( factory.createUnit( "MyCustomUnit", "en" ) );
        fixture.save( factory.createCategory( "MyImportCategory", null, "MyCustomContentType", "MyCustomUnit", "testuser", "testuser" ) );
        fixture.save( factory.createCategoryAccessForUser( "MyImportCategory", "testuser", "read, browse, create, approve" ) );
        fixture.save( factory.createCategoryAccessForUser( "MyImportCategory", "testuser2", "read, browse, create, approve" ) );
        fixture.flushAndClearHibernateSession();
        fixture.flushIndexTransaction();
    }

    protected void setupImageCategory()
    {
        fixture.save( factory.createContentHandler( "Files", ContentHandlerName.FILE.getHandlerClassShortName() ) );
        fixture.save(
            factory.createContentType( "MyImageContentType", ContentHandlerName.FILE.getHandlerClassShortName(), getConfigImage() ) );
        fixture.save( factory.createUnit( "MyFileUnit", "en" ) );
        fixture.save( factory.createCategory( "MyImageCategory", null, "MyImageContentType", "MyFileUnit", "testuser", "testuser" ) );
        fixture.save( factory.createCategoryAccessForUser( "MyImageCategory", "testuser", "read, browse, create, approve" ) );
    }

    protected void setupRelatedContentCategory()
    {
        fixture.save( factory.createContentType( "MyRelatedContentType", ContentHandlerName.CUSTOM.getHandlerClassShortName(),
                                                 getConfigRelatedContent() ) );
        fixture.save( factory.createUnit( "MyRelatedContentUnit", "en" ) );
        fixture.save( factory.createCategory( "MyRelatedContentCategory", null, "MyRelatedContentType", "MyRelatedContentUnit", "testuser",
                                              "testuser" ) );
        fixture.save( factory.createCategoryAccessForUser( "MyRelatedContentCategory", "testuser", "read, browse, create, approve" ) );
    }

    protected ContentKey setupImage()
    {
        return setupImage( "Image I" );
    }

    protected ContentKey setupImage( final String name )
    {
        final ContentEntity content = factory.createContent( "MyImageCategory", "en", "testuser", "0", new Date() );
        final ContentVersionEntity version = factory.createContentVersion( "0", "testuser" );

        final LegacyImageContentData contentData = new LegacyImageContentData( createImageContentDataDoc( name ) );
        version.setContentData( contentData );

        final UserEntity runningUser = fixture.findUserByName( "testuser" );

        final CreateContentCommand createCommand = new CreateContentCommand();
        createCommand.setCreator( runningUser );

        createCommand.populateCommandWithContentValues( content );
        createCommand.populateCommandWithContentVersionValues( version );

        createCommand.setBinaryDatas( new ArrayList<BinaryDataAndBinary>() );
        createCommand.setUseCommandsBinaryDataToAdd( true );

        return contentService.createContent( createCommand );
    }

    protected ContentKey setupRelatedContent( final String name )
    {
        final ContentEntity content = factory.createContent( "MyRelatedContentCategory", "en", "testuser", "0", new Date() );
        final ContentVersionEntity version = factory.createContentVersion( "0", "testuser" );

        final LegacyImageContentData contentData = new LegacyImageContentData( createImageContentDataDoc( name ) );
        version.setContentData( contentData );

        final UserEntity runningUser = fixture.findUserByName( "testuser" );

        final CreateContentCommand command = new CreateContentCommand();
        command.setCreator( runningUser );
        command.setPriority( 0 );
        command.setCategory( fixture.findCategoryByName( "MyRelatedContentCategory" ) );
        command.setLanguage( fixture.findLanguageByCode( "en" ) );
        command.populateCommandWithContentValues( content );
        command.populateCommandWithContentVersionValues( version );

        command.setBinaryDatas( new ArrayList<BinaryDataAndBinary>() );
        command.setUseCommandsBinaryDataToAdd( true );

        return contentService.createContent( command );
    }

    private Document createImageContentDataDoc( final String name )
    {
        final Element contentdataEl = new Element( "contentdata" );
        contentdataEl.addContent( new Element( "name" ).setText( name ) );
        final Element imagesEl = new Element( "images" );
        contentdataEl.addContent( imagesEl );
        return new Document( contentdataEl );
    }

    protected void doImport( final String data )
        throws Exception
    {
        doImport( data, "testuser", "MyImport" );
    }

    protected void doImport( final String data, final String userName, final String importName )
        throws Exception
    {
        final Date publishFrom = new SimpleDateFormat( "yyyy.MM.dd HH:mm:ss" ).parse( "2001.01.02 03:04:05" );
        final Date publishTo = new SimpleDateFormat( "yyyy.MM.dd HH:mm:ss" ).parse( "2020.21.22 23:24:25" );

        doImport( data, userName, importName, publishFrom, publishTo );
    }

    protected void doImport( final String data, final String userName, final String importName, final Date publishFrom,
                             final Date publishTo )
        throws Exception
    {
        final UserEntity runningUser = fixture.findUserByName( userName );
        PortalSecurityHolder.setImpersonatedUser( runningUser.getKey() );

        final ImportContentsParams importParams = new ImportContentsParams();
        importParams.publishFrom = publishFrom;
        importParams.publishTo = publishTo;
        importParams.categoryKey = fixture.findCategoryByName( "MyImportCategory" ).getKey().toInt();
        importParams.importName = importName;
        importParams.data = data;

        internalClient.importContents( importParams );
        hibernateTemplate.clear();
    }

    private Document getConfigImage()
    {
        final StringBuilder config = new StringBuilder();
        config.append( "<contenttype>" );
        config.append( "  <config>" );
        config.append( "    <sizes>" );
        config.append( "      <defaultcustom type=\"width\" value=\"310\"/>" );
        config.append( "    </sizes>" );
        config.append( "  </config>" );
        config.append( "  <indexparameters>" );
        config.append( "    <index xpath=\"contentdata/name\"/>" );
        config.append( "  </indexparameters>" );
        config.append( "</contenttype>" );
        return XMLDocumentFactory.create( config.toString() ).getAsJDOMDocument();
    }

    private Document getConfigRelatedContent()
    {
        final StringBuilder config = new StringBuilder();
        config.append( "<contenttype>" );
        config.append( "  <config name=\"MyRelatedContentType\" version=\"1.0\">" );
        config.append( "    <form>" );
        config.append( "      <title name=\"name\"/>" );
        config.append( "      <block name=\"info\">" );
        config.append( "        <input name=\"name\" required=\"true\" type=\"text\">" );
        config.append( "          <display>Name</display>" );
        config.append( "          <xpath>contentdata/name</xpath>" );
        config.append( "        </input>" );
        config.append( "      </block>" );
        config.append( "   </form>" );
        config.append( "  </config>" );
        config.append( "  <indexparameters>" );
        config.append( "    <index xpath=\"contentdata/name\"/>" );
        config.append( "  </indexparameters>" );
        config.append( "</contenttype>" );
        return XMLDocumentFactory.create( config.toString() ).getAsJDOMDocument();
    }

    protected boolean contentKeyExistInContentCollection( Collection<ContentEntity> contents, ContentKey key )
    {
        if ( contents == null || key == null )
        {
            return false;
        }

        for ( ContentEntity content : contents )
        {
            if ( content.getKey().equals( key ) )
            {
                return true;
            }
        }
        return false;
    }
}