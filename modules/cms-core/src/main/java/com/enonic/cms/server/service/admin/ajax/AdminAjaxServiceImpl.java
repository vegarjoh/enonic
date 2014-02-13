/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.server.service.admin.ajax;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpSession;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;

import org.apache.commons.lang.StringUtils;
import org.directwebremoting.annotations.RemoteMethod;
import org.directwebremoting.annotations.RemoteProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.enonic.esl.xml.XMLTool;
import com.enonic.vertical.adminweb.AdminStore;
import com.enonic.vertical.adminweb.VerticalAdminLogger;

import com.enonic.cms.framework.xml.XMLDocument;

import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.ContentService;
import com.enonic.cms.core.content.ContentVersionEntity;
import com.enonic.cms.core.content.ContentVersionKey;
import com.enonic.cms.core.content.ContentXMLCreator;
import com.enonic.cms.core.content.access.ContentAccessResolver;
import com.enonic.cms.core.content.query.ContentByContentQuery;
import com.enonic.cms.core.content.query.RelatedContentQuery;
import com.enonic.cms.core.content.resultset.ContentResultSet;
import com.enonic.cms.core.content.resultset.RelatedContentResultSet;
import com.enonic.cms.core.country.Country;
import com.enonic.cms.core.country.CountryCode;
import com.enonic.cms.core.country.CountryService;
import com.enonic.cms.core.country.Region;
import com.enonic.cms.core.preference.PreferenceEntity;
import com.enonic.cms.core.preference.PreferenceSpecification;
import com.enonic.cms.core.security.SecurityService;
import com.enonic.cms.core.security.group.GroupKey;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.security.user.UserKey;
import com.enonic.cms.core.security.userstore.MemberOfResolver;
import com.enonic.cms.core.security.userstore.UserStoreKey;
import com.enonic.cms.core.security.userstore.connector.synchronize.SynchronizeUserStoreJobFactory;
import com.enonic.cms.core.service.AdminService;
import com.enonic.cms.core.servlet.ServletRequestAccessor;
import com.enonic.cms.core.structure.SiteKey;
import com.enonic.cms.core.structure.menuitem.MenuItemEntity;
import com.enonic.cms.core.structure.menuitem.MenuItemKey;
import com.enonic.cms.core.structure.menuitem.MenuItemSpecification;
import com.enonic.cms.core.xslt.XsltProcessor;
import com.enonic.cms.core.xslt.XsltProcessorException;
import com.enonic.cms.core.xslt.XsltResource;
import com.enonic.cms.core.xslt.admin.AdminXsltProcessor;
import com.enonic.cms.core.xslt.admin.AdminXsltProcessorFactory;
import com.enonic.cms.server.service.admin.ajax.dto.PreferenceDto;
import com.enonic.cms.server.service.admin.ajax.dto.RegionDto;
import com.enonic.cms.server.service.admin.ajax.dto.SynchronizeStatusDto;
import com.enonic.cms.server.service.admin.ajax.dto.UserDto;
import com.enonic.cms.store.dao.ContentDao;
import com.enonic.cms.store.dao.GroupDao;
import com.enonic.cms.store.dao.MenuItemDao;
import com.enonic.cms.store.dao.PortletDao;
import com.enonic.cms.store.dao.PreferenceDao;
import com.enonic.cms.store.dao.SiteDao;
import com.enonic.cms.store.dao.UserDao;

@RemoteProxy(name = "AjaxService", creator = AdminAjaxServiceCreator.class)
public class AdminAjaxServiceImpl
    implements AdminAjaxService, InitializingBean
{
    private static final Logger LOG = LoggerFactory.getLogger( AdminAjaxServiceImpl.class );

    private static final String STRING_EMPTY_RESULT_RETURN_VALUE = null;

    @Autowired
    private AdminService adminService;

    @Autowired
    private ContentService contentService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private MemberOfResolver memberOfResolver;

    @Autowired
    private CountryService countryService;

    @Autowired
    private SynchronizeUserStoreJobFactory syncUserStoreJobFactory;

    @Autowired
    private MenuItemDao menuItemDao;

    @Autowired
    private ContentDao contentDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private GroupDao groupDao;

    @Autowired
    private PortletDao portletDao;

    @Autowired
    private PreferenceDao preferenceDao;

    @Autowired
    private SiteDao siteDao;

    @Autowired
    private AdminXsltProcessorFactory xsltProcessorFactory;

    private SyncUserStoreExecutorManager syncUserStoreExecutorManager;

    public void afterPropertiesSet()
        throws Exception
    {
        this.syncUserStoreExecutorManager = new SyncUserStoreExecutorManager( this.syncUserStoreJobFactory );
    }

    @RemoteMethod
    public String deleteContentVersion( int versionKey )
    {
        ensureUserIsLoggedIn();

        UserEntity deleter = getLoggedInAdminConsoleUser();
        try
        {
            contentService.deleteVersion( deleter, new ContentVersionKey( versionKey ) );
        }
        catch ( Exception e )
        {
            LOG.warn( "ERROR: " + e.getMessage(), e );
            return "An error occured while deleting content version: " + e.getLocalizedMessage();
        }

        return null;
    }

    @RemoteMethod
    public String getArchiveSizeByCategory( int categoryKey )
    {
        ensureUserHasEnterpriseAdministratorPowers();

        try
        {
            return String.valueOf( adminService.getArchiveSizeByCategory( categoryKey ) );
        }
        catch ( Exception e )
        {
            LOG.warn( "ERROR: " + e.getMessage(), e );
            return "ERROR: " + e.getMessage();
        }
    }

    @RemoteMethod
    public String getArchiveSizeByUnit( int unitKey )
    {
        ensureUserHasEnterpriseAdministratorPowers();

        try
        {
            return String.valueOf( adminService.getArchiveSizeByUnit( unitKey ) );
        }
        catch ( Exception e )
        {
            LOG.warn( "ERROR: " + e.getMessage(), e );
            return "ERROR: " + e.getMessage();
        }
    }

    @RemoteMethod
    public boolean isContentInUse( String[] contentkeys )
    {
        ensureUserIsLoggedIn();

        return contentService.isContentInUse( ContentKey.convertToList( contentkeys ) );
    }

    @RemoteMethod
    public String getContentUsedByAsHtml( int contentKey )
    {
        ensureUserIsLoggedIn();

        UserEntity user = getLoggedInAdminConsoleUser();

        try
        {
            final Date now = new Date();
            List<ContentKey> contentKeyList = new ArrayList<ContentKey>();
            contentKeyList.add( new ContentKey( contentKey ) );

            ContentByContentQuery contentByContentQuery = new ContentByContentQuery();
            contentByContentQuery.setContentKeyFilter( contentKeyList );
            contentByContentQuery.setUser( user );

            ContentResultSet contentResultSet = contentService.queryContent( contentByContentQuery );
            if ( contentResultSet.getLength() == 1 )
            {

                RelatedContentQuery relatedContentQuery = new RelatedContentQuery( now );
                relatedContentQuery.setUser( user );
                relatedContentQuery.setContentResultSet( contentResultSet );
                relatedContentQuery.setParentLevel( 1 );
                relatedContentQuery.setChildrenLevel( 0 );
                relatedContentQuery.setParentChildrenLevel( 0 );
                relatedContentQuery.setIncludeOnlyMainVersions( true );
                relatedContentQuery.setFilterIncludeOfflineContent();

                RelatedContentResultSet relatedContents = contentService.queryRelatedContent( relatedContentQuery );

                ContentXMLCreator contentXMLCreator = new ContentXMLCreator();
                contentXMLCreator.setIncludeRelatedContentsInfo( true );
                contentXMLCreator.setIncludeRepositoryPathInfo( true );
                ContentVersionEntity versionEntity = contentResultSet.getContent( 0 ).getMainVersion();
                XMLDocument xmlDoc = contentXMLCreator.createContentsDocument( user, versionEntity, relatedContents );

                return transformXML( xmlDoc.getAsDOMDocument(), "ajax_get_used_by_for_content.xsl" );
            }

            //return something if content is not found?
            return "";

        }
        catch ( Exception e )
        {
            LOG.warn( "ERROR: " + e.getMessage(), e );
            return "ERROR: " + e.getMessage();
        }
    }

    @RemoteMethod
    public String getPortletUsedByAsHtml( int portletKey )
    {
        ensureUserIsLoggedIn();

        UserEntity user = getLoggedInAdminConsoleUser();

        try
        {
            final XMLDocument menuItemsByContentObject = adminService.getMenuItemsByContentObject( user, portletKey );
            final Document menuItemsDoc = menuItemsByContentObject.getAsDOMDocument();

            final Element[] menuitems = XMLTool.getElements( menuItemsDoc, "menuitem" );

            for ( Element menuitem : menuitems )
            {
                final String key = menuitem.getAttribute( "key" );
                MenuItemEntity selectedMenuItem = menuItemDao.findByKey( Integer.parseInt( key ) );
                menuitem.setAttribute( "path-to-menu", selectedMenuItem.getPathAsString() );
            }

            return transformXML( menuItemsDoc, "ajax_get_used_by_for_portlet.xsl" );

        }
        catch ( Exception e )
        {
            LOG.error( "ERROR: " + e.getMessage(), e );

            return "ERROR: " + e.getMessage();
        }
    }

    @RemoteMethod
    public String getPageTemplateUsedByAsHtml( int pageTemplateKey )
    {
        ensureUserIsLoggedIn();

        UserEntity user = getLoggedInAdminConsoleUser();

        try
        {
            final XMLDocument menuItemsByPageTemplates = adminService.getMenuItemsByPageTemplates( user, new int[]{pageTemplateKey} );
            final Document menuItemsDoc = menuItemsByPageTemplates.getAsDOMDocument();

            final Element[] menuitems = XMLTool.getElements( menuItemsDoc, "menuitem" );

            for ( Element menuitem : menuitems )
            {
                final String key = menuitem.getAttribute( "key" );
                MenuItemEntity selectedMenuItem = menuItemDao.findByKey( Integer.parseInt( key ) );
                menuitem.setAttribute( "path-to-menu", selectedMenuItem.getPathAsString() );
            }

            return transformXML( menuItemsDoc, "ajax_get_used_by_for_pagetemplate.xsl" );

        }
        catch ( Exception e )
        {
            LOG.error( "ERROR: " + e.getMessage(), e );
            return "ERROR: " + e.getMessage();
        }
    }

    private String transformXML( Document doc, String xslPath )
    {
        try
        {

            HttpSession session = ServletRequestAccessor.getSession();
            if ( session == null )
            {
                VerticalAdminLogger.errorAdmin( "Http session is null" );

                return "ERROR: Http session is null";
            }
            String languageCode = (String) session.getAttribute( "languageCode" );

            XMLDocument xslDoc = AdminStore.getStylesheetAsDocument( languageCode, xslPath );
            URIResolver uriResolver = AdminStore.getURIResolver( languageCode );
            XsltProcessor processor = createProcessor( xslDoc.getSystemId(), xslDoc, uriResolver );
            return processor.process( new DOMSource( doc ) );

        }
        catch ( XsltProcessorException xpe )
        {
            String msg = "Failed to transform xml: %t";
            VerticalAdminLogger.errorAdmin( msg, xpe );

            return msg;
        }

    }

    private XsltProcessor createProcessor( String name, XMLDocument xslt, URIResolver uriResolver )
        throws XsltProcessorException
    {
        XsltResource resource = new XsltResource( name, xslt.getAsString() );
        AdminXsltProcessor processor = xsltProcessorFactory.createProcessor( resource, uriResolver );
        processor.setOmitXmlDecl( true );
        return processor;
    }

    private UserEntity getLoggedInAdminConsoleUser()
    {
        return securityService.getLoggedInAdminConsoleUserAsEntity();
    }

    private void ensureUserIsLoggedIn()
    {
        UserEntity user = getLoggedInAdminConsoleUser();
        if ( user == null || user.isAnonymous() )
        {
            throw new IllegalStateException( "User not logged in" );
        }
    }

    private void ensureUserHasEnterpriseAdministratorPowers()
    {
        UserEntity user = getLoggedInAdminConsoleUser();
        if ( user == null )
        {
            throw new IllegalStateException( "User is not logged in" );
        }
        else if ( !memberOfResolver.hasEnterpriseAdminPowers( user ) )
        {
            throw new IllegalStateException( "User is not Administrator" );
        }
    }

    private void ensureUserStoreAdministratorPowers( UserStoreKey userStoreKey )
    {
        UserEntity user = getLoggedInAdminConsoleUser();
        if ( user == null )
        {
            throw new IllegalStateException( "User is not logged in" );
        }
        else if ( !memberOfResolver.hasUserStoreAdministratorPowers( getLoggedInAdminConsoleUser(), userStoreKey ) )
        {
            throw new IllegalStateException( "User is not User Store Administrator" );
        }
    }

    @RemoteMethod
    public Collection<RegionDto> getCountryRegions( final String countryCode )
    {
        ensureUserIsLoggedIn();

        final CountryCode code = new CountryCode( countryCode );
        final Country country = countryService.getCountry( code );

        final ArrayList<RegionDto> list = new ArrayList<RegionDto>();
        for ( final Region region : country.getRegions() )
        {
            final RegionDto dto = new RegionDto();
            dto.setCode( region.getCode() );
            dto.setEnglishName( region.getEnglishName() );
            dto.setLocalName( region.getLocalName() );

            list.add( dto );
        }

        return list;
    }

    @RemoteMethod
    public boolean startSyncUserStore( String userStoreKey, boolean users, boolean groups, int batchSize )
    {
        ensureUserStoreAdministratorPowers( new UserStoreKey( userStoreKey ) );

        return this.syncUserStoreExecutorManager.start( userStoreKey, users, groups, batchSize );
    }

    @RemoteMethod
    public SynchronizeStatusDto getSynchUserStoreStatus( String userStoreKey )
    {
        if ( !StringUtils.isEmpty( userStoreKey ) )
        {
            ensureUserStoreAdministratorPowers( new UserStoreKey( userStoreKey ) );

            String languageCode = (String) ServletRequestAccessor.getSession().getAttribute( "languageCode" );
            return this.syncUserStoreExecutorManager.getStatus( userStoreKey, languageCode );
        }
        else
        {
            // to prevent client-side java script to possibly fail
            return new SynchronizeStatusDto( "" );
        }
    }

    @RemoteMethod
    public boolean menuItemNameExistsUnderParent( int siteKeyInt, String menuItemName, int existingMenuItemKeyInt, int parentKey )
    {
        ensureUserIsLoggedIn();

        final SiteKey siteKey = new SiteKey( siteKeyInt );
        final MenuItemSpecification menuItemSpec = new MenuItemSpecification();
        menuItemSpec.setSiteKey( siteKey );
        menuItemSpec.setMenuItemName( menuItemName );

        if ( parentKey >= 0 )
        {
            menuItemSpec.setParentKey( new MenuItemKey( parentKey ) );
        }
        else
        {
            menuItemSpec.setRootLevelOnly( true );
        }

        final List<MenuItemEntity> foundMenuItems = menuItemDao.findBySpecification( menuItemSpec );

        if ( foundMenuItems.size() > 1 )
        {
            return true;
        }

        if ( foundMenuItems.size() == 1 )
        {

            final MenuItemKey existingMenuItemKey = new MenuItemKey( existingMenuItemKeyInt );
            if ( menuItemNameExistsButItsMeSoItsOk( existingMenuItemKey, foundMenuItems ) )
            {
                return false;
            }
            else
            {
                return true;
            }
        }

        return false;
    }

    private boolean menuItemNameExistsButItsMeSoItsOk( MenuItemKey existingMenuItemKey, List<MenuItemEntity> foundMenuItems )
    {
        if ( foundMenuItems.get( 0 ) != null && foundMenuItems.get( 0 ).getKey().equals( existingMenuItemKey ) )
        {
            return true;
        }

        return false;
    }

    @RemoteMethod
    public String getContentPath( int contentKey )
    {
        ensureUserIsLoggedIn();

        if ( contentKey == -1 )
        {
            return STRING_EMPTY_RESULT_RETURN_VALUE;
        }

        ContentEntity content = contentDao.findByKey( new ContentKey( contentKey ) );

        if ( content == null )
        {
            return STRING_EMPTY_RESULT_RETURN_VALUE;
        }

        return content.getPathAsString();

    }

    @RemoteMethod
    public String getPagePath( int menuItemKey )
    {
        ensureUserIsLoggedIn();

        if ( menuItemKey == -1 )
        {
            return STRING_EMPTY_RESULT_RETURN_VALUE;
        }

        MenuItemEntity menuItem = menuItemDao.findByKey( menuItemKey );

        if ( menuItem == null )
        {
            return STRING_EMPTY_RESULT_RETURN_VALUE;
        }

        return menuItem.getSite().getName() + menuItem.getPathAsString();
    }

    @RemoteMethod
    public Collection<UserDto> findUsers( String name )
    {
        ensureUserIsLoggedIn();

        List<UserDto> foundUserDtos = new ArrayList<UserDto>();

        List<UserEntity> foundUsers = userDao.findByQuery( null, name, null, true );

        for ( UserEntity user : foundUsers )
        {
            if ( verifyUserProperties( user ) )
            {
                String qualifiedName = user.getQualifiedName().toString();
                String userKey = user.getKey().toString();
                UserDto userDto = createUserDto( user, qualifiedName, userKey );
                foundUserDtos.add( userDto );
            }
        }

        return foundUserDtos;
    }

    @RemoteMethod
    public Collection<UserDto> findUsersAndAccessType( String name, int contentKey )
    {
        ensureUserIsLoggedIn();

        return doFindUsers( name, null, contentKey );
    }

    private Collection<UserDto> doFindUsers( String name, AccessType accessType, Integer contentKey )
    {
        List<UserDto> foundUserDtos = new ArrayList<UserDto>();

        List<UserEntity> foundUsers = userDao.findByQuery( null, name, null, true );

        if ( contentKey == null )
        {
            throw new IllegalArgumentException( "contentKey should not be null" );
        }

        ContentEntity content = contentDao.findByKey( new ContentKey( contentKey ) );

        boolean doCheckAccessRights = accessType != null && contentKey != null;

        if ( content == null )
        {
            throw new IllegalArgumentException( "Content with key: " + contentKey + " not found" );
        }

        ContentAccessResolver contentAccessResolver = new ContentAccessResolver( groupDao );

        for ( UserEntity user : foundUsers )
        {

            AccessRightsResolver accessRightsResolver = new AccessRightsResolver( user, content, contentAccessResolver );

            if ( doCheckAccessRights && !accessRightsResolver.hasAccess( accessType ) )
            {
                continue;
            }

            if ( verifyUserProperties( user ) )
            {
                String qualifiedName = user.getQualifiedName().toString();
                String userKey = user.getKey().toString();
                UserDto userDto = createUserDto( user, qualifiedName, userKey );
                userDto.setHighestAccessRight( accessRightsResolver.getHighestAccessRight() );

                foundUserDtos.add( userDto );
            }
        }

        return foundUserDtos;
    }

    private UserDto createUserDto( UserEntity user, String qualifiedName, String userKey )
    {
        UserDto userDto = new UserDto();

        GroupKey userGroupKey = user.getUserGroupKey();
        String userGroupKeyAsString = userGroupKey != null ? userGroupKey.toString() : "";

        userDto.setDisplayName( user.getDisplayName() );
        userDto.setQualifiedName( qualifiedName );
        userDto.setEmail( user.getEmail() );
        userDto.setKey( userKey );
        userDto.setUserGroupKey( userGroupKeyAsString );
        userDto.setPhotoExists( user.hasPhoto() );
        userDto.setLabel( user.getDisplayName() );
        userDto.setValue( user.getDisplayName() );
        return userDto;
    }

    private boolean verifyUserProperties( UserEntity user )
    {
        boolean verified = true;

        if ( StringUtils.isBlank( user.getEmail() ) )
        {
            return false;
        }

        if ( StringUtils.isBlank( user.getDisplayName() ) )
        {
            return false;
        }

        return verified;
    }

    private enum AccessType
    {
        APPROVER( "approve", 3 ),
        EDITOR( "update", 2 ),
        READER( "read", 1 );

        String stringValue;

        int priority;

        AccessType( String stringValue, int priority )
        {
            this.stringValue = stringValue;
            this.priority = priority;
        }
    }

    private class AccessRightsResolver
    {
        AccessType highestAccessRight;

        AccessRightsResolver( UserEntity user, ContentEntity content, ContentAccessResolver contentAccessResolver )
        {
            if ( contentAccessResolver.hasApproveContentAccess( user, content ) )
            {
                highestAccessRight = AccessType.APPROVER;
            }
            else if ( contentAccessResolver.hasUpdateDraftVersionAccess( user, content ) )
            {
                highestAccessRight = AccessType.EDITOR;
            }
            else
            {
                highestAccessRight = AccessType.READER;
            }
        }

        public boolean hasAccess( AccessType accessType )
        {
            if ( highestAccessRight.priority >= accessType.priority )
            {
                return true;
            }

            return false;
        }

        public String getHighestAccessRight()
        {
            return highestAccessRight.stringValue;
        }

    }

    @RemoteMethod
    public Collection<PreferenceDto> getUserPreferences( String uid )
    {
        ensureUserIsLoggedIn();

        if ( StringUtils.isBlank( uid ) )
        {
            throw new IllegalArgumentException( "Uid is null or empty" );
        }

        List<PreferenceDto> preferenceDtos = new ArrayList<PreferenceDto>();

        UserKey userKey = uid == null ? null : new UserKey( uid );
        UserEntity user = userDao.findByKey( userKey );

        if ( user == null )
        {
            return preferenceDtos;
        }

        PreferenceSpecification preferenceSpec = new PreferenceSpecification( user );

        List<PreferenceEntity> preferences = preferenceDao.findBy( preferenceSpec );

        PreferenceDtoCreator preferenceDtoCreator = new PreferenceDtoCreator( portletDao, preferenceDao, siteDao, menuItemDao );

        for ( PreferenceEntity preference : preferences )
        {
            PreferenceDto preferenceDto = preferenceDtoCreator.createPreferenceDto( preference );

            if ( preferenceDto != null )
            {
                preferenceDtos.add( preferenceDto );
            }
        }

        return preferenceDtos;
    }

}
