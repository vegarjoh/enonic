/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.itest.structure.menuitem;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jdom.Document;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import com.enonic.cms.framework.xml.XMLDocumentFactory;

import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.ContentLocation;
import com.enonic.cms.core.content.ContentLocationSpecification;
import com.enonic.cms.core.content.ContentLocations;
import com.enonic.cms.core.content.ContentService;
import com.enonic.cms.core.content.ContentStatus;
import com.enonic.cms.core.content.category.CategoryAccessException;
import com.enonic.cms.core.content.category.CategoryEntity;
import com.enonic.cms.core.content.command.CreateContentCommand;
import com.enonic.cms.core.content.contentdata.custom.CustomContentData;
import com.enonic.cms.core.content.contentdata.custom.stringbased.TextDataEntry;
import com.enonic.cms.core.content.contenttype.ContentHandlerName;
import com.enonic.cms.core.content.contenttype.ContentTypeConfigBuilder;
import com.enonic.cms.core.content.contenttype.ContentTypeEntity;
import com.enonic.cms.core.content.contenttype.dataentryconfig.TextDataEntryConfig;
import com.enonic.cms.core.resource.ResourceKey;
import com.enonic.cms.core.security.user.UserKey;
import com.enonic.cms.core.structure.menuitem.AddContentToSectionCommand;
import com.enonic.cms.core.structure.menuitem.ApproveContentInSectionCommand;
import com.enonic.cms.core.structure.menuitem.ApproveContentsInSectionCommand;
import com.enonic.cms.core.structure.menuitem.ContentHomeEntity;
import com.enonic.cms.core.structure.menuitem.ContentHomeKey;
import com.enonic.cms.core.structure.menuitem.ContentTypeNotSupportedException;
import com.enonic.cms.core.structure.menuitem.MenuItemAccessEntity;
import com.enonic.cms.core.structure.menuitem.MenuItemAccessException;
import com.enonic.cms.core.structure.menuitem.MenuItemEntity;
import com.enonic.cms.core.structure.menuitem.MenuItemService;
import com.enonic.cms.core.structure.menuitem.OrderContentsInSectionCommand;
import com.enonic.cms.core.structure.menuitem.RemoveContentsFromSectionCommand;
import com.enonic.cms.core.structure.menuitem.SetContentHomeCommand;
import com.enonic.cms.core.structure.menuitem.UnapproveContentsInSectionCommand;
import com.enonic.cms.core.structure.menuitem.section.SectionContentEntity;
import com.enonic.cms.core.structure.page.template.PageTemplateEntity;
import com.enonic.cms.core.structure.page.template.PageTemplateType;
import com.enonic.cms.itest.AbstractSpringTest;
import com.enonic.cms.itest.util.DomainFactory;
import com.enonic.cms.itest.util.DomainFixture;

import static org.junit.Assert.*;

public class MenuItemServiceImplTest
    extends AbstractSpringTest
{
    @Autowired
    private MenuItemService menuItemService;

    private DomainFactory factory;

    @Autowired
    private DomainFixture fixture;

    private int menuItemOrderCount = 0;

    @Autowired
    private ContentService contentService;

    @Before
    public void setUp()
    {

        factory = fixture.getFactory();

        // setup needed common data for each test
        fixture.initSystemData();

        fixture.save( factory.createContentHandler( "Custom content", ContentHandlerName.CUSTOM.getHandlerClassShortName() ) );

        fixture.flushAndClearHibernateSession();

        // Create an article content type that will be used in the section:
        ContentTypeConfigBuilder ctyconf = new ContentTypeConfigBuilder( "article", "heading" );
        ctyconf.startBlock( "intro" );
        ctyconf.addInput( "heading", "text", "contentdata/intro/heading", "heading", true );
        ctyconf.addInput( "teaser", "text", "contentdata/intro/teaser", "teaser" );
        ctyconf.endBlock();
        Document configAsXmlBytes = XMLDocumentFactory.create( ctyconf.toString() ).getAsJDOMDocument();
        fixture.save( factory.createContentType( "article", ContentHandlerName.CUSTOM.getHandlerClassShortName(), configAsXmlBytes ) );

        fixture.flushAndClearHibernateSession();

        // Create users that have all and no rights to work with the sections.
        fixture.createAndStoreNormalUserWithUserGroup( "aru", "All rights user", "testuserstore" );
        fixture.createAndStoreNormalUserWithUserGroup( "nru", "No rights user", "testuserstore" );

        fixture.flushAndClearHibernateSession();

        // Create a unit and a category in the archive to store the articles in, including access rights on the category.
        fixture.save( factory.createUnit( "Archive" ) );
        fixture.save( factory.createCategory( "Articles", null, "article", "Archive", "aru", "aru" ) );
        fixture.save( factory.createCategoryAccessForUser( "Articles", "aru", "read, admin_browse, create, delete, approve" ) );
        fixture.save( factory.createCategoryAccessForUser( "Articles", "nru", "read" ) );

        // Create a site and a section page for testing working with sections.
        fixture.save( factory.createSite( "The Newspaper", new Date(), null, "en" ) );

        fixture.flushAndClearHibernateSession();
    }

    @Test
    public void orderContentToSectionTest()
    {
        fixture.save( createOrderedSection( "News" ) );
        fixture.save( createMenuItemAccess( "News", "aru", "read, create, update, delete, add, publish" ) );

        createContent( "c-1", "Articles" );
        createContent( "c-2", "Articles" );

        fixture.flushAndClearHibernateSession();

        AddContentToSectionCommand addContentToSectionCommand1 =
            createAddContentToSectionCommand( "aru", fixture.findContentByName( "c-1" ), fixture.findMenuItemByName( "News" ), true );
        menuItemService.execute( addContentToSectionCommand1 );

        AddContentToSectionCommand addContentToSectionCommand2 =
            createAddContentToSectionCommand( "aru", fixture.findContentByName( "c-2" ), fixture.findMenuItemByName( "News" ), true );
        menuItemService.execute( addContentToSectionCommand2 );

        fixture.flushAndClearHibernateSession();

        // verify: content added in section
        assertEquals( 1, fixture.findContentByName( "c-1" ).getSectionContents().size() );
        assertEquals( 1, fixture.findContentByName( "c-2" ).getSectionContents().size() );
        final Set<SectionContentEntity> sectionContents = fixture.findMenuItemByName( "News" ).getSectionContents();
        assertEquals( 2, sectionContents.size() );

        List<ContentKey> wantedOrder = new ArrayList<ContentKey>( 2 );

        for ( SectionContentEntity sectionContentEntity : sectionContents )
        {
            // check that these are not ordered !
            assertEquals( 0, sectionContentEntity.getOrder() );

            wantedOrder.add( sectionContentEntity.getContent().getKey() );
        }

        OrderContentsInSectionCommand orderContentsInSectionCommand = new OrderContentsInSectionCommand();
        orderContentsInSectionCommand.setSectionKey( fixture.findMenuItemByName( "News" ).getKey() );
        orderContentsInSectionCommand.setWantedOrder( wantedOrder );
        menuItemService.execute( orderContentsInSectionCommand );

        fixture.flushAndClearHibernateSession();

        final Set<SectionContentEntity> sectionContents2 = fixture.findMenuItemByName( "News" ).getSectionContents();
        assertEquals( 2, sectionContents2.size() );

        for ( SectionContentEntity sectionContentEntity : sectionContents )
        {
            // check that these are ordered !
            assertFalse( 0 == sectionContentEntity.getOrder() );
        }
    }

    @Test
    public void addContentToSection_explicit_content_home_when_adding_content_to_section()
    {
        // setup: content
        fixture.save( createSection( "My section", "The Newspaper", false ) );
        fixture.save( createMenuItemAccess( "My section", "aru", "add publish" ) );
        createContent( "first-content", "Articles" );
        fixture.flushAndClearHibernateSession();

        fixture.flushAndClearHibernateSession();

        // exercise
        AddContentToSectionCommand command = new AddContentToSectionCommand();
        command.setAddOnTop( false );
        command.setApproveInSection( false );
        command.setContributor( fixture.findUserByName( "aru" ).getKey() );
        command.setSection( fixture.findMenuItemByName( "My section" ).getKey() );
        command.setContent( fixture.findContentByName( "first-content" ).getKey() );
        menuItemService.execute( command );

        // verify: content added in section
        assertEquals( 1, fixture.findContentByName( "first-content" ).getSectionContents().size() );
        assertEquals( 1, fixture.findMenuItemByName( "My section" ).getSectionContents().size() );

        // verify content home is stored
        ContentHomeEntity actualContentHome = fixture.findContentHomeByKey(
            new ContentHomeKey( fixture.findSiteByName( "The Newspaper" ).getKey(), fixture.findContentByPriority( 0 ).getKey() ) );
        assertNotNull( actualContentHome );
        assertEquals( fixture.findContentByName( "first-content" ), actualContentHome.getContent() );
        assertEquals( fixture.findMenuItemByName( "My section" ), actualContentHome.getMenuItem() );
        assertEquals( fixture.findSiteByName( "The Newspaper" ), actualContentHome.getSite() );
        assertEquals( null, actualContentHome.getPageTemplate() );
    }

    @Test
    public void addContentToSection_content_home_does_not_change_when_adding_content_to_another_section()
    {
        // setup: content
        fixture.save( createSection( "My section", "The Newspaper", false ) );
        fixture.save( createSection( "My section2", "The Newspaper", false ) );
        fixture.save( createMenuItemAccess( "My section", "aru", "add publish" ) );
        fixture.save( createMenuItemAccess( "My section2", "aru", "add publish" ) );
        createContent( "first-content", "Articles" );
        fixture.flushAndClearHibernateSession();

        fixture.flushAndClearHibernateSession();

        AddContentToSectionCommand command = new AddContentToSectionCommand();
        command.setAddOnTop( false );
        command.setApproveInSection( false );
        command.setContributor( fixture.findUserByName( "aru" ).getKey() );
        command.setSection( fixture.findMenuItemByName( "My section" ).getKey() );
        command.setContent( fixture.findContentByName( "first-content" ).getKey() );
        menuItemService.execute( command );

        // setup: verify content added in section
        assertEquals( 1, fixture.findContentByName( "first-content" ).getSectionContents().size() );
        assertEquals( 1, fixture.findMenuItemByName( "My section" ).getSectionContents().size() );

        // exercise
        command = new AddContentToSectionCommand();
        command.setAddOnTop( false );
        command.setApproveInSection( false );
        command.setContributor( fixture.findUserByName( "aru" ).getKey() );
        command.setSection( fixture.findMenuItemByName( "My section2" ).getKey() );
        command.setContent( fixture.findContentByName( "first-content" ).getKey() );
        menuItemService.execute( command );

        // verify: content added in section
        assertEquals( 2, fixture.findContentByName( "first-content" ).getSectionContents().size() );
        assertEquals( 1, fixture.findMenuItemByName( "My section" ).getSectionContents().size() );
        assertEquals( 1, fixture.findMenuItemByName( "My section2" ).getSectionContents().size() );

        // verify content home is stored
        ContentHomeEntity actualContentHome =
            fixture.findContentByName( "first-content" ).getContentHome( fixture.findSiteByName( "The Newspaper" ).getKey() );
        assertNotNull( actualContentHome );
        assertEquals( fixture.findMenuItemByName( "My section" ), actualContentHome.getMenuItem() );
    }

    @Test
    public void removeContentsFromSection_removes_content_from_section()
    {
        fixture.save( createOrderedSection( "News" ) );
        fixture.save( createMenuItemAccess( "News", "aru", "read, create, update, delete, add, publish" ) );

        createContent( "c-1", "Articles" );
        createContent( "c-2", "Articles" );
        fixture.flushAndClearHibernateSession();

        ContentKey c1Key = fixture.findContentByName( "c-1" ).getKey();
        ContentKey c2Key = fixture.findContentByName( "c-2" ).getKey();

        AddContentToSectionCommand addContentToSectionCommand =
            createAddContentToSectionCommand( "aru", fixture.findContentByName( "c-1" ), fixture.findMenuItemByName( "News" ), true );
        menuItemService.execute( addContentToSectionCommand );

        fixture.flushAndClearHibernateSession();

        addContentToSectionCommand =
            createAddContentToSectionCommand( "aru", fixture.findContentByName( "c-2" ), fixture.findMenuItemByName( "News" ), true );
        menuItemService.execute( addContentToSectionCommand );

        fixture.flushAndClearHibernateSession();

        // exercise
        RemoveContentsFromSectionCommand command = new RemoveContentsFromSectionCommand();
        command.setRemover( fixture.findUserByName( "aru" ).getKey() );
        command.setSection( fixture.findMenuItemByName( "News" ).getKey() );
        command.addContentToRemove( c1Key );
        menuItemService.execute( command );

        fixture.flushAndClearHibernateSession();

        // verify: only c-2 is left in section
        List<SectionContentEntity> actualSectionContents = Lists.newArrayList( fixture.findMenuItemByName( "News" ).getSectionContents() );
        assertEquals( 1, actualSectionContents.size() );
        SectionContentEntity testDetailResult = actualSectionContents.get( 0 );
        assertEquals( c2Key, testDetailResult.getContent().getKey() );
    }


    @Test
    public void removeContentsFromSection_removes_home_also_when_last()
    {
        // setup sections
        fixture.save( createSection( "News", "The Newspaper", false ) );
        fixture.save( createMenuItemAccess( "News", "aru", "read, create, update, delete, add, publish" ) );
        fixture.save( createSection( "Culture", "The Newspaper", false ) );
        fixture.save( createMenuItemAccess( "Culture", "aru", "read, create, update, delete, add, publish" ) );

        // setup content
        createContent( "c-123", "Articles" );

        // add content to sections
        AddContentToSectionCommand addContentToSectionCommand =
            createAddContentToSectionCommand( "aru", fixture.findContentByName( "c-123" ), fixture.findMenuItemByName( "News" ), true );
        menuItemService.execute( addContentToSectionCommand );

        fixture.flushAndClearHibernateSession();

        addContentToSectionCommand =
            createAddContentToSectionCommand( "aru", fixture.findContentByName( "c-123" ), fixture.findMenuItemByName( "Culture" ), true );
        menuItemService.execute( addContentToSectionCommand );

        fixture.flushAndClearHibernateSession();

        // set home to one of them
        SetContentHomeCommand setContentHomeCommand = new SetContentHomeCommand();
        setContentHomeCommand.setSetter( fixture.findUserByName( "aru" ).getKey() );
        setContentHomeCommand.setSection( fixture.findMenuItemByName( "Culture" ).getKey() );
        setContentHomeCommand.setContent( fixture.findContentByName( "c-123" ).getKey() );
        menuItemService.execute( setContentHomeCommand );

        fixture.flushAndClearHibernateSession();

        ContentLocations contentLocations = fixture.findContentByName( "c-123" ).getLocations( new ContentLocationSpecification() );
        assertEquals( "Culture", contentLocations.getHomeLocation( fixture.findSiteByName( "The Newspaper" ).getKey() ).getMenuItemName() );

        // exercise
        RemoveContentsFromSectionCommand removeCommand = new RemoveContentsFromSectionCommand();
        removeCommand.setRemover( fixture.findUserByName( "aru" ).getKey() );
        removeCommand.setSection( fixture.findMenuItemByName( "News" ).getKey() );
        removeCommand.addContentToRemove( fixture.findContentByName( "c-123" ).getKey() );
        menuItemService.execute( removeCommand );

        fixture.flushAndClearHibernateSession();

        // verify content home is still there
        contentLocations = fixture.findContentByName( "c-123" ).getLocations( new ContentLocationSpecification() );
        ContentLocation actualHomeLocation = contentLocations.getHomeLocation( fixture.findSiteByName( "The Newspaper" ).getKey() );
        assertEquals( fixture.findMenuItemByName( "Culture" ).getKey(), actualHomeLocation.getMenuItemKey() );

        removeCommand.setSection( fixture.findMenuItemByName( "Culture" ).getKey() );
        menuItemService.execute( removeCommand );

        fixture.flushAndClearHibernateSession();

        // verify: content has no longer home
        contentLocations = fixture.findContentByName( "c-123" ).getLocations( new ContentLocationSpecification() );
        actualHomeLocation = contentLocations.getHomeLocation( fixture.findSiteByName( "The Newspaper" ).getKey() );
        assertNull( actualHomeLocation );
    }


    @Test
    public void removeContentsFromSection()
    {
        // setup
        createContent( "other-content-1", "Articles" );
        createContent( "content-to-remove", "Articles" );
        createContent( "other-content-2", "Articles" );

        MenuItemEntity mysection = createSection( "mysection", "The Newspaper", true );
        fixture.save( mysection );
        fixture.save( createMenuItemAccess( "mysection", "aru", "read, create, update, delete, add, publish" ) );

        MenuItemEntity othersection = createSection( "othersection", "The Newspaper", true );
        fixture.save( othersection );
        fixture.save( createMenuItemAccess( "othersection", "aru", "read, create, update, delete, add, publish" ) );

        fixture.flushAndClearHibernateSession();

        menuItemService.execute(
            createAddContentToSectionCommand( "aru", fixture.findContentByName( "other-content-1" ), mysection, true ) );
        fixture.flushAndClearHibernateSession();
        menuItemService.execute(
            createAddContentToSectionCommand( "aru", fixture.findContentByName( "content-to-remove" ), mysection, true ) );
        fixture.flushAndClearHibernateSession();
        menuItemService.execute(
            createAddContentToSectionCommand( "aru", fixture.findContentByName( "content-to-remove" ), othersection, true ) );
        fixture.flushAndClearHibernateSession();
        menuItemService.execute(
            createAddContentToSectionCommand( "aru", fixture.findContentByName( "other-content-2" ), mysection, true ) );
        fixture.flushAndClearHibernateSession();

        fixture.flushAndClearHibernateSession();

        // setup: verify
        assertEquals( 3, fixture.findMenuItemByName( "mysection" ).getSectionContents().size() );
        assertEquals( 1, fixture.findMenuItemByName( "othersection" ).getSectionContents().size() );

        assertEquals( 2, fixture.findContentByName( "content-to-remove" ).getSectionContents().size() );
        assertEquals( 1, fixture.findContentByName( "other-content-1" ).getSectionContents().size() );
        assertEquals( 1, fixture.findContentByName( "other-content-2" ).getSectionContents().size() );

        // exercise
        RemoveContentsFromSectionCommand command = new RemoveContentsFromSectionCommand();
        command.setRemover( fixture.findUserByName( "aru" ).getKey() );
        command.setSection( fixture.findMenuItemByName( "mysection" ).getKey() );
        command.addContentToRemove( fixture.findContentByName( "content-to-remove" ).getKey() );
        menuItemService.execute( command );

        fixture.flushAndClearHibernateSession();

        // verify: mysection has one less content
        assertEquals( 2, fixture.findMenuItemByName( "mysection" ).getSectionContents().size() );

        assertEquals( 1, fixture.findMenuItemByName( "othersection" ).getSectionContents().size() );
        assertEquals( 1, fixture.findContentByName( "content-to-remove" ).getSectionContents().size() );
        assertEquals( 1, fixture.findContentByName( "other-content-1" ).getSectionContents().size() );
        assertEquals( 1, fixture.findContentByName( "other-content-2" ).getSectionContents().size() );
    }

    @Test(expected = IllegalArgumentException.class)
    public void addContentToSection_add_content_to_ordered_section_as_unapproved_and_on_top_throws_IllegalArgumentException()
    {
        // setup
        fixture.save( createSection( "My section", "The Newspaper", true ) );
        fixture.save( createMenuItemAccess( "My section", "aru", "add" ) );
        createContent( "my-content", "Articles" );

        // exercise
        AddContentToSectionCommand command = new AddContentToSectionCommand();
        command.setAddOnTop( true );
        command.setApproveInSection( false );
        command.setContributor( fixture.findUserByName( "aru" ).getKey() );
        command.setSection( fixture.findMenuItemByName( "My section" ).getKey() );
        command.setContent( fixture.findContentByName( "my-content" ).getKey() );
        menuItemService.execute( command );
    }

    @Test(expected = ContentTypeNotSupportedException.class)
    @Ignore
    // TODO: Ignore until it is easy to setup content type filter for a section page
    public void addContentToSection_adding_content_that_is_not_supported_by_section_page__trows_exception()
    {
        // setup
        fixture.save( factory.createCategory( "Unsupported contents", null, "just-another-cty", "Archive", "aru", "aru" ) );
        fixture.save( factory.createCategoryAccessForUser( "Unsupported contents", "aru", "read, admin_browse, create" ) );

        fixture.save( createSection( "My section", "The Newspaper", false ) );
        fixture.save( createMenuItemAccess( "My section", "aru", "add" ) );
        createContent( "my-unsupported-content", "Articles" );

        fixture.save( factory.createContentType( "just-another-cty", ContentHandlerName.CUSTOM.getHandlerClassShortName(), null ) );
        fixture.save( createPageTemplate( "my-template", PageTemplateType.SECTIONPAGE, "The Newspaper", "just-another-cty" ) );

        // exercise
        AddContentToSectionCommand command = new AddContentToSectionCommand();
        command.setAddOnTop( false );
        command.setApproveInSection( false );
        command.setContributor( fixture.findUserByName( "aru" ).getKey() );
        command.setSection( fixture.findMenuItemByName( "My section" ).getKey() );
        command.setContent( fixture.findContentByName( "my-unsupported-content" ).getKey() );
        try
        {
            menuItemService.execute( command );
        }
        catch ( Exception e )
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    @Test
    public void addContentToSection_adding_content_that_is_supported_by_section_passes()
    {
        // setup
        MenuItemEntity section = createSection( "My section", "The Newspaper", false );
        section.addAllowedSectionContentType( fixture.findContentTypeByName( "article" ) );
        fixture.save( section );
        fixture.save( createMenuItemAccess( "My section", "aru", "add" ) );
        createContent( "my-supported-content", "Articles" );

        fixture.flushAndClearHibernateSession();

        // exercise
        AddContentToSectionCommand command = new AddContentToSectionCommand();
        command.setAddOnTop( false );
        command.setApproveInSection( false );
        command.setContributor( fixture.findUserByName( "aru" ).getKey() );
        command.setSection( fixture.findMenuItemByName( "My section" ).getKey() );
        command.setContent( fixture.findContentByName( "my-supported-content" ).getKey() );
        menuItemService.execute( command );

        // verify: content added in section
        assertEquals( 1, fixture.findContentByName( "my-supported-content" ).getSectionContents().size() );
        assertEquals( 1, fixture.findMenuItemByName( "My section" ).getSectionContents().size() );
    }

    @Test(expected = ContentTypeNotSupportedException.class)
    public void addContentToSection_adding_content_that_is_not_supported_by_section_throws_exception()
    {
        // setup section with different content type supported
        fixture.save( factory.createContentType( "just-another-cty", ContentHandlerName.CUSTOM.getHandlerClassShortName(), null ) );
        MenuItemEntity section = createSection( "My section", "The Newspaper", false );
        section.addAllowedSectionContentType( fixture.findContentTypeByName( "just-another-cty" ) );
        fixture.save( section );
        fixture.save( createMenuItemAccess( "My section", "aru", "add" ) );
        createContent( "my-unsupported-content", "Articles" );

        fixture.flushAndClearHibernateSession();

        // verify setup:
        assertEquals( 1, fixture.findMenuItemByName( "My section" ).getAllowedSectionContentTypes().size() );
        assertEquals( "just-another-cty",
                      fixture.findMenuItemByName( "My section" ).getAllowedSectionContentTypes().iterator().next().getName() );

        // exercise
        AddContentToSectionCommand command = new AddContentToSectionCommand();
        command.setAddOnTop( false );
        command.setApproveInSection( false );
        command.setContributor( fixture.findUserByName( "aru" ).getKey() );
        command.setSection( fixture.findMenuItemByName( "My section" ).getKey() );
        command.setContent( fixture.findContentByName( "my-unsupported-content" ).getKey() );
        menuItemService.execute( command );
    }

    @Test
    public void addContentToSection_add_content_to_ordered_section_as_unapproved_and_not_on_top_when_section_is_empty()
    {
        // setup
        fixture.save( createSection( "My section", "The Newspaper", true ) );
        fixture.save( createMenuItemAccess( "My section", "aru", "add" ) );
        createContent( "my-content", "Articles" );

        // exercise
        AddContentToSectionCommand command = new AddContentToSectionCommand();
        command.setAddOnTop( false );
        command.setApproveInSection( false );
        command.setContributor( fixture.findUserByName( "aru" ).getKey() );
        command.setSection( fixture.findMenuItemByName( "My section" ).getKey() );
        command.setContent( fixture.findContentByName( "my-content" ).getKey() );
        menuItemService.execute( command );
        fixture.flushAndClearHibernateSession();

        // verify
        assertEquals( 1, fixture.findMenuItemByName( "My section" ).getSectionContents().size() );
        SectionContentEntity sectionContentAdded = fixture.findMenuItemByName( "My section" ).getSectionContents().iterator().next();
        assertNotNull( sectionContentAdded );
        assertEquals( "my-content", sectionContentAdded.getContent().getName() );
        assertEquals( false, sectionContentAdded.isApproved() );
        assertEquals( 0, sectionContentAdded.getOrder() );
    }

    @Test
    public void addContentToSection_add_content_to_ordered_section_as_unapproved_and_not_on_top_when_section_is_not_empty()
    {
        // setup
        fixture.save( createSection( "My section", "The Newspaper", true ) );
        fixture.save( createMenuItemAccess( "My section", "aru", "add" ) );
        createContent( "first-content", "Articles" );
        createContent( "second-content", "Articles" );

        AddContentToSectionCommand command = new AddContentToSectionCommand();
        command.setAddOnTop( false );
        command.setApproveInSection( false );
        command.setContributor( fixture.findUserByName( "aru" ).getKey() );
        command.setSection( fixture.findMenuItemByName( "My section" ).getKey() );
        command.setContent( fixture.findContentByName( "first-content" ).getKey() );
        menuItemService.execute( command );
        fixture.flushAndClearHibernateSession();

        // exercise
        command = new AddContentToSectionCommand();
        command.setAddOnTop( false );
        command.setApproveInSection( false );
        command.setContributor( fixture.findUserByName( "aru" ).getKey() );
        command.setSection( fixture.findMenuItemByName( "My section" ).getKey() );
        command.setContent( fixture.findContentByName( "second-content" ).getKey() );
        menuItemService.execute( command );
        fixture.flushAndClearHibernateSession();

        // verify: both have 0 as order
        List<SectionContentEntity> sectionContentAdded =
            Lists.newArrayList( fixture.findMenuItemByName( "My section" ).getSectionContents() );
        assertEquals( 2, sectionContentAdded.size() );
        assertEquals( false, sectionContentAdded.get( 0 ).isApproved() );
        assertEquals( 0, sectionContentAdded.get( 0 ).getOrder() );
        assertEquals( false, sectionContentAdded.get( 1 ).isApproved() );
        assertEquals( 0, sectionContentAdded.get( 1 ).getOrder() );
    }

    @Test
    public void addContentToSection_add_content_to_ordered_section_as_approved_and_on_top_when_section_is_empty()
    {
        // setup
        fixture.save( createSection( "My section", "The Newspaper", true ) );
        fixture.save( createMenuItemAccess( "My section", "aru", "add publish" ) );
        createContent( "my-content", "Articles" );

        // exercise
        AddContentToSectionCommand command = new AddContentToSectionCommand();
        command.setAddOnTop( true );
        command.setApproveInSection( true );
        command.setContributor( fixture.findUserByName( "aru" ).getKey() );
        command.setSection( fixture.findMenuItemByName( "My section" ).getKey() );
        command.setContent( fixture.findContentByName( "my-content" ).getKey() );
        menuItemService.execute( command );
        fixture.flushAndClearHibernateSession();

        // verify: order is 0
        assertEquals( 1, fixture.findMenuItemByName( "My section" ).getSectionContents().size() );
        SectionContentEntity sectionContentAdded = fixture.findMenuItemByName( "My section" ).getSectionContents().iterator().next();
        assertNotNull( sectionContentAdded );
        assertEquals( "my-content", sectionContentAdded.getContent().getName() );
        assertEquals( true, sectionContentAdded.isApproved() );
        assertEquals( 0, sectionContentAdded.getOrder() );
    }

    @Test
    public void addContentToSection_order_of_second_content_is_higher_than_order_of_first_content_that_is_already_added_and_approved_in_section()
    {
        // setup
        fixture.save( createSection( "My section", "The Newspaper", true ) );
        fixture.save( createMenuItemAccess( "My section", "aru", "add publish" ) );
        createContent( "first-content", "Articles" );
        createContent( "second-content", "Articles" );

        AddContentToSectionCommand command = new AddContentToSectionCommand();
        command.setAddOnTop( true );
        command.setApproveInSection( true );
        command.setContributor( fixture.findUserByName( "aru" ).getKey() );
        command.setSection( fixture.findMenuItemByName( "My section" ).getKey() );
        command.setContent( fixture.findContentByName( "first-content" ).getKey() );
        menuItemService.execute( command );
        fixture.flushAndClearHibernateSession();

        // exercise
        command = new AddContentToSectionCommand();
        command.setAddOnTop( true );
        command.setApproveInSection( true );
        command.setContributor( fixture.findUserByName( "aru" ).getKey() );
        command.setSection( fixture.findMenuItemByName( "My section" ).getKey() );
        command.setContent( fixture.findContentByName( "second-content" ).getKey() );
        menuItemService.execute( command );
        fixture.flushAndClearHibernateSession();

        // verify: order of second-content is higher than the order of the first-content
        List<SectionContentEntity> sectionContentAdded =
            Lists.newArrayList( fixture.findMenuItemByName( "My section" ).getSectionContents() );
        assertEquals( 2, sectionContentAdded.size() );
        assertTrue( sectionContentAdded.get( 1 ).getOrder() > sectionContentAdded.get( 0 ).getOrder() );
    }

    @Test
    public void addContentToSection_add_content_to_ordered_section_as_approved_and_on_top_when_section_is_not_empty()
    {
        // setup
        fixture.save( createSection( "My section", "The Newspaper", true ) );
        fixture.save( createMenuItemAccess( "My section", "aru", "add publish" ) );
        createContent( "first-content", "Articles" );
        createContent( "second-content", "Articles" );

        AddContentToSectionCommand command = new AddContentToSectionCommand();
        command.setAddOnTop( true );
        command.setApproveInSection( true );
        command.setContributor( fixture.findUserByName( "aru" ).getKey() );
        command.setSection( fixture.findMenuItemByName( "My section" ).getKey() );
        command.setContent( fixture.findContentByName( "first-content" ).getKey() );
        menuItemService.execute( command );
        fixture.flushAndClearHibernateSession();

        // exercise
        command = new AddContentToSectionCommand();
        command.setAddOnTop( true );
        command.setApproveInSection( true );
        command.setContributor( fixture.findUserByName( "aru" ).getKey() );
        command.setSection( fixture.findMenuItemByName( "My section" ).getKey() );
        command.setContent( fixture.findContentByName( "second-content" ).getKey() );
        menuItemService.execute( command );
        fixture.flushAndClearHibernateSession();

        // verify: second content is on top and approved
        List<SectionContentEntity> sectionContentAdded =
            Lists.newArrayList( fixture.findMenuItemByName( "My section" ).getSectionContents() );
        assertEquals( 2, sectionContentAdded.size() );
        assertEquals( "second-content", sectionContentAdded.get( 0 ).getContent().getName() );
        assertEquals( "first-content", sectionContentAdded.get( 1 ).getContent().getName() );
        assertEquals( true, sectionContentAdded.get( 0 ).isApproved() );
        assertEquals( true, sectionContentAdded.get( 1 ).isApproved() );

        // verify: collections on content and menu-item
        assertEquals( 1, fixture.findContentByName( "first-content" ).getSectionContents().size() );
        assertEquals( 1, fixture.findContentByName( "second-content" ).getSectionContents().size() );
        assertEquals( 2, fixture.findMenuItemByName( "My section" ).getSectionContents().size() );
    }

    @Test
    public void addContentToSection_addAndApprove_content_to_ordered_section_and_order_it_to_bottom_when_section_is_not_empty()
    {
        // setup
        fixture.save( createSection( "My section", "The Newspaper", true ) );
        fixture.save( createMenuItemAccess( "My section", "aru", "add publish" ) );
        createContent( "first-content", "Articles" );
        createContent( "second-content", "Articles" );

        AddContentToSectionCommand command = new AddContentToSectionCommand();
        command.setAddOnTop( true );
        command.setApproveInSection( true );
        command.setContributor( fixture.findUserByName( "aru" ).getKey() );
        command.setSection( fixture.findMenuItemByName( "My section" ).getKey() );
        command.setContent( fixture.findContentByName( "first-content" ).getKey() );
        menuItemService.execute( command );
        fixture.flushAndClearHibernateSession();

        // exercise
        command = new AddContentToSectionCommand();
        command.setAddOnTop( true );
        command.setApproveInSection( true );
        command.setContributor( fixture.findUserByName( "aru" ).getKey() );
        command.setSection( fixture.findMenuItemByName( "My section" ).getKey() );
        command.setContent( fixture.findContentByName( "second-content" ).getKey() );
        OrderContentsInSectionCommand orderContentsInSectionCommand = command.createOrderContentsInSectionCommand();
        orderContentsInSectionCommand.addContent( fixture.findContentByName( "first-content" ).getKey() );
        orderContentsInSectionCommand.addContent( fixture.findContentByName( "second-content" ).getKey() );
        menuItemService.execute( command );
        fixture.flushAndClearHibernateSession();

        // verify: order of first-content is lower than the order of the second-content
        List<SectionContentEntity> sectionContentAdded =
            Lists.newArrayList( fixture.findMenuItemByName( "My section" ).getSectionContents() );
        assertEquals( 2, sectionContentAdded.size() );
        assertTrue( sectionContentAdded.get( 0 ).getOrder() < sectionContentAdded.get( 1 ).getOrder() );
        assertEquals( "first-content", sectionContentAdded.get( 0 ).getContent().getName() );
        assertEquals( "second-content", sectionContentAdded.get( 1 ).getContent().getName() );
        assertEquals( true, sectionContentAdded.get( 0 ).isApproved() );
        assertEquals( true, sectionContentAdded.get( 1 ).isApproved() );

        // verify: collections on content and menu-item
        assertEquals( 1, fixture.findContentByName( "first-content" ).getSectionContents().size() );
        assertEquals( 1, fixture.findContentByName( "second-content" ).getSectionContents().size() );
        assertEquals( 2, fixture.findMenuItemByName( "My section" ).getSectionContents().size() );
    }

    @Test
    public void addContentToSection_add_content_to_ordered_section_as_approved_with_given_order_when_section_is_empty()
    {
        // setup
        fixture.save( createSection( "My section", "The Newspaper", true ) );
        fixture.save( createMenuItemAccess( "My section", "aru", "add publish" ) );
        createContent( "my-content", "Articles" );
        fixture.flushAndClearHibernateSession();

        // exercise
        AddContentToSectionCommand command = new AddContentToSectionCommand();
        command.setAddOnTop( true );
        command.setApproveInSection( true );
        command.setContributor( fixture.findUserByName( "aru" ).getKey() );
        command.setSection( fixture.findMenuItemByName( "My section" ).getKey() );
        command.setContent( fixture.findContentByName( "my-content" ).getKey() );
        List<ContentKey> wantedOrder = Lists.newArrayList( fixture.findContentByName( "my-content" ).getKey() );
        command.createOrderContentsInSectionCommand().setWantedOrder( wantedOrder );
        menuItemService.execute( command );

        fixture.flushAndClearHibernateSession();

        // verify: order is 0
        assertEquals( 1, fixture.findMenuItemByName( "My section" ).getSectionContents().size() );
        SectionContentEntity sectionContentAdded = fixture.findMenuItemByName( "My section" ).getSectionContents().iterator().next();
        assertNotNull( sectionContentAdded );
        assertEquals( "my-content", sectionContentAdded.getContent().getName() );
        assertEquals( true, sectionContentAdded.isApproved() );
        assertEquals( 0, sectionContentAdded.getOrder() );
        assertEquals( 1, fixture.findMenuItemByName( "My section" ).getSectionContents().size() );
        assertEquals( 1, fixture.findContentByName( "my-content" ).getSectionContents().size() );
    }

    @Test
    public void add_one_content_to_a_unorderedSsection_with_no_content_as_unapproved()
    {
        // setup
        createContent( "c-1", "Articles" );
        fixture.save( createUnorderedSection( "mysection" ) );
        fixture.save( createMenuItemAccess( "mysection", "aru", "add publish" ) );

        fixture.flushAndClearHibernateSession();

        // exercise
        AddContentToSectionCommand command =
            createAddContentToSectionCommand( "aru", fixture.findContentByName( "c-1" ), fixture.findMenuItemByName( "mysection" ), false );
        command.setAddOnTop( false );
        menuItemService.execute( command );
        fixture.flushAndClearHibernateSession();

        // verify
        List<SectionContentEntity> sectionContents = Lists.newArrayList( fixture.findMenuItemByName( "mysection" ).getSectionContents() );
        assertEquals( 1, sectionContents.size() );
        assertEquals( false, sectionContents.get( 0 ).isApproved() );
    }

    @Test
    public void add_one_content_to_two_unorderedSections_one_as_published_other_as_unpublished()
    {
        // setup
        String siteName = "The Newspaper";
        String categoryName = "Articles";

        final ContentKey contentKey = createContent( "a-content", categoryName );
        final ContentEntity content = fixture.findContentByKey( contentKey );

        boolean isOrderedSection = false;
        MenuItemEntity section1 =
            factory.createSectionMenuItem( "mysection1", 0, null, "My Section", siteName, "admin", "admin", "en", null, null,
                                           isOrderedSection, null, false, null );
        fixture.save( section1 );

        MenuItemEntity section2 =
            factory.createSectionMenuItem( "mysection2", 0, null, "My Section", siteName, "admin", "admin", "en", null, null,
                                           isOrderedSection, null, false, null );
        fixture.save( section2 );
        fixture.save( createMenuItemAccess( "mysection1", "aru", "add publish" ) );
        fixture.save( createMenuItemAccess( "mysection2", "aru", "add publish" ) );

        fixture.flushAndClearHibernateSession();

        // exercise
        AddContentToSectionCommand command1 = createAddContentToSectionCommand( "aru", content, section1, true );
        menuItemService.execute( command1 );
        fixture.flushAndClearHibernateSession();
        AddContentToSectionCommand command2 = createAddContentToSectionCommand( "aru", content, section2, false );
        command2.setAddOnTop( false );
        menuItemService.execute( command2 );
        fixture.flushAndClearHibernateSession();

        // verify: mysection1
        section1 = fixture.findMenuItemByName( "mysection1" );
        assertNotNull( section1 );
        Set<SectionContentEntity> sectionContents = section1.getSectionContents();
        assertEquals( 1, sectionContents.size() );

        SectionContentEntity sectionContent = sectionContents.iterator().next();
        assertEquals( true, sectionContent.isApproved() );

        // verify: mysection2
        assertNotNull( section2 );
        section2 = fixture.findMenuItemByName( "mysection2" );
        sectionContents = section2.getSectionContents();
        assertEquals( 1, sectionContents.size() );

        sectionContent = sectionContents.iterator().next();
        assertEquals( false, sectionContent.isApproved() );
    }

    @Test
    public void add_one_content_to_a_unorderedSection_as_published()
    {
        // setup
        String siteName = "The Newspaper";
        String categoryName = "Articles";

        final ContentKey contentKey = createContent( "a-content", categoryName );
        final ContentEntity content = fixture.findContentByKey( contentKey );

        boolean isOrderedSection = false;
        MenuItemEntity section =
            factory.createSectionMenuItem( "mysection", 0, null, "My section", siteName, "admin", "admin", "en", null, null,
                                           isOrderedSection, null, false, null );
        fixture.save( section );

        fixture.save( createMenuItemAccess( "mysection", "aru", "add publish" ) );

        AddContentToSectionCommand command = createAddContentToSectionCommand( "aru", content, section, true );

        fixture.flushAndClearHibernateSession();

        // exercise
        menuItemService.execute( command );

        // verify
        fixture.flushAndClearHibernateSession();

        section = fixture.findMenuItemByName( "mysection" );
        Set<SectionContentEntity> sectionContents = section.getSectionContents();
        assertEquals( 1, sectionContents.size() );

        SectionContentEntity sectionContent = sectionContents.iterator().next();
        assertEquals( true, sectionContent.isApproved() );
    }

    @Test
    public void approveContentsInSection_only_the_changed_section_content_have_updated_timestamp()
    {
        // setup
        fixture.save( createSection( "My section", "The Newspaper", true ) );
        fixture.save( createMenuItemAccess( "My section", "aru", "add publish" ) );
        createContent( "first-content", "Articles" );
        createContent( "second-content", "Articles" );
        createContent( "third-content", "Articles" );

        fixture.flushAndClearHibernateSession();

        ContentKey c1 = fixture.findContentByName( "first-content" ).getKey();
        ContentKey c2 = fixture.findContentByName( "second-content" ).getKey();
        ContentKey c3 = fixture.findContentByName( "third-content" ).getKey();

        AddContentToSectionCommand addCommand = createAddContentToSectionCommand( "aru", fixture.findContentByName( "first-content" ),
                                                                                  fixture.findMenuItemByName( "My section" ), true );
        menuItemService.execute( addCommand );
        fixture.flushAndClearHibernateSession();
        addCommand = createAddContentToSectionCommand( "aru", fixture.findContentByName( "second-content" ),
                                                       fixture.findMenuItemByName( "My section" ), true );
        menuItemService.execute( addCommand );
        fixture.flushAndClearHibernateSession();

        addCommand = createAddContentToSectionCommand( "aru", fixture.findContentByName( "third-content" ),
                                                       fixture.findMenuItemByName( "My section" ), true );
        menuItemService.execute( addCommand );
        fixture.flushAndClearHibernateSession();

        Date c1Timestamp = fixture.findMenuItemByName( "My section" ).getSectionContent( c1 ).getTimestamp();
        int c1Order = fixture.findMenuItemByName( "My section" ).getSectionContent( c1 ).getOrder();
        Date c2Timestamp = fixture.findMenuItemByName( "My section" ).getSectionContent( c2 ).getTimestamp();
        int c2Order = fixture.findMenuItemByName( "My section" ).getSectionContent( c2 ).getOrder();
        Date c3Timestamp = fixture.findMenuItemByName( "My section" ).getSectionContent( c3 ).getTimestamp();
        int c3Order = fixture.findMenuItemByName( "My section" ).getSectionContent( c3 ).getOrder();

        for ( SectionContentEntity sc : fixture.findMenuItemByName( "My section" ).getSectionContents() )
        {
            System.out.println(
                sc.getContent().getName() + " (" + sc.getContent().getKey() + "), order: " + sc.getOrder() + ", ts: " + sc.getTimestamp() );
        }

        // exercise
        ApproveContentsInSectionCommand approveContentsInSectionCommand = new ApproveContentsInSectionCommand();
        approveContentsInSectionCommand.setApprover( fixture.findUserByName( "aru" ).getKey() );
        approveContentsInSectionCommand.setSection( fixture.findMenuItemByName( "My section" ).getKey() );
        approveContentsInSectionCommand.addContentToApprove( c3 );
        approveContentsInSectionCommand.addContentToApprove( c1 );
        approveContentsInSectionCommand.addContentToApprove( c2 );
        List<ContentKey> wantedOrder = Lists.newArrayList( c3, c1, c2 );
        OrderContentsInSectionCommand orderContentsInSectionCommand =
            approveContentsInSectionCommand.createAndReturnOrderContentsInSectionCommand();
        orderContentsInSectionCommand.setWantedOrder( wantedOrder );
        menuItemService.execute( approveContentsInSectionCommand );
        fixture.flushAndClearHibernateSession();

        for ( SectionContentEntity sc : fixture.findMenuItemByName( "My section" ).getSectionContents() )
        {
            System.out.println(
                sc.getContent().getName() + " (" + sc.getContent().getKey() + "), order: " + sc.getOrder() + ", ts: " + sc.getTimestamp() );
        }

        // verify: only first content have changed
        assertTrue( c1Order != fixture.findMenuItemByName( "My section" ).getSectionContent( c1 ).getOrder() );
        assertTrue( c2Order == fixture.findMenuItemByName( "My section" ).getSectionContent( c2 ).getOrder() );
        assertTrue( c3Order == fixture.findMenuItemByName( "My section" ).getSectionContent( c3 ).getOrder() );

        // verify: only first content have timestamp changed
        assertTrue( c1Timestamp.getTime() < fixture.findMenuItemByName( "My section" ).getSectionContent( c1 ).getTimestamp().getTime() );
        assertEquals( c2Timestamp.getTime(), fixture.findMenuItemByName( "My section" ).getSectionContent( c2 ).getTimestamp().getTime() );
        assertEquals( c3Timestamp.getTime(), fixture.findMenuItemByName( "My section" ).getSectionContent( c3 ).getTimestamp().getTime() );
    }

    @Test
    public void approveContentInSection_approve_one_content_in_empty_section()
    {
        // setup: content
        fixture.save( createSection( "My section", "The Newspaper", true ) );
        fixture.save( createMenuItemAccess( "My section", "aru", "add publish" ) );
        createContent( "first-content", "Articles" );
        fixture.flushAndClearHibernateSession();

        // setup: add content to section without approving it
        AddContentToSectionCommand addCommand = createAddContentToSectionCommand( "aru", fixture.findContentByName( "first-content" ),
                                                                                  fixture.findMenuItemByName( "My section" ), false );
        addCommand.setAddOnTop( false );
        menuItemService.execute( addCommand );
        fixture.flushAndClearHibernateSession();

        // verify setup:
        List<SectionContentEntity> sectionContents = Lists.newArrayList( fixture.findMenuItemByName( "My section" ).getSectionContents() );
        assertEquals( 1, sectionContents.size() );
        assertEquals( false, sectionContents.get( 0 ).isApproved() );

        fixture.flushAndClearHibernateSession();

        // exercise
        ApproveContentInSectionCommand approveContentInSectionCommand = new ApproveContentInSectionCommand();
        approveContentInSectionCommand.setApprover( fixture.findUserByName( "aru" ).getKey() );
        approveContentInSectionCommand.setSection( fixture.findMenuItemByName( "My section" ).getKey() );
        approveContentInSectionCommand.setContentToApprove( fixture.findContentByName( "first-content" ).getKey() );
        menuItemService.execute( approveContentInSectionCommand );
        fixture.flushAndClearHibernateSession();

        // verify: content is approved in section
        sectionContents = Lists.newArrayList( fixture.findMenuItemByName( "My section" ).getSectionContents() );
        assertEquals( 1, sectionContents.size() );
        assertEquals( true, sectionContents.get( 0 ).isApproved() );
    }

    @Test
    public void approveContentsInSection_approveAndOrder_several_content_gets_order_with_order_space_between()
    {
        // setup: content
        fixture.save( createSection( "My section", "The Newspaper", true ) );
        fixture.save( createMenuItemAccess( "My section", "aru", "add publish" ) );
        createContent( "c-1", "Articles" );
        createContent( "c-2", "Articles" );
        createContent( "c-3", "Articles" );
        createContent( "c-4", "Articles" );
        createContent( "c-5", "Articles" );
        fixture.flushAndClearHibernateSession();

        // setup: add content to section without approving it
        AddContentToSectionCommand addCommand =
            createAddContentToSectionCommand( "aru", fixture.findContentByName( "c-1" ), fixture.findMenuItemByName( "My section" ),
                                              false );
        addCommand.setAddOnTop( false );
        menuItemService.execute( addCommand );
        addCommand.setContent( fixture.findContentByName( "c-2" ).getKey() );
        menuItemService.execute( addCommand );
        addCommand.setContent( fixture.findContentByName( "c-3" ).getKey() );
        menuItemService.execute( addCommand );
        addCommand.setContent( fixture.findContentByName( "c-4" ).getKey() );
        menuItemService.execute( addCommand );
        addCommand.setContent( fixture.findContentByName( "c-5" ).getKey() );
        menuItemService.execute( addCommand );
        fixture.flushAndClearHibernateSession();

        // verify setup:
        List<SectionContentEntity> sectionContents = Lists.newArrayList( fixture.findMenuItemByName( "My section" ).getSectionContents() );
        assertEquals( 5, sectionContents.size() );
        assertEquals( false, sectionContents.get( 0 ).isApproved() );
        assertEquals( false, sectionContents.get( 4 ).isApproved() );

        fixture.flushAndClearHibernateSession();

        // exercise
        ApproveContentsInSectionCommand approveContentsInSectionCommand = new ApproveContentsInSectionCommand();
        approveContentsInSectionCommand.setApprover( fixture.findUserByName( "aru" ).getKey() );
        approveContentsInSectionCommand.setSection( fixture.findMenuItemByName( "My section" ).getKey() );
        approveContentsInSectionCommand.addContentToApprove( fixture.findContentByName( "c-1" ).getKey() );
        approveContentsInSectionCommand.addContentToApprove( fixture.findContentByName( "c-2" ).getKey() );
        approveContentsInSectionCommand.addContentToApprove( fixture.findContentByName( "c-3" ).getKey() );
        approveContentsInSectionCommand.addContentToApprove( fixture.findContentByName( "c-4" ).getKey() );
        approveContentsInSectionCommand.addContentToApprove( fixture.findContentByName( "c-5" ).getKey() );
        OrderContentsInSectionCommand orderContentsInSectionCommand =
            approveContentsInSectionCommand.createAndReturnOrderContentsInSectionCommand();
        orderContentsInSectionCommand.addContent( fixture.findContentByName( "c-1" ).getKey() );
        orderContentsInSectionCommand.addContent( fixture.findContentByName( "c-2" ).getKey() );
        orderContentsInSectionCommand.addContent( fixture.findContentByName( "c-3" ).getKey() );
        orderContentsInSectionCommand.addContent( fixture.findContentByName( "c-4" ).getKey() );
        orderContentsInSectionCommand.addContent( fixture.findContentByName( "c-5" ).getKey() );
        menuItemService.execute( approveContentsInSectionCommand );
        fixture.flushAndClearHibernateSession();

        // verify: content is approved in section
        sectionContents = Lists.newArrayList( fixture.findMenuItemByName( "My section" ).getSectionContents() );
        assertEquals( 5, sectionContents.size() );
        assertEquals( true, sectionContents.get( 0 ).isApproved() );
        assertEquals( true, sectionContents.get( 1 ).isApproved() );
        assertEquals( true, sectionContents.get( 2 ).isApproved() );
        assertEquals( true, sectionContents.get( 3 ).isApproved() );
        assertEquals( true, sectionContents.get( 4 ).isApproved() );

        // verify: content is in ascending order with 1000 in between each
        assertEquals( 1000, sectionContents.get( 0 ).getOrder() );
        assertEquals( 2000, sectionContents.get( 1 ).getOrder() );
        assertEquals( 3000, sectionContents.get( 2 ).getOrder() );
        assertEquals( 4000, sectionContents.get( 3 ).getOrder() );
        assertEquals( 5000, sectionContents.get( 4 ).getOrder() );
    }

    @Test
    public void approveContentInSection_second_content_approved_in_section_is_ordered_above_first()
    {
        // setup: content
        fixture.save( createSection( "My section", "The Newspaper", true ) );
        fixture.save( createMenuItemAccess( "My section", "aru", "add publish" ) );
        createContent( "first-content", "Articles" );
        createContent( "second-content", "Articles" );
        fixture.flushAndClearHibernateSession();

        // setup: add content to section without approving it
        menuItemService.execute( createAddContentToSectionCommand( "aru", fixture.findContentByName( "first-content" ),
                                                                   fixture.findMenuItemByName( "My section" ), false ),
                                 createAddContentToSectionCommand( "aru", fixture.findContentByName( "second-content" ),
                                                                   fixture.findMenuItemByName( "My section" ), false ) );
        fixture.flushAndClearHibernateSession();

        // verify setup:
        List<SectionContentEntity> sectionContents = Lists.newArrayList( fixture.findMenuItemByName( "My section" ).getSectionContents() );
        assertEquals( 2, sectionContents.size() );
        assertEquals( false, sectionContents.get( 0 ).isApproved() );
        assertEquals( false, sectionContents.get( 1 ).isApproved() );

        fixture.flushAndClearHibernateSession();

        // exercise
        ApproveContentInSectionCommand approve1 = new ApproveContentInSectionCommand();
        approve1.setApprover( fixture.findUserByName( "aru" ).getKey() );
        approve1.setSection( fixture.findMenuItemByName( "My section" ).getKey() );
        approve1.setContentToApprove( fixture.findContentByName( "first-content" ).getKey() );
        ApproveContentInSectionCommand approve2 = new ApproveContentInSectionCommand();
        approve2.setApprover( fixture.findUserByName( "aru" ).getKey() );
        approve2.setSection( fixture.findMenuItemByName( "My section" ).getKey() );
        approve2.setContentToApprove( fixture.findContentByName( "second-content" ).getKey() );
        menuItemService.execute( approve1, approve2 );
        fixture.flushAndClearHibernateSession();

        // verify: content is approved in section
        sectionContents = Lists.newArrayList( fixture.findMenuItemByName( "My section" ).getSectionContents() );
        assertEquals( 2, sectionContents.size() );
        assertEquals( true, sectionContents.get( 0 ).isApproved() );
        assertEquals( true, sectionContents.get( 1 ).isApproved() );
        assertTrue( sectionContents.get( 0 ).getOrder() < sectionContents.get( 1 ).getOrder() );
        assertEquals( "second-content", sectionContents.get( 0 ).getContent().getName() );
        assertEquals( "first-content", sectionContents.get( 1 ).getContent().getName() );
    }

    @Test(expected = MenuItemAccessException.class)
    public void unapproveContentInSection_throws_exception_when_unapprover_does_not_have_approve_right()
    {
        // setup: content
        fixture.save( createSection( "My section", "The Newspaper", true ) );
        fixture.createAndStoreNormalUserWithUserGroup( "add-only-user", "Add only rights user", "testuserstore" );
        fixture.save( createMenuItemAccess( "My section", "aru", "add publish" ) );
        fixture.save( createMenuItemAccess( "My section", "add-only-user", "add" ) );
        createContent( "first-content", "Articles" );
        fixture.flushAndClearHibernateSession();

        // setup: add content to section without approving it
        menuItemService.execute( createAddContentToSectionCommand( "aru", fixture.findContentByName( "first-content" ),
                                                                   fixture.findMenuItemByName( "My section" ), true ) );
        fixture.flushAndClearHibernateSession();

        // verify setup:
        List<SectionContentEntity> sectionContents = Lists.newArrayList( fixture.findMenuItemByName( "My section" ).getSectionContents() );
        assertEquals( 1, sectionContents.size() );
        assertEquals( true, sectionContents.get( 0 ).isApproved() );

        fixture.flushAndClearHibernateSession();

        // exercise
        UnapproveContentsInSectionCommand command = new UnapproveContentsInSectionCommand();
        command.setUnapprover( fixture.findUserByName( "add-only-user" ).getKey() );
        command.setSection( fixture.findMenuItemByName( "My section" ).getKey() );
        command.addContentToUnapprove( fixture.findContentByName( "first-content" ).getKey() );
        menuItemService.execute( command );
    }

    @Test
    public void unapproveContentInSection_unapprove_one_and_only_content_in_section()
    {
        // setup: content
        fixture.save( createSection( "My section", "The Newspaper", true ) );
        fixture.save( createMenuItemAccess( "My section", "aru", "add publish" ) );
        createContent( "first-content", "Articles" );
        fixture.flushAndClearHibernateSession();

        // setup: add content to section without approving it
        menuItemService.execute( createAddContentToSectionCommand( "aru", fixture.findContentByName( "first-content" ),
                                                                   fixture.findMenuItemByName( "My section" ), true ) );
        fixture.flushAndClearHibernateSession();

        // verify setup:
        List<SectionContentEntity> sectionContents = Lists.newArrayList( fixture.findMenuItemByName( "My section" ).getSectionContents() );
        assertEquals( 1, sectionContents.size() );
        assertEquals( true, sectionContents.get( 0 ).isApproved() );

        fixture.flushAndClearHibernateSession();

        // exercise
        UnapproveContentsInSectionCommand command = new UnapproveContentsInSectionCommand();
        command.setUnapprover( fixture.findUserByName( "aru" ).getKey() );
        command.setSection( fixture.findMenuItemByName( "My section" ).getKey() );
        command.addContentToUnapprove( fixture.findContentByName( "first-content" ).getKey() );
        menuItemService.execute( command );
        fixture.flushAndClearHibernateSession();

        // verify: content is unapproved in section
        sectionContents = Lists.newArrayList( fixture.findMenuItemByName( "My section" ).getSectionContents() );
        assertEquals( 1, sectionContents.size() );
        assertEquals( false, sectionContents.get( 0 ).isApproved() );
        assertEquals( false, fixture.findContentByName( "first-content" ).getSectionContents().iterator().next().isApproved() );
    }

    @Test
    public void unapproveContentInSection_unapprove_one_of_two_content_in_section()
    {
        // setup: content
        fixture.save( createSection( "My section", "The Newspaper", true ) );
        fixture.save( createMenuItemAccess( "My section", "aru", "add publish" ) );
        createContent( "first-content", "Articles" );
        createContent( "second-content", "Articles" );
        fixture.flushAndClearHibernateSession();

        // setup: add content to section without approving it
        menuItemService.execute( createAddContentToSectionCommand( "aru", fixture.findContentByName( "first-content" ),
                                                                   fixture.findMenuItemByName( "My section" ), true ),
                                 createAddContentToSectionCommand( "aru", fixture.findContentByName( "second-content" ),
                                                                   fixture.findMenuItemByName( "My section" ), true ) );
        fixture.flushAndClearHibernateSession();

        // verify setup:
        List<SectionContentEntity> sectionContents = Lists.newArrayList( fixture.findMenuItemByName( "My section" ).getSectionContents() );
        assertEquals( 2, sectionContents.size() );
        assertEquals( true, sectionContents.get( 0 ).isApproved() );
        assertEquals( true, sectionContents.get( 1 ).isApproved() );

        fixture.flushAndClearHibernateSession();

        // exercise
        UnapproveContentsInSectionCommand command = new UnapproveContentsInSectionCommand();
        command.setUnapprover( fixture.findUserByName( "aru" ).getKey() );
        command.setSection( fixture.findMenuItemByName( "My section" ).getKey() );
        command.addContentToUnapprove( fixture.findContentByName( "first-content" ).getKey() );
        menuItemService.execute( command );
        fixture.flushAndClearHibernateSession();

        // verify: content is unapproved in section
        sectionContents = Lists.newArrayList( fixture.findMenuItemByName( "My section" ).getSectionContents() );
        assertEquals( 2, sectionContents.size() );

        assertEquals( false, fixture.findContentByName( "first-content" ).getSectionContents().iterator().next().isApproved() );
        assertEquals( true, fixture.findContentByName( "second-content" ).getSectionContents().iterator().next().isApproved() );
    }

    @Test
    public void setContentHome_set_content_home_when_no_home_exist()
    {
        fixture.save( createSection( "News", "The Newspaper", true ) );
        fixture.save( factory.createMenuItemAccess( fixture.findMenuItemByName( "News" ), fixture.findUserByName( "aru" ),
                                                    "read, create, update, delete, add, publish" ) );
        createContent( "c-1", "Articles" );

        fixture.flushAndClearHibernateSession();

        SetContentHomeCommand command = new SetContentHomeCommand();
        command.setSetter( fixture.findUserByName( "aru" ).getKey() );
        command.setContent( fixture.findContentByName( "c-1" ).getKey() );
        command.setSection( fixture.findMenuItemByName( "News" ).getKey() );
        menuItemService.execute( command );

        fixture.flushAndClearHibernateSession();

        // verify content home is stored
        ContentHomeEntity actualContentHome = fixture.findContentHomeByKey(
            new ContentHomeKey( fixture.findSiteByName( "The Newspaper" ).getKey(), fixture.findContentByName( "c-1" ).getKey() ) );
        assertNotNull( actualContentHome );
        assertEquals( fixture.findContentByName( "c-1" ), actualContentHome.getContent() );
        assertEquals( fixture.findMenuItemByName( "News" ), actualContentHome.getMenuItem() );
        assertEquals( fixture.findSiteByName( "The Newspaper" ), actualContentHome.getSite() );
        assertNull( actualContentHome.getPageTemplate() );

        // verify content home via content.getLocations
        ContentLocationSpecification contentLocationSpec = new ContentLocationSpecification();
        contentLocationSpec.setSiteKey( fixture.findSiteByName( "The Newspaper" ).getKey() );
        final ContentLocations contentLocations = fixture.findContentByName( "c-1" ).getLocations( contentLocationSpec );
        assertNotNull( contentLocations );
        final ContentLocation actualHomeLocation = contentLocations.getHomeLocation( fixture.findSiteByName( "The Newspaper" ).getKey() );
        assertNotNull( actualHomeLocation );
        assertEquals( fixture.findMenuItemByName( "News" ).getKey(), actualHomeLocation.getMenuItemKey() );
    }

    @Test
    public void setContentHome_set_content_home_with_page_template()
    {
        fixture.save( createSection( "News", "The Newspaper", true ) );
        fixture.save( factory.createMenuItemAccess( fixture.findMenuItemByName( "News" ), fixture.findUserByName( "aru" ),
                                                    "read, create, update, delete, add, publish" ) );
        createContent( "c-1", "Articles" );
        fixture.save(
            factory.createPageTemplate( "my-page-template", PageTemplateType.CONTENT, "The Newspaper", ResourceKey.from( "ABC" ) ) );

        fixture.flushAndClearHibernateSession();

        SetContentHomeCommand command = new SetContentHomeCommand();
        command.setSetter( fixture.findUserByName( "aru" ).getKey() );
        command.setContent( fixture.findContentByName( "c-1" ).getKey() );
        command.setSection( fixture.findMenuItemByName( "News" ).getKey() );
        command.setPageTemplate( fixture.findPageTemplateByName( "my-page-template" ).getPageTemplateKey() );
        menuItemService.execute( command );

        fixture.flushAndClearHibernateSession();

        // verify content home is stored
        ContentHomeEntity actualContentHome = fixture.findContentHomeByKey(
            new ContentHomeKey( fixture.findSiteByName( "The Newspaper" ).getKey(), fixture.findContentByName( "c-1" ).getKey() ) );
        assertNotNull( actualContentHome );
        assertEquals( fixture.findContentByName( "c-1" ), actualContentHome.getContent() );
        assertEquals( fixture.findMenuItemByName( "News" ), actualContentHome.getMenuItem() );
        assertEquals( fixture.findSiteByName( "The Newspaper" ), actualContentHome.getSite() );
        assertEquals( fixture.findPageTemplateByName( "my-page-template" ), actualContentHome.getPageTemplate() );

        // verify content home via content.getLocations
        ContentLocationSpecification contentLocationSpec = new ContentLocationSpecification();
        contentLocationSpec.setSiteKey( fixture.findSiteByName( "The Newspaper" ).getKey() );
        final ContentLocations contentLocations = fixture.findContentByName( "c-1" ).getLocations( contentLocationSpec );
        assertNotNull( contentLocations );
        final ContentLocation actualHomeLocation = contentLocations.getHomeLocation( fixture.findSiteByName( "The Newspaper" ).getKey() );
        assertNotNull( actualHomeLocation );
        assertEquals( fixture.findMenuItemByName( "News" ).getKey(), actualHomeLocation.getMenuItemKey() );

    }

    @Test
    public void setContentHome_set_content_home_when_home_exist()
    {
        fixture.save( createSection( "News", "The Newspaper", true ) );
        fixture.save( createSection( "Tabloid", "The Newspaper", true ) );

        fixture.save( factory.createMenuItemAccess( fixture.findMenuItemByName( "Tabloid" ), fixture.findUserByName( "aru" ),
                                                    "read, create, update, delete, add, publish" ) );

        createContent( "c-1", "Articles" );

        fixture.save( factory.createContentHome( fixture.findContentByName( "c-1" ), fixture.findMenuItemByName( "News" ), null ) );

        fixture.flushAndClearHibernateSession();

        assertNotNull( fixture.findMenuItemByName( "News" ) );
        assertNotNull( fixture.findContentByName( "c-1" ) );

        fixture.flushAndClearHibernateSession();

        SetContentHomeCommand command = new SetContentHomeCommand();
        command.setSetter( fixture.findUserByName( "aru" ).getKey() );
        command.setContent( fixture.findContentByName( "c-1" ).getKey() );
        command.setSection( fixture.findMenuItemByName( "Tabloid" ).getKey() );
        menuItemService.execute( command );

        fixture.flushAndClearHibernateSession();

        // verify content home is stored
        ContentHomeEntity actualContentHome = fixture.findContentHomeByKey(
            new ContentHomeKey( fixture.findSiteByName( "The Newspaper" ).getKey(), fixture.findContentByName( "c-1" ).getKey() ) );
        assertNotNull( actualContentHome );
        assertEquals( fixture.findContentByName( "c-1" ), actualContentHome.getContent() );
        assertEquals( fixture.findMenuItemByName( "Tabloid" ), actualContentHome.getMenuItem() );
        assertEquals( fixture.findSiteByName( "The Newspaper" ), actualContentHome.getSite() );
        assertEquals( null, actualContentHome.getPageTemplate() );

        // verify content home via content.getLocations
        ContentLocationSpecification contentLocationSpec = new ContentLocationSpecification();
        contentLocationSpec.setSiteKey( fixture.findSiteByName( "The Newspaper" ).getKey() );
        final ContentLocations contentLocations = fixture.findContentByName( "c-1" ).getLocations( contentLocationSpec );
        assertNotNull( contentLocations );
        final ContentLocation actualHomeLocation = contentLocations.getHomeLocation( fixture.findSiteByName( "The Newspaper" ).getKey() );
        assertNotNull( actualHomeLocation );
        assertEquals( fixture.findMenuItemByName( "Tabloid" ).getKey(), actualHomeLocation.getMenuItemKey() );
    }

    @Test(expected = CategoryAccessException.class)
    public void setContentHome_throws_exception_when_setter_have_no_approve_right_on_category()
    {
        fixture.createAndStoreNormalUserWithUserGroup( "permission-test-user", "User", "testuserstore" );
        fixture.save( createSection( "News", "The Newspaper", true ) );
        fixture.save( factory.createMenuItemAccess( fixture.findMenuItemByName( "News" ), fixture.findUserByName( "aru" ),
                                                    "read, create, update, delete, add, publish" ) );
        fixture.save( factory.createCategoryAccessForUser( "Articles", "permission-test-user", "read, create" ) );
        createContent( "c-1", "Articles" );

        // exercise
        SetContentHomeCommand command = new SetContentHomeCommand();
        command.setSetter( fixture.findUserByName( "permission-test-user" ).getKey() );
        command.setContent( fixture.findContentByName( "c-1" ).getKey() );
        command.setSection( fixture.findMenuItemByName( "News" ).getKey() );
        menuItemService.execute( command );
    }

    @Test
    public void removeContentsFromSection_checkRemoverPermissions_read_APPROVED()
    {
        checkUserPermissionForRemoveContentFromSection( "read", SectionStatus.APPROVE_CONTENT, RemoveAccess.DENIED );
    }

    @Test
    public void removeContentsFromSection_checkRemoverPermissions_read_update_APPROVED()
    {
        checkUserPermissionForRemoveContentFromSection( "read, update", SectionStatus.APPROVE_CONTENT, RemoveAccess.DENIED );
    }

    @Test
    public void removeContentsFromSection_checkRemoverPermissions_read_create_APPROVED()
    {
        checkUserPermissionForRemoveContentFromSection( "read, create", SectionStatus.APPROVE_CONTENT, RemoveAccess.DENIED );
    }

    @Test
    public void removeContentsFromSection_checkRemoverPermissions_read_delete_APPROVED()
    {
        checkUserPermissionForRemoveContentFromSection( "read, delete", SectionStatus.APPROVE_CONTENT, RemoveAccess.DENIED );
    }


    @Test
    public void removeContentsFromSection_checkRemoverPermissions_read_add_APPROVED()
    {
        checkUserPermissionForRemoveContentFromSection( "read, add", SectionStatus.APPROVE_CONTENT, RemoveAccess.DENIED );
    }

    @Test
    public void removeContentsFromSection_checkRemoverPermissions_read_add_delete_APPROVED()
    {
        checkUserPermissionForRemoveContentFromSection( "read, add, delete", SectionStatus.APPROVE_CONTENT, RemoveAccess.DENIED );
    }


    @Test
    public void removeContentsFromSection_checkRemoverPermissions_read_delete_publish_APPROVED()
    {
        checkUserPermissionForRemoveContentFromSection( "read, delete, publish", SectionStatus.APPROVE_CONTENT, RemoveAccess.ALLOWED );
    }

    @Test
    public void removeContentsFromSection_checkRemoverPermissions_read_add_publish_APPROVED()
    {
        checkUserPermissionForRemoveContentFromSection( "read, add, publish", SectionStatus.APPROVE_CONTENT, RemoveAccess.ALLOWED );
    }

    @Test
    public void removeContentsFromSection_checkRemoverPermissions_read_add_publish_delete_APPROVED()
    {
        checkUserPermissionForRemoveContentFromSection( "read, add, publish, delete", SectionStatus.APPROVE_CONTENT, RemoveAccess.ALLOWED );
    }

    @Test
    public void removeContentsFromSection_checkRemoverPermissions_read_NOT_APPROVED()
    {
        checkUserPermissionForRemoveContentFromSection( "read", SectionStatus.DO_NOT_APPROVE_CONTENT, RemoveAccess.DENIED );
    }

    @Test
    public void removeContentsFromSection_checkRemoverPermissions_read_update_NOT_APPROVED()
    {
        checkUserPermissionForRemoveContentFromSection( "read, update", SectionStatus.DO_NOT_APPROVE_CONTENT, RemoveAccess.DENIED );
    }

    @Test
    public void removeContentsFromSection_checkRemoverPermissions_read_create_NOT_APPROVED()
    {
        checkUserPermissionForRemoveContentFromSection( "read, create", SectionStatus.DO_NOT_APPROVE_CONTENT, RemoveAccess.DENIED );
    }

    @Test
    public void removeContentsFromSection_checkRemoverPermissions_read_delete_NOT_APPROVED()
    {
        checkUserPermissionForRemoveContentFromSection( "read, delete", SectionStatus.DO_NOT_APPROVE_CONTENT, RemoveAccess.DENIED );
    }

    @Test
    public void removeContentsFromSection_checkRemoverPermissions_read_add_NOT_APPROVED()
    {
        checkUserPermissionForRemoveContentFromSection( "read, add", SectionStatus.DO_NOT_APPROVE_CONTENT, RemoveAccess.ALLOWED );
    }

    @Test
    public void removeContentsFromSection_checkRemoverPermissions_read_add_delete_NOT_APPROVED()
    {
        checkUserPermissionForRemoveContentFromSection( "read, add, delete", SectionStatus.DO_NOT_APPROVE_CONTENT, RemoveAccess.ALLOWED );
    }

    @Test
    public void removeContentsFromSection_checkRemoverPermissions_read_delete_publish_NOT_APPROVED()
    {
        checkUserPermissionForRemoveContentFromSection( "read, delete, publish", SectionStatus.DO_NOT_APPROVE_CONTENT,
                                                        RemoveAccess.DENIED );
    }

    @Test
    public void removeContentsFromSection_checkRemoverPermissions_read_add_publish_NOT_APPROVED()
    {
        checkUserPermissionForRemoveContentFromSection( "read, add, publish", SectionStatus.DO_NOT_APPROVE_CONTENT, RemoveAccess.ALLOWED );
    }

    @Test
    public void removeContentsFromSection_checkRemoverPermissions_read_add_publish_delete_NOT_APPROVED()
    {
        checkUserPermissionForRemoveContentFromSection( "read, add, publish, delete", SectionStatus.DO_NOT_APPROVE_CONTENT,
                                                        RemoveAccess.ALLOWED );
    }

    private void checkUserPermissionForRemoveContentFromSection( String permissions, SectionStatus sectionStatus, RemoveAccess expecting )
    {
        boolean approveContent = sectionStatus == SectionStatus.APPROVE_CONTENT;

        fixture.createAndStoreNormalUserWithUserGroup( "permission-test-user", "User", "testuserstore" );
        fixture.flushAndClearHibernateSession();

        fixture.save( factory.createCategoryAccessForUser( "Articles", "permission-test-user", "read" ) );
        fixture.save( createUnorderedSection( "Opinion" ) );
        fixture.save( createMenuItemAccess( "Opinion", "permission-test-user", permissions ) );
        fixture.save( createMenuItemAccess( "Opinion", "aru", "add publish" ) );

        final ContentKey contentKey = createContent( "c-1", "Articles" );

        fixture.flushAndClearHibernateSession();

        AddContentToSectionCommand addContentToSectionCommand = new AddContentToSectionCommand();
        addContentToSectionCommand.setContributor( fixture.findUserByName( "aru" ).getKey() );
        addContentToSectionCommand.setContent( contentKey );
        addContentToSectionCommand.setSection( fixture.findMenuItemByName( "Opinion" ).getKey() );
        addContentToSectionCommand.setApproveInSection( approveContent );
        if ( approveContent )
        {
            addContentToSectionCommand.setAddOnTop( true );
        }
        else
        {
            addContentToSectionCommand.setAddOnTop( false );
        }
        menuItemService.execute( addContentToSectionCommand );
        fixture.flushAndClearHibernateSession();

        //fixture.save( sectionContent );
        //fixture.flushAndClearHibernateSesssion();

        // Verify that a user with only list rights can not remove from the section.
        RemoveContentsFromSectionCommand removeContentFromSectionCommand = new RemoveContentsFromSectionCommand();
        removeContentFromSectionCommand.setRemover( fixture.findUserByName( "permission-test-user" ).getKey() );
        removeContentFromSectionCommand.setSection( fixture.findMenuItemByName( "Opinion" ).getKey() );
        removeContentFromSectionCommand.addContentToRemove( fixture.findContentByName( "c-1" ).getKey() );

        try
        {
            menuItemService.execute( removeContentFromSectionCommand );

            if ( expecting == RemoveAccess.DENIED )
            {
                fail( "The removeContentFromSection method should throw a MenuItemAccessException in this case." );
            }
        }
        catch ( MenuItemAccessException e )
        {
            if ( expecting == RemoveAccess.ALLOWED )
            {
                fail( "The removeContentFromSection method should remove item in this case." );
            }
        }
    }


    private MenuItemEntity createSection( String name, String siteName, boolean isOrdered )
    {
        return factory.createSectionMenuItem( name, ++menuItemOrderCount, null, name, siteName, "aru", "aru", "en", null, null, isOrdered,
                                              null, false, null );
    }

    private MenuItemAccessEntity createMenuItemAccess( String menuItemName, String userName, String accesses )
    {
        return factory.createMenuItemAccess( fixture.findMenuItemByName( menuItemName ), fixture.findUserByName( userName ).getUserGroup(),
                                             accesses );
    }

    private AddContentToSectionCommand createAddContentToSectionCommand( String contributor, ContentEntity content, MenuItemEntity section,
                                                                         boolean approve )
    {
        AddContentToSectionCommand command = new AddContentToSectionCommand();
        command.setContributor( fixture.findUserByName( contributor ).getKey() );
        command.setContent( content.getKey() );
        command.setSection( section.getKey() );
        command.setApproveInSection( approve );
        command.setAddOnTop( approve );
        return command;
    }

    private MenuItemEntity createUnorderedSection( String name )
    {
        return factory.createSectionMenuItem( name, 0, null, name, null, "admin", "admin", "en", null, null, false, null, false, null );
    }

    private MenuItemEntity createUnorderedSection( String name, String parentMenuItemName, String siteName )
    {
        return factory.createSectionMenuItem( name, 0, null, name, siteName, "admin", "admin", "en", parentMenuItemName, null, false, null,
                                              false, null );
    }

    private MenuItemEntity createOrderedSection( String name )
    {
        return factory.createSectionMenuItem( name, 0, null, name, null, "admin", "admin", "en", null, null, false, null, false, null );
    }

    private ContentKey createContent( String contentName, String categoryName )
    {
        final UserKey user = fixture.findUserByName( "aru" ).getKey();
        CreateContentCommand createCommand = createCreateContentCommand( contentName, categoryName, ContentStatus.APPROVED, user );
        return contentService.createContent( createCommand );
    }

    private CreateContentCommand createCreateContentCommand( String contentName, String categoryName, ContentStatus status,
                                                             UserKey creator )
    {
        CategoryEntity category = fixture.findCategoryByName( categoryName );

        ContentTypeEntity contentType = category.getContentType();
        CustomContentData contentData = new CustomContentData( contentType.getContentTypeConfig() );
        TextDataEntryConfig headingConfig = new TextDataEntryConfig( "heading", true, "Tittel", "contentdata/intro/heading" );
        contentData.add( new TextDataEntry( headingConfig, "test title" ) );

        CreateContentCommand createContentCommand = new CreateContentCommand();
        createContentCommand.setCreator( creator );
        createContentCommand.setLanguage( fixture.findLanguageByCode( "en" ) );
        createContentCommand.setCategory( category );
        createContentCommand.setPriority( 0 );
        createContentCommand.setStatus( status );
        createContentCommand.setContentData( contentData );
        createContentCommand.setContentName( contentName );

        return createContentCommand;
    }

    private PageTemplateEntity createPageTemplate( String name, PageTemplateType type, String siteName, String... contentTypeNames )
    {
        PageTemplateEntity pageTemplate = factory.createPageTemplate( name, type, siteName, ResourceKey.from( "DUMMYKEY" ) );
        Set<ContentTypeEntity> supportedContentTypes = new HashSet<ContentTypeEntity>();
        for ( String contentTypeName : contentTypeNames )
        {
            supportedContentTypes.add( fixture.findContentTypeByName( contentTypeName ) );
        }
        pageTemplate.setContentTypes( supportedContentTypes );
        return pageTemplate;
    }

    private enum RemoveAccess
    {
        ALLOWED,
        DENIED
    }

    private enum SectionStatus
    {
        DO_NOT_APPROVE_CONTENT,
        APPROVE_CONTENT
    }
}
