/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.vertical.engine;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.enonic.esl.containers.MultiValueMap;
import com.enonic.esl.xml.XMLTool;
import com.enonic.vertical.engine.criteria.CategoryCriteria;
import com.enonic.vertical.engine.filters.Filter;
import com.enonic.vertical.engine.handlers.BinaryDataHandler;
import com.enonic.vertical.engine.handlers.CategoryHandler;
import com.enonic.vertical.engine.handlers.CommonHandler;
import com.enonic.vertical.engine.handlers.ContentHandler;
import com.enonic.vertical.engine.handlers.ContentObjectHandler;
import com.enonic.vertical.engine.handlers.GroupHandler;
import com.enonic.vertical.engine.handlers.LanguageHandler;
import com.enonic.vertical.engine.handlers.LogHandler;
import com.enonic.vertical.engine.handlers.MenuHandler;
import com.enonic.vertical.engine.handlers.PageHandler;
import com.enonic.vertical.engine.handlers.PageTemplateHandler;
import com.enonic.vertical.engine.handlers.SectionHandler;
import com.enonic.vertical.engine.handlers.SecurityHandler;
import com.enonic.vertical.engine.handlers.SystemHandler;
import com.enonic.vertical.engine.handlers.UnitHandler;
import com.enonic.vertical.engine.handlers.UserHandler;

import com.enonic.cms.framework.xml.XMLDocument;
import com.enonic.cms.framework.xml.XMLDocumentFactory;

import com.enonic.cms.core.content.ContentPublishedResolver;
import com.enonic.cms.core.content.ContentService;
import com.enonic.cms.core.content.ContentXMLCreator;
import com.enonic.cms.core.content.IndexService;
import com.enonic.cms.core.content.RegenerateIndexBatcher;
import com.enonic.cms.core.content.access.ContentAccessResolver;
import com.enonic.cms.core.content.binary.BinaryData;
import com.enonic.cms.core.content.category.CategoryAccessResolver;
import com.enonic.cms.core.content.category.CategoryKey;
import com.enonic.cms.core.content.contenttype.ContentTypeEntity;
import com.enonic.cms.core.content.contenttype.ContentTypeKey;
import com.enonic.cms.core.content.query.ContentByCategoryQuery;
import com.enonic.cms.core.content.query.RelatedContentQuery;
import com.enonic.cms.core.content.resultset.ContentResultSet;
import com.enonic.cms.core.content.resultset.RelatedContentResultSet;
import com.enonic.cms.core.language.LanguageKey;
import com.enonic.cms.core.resource.ResourceKey;
import com.enonic.cms.core.security.SecurityService;
import com.enonic.cms.core.security.user.User;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.security.userstore.MemberOfResolver;
import com.enonic.cms.core.security.userstore.UserStoreKey;
import com.enonic.cms.core.structure.SiteKey;
import com.enonic.cms.core.structure.menuitem.MenuItemKey;
import com.enonic.cms.core.structure.page.template.PageTemplateKey;
import com.enonic.cms.core.structure.page.template.PageTemplateType;
import com.enonic.cms.store.dao.ContentTypeDao;
import com.enonic.cms.store.dao.GroupDao;
import com.enonic.cms.store.dao.SectionContentDao;

@Component
public final class AdminEngine
    extends BaseEngine
    implements InitializingBean
{
    private BinaryDataHandler binaryDataHandler;

    private CategoryHandler categoryHandler;

    private CommonHandler commonHandler;

    private ContentHandler contentHandler;

    private ContentService contentService;

    private ContentObjectHandler contentObjectHandler;

    private GroupHandler groupHandler;

    @Autowired
    private MemberOfResolver memberOfResolver;

    private IndexService indexService;

    private LanguageHandler languageHandler;


    private LogHandler logHandler;

    private MenuHandler menuHandler;

    private PageHandler pageHandler;

    private PageTemplateHandler pageTemplateHandler;

    private SectionHandler sectionHandler;

    private SecurityHandler securityHandler;

    private SecurityService securityService;

    private SystemHandler systemHandler;

    private UnitHandler unitHandler;

    private UserHandler userHandler;

    @Autowired
    private ContentTypeDao contentTypeDao;

    @Autowired
    private GroupDao groupDao;

    @Autowired
    private SectionContentDao sectionContentDao;

    public void afterPropertiesSet()
        throws Exception
    {
        // event listeners
        menuHandler.addListener( logHandler );
    }

    public CategoryHandler getCategoryHandler()
    {
        return categoryHandler;
    }

    public CommonHandler getCommonHandler()
    {
        return commonHandler;
    }

    public ContentHandler getContentHandler()
    {
        return contentHandler;
    }

    public ContentObjectHandler getContentObjectHandler()
    {
        return contentObjectHandler;
    }

    public GroupHandler getGroupHandler()
    {
        return groupHandler;
    }

    public LanguageHandler getLanguageHandler()
    {
        return languageHandler;
    }

    public MenuHandler getMenuHandler()
    {
        return menuHandler;
    }

    public PageHandler getPageHandler()
    {
        return pageHandler;
    }

    public PageTemplateHandler getPageTemplateHandler()
    {
        return pageTemplateHandler;
    }

    public SectionHandler getSectionHandler()
    {
        return sectionHandler;
    }

    public SecurityHandler getSecurityHandler()
    {
        return securityHandler;
    }

    public UserHandler getUserHandler()
    {
        return userHandler;
    }

    public XMLDocument getPageTemplates( PageTemplateType type )
    {
        Document doc = pageTemplateHandler.getPageTemplates( type );
        return XMLDocumentFactory.create( doc );
    }

    public void copyMenu( User user, int menuKey, boolean includeContent )
        throws VerticalSecurityException
    {

        if ( !user.isEnterpriseAdmin() )
        {
            String enterpriseGroupKey = groupHandler.getEnterpriseAdministratorGroupKey();
            String[] groupKeys = groupHandler.getAllGroupMembershipsForUser( user );
            Arrays.sort( groupKeys );
            if ( Arrays.binarySearch( groupKeys, enterpriseGroupKey ) < 0 )
            {
                String message = "User does not have rights to copy menu.";
                VerticalEngineLogger.errorSecurity( message, null );
            }
        }

        menuHandler.copyMenu( user, menuKey, includeContent );
    }


    public boolean contentExists( int categoryKey, String contentTitle )
    {
        return contentHandler.contentExists( CategoryKey.parse( categoryKey ), contentTitle );
    }

    public int getContentKey( int categoryKey, String contentTitle )
    {
        return contentHandler.getContentKey( CategoryKey.parse( categoryKey ), contentTitle );
    }

    public String getContentCreatedTimestamp( int contentKey )
    {
        return contentHandler.getCreatedTimestamp( contentKey );
    }

    public Date getContentPublishFromTimestamp( int contentKey )
    {
        return contentHandler.getPublishFromTimestamp( contentKey );
    }

    public Date getContentPublishToTimestamp( int contentKey )
    {
        return contentHandler.getPublishToTimestamp( contentKey );
    }

    public int getCategoryKey( int superCategoryKey, String name )
    {
        return categoryHandler.getCategoryKey( superCategoryKey, name );
    }

    public int createContentObject( String xmlData )
    {
        return contentObjectHandler.createContentObject( xmlData );
    }

    public int createContentType( User user, String xmlData )
    {

        Document doc = XMLTool.domparse( xmlData, "contenttype" );

        if ( !( securityHandler.isSiteAdmin( user ) || isDeveloper( user ) ) )
        {
            String message = "User is not administrator or developer";
            VerticalEngineLogger.errorSecurity( message, null );
        }

        return contentHandler.createContentType( doc );
    }

    public void createLanguage( User user, String languageCode, String description )
    {

        if ( !isEnterpriseAdmin( user ) )
        {
            String message = "User is not enterprise administrator";
            VerticalEngineLogger.errorSecurity( message, null );
        }

        languageHandler.createLanguage( languageCode, description );
    }

    public int createMenu( User user, String xmlData )
    {
        return menuHandler.createMenu( user, xmlData );
    }

    public void updateMenuItem( User user, String xmlData )
    {
        menuHandler.updateMenuItem( user, xmlData );
    }

    public void removeMenuItem( User user, int mikey )
    {
        menuHandler.removeMenuItem( user, mikey );
    }

    public int createMenuItem( User user, String xmlData )
    {
        return menuHandler.createMenuItem( user, xmlData );
    }

    public String generateUID( String fName, String sName, UserStoreKey userStoreKey )
    {
        return userHandler.generateUID( fName, sName, userStoreKey );
    }

    public BinaryData getBinaryData( int binaryDataKey )
    {
        return binaryDataHandler.getBinaryData( binaryDataKey );
    }

    public XMLDocument getContent( User user, int contentKey, int parentLevel, int childrenLevel, int parentChildrenLevel )
    {
        Document doc = contentHandler.getContent( user, contentKey, false, parentLevel, childrenLevel, parentChildrenLevel );
        securityHandler.appendAccessRights( user, doc );
        return XMLDocumentFactory.create( doc );
    }

    public String getCategoryName( int categoryKey )
    {

        return categoryHandler.getCategoryName( CategoryKey.parse( categoryKey ) );
    }

    public XMLDocument getCategoryNameXML( int categoryKey )
    {

        return XMLDocumentFactory.create( categoryHandler.getCategoryNameDoc( CategoryKey.parse( categoryKey ) ) );
    }

    public XMLDocument getCategory( User user, int categoryKey )
    {
        Document doc = categoryHandler.getCategory( user, CategoryKey.parse( categoryKey ) );
        boolean hasSubs = categoryHandler.hasSubCategories( CategoryKey.parse( categoryKey ) );
        Element categoryElem = XMLTool.getElement( doc.getDocumentElement(), "category" );
        if ( categoryElem != null )
        {
            categoryElem.setAttribute( "subcategories", String.valueOf( hasSubs ) );
        }
        securityHandler.appendAccessRights( user, doc );

        return XMLDocumentFactory.create( doc );
    }

    public MenuItemAccessRight getMenuItemAccessRight( User user, MenuItemKey key )
    {
        return securityHandler.getMenuItemAccessRight( user, key );
    }

    public MenuAccessRight getMenuAccessRight( User user, int key )
    {
        return securityHandler.getMenuAccessRight( user, key );
    }

    public CategoryAccessRight getCategoryAccessRight( User user, int key )
    {
        return securityHandler.getCategoryAccessRight( user, CategoryKey.parse( key ) );
    }

    public ContentAccessRight getContentAccessRight( User user, int key )
    {
        return securityHandler.getContentAccessRight( user, key );
    }

    public XMLDocument getMenuItem( User user, int key, boolean withParents )
    {
        Document doc = menuHandler.getMenuItem( user, key, withParents );
        securityHandler.appendAccessRights( user, doc );
        return XMLDocumentFactory.create( doc );
    }

    public XMLDocument getMenuItem( User user, int key, boolean withParents, boolean complete )
    {
        Document doc = menuHandler.getMenuItem( user, key, withParents, complete, true );
        securityHandler.appendAccessRights( user, doc );
        return XMLDocumentFactory.create( doc );
    }

    public int getCategoryKey( int contentKey )
    {
        CategoryKey categoryKey = contentHandler.getCategoryKey( contentKey );
        if ( categoryKey == null )
        {
            return -1;
        }
        return categoryKey.toInt();
    }

    public int getSuperCategoryKey( int categoryKey )
    {
        CategoryKey parentCategoryKey = categoryHandler.getParentCategoryKey( CategoryKey.parse( categoryKey ) );
        if ( parentCategoryKey == null )
        {
            return -1;
        }
        return parentCategoryKey.toInt();
    }

    public XMLDocument getSuperCategoryNames( int categoryKey, boolean withContentCount, boolean includeCategory )
    {
        return categoryHandler.getSuperCategoryNames( CategoryKey.parse( categoryKey ), withContentCount, includeCategory );
    }

    public int getContentCount( int categoryKey, boolean recursive )
    {

        return categoryHandler.getContentCount( CategoryKey.parse( categoryKey ), recursive );
    }

    public XMLDocument getContentObject( int contentObjectKey )
    {
        return contentObjectHandler.getContentObject( contentObjectKey );
    }

    public XMLDocument getContentObjectsByMenu( int menuKey )
    {
        return XMLDocumentFactory.create( contentObjectHandler.getContentObjectsByMenu( menuKey ) );
    }

    public String getContentTitle( int versionKey )
    {
        return contentHandler.getContentTitle( versionKey );
    }

    public XMLDocument getContentType( int contentTypeKey )
    {
        Document doc = contentHandler.getContentType( contentTypeKey, false );
        return XMLDocumentFactory.create( doc );
    }

    public XMLDocument getContentType( int contentTypeKey, boolean includeContentCount )
    {
        Document doc = contentHandler.getContentType( contentTypeKey, includeContentCount );
        return XMLDocumentFactory.create( doc );
    }

    public int getContentTypeKey( int contentKey )
    {
        return contentHandler.getContentTypeKey( contentKey );
    }

    public int[] getContentTypeKeysByHandler( String handlerClass )
    {
        return contentHandler.getContentTypeKeysByHandler( handlerClass );
    }

    public int getContentTypeKeyByCategory( int categoryKey )
    {
        return categoryHandler.getContentTypeKey( CategoryKey.parse( categoryKey ) );
    }

    public String getContentTypeName( int contentTypeKey )
    {
        return contentHandler.getContentTypeName( contentTypeKey );
    }

    public XMLDocument getContentTypeModuleData( int contentTypeKey )
    {
        return XMLDocumentFactory.create( contentHandler.getContentTypeModuleData( contentTypeKey ) );
    }

    public XMLDocument getLanguage( LanguageKey languageKey )
    {
        return languageHandler.getLanguage( languageKey );
    }

    public XMLDocument getLanguages()
    {
        return languageHandler.getLanguages();
    }

    public XMLDocument getMenu( User user, int menuKey, boolean complete )
    {
        Document doc = menuHandler.getMenu( user, menuKey, complete );
        securityHandler.appendAccessRights( user, doc );
        return XMLDocumentFactory.create( doc );
    }

    public String getMenuItemName( int menuItemKey )
    {
        return menuHandler.getMenuItemName( menuItemKey );
    }

    public String getPageTemplate( PageTemplateKey pageTemplateKey )
    {
        return pageTemplateHandler.getPageTemplate( pageTemplateKey ).getAsString();
    }

    public XMLDocument getPageTemplatesByMenu( int menuKey, int[] excludeTypeKeys )
    {
        return doGetPageTemplatesByMenu( menuKey, excludeTypeKeys );
    }

    private XMLDocument doGetPageTemplatesByMenu( int menuKey, int[] excludeTypeKeys )
    {
        Document doc = pageTemplateHandler.getPageTemplatesByMenu( menuKey, excludeTypeKeys );
        return XMLDocumentFactory.create( doc );
    }

    public XMLDocument getPageTemplatesByContentObject( int contentObjectKey )
    {
        Document doc = pageTemplateHandler.getPageTemplatesByContentObject( contentObjectKey );
        return XMLDocumentFactory.create( doc );
    }

    public String getPageTemplParams( int pageTemplateKey )
    {
        return pageTemplateHandler.getPageTemplParams( pageTemplateKey );
    }

    public int getUnitLanguageKey( int unitKey )
    {
        return unitHandler.getUnitLanguageKey( unitKey );
    }

    public XMLDocument getUnit( int unitKey )
    {
        return unitHandler.getUnit( unitKey );
    }

    public String getUnitName( int unitKey )
    {
        return unitHandler.getUnitName( unitKey );
    }

    public int getUnitKey( int categoryKey )
    {
        return categoryHandler.getUnitKey( CategoryKey.parse( categoryKey ) );
    }

    public XMLDocument getUnitNamesXML( Filter filter )
    {
        return XMLDocumentFactory.create( unitHandler.getUnitNamesXML( filter ) );
    }

    public XMLDocument getUnits()
    {
        return unitHandler.getUnits();
    }

    public boolean hasContent( int categoryKey )
    {
        return categoryHandler.hasContent( CategoryKey.parse( categoryKey ) );
    }

    public boolean hasSubCategories( int categoryKey )
    {
        return categoryHandler.hasSubCategories( CategoryKey.parse( categoryKey ) );
    }

    public void regenerateIndexForContentType( int contentTypeKey )
    {
        ContentTypeEntity contentType = contentTypeDao.findByKey( new ContentTypeKey( contentTypeKey ) );
        final int batchSize = 10;
        RegenerateIndexBatcher batcher = new RegenerateIndexBatcher( indexService, contentService );

        if ( !indexService.indexExists() )
        {
            indexService.createIndex();
        }

        batcher.regenerateIndex( contentType, batchSize, null );
    }

    public void regenerateIndexForContentHandler( int contentHandlerKey )
    {
        ContentHandler handler = getContentHandler();
        int[] contentTypes = handler.getContentTypeKeysByHandler( contentHandlerKey );

        for ( int contentType : contentTypes )
        {
            regenerateIndexForContentType( contentType );
        }
    }

    public void removeContentObject( int contentObjectKey )
        throws VerticalSecurityException, VerticalRemoveException
    {
        contentObjectHandler.removeContentObject( contentObjectKey );
    }

    public void removeContentType( User user, int contentTypeKey )
        throws VerticalSecurityException, VerticalRemoveException
    {

        if ( !( securityHandler.isSiteAdmin( user ) || isDeveloper( user ) ) )
        {
            String message = "User is not administrator or developer";
            VerticalEngineLogger.errorSecurity( message, null );
        }

        contentHandler.removeContentType( contentTypeKey );
    }

    public void removeLanguage( LanguageKey languageKey )
        throws VerticalSecurityException, VerticalRemoveException
    {
        languageHandler.removeLanguage( languageKey );
    }

    public void removeMenu( User user, int menuKey )
        throws VerticalRemoveException, VerticalSecurityException
    {
        menuHandler.removeMenu( user, menuKey );
    }

    public void updateContentObject( String xmlData )
    {
        contentObjectHandler.updateContentObject( xmlData );
    }

    public void updateContentType( User user, String xmlData )
    {
        Document doc = XMLTool.domparse( xmlData, "contenttype" );

        if ( !( securityHandler.isSiteAdmin( user ) || isDeveloper( user ) ) )
        {
            String message = "User is not administrator or developer";
            VerticalEngineLogger.errorSecurity( message, null );
        }

        contentHandler.updateContentType( doc );
    }

    public void updateLanguage( LanguageKey languageKey, String languageCode, String description )
    {
        languageHandler.updateLanguage( languageKey, languageCode, description );
    }

    public void updateMenuData( User user, String xmlData )
    {
        Document doc = XMLTool.domparse( xmlData, "menu" );
        if ( !isAdmin( user ) )
        {
            String message = "User does not have rights to update menu data.";
            VerticalEngineLogger.errorSecurity( message, null );
        }

        menuHandler.updateMenuData( doc );
    }

    public XMLDocument getMenuItemsByContentObject( User user, int cobKey )
    {
        Document doc = menuHandler.getMenuItemsByContentObject( user, cobKey );
        return XMLDocumentFactory.create( doc );
    }

    public XMLDocument getMenuItemsByPageTemplates( User user, int[] pageTemplateKeys )
    {
        Document doc = menuHandler.getMenuItemsByPageTemplates( user, pageTemplateKeys );
        return XMLDocumentFactory.create( doc );
    }

    public XMLDocument getAccessRights( User user, int type, int key, boolean includeUserright )
    {
        Document doc = securityHandler.getAccessRights( user, type, key, includeUserright );

        return XMLDocumentFactory.create( doc );
    }

    public XMLDocument getDefaultAccessRights( User user, int type, int key )
    {
        Document doc = securityHandler.getDefaultAccessRights( user, type, key );

        return XMLDocumentFactory.create( doc );
    }

    public XMLDocument getGroup( String gKey )
    {
        return XMLDocumentFactory.create( groupHandler.getGroup( gKey ) );
    }

    public void updateAccessRights( User user, String xmlData )
    {
        Document doc = XMLTool.domparse( xmlData, "accessrights" );
        securityHandler.updateAccessRights( user, doc );
    }

    public XMLDocument getContent( User oldTypeUser, CategoryKey categoryKey, boolean includeSubCategories, String orderBy, int index,
                                   int count, int childrenLevel, int parentLevel, int parentChildrenLevel )
    {
        UserEntity user = securityService.getUser( oldTypeUser.getKey() );
        List<CategoryKey> categories = CategoryKey.convertToList( categoryKey );

        ContentByCategoryQuery contentByCategoryQuery = new ContentByCategoryQuery();
        contentByCategoryQuery.setUser( user );
        contentByCategoryQuery.setCategoryKeyFilter( categories, includeSubCategories ? Integer.MAX_VALUE : 1 );
        contentByCategoryQuery.setOrderBy( orderBy );
        contentByCategoryQuery.setIndex( index );
        contentByCategoryQuery.setCount( count );
        contentByCategoryQuery.setFilterIncludeOfflineContent();
        contentByCategoryQuery.setFilterAdminBrowseOnly( false );

        ContentResultSet contents = contentService.queryContent( contentByCategoryQuery );

        RelatedContentQuery relatedContentQuery = new RelatedContentQuery( new Date() );
        relatedContentQuery.setUser( user );
        relatedContentQuery.setContentResultSet( contents );
        relatedContentQuery.setParentLevel( parentLevel );
        relatedContentQuery.setChildrenLevel( childrenLevel );
        relatedContentQuery.setParentChildrenLevel( parentChildrenLevel );
        relatedContentQuery.setIncludeOnlyMainVersions( true );

        RelatedContentResultSet relatedContents = contentService.queryRelatedContent( relatedContentQuery );

        ContentXMLCreator xmlCreator = new ContentXMLCreator();
        xmlCreator.setResultIndexing( index, count );
        xmlCreator.setIncludeOwnerAndModifierData( true );
        xmlCreator.setIncludeContentData( true );
        xmlCreator.setIncludeCategoryData( true );
        xmlCreator.setIncludeRelatedContentData( true );
        xmlCreator.setIncludeUserRightsInfo( true, new CategoryAccessResolver( groupDao ), new ContentAccessResolver( groupDao ) );
        xmlCreator.setIncludeVersionsInfoForAdmin( true );
        xmlCreator.setIncludeAssignment( true );
        xmlCreator.setIncludeDraftInfo( true );
        xmlCreator.setIncludeRepositoryPathInfo( false );
        xmlCreator.setPublishedResolver( new ContentPublishedResolver( sectionContentDao ) );
        return xmlCreator.createContentsDocument( user, contents, relatedContents );
    }

    public boolean isEnterpriseAdmin( User user )
    {
        return memberOfResolver.hasEnterpriseAdminPowers( user.getKey() );
    }

    public boolean isSiteAdmin( User user, SiteKey siteKey )
    {
        return getSecurityHandler().getMenuAccessRight( user, siteKey.toInt() ).getAdministrate();
    }

    public boolean isAdmin( User user )
    {
        return memberOfResolver.hasAdministratorPowers( user.getKey() );
    }

    public boolean isUserStoreAdmin( User user, UserStoreKey userStoreKey )
    {
        return memberOfResolver.hasUserStoreAdministratorPowers( user.getKey(), userStoreKey );
    }

    public boolean isDeveloper( User user )
    {
        return memberOfResolver.hasDeveloperPowers( user.getKey() );
    }

    public XMLDocument getMenu( User user, CategoryCriteria criteria )
    {
        Document doc = XMLTool.createDocument( "data" );
        Element root = doc.getDocumentElement();
        categoryHandler.getMenu( user, root, criteria );
        return XMLDocumentFactory.create( doc );
    }

    public XMLDocument getPath( User user, int type, int key )
    {
        Document doc = XMLTool.createDocument( "data" );

        if ( type == Types.CATEGORY )
        {
            // Get unit
            int unitKey = categoryHandler.getUnitKey( CategoryKey.parse( key ) );
            Document unitDoc = commonHandler.getSingleData( Types.UNIT, unitKey );
            Element unitElem = (Element) unitDoc.getDocumentElement().getFirstChild();

            // Get categories
            CategoryCriteria criteria = new CategoryCriteria();
            criteria.setCategoryKey( key );
            criteria.setUseDisableAttribute( false );
            categoryHandler.getMenu( user, unitElem, criteria );

            doc.getDocumentElement().appendChild( doc.importNode( unitElem, true ) );
        }

        return XMLDocumentFactory.create( doc );
    }

    public String getPathString( int type, int key )
    {
        if ( type == Types.MENUITEM )
        {
            return menuHandler.getPathString( key ).toString();
        }
        return null;
    }

    public XMLDocument getContentTitleXML( int versionKey )
    {
        Document doc = contentHandler.getContentTitleDoc( versionKey );
        return XMLDocumentFactory.create( doc );
    }

    public XMLDocument getMenusForAdmin( User user )
    {
        Document doc = menuHandler.getMenusForAdmin( user );
        return XMLDocumentFactory.create( doc );
    }

    public void moveMenuItem( User user, Element[] menuItemElems, int menuItemKey, int fromMenuKey, int fromParentKey, int toMenuKey,
                              int toParentKey )
    {
        menuHandler.moveMenuItem( user, menuItemElems, menuItemKey, fromMenuKey, fromParentKey, toMenuKey, toParentKey );
    }

    public void shiftMenuItems( User user, Element[] menuItemElems, int menuKey, int parentMenuItemKey )
    {
        menuHandler.shiftMenuItems( user, menuItemElems, menuKey, parentMenuItemKey );
    }

    public Set<UserEntity> getUserNames( String[] groupKeys )
    {
        return groupHandler.getUserNames( groupKeys );
    }

    public XMLDocument getContentHandler( int contentHandlerKey )
    {
        return XMLDocumentFactory.create( contentHandler.getContentHandler( contentHandlerKey ) );
    }

    public String getContentHandlerClassForContentType( int contentTypeKey )
    {
        return contentHandler.getContentHandlerClassForContentType( contentTypeKey );
    }

    public XMLDocument getContentHandlers()
    {
        return XMLDocumentFactory.create( contentHandler.getContentHandlers() );
    }

    public int createContentHandler( User user, String xmlData )
    {

        if ( !securityHandler.isEnterpriseAdmin( user ) )
        {
            String message = "User does not have access rights to create content handlers.";
            VerticalEngineLogger.errorSecurity( message, null );
        }

        Document doc = XMLTool.domparse( xmlData );
        return contentHandler.createContentHandler( doc );
    }

    public void updateContentHandler( User user, String xmlData )
    {

        if ( !securityHandler.isEnterpriseAdmin( user ) )
        {
            String message = "User does not have access rights to update content handlers.";
            VerticalEngineLogger.errorSecurity( message, null );
        }

        Document doc = XMLTool.domparse( xmlData );
        contentHandler.updateContentHandler( doc );
    }

    public void removeContentHandler( User user, int contentHandlerKey )
    {

        if ( !isEnterpriseAdmin( user ) )
        {
            String message = "User does not have access rights to delete content handlers.";
            VerticalEngineLogger.errorSecurity( message, null );
        }

        contentHandler.removeContentHandler( contentHandlerKey );
    }

    public XMLDocument getContentTypes()
    {
        Document doc = contentHandler.getContentTypes( null, false );
        return XMLDocumentFactory.create( doc );
    }

    public XMLDocument getContentTypes( boolean includeContentCount )
    {
        Document doc = contentHandler.getContentTypes( includeContentCount );
        return XMLDocumentFactory.create( doc );
    }

    public XMLDocument getContentTypes( int[] contentTypeKeys, boolean includeContentCount )
    {
        Document doc = contentHandler.getContentTypes( contentTypeKeys, includeContentCount );
        return XMLDocumentFactory.create( doc );
    }

    public String getIndexingParametersXML( ContentTypeKey contentTypeKey )
    {
        XMLOutputter printer = new XMLOutputter( Format.getPrettyFormat() );
        org.jdom.Element element = new org.jdom.Element( "indexparameters" );

        if ( contentTypeKey == null )
        {
            return printer.outputString( element );
        }

        ContentTypeEntity contentType = contentTypeDao.findByKey( contentTypeKey );
        if ( contentType != null )
        {
            element = contentType.getIndexingParametersXML();
        }
        return printer.outputString( element );
    }

    public long getSectionContentTimestamp( MenuItemKey sectionKey )
    {
        return sectionHandler.getSectionContentTimestamp( sectionKey.toInt() );
    }

    public XMLDocument getSections( User user, SectionCriteria criteria )
    {
        Document doc = sectionHandler.getSections( user, criteria );
        return XMLDocumentFactory.create( doc );
    }

    public void removeSection( int sectionKey, boolean recursive )
        throws VerticalRemoveException, VerticalSecurityException
    {
        sectionHandler.removeSection( sectionKey, recursive );
    }

    public void copySection( int sectionKey )
        throws VerticalSecurityException
    {
        sectionHandler.copySection( sectionKey );
    }

    public boolean isSectionOrdered( int sectionKey )
    {
        return sectionHandler.isSectionOrdered( sectionKey );
    }

    public int getMenuKeyBySection( MenuItemKey sectionKey )
    {
        return sectionHandler.getMenuKeyBySection( sectionKey.toInt() );
    }

    public MenuItemKey getMenuItemKeyBySection( MenuItemKey sectionKey )
    {
        return sectionHandler.getMenuItemKeyBySection( sectionKey.toInt() );
    }

    public int getMenuKeyByMenuItem( MenuItemKey menuItemKey )
    {
        return menuHandler.getMenuKeyByMenuItem( menuItemKey.toInt() );
    }

    public int getParentMenuItemKey( int menuItemKey )
    {
        return menuHandler.getParentMenuItemKey( menuItemKey );
    }

    public XMLDocument getContentTitlesBySection( MenuItemKey sectionKey, String orderBy, int fromIndex, int count,
                                                  boolean includeTotalCount, boolean approveOnly )
    {
        return sectionHandler.getContentTitlesBySection( sectionKey.toInt(), orderBy, fromIndex, count, includeTotalCount, approveOnly );
    }

    public XMLDocument getContentTitles( int[] contentKeys )
    {
        return contentHandler.getContentTitles( contentKeys, null, false, null );
    }

    public XMLDocument getUsersWithPublishRight( int categoryKey )
    {
        Document doc = securityHandler.getUsersWithPublishRight( CategoryKey.parse( categoryKey ) );
        return XMLDocumentFactory.create( doc );
    }

    public XMLDocument getContentOwner( int contentKey )
    {
        Document doc = contentHandler.getContentOwner( contentKey );
        return XMLDocumentFactory.create( doc );
    }

    public XMLDocument getLogEntries( MultiValueMap adminParams, int fromIdx, int count, boolean complete )
    {
        Document doc = logHandler.getLogEntries( adminParams, fromIdx, count, complete );
        return XMLDocumentFactory.create( doc );
    }

    public XMLDocument getLogEntry( String key )
    {
        Document doc = logHandler.getLogEntry( key );
        return XMLDocumentFactory.create( doc );
    }

    public int getContentCountByContentType( int contentTypeKey )
    {
        return contentHandler.getContentCountByContentType( contentTypeKey );
    }

    public XMLDocument getCategoryPathXML( CategoryKey categoryKey, int[] contentTypes )
    {
        Document doc = XMLTool.createDocument( "path" );
        categoryHandler.getPathXML( doc, null, categoryKey, contentTypes );
        return XMLDocumentFactory.create( doc );
    }

    public ResourceKey getContentTypeCSSKey( int contentTypeKey )
    {
        return contentHandler.getContentTypeCSSKey( contentTypeKey );
    }

    public XMLDocument getData( int type, int[] keys )
    {
        Document doc = commonHandler.getData( type, keys );
        return XMLDocumentFactory.create( doc );
    }

    public ResourceKey getDefaultCSSByMenu( int menuKey )
    {
        return menuHandler.getDefaultCSSByMenu( menuKey );
    }

    public int getCurrentVersionKey( int contentKey )
    {
        return contentHandler.getCurrentVersionKey( contentKey );
    }

    public int getContentKeyByVersionKey( int versionKey )
    {
        return contentHandler.getContentKeyByVersionKey( versionKey );
    }

    public int[] getBinaryDataKeysByVersion( int versionKey )
    {
        return binaryDataHandler.getBinaryDataKeysByVersion( versionKey );
    }

    public XMLDocument getContentVersion( User user, int versionKey )
    {
        Document doc = contentHandler.getContentVersion( user, versionKey );
        return XMLDocumentFactory.create( doc );
    }

    public XMLDocument getContentXMLField( int versionKey )
    {
        Document doc = contentHandler.getContentXMLField( versionKey );
        return XMLDocumentFactory.create( doc );
    }

    public int[] getContentTypesByHandlerClass( String className )
    {
        return contentHandler.getContentTypesByHandlerClass( className );
    }

    public int getBinaryDataKey( int contentKey, String label )
    {
        return binaryDataHandler.getBinaryDataKey( contentKey, label );
    }

    public XMLDocument getCategoryMenu( User user, int categoryKey, int[] contentTypes, boolean includeRootCategories )
    {
        Document doc = categoryHandler.getCategoryMenu( user, CategoryKey.parse( categoryKey ), contentTypes, includeRootCategories );
        return XMLDocumentFactory.create( doc );
    }

    public int getContentVersionState( int versionKey )
    {
        return contentHandler.getState( versionKey );
    }

    public MenuItemKey getSectionKeyByMenuItemKey( MenuItemKey menuItemKey )
    {
        return sectionHandler.getSectionKeyByMenuItem( menuItemKey );
    }

    public boolean initializeDatabaseSchema()
        throws Exception
    {
        return this.systemHandler.initializeDatabaseSchema();
    }

    public boolean initializeDatabaseValues()
        throws Exception
    {
        return this.systemHandler.initializeDatabaseValues();
    }

    public boolean isContentVersionApproved( int versionKey )
    {
        return contentHandler.isContentVersionApproved( versionKey );
    }

    public XMLDocument getContentHomes( int contentKey )
    {
        Document doc = contentHandler.getContentHomes( contentKey );
        return XMLDocumentFactory.create( doc );
    }

    public boolean hasContentPageTemplates( int menuKey, int contentTypeKey )
    {
        return pageTemplateHandler.hasContentPageTemplates( menuKey, contentTypeKey );
    }

    public int getContentStatus( int versionKey )
    {
        return contentHandler.getContentStatus( versionKey );
    }

    public Document getAdminMenu( User user, int[] menuKeys, String[] menuItemTypes, boolean includeReadOnlyAccessRight )
    {
        return doGetAdminMenu( user, menuKeys, menuItemTypes, includeReadOnlyAccessRight );
    }

    private Document doGetAdminMenu( User user, int[] menuKeys, String[] menuItemTypes, boolean includeReadOnlyAccessRight )
    {

        Document menuDoc = menuHandler.getAdminMenu( user, menuKeys, menuItemTypes, includeReadOnlyAccessRight );

        // Sort menuitems based on order
        Element[] siteElems = XMLTool.getElements( menuDoc.getDocumentElement() );
        for ( Element siteElem : siteElems )
        {
            XMLTool.sortChildElements( siteElem, "order", false, true );
        }

        return menuDoc;
    }

    public void updateMenuDetails( int menuKey, int frontPageKey, int loginPageKey, int errorPageKey, int defaultPageTemplateKey )
    {
        menuHandler.updateMenuDetails( menuKey, frontPageKey, loginPageKey, errorPageKey, defaultPageTemplateKey );
    }

    public int getContentTypeKeyByName( String name )
    {
        return this.contentHandler.getContentTypeKeyByName( name );
    }

    public long getArchiveSizeByCategory( int categoryKey )
    {
        return this.categoryHandler.getArchiveSizeByCategory( CategoryKey.parse( categoryKey ) );
    }

    public long getArchiveSizeByUnit( int unitKey )
    {
        return this.categoryHandler.getArchiveSizeByUnit( unitKey );
    }

    @Autowired
    public void setBinaryDataHandler( BinaryDataHandler binaryDataHandler )
    {
        this.binaryDataHandler = binaryDataHandler;
    }

    @Autowired
    public void setCategoryHandler( CategoryHandler categoryHandler )
    {
        this.categoryHandler = categoryHandler;
    }

    @Autowired
    public void setCommonHandler( CommonHandler commonHandler )
    {
        this.commonHandler = commonHandler;
    }

    @Autowired
    public void setContentHandler( ContentHandler contentHandler )
    {
        this.contentHandler = contentHandler;
    }

    @Autowired
    public void setContentService( ContentService service )
    {
        contentService = service;
    }

    @Autowired
    public void setContentObjectHandler( ContentObjectHandler contentObjectHandler )
    {
        this.contentObjectHandler = contentObjectHandler;
    }

    @Autowired
    public void setGroupHandler( GroupHandler groupHandler )
    {
        this.groupHandler = groupHandler;
    }

    @Autowired
    public void setLanguageHandler( LanguageHandler languageHandler )
    {
        this.languageHandler = languageHandler;
    }

    @Autowired
    public void setLogHandler( LogHandler logHandler )
    {
        this.logHandler = logHandler;
    }

    @Autowired
    public void setMenuHandler( MenuHandler menuHandler )
    {
        this.menuHandler = menuHandler;
    }

    @Autowired
    @Qualifier("enginePageHandler")
    public void setPageHandler( PageHandler pageHandler )
    {
        this.pageHandler = pageHandler;
    }

    @Autowired
    public void setPageTemplateHandler( PageTemplateHandler pageTemplateHandler )
    {
        this.pageTemplateHandler = pageTemplateHandler;
    }

    @Autowired
    public void setSectionHandler( SectionHandler sectionHandler )
    {
        this.sectionHandler = sectionHandler;
    }

    @Autowired
    public void setUserHandler( UserHandler userHandler )
    {
        this.userHandler = userHandler;
    }

    @Autowired
    public void setUnitHandler( UnitHandler unitHandler )
    {
        this.unitHandler = unitHandler;
    }

    @Autowired
    public void setSystemHandler( SystemHandler systemHandler )
    {
        this.systemHandler = systemHandler;
    }

    @Autowired
    public void setSecurityHandler( SecurityHandler securityHandler )
    {
        this.securityHandler = securityHandler;
    }

    @Autowired
    public void setSecurityService( SecurityService service )
    {
        securityService = service;
    }

    @Autowired
    public void setIndexService( IndexService service )
    {
        indexService = service;
    }
}
