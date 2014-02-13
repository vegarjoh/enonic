/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.vertical.adminweb;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.fileupload.FileItem;
import org.jdom.transform.JDOMSource;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.util.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.enonic.esl.containers.ExtendedMap;
import com.enonic.esl.containers.MultiValueMap;
import com.enonic.esl.servlet.http.CookieUtil;
import com.enonic.esl.util.DateUtil;
import com.enonic.esl.util.StringUtil;
import com.enonic.esl.xml.XMLTool;
import com.enonic.vertical.VerticalException;
import com.enonic.vertical.adminweb.handlers.ListCountResolver;
import com.enonic.vertical.engine.VerticalEngineException;
import com.enonic.vertical.engine.VerticalEngineLogger;

import com.enonic.cms.framework.xml.XMLDocument;
import com.enonic.cms.framework.xml.XMLDocumentFactory;

import com.enonic.cms.core.AbstractPagedXmlCreator;
import com.enonic.cms.core.AdminConsoleTranslationService;
import com.enonic.cms.core.DeploymentPathResolver;
import com.enonic.cms.core.country.Country;
import com.enonic.cms.core.country.CountryXmlCreator;
import com.enonic.cms.core.locale.LocaleXmlCreator;
import com.enonic.cms.core.mail.MailRecipientType;
import com.enonic.cms.core.mail.SimpleMailTemplate;
import com.enonic.cms.core.resource.ResourceFile;
import com.enonic.cms.core.resource.ResourceKey;
import com.enonic.cms.core.security.LoginAdminUserCommand;
import com.enonic.cms.core.security.PasswordGenerator;
import com.enonic.cms.core.security.group.GroupEntity;
import com.enonic.cms.core.security.group.GroupKey;
import com.enonic.cms.core.security.group.GroupSpecification;
import com.enonic.cms.core.security.group.GroupType;
import com.enonic.cms.core.security.group.GroupXmlCreator;
import com.enonic.cms.core.security.user.DeleteUserCommand;
import com.enonic.cms.core.security.user.DisplayNameResolver;
import com.enonic.cms.core.security.user.QualifiedUsername;
import com.enonic.cms.core.security.user.ReadOnlyUserFieldValidator;
import com.enonic.cms.core.security.user.RequiredUserFieldsValidator;
import com.enonic.cms.core.security.user.StoreNewUserCommand;
import com.enonic.cms.core.security.user.UpdateUserCommand;
import com.enonic.cms.core.security.user.User;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.security.user.UserFieldsXmlCreator;
import com.enonic.cms.core.security.user.UserKey;
import com.enonic.cms.core.security.user.UserNotFoundException;
import com.enonic.cms.core.security.user.UserSpecification;
import com.enonic.cms.core.security.user.UserStorageExistingEmailException;
import com.enonic.cms.core.security.user.UserType;
import com.enonic.cms.core.security.user.UserXmlCreator;
import com.enonic.cms.core.security.userstore.UserStoreEntity;
import com.enonic.cms.core.security.userstore.UserStoreKey;
import com.enonic.cms.core.security.userstore.UserStoreXmlCreator;
import com.enonic.cms.api.plugin.ext.userstore.UserStoreConfig;
import com.enonic.cms.core.security.userstore.connector.config.InvalidUserStoreConnectorConfigException;
import com.enonic.cms.core.service.AdminService;
import com.enonic.cms.core.stylesheet.StylesheetNotFoundException;
import com.enonic.cms.core.timezone.TimeZoneXmlCreator;
import com.enonic.cms.core.user.field.UserFieldTransformer;
import com.enonic.cms.api.plugin.ext.userstore.UserFieldType;
import com.enonic.cms.api.plugin.ext.userstore.UserFields;
import com.enonic.cms.core.xslt.XsltProcessorException;
import com.enonic.cms.core.xslt.XsltResource;
import com.enonic.cms.core.xslt.admin.AdminXsltProcessor;
import com.enonic.cms.store.dao.GroupQuery;


public class UserHandlerServlet
    extends AdminHandlerBaseServlet
{
    private final static String DUMMY_OID = "dummy";

    private static final String SESSION_PHOTO_ITEM_KEY = "photo_form_item";

    public static final int COOKIE_TIMEOUT = 60 * 60 * 24 * 365 * 50;

    private class UserPhotoHolder
    {
        private transient byte[] photo;

        public byte[] getPhoto()
        {
            return photo;
        }

        public void setPhoto( byte[] photo )
        {
            this.photo = photo;
        }
    }

    public void handlerBrowse( HttpServletRequest request, HttpServletResponse response, HttpSession session, AdminService admin,
                               ExtendedMap formItems )
        throws VerticalAdminException
    {

        User oldUser = securityService.getLoggedInAdminConsoleUser();

        UserEntity user = securityService.getUser( oldUser );

        UserStoreEntity userStoreInContext = resolveUserStoreInContext( formItems );
        UserStoreEntity userStore = resolveChosenUserStore( formItems );

        String query = formItems.getString( "query", null );

        final String modeStr = formItems.getString( "mode", "" );

        boolean userStoreGroups = "groups".equals( modeStr );
        boolean globalGroups = "globalgroups".equals( modeStr );
        boolean isGroups = userStoreGroups || globalGroups;

        final String cookieName = ( isGroups ) ? "groupBrowseItemsPerPage" : "userBrowseItemsPerPage";
        int index = formItems.getInt( "index", 0 );
        int count = ListCountResolver.resolveCount( request, formItems, cookieName );
        CookieUtil.setCookie( response, cookieName, Integer.toString( count ), COOKIE_TIMEOUT,
                              DeploymentPathResolver.getAdminDeploymentPath( request ) );

        String sortBy;
        String sortByDirection;

        boolean userMode = modeStr.equals( "users" ) || modeStr.equals( "" );
        if ( userMode )
        {
            sortBy = formItems.getString( "sortby", "timestamp" );
            sortByDirection = formItems.getString( "sortby-direction", "descending" );
        }
        else
        {
            sortBy = formItems.getString( "sortby", "name" );
            sortByDirection = formItems.getString( "sortby-direction", "ascending" );
        }

        String orderBy = sortBy;
        boolean orderByAscending = !"descending".equals( sortByDirection );

        Document doc;
        if ( isGroups )
        {
            Collection<GroupType> groupTypes = new ArrayList<GroupType>();
            if ( user.isEnterpriseAdmin() )
            {
                groupTypes.add( GroupType.ENTERPRISE_ADMINS );
            }
            groupTypes.add( GroupType.USERSTORE_GROUP );
            groupTypes.add( GroupType.USERSTORE_ADMINS );
            groupTypes.add( GroupType.AUTHENTICATED_USERS );
            groupTypes.add( GroupType.GLOBAL_GROUP );
            groupTypes.add( GroupType.ADMINS );
            // groupTypes.add(GroupType.USER);
            // groupTypes.add(GroupType.ANONYMOUS);
            groupTypes.add( GroupType.CONTRIBUTORS );
            groupTypes.add( GroupType.DEVELOPERS );
            groupTypes.add( GroupType.EXPERT_CONTRIBUTORS );

            GroupQuery spec = new GroupQuery();
            if ( userStore != null )
            {
                spec.setUserStoreKey( userStore.getKey() );
            }

            spec.setGlobalOnly( globalGroups );
            spec.setGroupTypes( groupTypes );
            spec.setQuery( query );
            spec.setOrderBy( orderBy );
            spec.setOrderAscending( orderByAscending );
            final List<GroupEntity> groups = securityService.getGroups( spec );
            final List<GroupEntity> inContextGroups = checkInContextGroups( userStoreInContext, groups );

            GroupXmlCreator groupXmlCreator = new GroupXmlCreator();
            groupXmlCreator.setAdminConsoleStyle( true );
            doc = XMLDocumentFactory.create( groupXmlCreator.createPagedDocument( inContextGroups, index, count ) ).getAsDOMDocument();
        }
        else
        {
            if ( "name".equals( orderBy ) )
            {
                orderBy = "displayName";
            }
            List<UserEntity> users =
                securityService.findUsersByQuery( userStore != null ? userStore.getKey() : null, query, orderBy, orderByAscending );
            AbstractPagedXmlCreator userXmlCreator = new UserXmlCreator();
            org.jdom.Document usersDoc = userXmlCreator.createPagedDocument( users, index, count );
            doc = XMLDocumentFactory.create( usersDoc ).getAsDOMDocument();
        }

        final UserStoreXmlCreator xmlCreator = new UserStoreXmlCreator( userStoreService.getUserStoreConnectorConfigs() );

        final List<UserStoreEntity> validUserStores = new ArrayList<UserStoreEntity>();

        if ( userStore != null )
        {
            if ( memberOfResolver.hasUserStoreAdministratorPowers( user, userStore.getKey() ) )
            {
                validUserStores.add( userStore );
            }

        }
        else
        {
            if ( userStoreInContext != null )
            {
                if ( memberOfResolver.hasUserStoreAdministratorPowers( user, userStoreInContext.getKey() ) )
                {
                    validUserStores.add( userStoreInContext );
                }
            }
            else
            {

                final List<UserStoreEntity> userStores = securityService.getUserStores();
                for ( UserStoreEntity userStoreEntity : userStores )
                {
                    if ( memberOfResolver.hasUserStoreAdministratorPowers( user, userStoreEntity.getKey() ) )
                    {
                        validUserStores.add( userStoreEntity );
                    }
                }
            }
        }

        Document userStoresDoc = XMLDocumentFactory.create( xmlCreator.createPagedDocument( validUserStores, 0, 100 ) ).getAsDOMDocument();
        XMLTool.mergeDocuments( doc, userStoresDoc, true );

        ExtendedMap parameters = new ExtendedMap();
        parameters.put( "mode", formItems.getString( "mode", "users" ) );
        parameters.put( "query", query );
        if ( userStore != null )
        {
            parameters.put( "userstorekey", String.valueOf( userStore.getKey() ) );
            parameters.put( "userstorename", userStore.getName() );
        }
        if ( userStoreInContext != null )
        {
            parameters.put( "userstorekeyincontext", String.valueOf( userStoreInContext.getKey() ) );
        }

        parameters.put( "index", String.valueOf( index ) );
        parameters.put( "count", String.valueOf( count ) );
        if ( formItems.containsKey( "callback" ) )
        {
            parameters.put( "callback", formItems.getString( "callback" ) );
        }
        if ( formItems.containsKey( "modeselector" ) )
        {
            parameters.put( "modeselector", formItems.getString( "modeselector" ) );
        }

        final boolean isEnterpriseAdmin = admin.isEnterpriseAdmin( oldUser );

        boolean isUserStoreAdmin = false;
        if ( userStoreInContext != null )
        {
            isUserStoreAdmin = admin.isUserStoreAdmin( user, userStoreInContext.getKey() );
        }

        String userstoreSelector = "false";
        if ( ( isUserStoreAdmin || isEnterpriseAdmin ) && formItems.containsKey( "userstoreselector" ) )
        {
            userstoreSelector = formItems.getString( "userstoreselector" );
        }
        parameters.put( "userstoreselector", userstoreSelector );

        if ( formItems.containsKey( "excludekey" ) )
        {
            parameters.put( "excludekey", formItems.getString( "excludekey" ) );
        }

        if ( formItems.containsKey( "allowauthenticated" ) )
        {
            parameters.put( "allowauthenticated", formItems.getString( "allowauthenticated" ) );
        }

        if ( formItems.containsKey( "allow-all-to-be-added" ) )
        {
            parameters.put( "allow-all-to-be-added", formItems.getString( "allow-all-to-be-added" ) );
        }

        parameters.put( "sortby", sortBy );
        parameters.put( "sortby-direction", sortByDirection );
        if ( userStore != null )
        {
            parameters.put( "userstoreadmin", String.valueOf( admin.isUserStoreAdmin( oldUser, userStore.getKey() ) ) );
        }
        else
        {
            parameters.put( "userstoreadmin", String.valueOf( isEnterpriseAdmin ) );
        }
        parameters.put( "admin", String.valueOf( admin.isAdmin( oldUser ) ) );

        if ( userStore != null )
        {
            try
            {
                parameters.put( "canCreateUser", String.valueOf( userStoreService.canCreateUser( userStore.getKey() ) ) );
                parameters.put( "canUpdateUser", String.valueOf( userStoreService.canUpdateUser( userStore.getKey() ) ) );
                parameters.put( "canDeleteUser", String.valueOf( userStoreService.canDeleteUser( userStore.getKey() ) ) );
                parameters.put( "canUpdatePassword", String.valueOf( userStoreService.canUpdateUserPassword( userStore.getKey() ) ) );
                parameters.put( "canCreateGroup", String.valueOf( userStoreService.canCreateGroup( userStore.getKey() ) ) );
                parameters.put( "canUpdateGroup", String.valueOf( userStoreService.canUpdateGroup( userStore.getKey() ) ) );
                parameters.put( "canDeleteGroup", String.valueOf( userStoreService.canDeleteGroup( userStore.getKey() ) ) );
            }
            catch ( final InvalidUserStoreConnectorConfigException e )
            {
                parameters.put( "userStoreConfigError", e.getMessage() );
            }

        }

        parameters.put( "opener", formItems.getString( "opener", "" ) );
        parameters.put( "use-user-group-key", formItems.getString( "use-user-group-key", "" ) );
        parameters.put( "user-picker-key-field", formItems.getString( "user-picker-key-field", "" ) );

        transformXML( request, response, doc, "user_group_list.xsl", parameters );
    }

    private List<GroupEntity> checkInContextGroups( UserStoreEntity userStoreInContext, List<GroupEntity> groups )
    {

        if ( userStoreInContext == null )
        {
            return groups;
        }

        List<GroupEntity> inContextGroups = new ArrayList<GroupEntity>();

        for ( GroupEntity group : groups )
        {
            if ( group.isGlobal() )
            {
                inContextGroups.add( group );
            }
            else
            {
                UserStoreKey userStoreKey = group.getUserStoreKey();

                Assert.isTrue( userStoreKey != null, "All non-global groups expected to have userStoreKey" );

                if ( userStoreKey.equals( userStoreInContext.getKey() ) )
                {
                    inContextGroups.add( group );
                }
            }
        }

        return inContextGroups;
    }

    private UserStoreEntity resolveUserStoreInContext( ExtendedMap formItems )
    {
        UserStoreKey userStoreKeyInContext = null;
        if ( !"-1".equals( formItems.getString( "userstorekeyincontext", "-1" ) ) &&
            formItems.getString( "userstorekeyincontext", "-1" ) != null )
        {
            userStoreKeyInContext = new UserStoreKey( formItems.getString( "userstorekeyincontext" ) );
        }

        UserStoreEntity userStoreInContext = null;
        if ( userStoreKeyInContext != null )
        {
            userStoreInContext = userStoreDao.findByKey( userStoreKeyInContext );
        }
        return userStoreInContext;
    }

    private UserStoreEntity resolveChosenUserStore( ExtendedMap formItems )
    {
        UserStoreKey userStoreKey = null;
        if ( !"".equals( formItems.getString( "userstorekey", "" ) ) )
        {
            userStoreKey = new UserStoreKey( formItems.getString( "userstorekey" ) );
        }

        UserStoreEntity userStore = null;
        if ( userStoreKey != null )
        {
            userStore = userStoreDao.findByKey( userStoreKey );
        }
        return userStore;
    }

    private Source buildDummyObjectClasses()
    {
        ObjectClassesXmlCreator xmlCreator = new ObjectClassesXmlCreator();
        return new JDOMSource( xmlCreator.createDocument( DUMMY_OID ) );
    }

    public void handlerForm( HttpServletRequest request, HttpServletResponse response, HttpSession session, AdminService admin,
                             ExtendedMap formItems )
        throws VerticalAdminException
    {

        User loggedInUser = securityService.getLoggedInAdminConsoleUser();
        UserStoreKey userStoreKey = UserStoreKey.parse( formItems.getString( "userstorekey", null ) );
        UserStoreEntity userStore = userStoreDao.findByKey( userStoreKey );
        try
        {
            ExtendedMap xslParams = new ExtendedMap();

            Source xmlSource;

            // build XSL
            Source tempXSLSource = AdminStore.getStylesheet( session, "__build_user_form.xsl" );
            StreamSource xslSource = buildXSL( session, tempXSLSource, buildDummyObjectClasses() );

            int create = 0;
            Document userDoc;
            if ( formItems.containsKey( "key" ) )
            {
                final String userGroupKeyStr = formItems.getString( "key" );
                final GroupKey userGroupKey = new GroupKey( userGroupKeyStr );
                final UserSpecification userSpec = new UserSpecification();
                userSpec.setDeletedState( UserSpecification.DeletedState.ANY );
                userSpec.setUserGroupKey( userGroupKey );
                UserEntity user = userDao.findSingleBySpecification( userSpec );
                if ( user == null )
                {
                    throw new UserNotFoundException( userSpec );
                }

                if ( user.isInRemoteUserStore() )
                {
                    userStoreService.synchronizeUser( user.getUserStoreKey(), user.getName() );
                    user = userDao.findSingleBySpecification( userSpec );
                }

                final UserXmlCreator userXmlCreator = new UserXmlCreator();
                userXmlCreator.setIncludeUserFields( true );
                XMLDocument userXmlDoc = XMLDocumentFactory.create( userXmlCreator.createUsersDocument( user, true, false ) );
                userDoc = userXmlDoc.getAsDOMDocument();

                DisplayNameResolver displayNameResolver = new DisplayNameResolver( userStore.getConfig() );

                xslParams.put( "generated-display-name",
                               displayNameResolver.resolveDisplayName( user.getName(), user.getDisplayName(), user.getUserFields() ) );
                Element usersElem = userDoc.getDocumentElement();
                Element userElem = XMLTool.getElement( usersElem, "user" );
                String userKey = userElem.getAttribute( "key" );

                MultiValueMap adminParams = new MultiValueMap();
                adminParams.put( "@userkey", userKey );
                adminParams.put( "@typekey", 1 );
                Document logEntries = admin.getLogEntries( loggedInUser, adminParams, 0, 5, true ).getAsDOMDocument();
                XMLTool.mergeDocuments( userDoc, logEntries, true );
            }
            else
            {
                create = 1;
                userDoc = XMLTool.createDocument( "users" );
            }

            final UserStoreXmlCreator userStoreXmlCreator = new UserStoreXmlCreator( userStoreService.getUserStoreConnectorConfigs() );
            XMLDocument userStoreXMLDoc =
                XMLDocumentFactory.create( new org.jdom.Document( userStoreXmlCreator.createUserStoreElement( userStore ) ) );
            XMLTool.mergeDocuments( userDoc, userStoreXMLDoc.getAsDOMDocument(), true );

            CountryXmlCreator countryXmlCreator = new CountryXmlCreator();
            Collection<Country> countries = countryService.getCountries();
            countryXmlCreator.setIncludeRegionsInfo( false );
            XMLDocument countriesXMLDoc = XMLDocumentFactory.create( countryXmlCreator.createCountriesDocument( countries ) );
            XMLTool.mergeDocuments( userDoc, countriesXMLDoc.getAsDOMDocument(), true );

            LocaleXmlCreator localeXmlCreator = new LocaleXmlCreator();
            Locale[] locales = localeService.getLocales();
            XMLDocument localesXMLDoc = XMLDocumentFactory.create( localeXmlCreator.createLocalesDocument( locales ) );
            XMLTool.mergeDocuments( userDoc, localesXMLDoc.getAsDOMDocument(), true );

            DateTime now = timeService.getNowAsDateTime();
            TimeZoneXmlCreator timeZoneXmlCreator = new TimeZoneXmlCreator( now );
            Collection<DateTimeZone> timeZones = timeZoneService.getTimeZones();
            XMLDocument timeZonesDoc = XMLDocumentFactory.create( timeZoneXmlCreator.createTimeZonesDocument( timeZones ) );
            XMLTool.mergeDocuments( userDoc, timeZonesDoc.getAsDOMDocument(), true );

            xmlSource = new DOMSource( userDoc );

            xslParams.put( "userstorename", userStore.getName() );
            xslParams.put( "userstorekey", String.valueOf( userStoreKey ) );
            xslParams.put( "page", formItems.getString( "page" ) );
            xslParams.put( "create", String.valueOf( create ) );
            if ( memberOfResolver.hasUserStoreAdministratorPowers( loggedInUser.getKey(), userStoreKey ) )
            {
                xslParams.put( "showdn", "true" );
            }

            if ( formItems.containsKey( "mode" ) )
            {
                xslParams.put( "mode", formItems.getString( "mode" ) );
            }
            if ( formItems.containsKey( "callback" ) )
            {
                xslParams.put( "callback", formItems.getString( "callback" ) );
            }
            if ( formItems.containsKey( "modeselector" ) )
            {
                xslParams.put( "modeselector", formItems.getString( "modeselector" ) );
            }
            if ( formItems.containsKey( "userstoreselector" ) )
            {
                xslParams.put( "userstoreselector", formItems.getString( "userstoreselector" ) );
            }
            if ( formItems.containsKey( "excludekey" ) )
            {
                xslParams.put( "excludekey", formItems.getString( "excludekey" ) );
            }

            // WIZARD
            if ( formItems.getString( "wizard", "" ).equals( "true" ) )
            {
                int step = 1;
                int prevstep = formItems.getInt( "prevstep", 0 );

                // If we just entered the wizard
                if ( !formItems.containsKey( "step" ) )
                {
                    session.removeAttribute( "userxml" );
                    session.removeAttribute( "groupxml" );
                    session.removeAttribute( "notification" );
                    session.removeAttribute( "to_name" );
                    session.removeAttribute( "to_mail" );
                    session.removeAttribute( "subject" );
                    session.removeAttribute( "mail_body" );
                    session.removeAttribute( "grouparray" );
                    session.removeAttribute( SESSION_PHOTO_ITEM_KEY );

                    session.setAttribute( "from_name", loggedInUser.getDisplayName() );
                    if ( loggedInUser.getEmail() != null )
                    {
                        session.setAttribute( "from_mail", loggedInUser.getEmail() );
                    }
                    else
                    {
                        session.removeAttribute( "from_mail" );
                    }
                }
                else
                {
                    step = formItems.getInt( "step" );
                }

                // Store data from step 1 in session object
                if ( prevstep == 1 )
                {
                    UserFields userFields = parseCustomUserFieldValues( userStoreKey, formItems );
                    Document newDoc = XMLTool.domparse( buildUserXML( userFields, formItems ) );

                    storeUserPhotoInSession( session, formItems );

                    // Check if username and password already exist
                    String uid = null;
                    String password = null;
                    String oldXML = (String) session.getAttribute( "userxml" );
                    if ( oldXML != null )
                    {
                        Document oldDoc = XMLTool.domparse( oldXML );
                        Element uidElem = (Element) XMLTool.selectNode( oldDoc.getDocumentElement(), "/user/block/uid" );
                        Element passwordElem = (Element) XMLTool.selectNode( oldDoc.getDocumentElement(), "/user/block/password" );
                        uid = XMLTool.getElementText( uidElem );
                        password = XMLTool.getElementText( passwordElem );
                    }
                    // Generate username and password, if not present
                    if ( uid == null )
                    {
                        String firstname = XMLTool.getElementText( newDoc, "/user/block/first-name" );
                        String lastname = XMLTool.getElementText( newDoc, "/user/block/last-name" );
                        synchronized ( admin )
                        {
                            uid = admin.generateUID( firstname, lastname, userStoreKey );
                        }
                    }
                    Element uidElem = (Element) XMLTool.selectNode( newDoc.getDocumentElement(), "/user/block/uid" );
                    XMLTool.createTextNode( newDoc, uidElem, uid );
                    if ( password == null )
                    {
                        synchronized ( admin )
                        {
                            try
                            {
                                password = PasswordGenerator.generateNewPassword();
                            }
                            catch ( VerticalException ve )
                            {
                                VerticalEngineLogger.warn( "Unable to generate password." );
                            }
                        }
                    }
                    Element passwordElem = (Element) XMLTool.selectNode( newDoc.getDocumentElement(), "/user/block/password" );
                    XMLTool.createTextNode( newDoc, passwordElem, password );

                    session.setAttribute( "userxml", XMLTool.documentToString( newDoc ) );

                    // Fill in values in notification form, if not present
                    session.setAttribute( "to_name", XMLTool.getElementText( newDoc, "/user/block/displayName" ) );
                    session.setAttribute( "to_mail", XMLTool.getElementText( newDoc, "/user/block/email" ) );
                    session.setAttribute( "mail_body",
                                          "Username: " + XMLTool.getElementText( newDoc, "/user/block/uid" ) + (char) 13 + "Password: " +
                                              XMLTool.getElementText( newDoc, "/user/block/password" ) );

                    // Check whether email address already exists
                    String email = XMLTool.getElementText( newDoc, "/user/block/email" );

                    try
                    {
                        userStoreService.verifyUniqueEmailAddress( email, userStoreKey );
                    }
                    catch ( UserStorageExistingEmailException e )
                    {
                        this.addError( 4, "email", null );
                        this.addErrorsXML( userDoc );
                        step = 1;
                    }
                }
                // Store data from step 2 in session object
                if ( prevstep == 2 )
                {
                    Document newDoc;
                    String newXML = (String) session.getAttribute( "userxml" );

                    if ( newXML != null )
                    {
                        newDoc = XMLTool.domparse( newXML );
                        Element uidElem = (Element) XMLTool.selectNode( newDoc.getDocumentElement(), "/user/block/uid" );
                        Element passwordElem = (Element) XMLTool.selectNode( newDoc.getDocumentElement(), "/user/block/password" );
                        uidElem.setTextContent( formItems.getString( "uid_dummy", "" ) );
                        passwordElem.setTextContent( formItems.getString( "password_dummy", "" ) );
                        session.setAttribute( "mail_body", "Username: " + XMLTool.getElementText( newDoc, "/user/block/uid" ) + (char) 13 +
                            "Password: " + XMLTool.getElementText( newDoc, "/user/block/password" ) );
                    }
                    else
                    {
                        UserFields userFields = parseCustomUserFieldValues( userStoreKey, formItems );
                        newDoc = XMLTool.domparse( buildUserXML( userFields, formItems ) );
                    }

                    session.setAttribute( "userxml", XMLTool.documentToString( newDoc ) );

                    // Check whether uid already exists
                    String uid = XMLTool.getElementText( newDoc, "/user/block/uid" );

                    if ( uid != null && uid.length() > 0 )
                    {
                        UserSpecification userSpec = new UserSpecification();
                        userSpec.setName( uid );
                        userSpec.setUserStoreKey( userStoreKey );
                        userSpec.setDeletedStateNotDeleted();
                        if ( userDao.findSingleBySpecification( userSpec ) != null )
                        {
                            this.addError( 5, "uid_" + DUMMY_OID, null );
                            this.addErrorsXML( userDoc );
                            step = 2;
                        }
                    }
                }
                // Store data from step 3 in session object
                else if ( prevstep == 3 )
                {
                    if ( formItems.containsKey( "member" ) )
                    {
                        org.jdom.Element userEl = new org.jdom.Element( "user" );
                        org.jdom.Element memberOfEl = new org.jdom.Element( "memberOf" );
                        userEl.addContent( memberOfEl );
                        org.jdom.Document membershipDoc = new org.jdom.Document( userEl );

                        String[] groupArray;
                        if ( isArrayFormItem( formItems, "member" ) )
                        {
                            groupArray = (String[]) formItems.get( "member" );
                        }
                        else
                        {
                            groupArray = new String[]{formItems.getString( "member" )};
                        }
                        session.setAttribute( "grouparray", groupArray );
                        for ( String aGroupArray : groupArray )
                        {
                            org.jdom.Document groupDocument = admin.getGroup( aGroupArray ).getAsJDOMDocument();
                            org.jdom.Element groupsEl = groupDocument.getRootElement();
                            memberOfEl.addContent( groupsEl.getChild( "group" ).detach() );
                        }
                        session.setAttribute( "groupxml", XMLTool.documentToString( membershipDoc ) );
                    }
                }
                // Store data from step 4 in session object
                else if ( prevstep == 4 )
                {
                    session.setAttribute( "notification", formItems.getString( "notification", "false" ) );
                    if ( formItems.getString( "notification", "false" ).equals( "true" ) )
                    {
                        session.setAttribute( "from_name", formItems.getString( "from_name", "" ) );
                        session.setAttribute( "from_mail", formItems.getString( "from_mail", "" ) );
                        session.setAttribute( "to_name", formItems.getString( "to_name", "" ) );
                        session.setAttribute( "to_mail", formItems.getString( "to_mail", "" ) );
                        session.setAttribute( "subject", formItems.getString( "subject", "" ) );
                        session.setAttribute( "mail_body", formItems.getString( "mail_body", "" ) );
                    }
                }

                // Load data from session object
                if ( step == 1 || step == 2 )
                {
                    String newXML = (String) session.getAttribute( "userxml" );
                    if ( newXML != null )
                    {
                        Document newDoc = XMLTool.domparse( newXML );
                        XMLTool.mergeDocuments( userDoc, newDoc, true );
                    }
                }
                else if ( step == 3 )
                {
                    String groupsXML = (String) session.getAttribute( "groupxml" );
                    if ( groupsXML != null )
                    {
                        Document groups = XMLTool.domparse( groupsXML );
                        XMLTool.mergeDocuments( userDoc, groups, true );
                    }
                }
                else if ( step == 4 )
                {
                    String notification = (String) session.getAttribute( "notification" );
                    if ( notification == null )
                    {
                        notification = "true";
                    }
                    xslParams.put( "notification", notification );
                    xslParams.put( "from_name", session.getAttribute( "from_name" ) );
                    xslParams.put( "from_mail", session.getAttribute( "from_mail" ) );
                    xslParams.put( "to_name", session.getAttribute( "to_name" ) );
                    xslParams.put( "to_mail", session.getAttribute( "to_mail" ) );
                    xslParams.put( "subject", session.getAttribute( "subject" ) );
                    // Need to remove ascii #10 (line feed) to prevent double line breaks
                    String mail_body = (String) session.getAttribute( "mail_body" );
                    StringBuffer mail_body_fixed;
                    if ( mail_body != null )
                    {
                        mail_body_fixed = new StringBuffer( mail_body.length() );
                        for ( int i = 0; i < mail_body.length(); i++ )
                        {
                            if ( ( (int) mail_body.charAt( i ) ) != 10 )
                            {
                                mail_body_fixed.append( mail_body.charAt( i ) );
                            }
                        }
                        xslParams.put( "mail_body", mail_body_fixed.toString() );
                    }
                    else
                    {
                        xslParams.put( "mail_body", "" );
                    }
                }
                xslParams.put( "step", Integer.toString( step ) );
                xslParams.put( "wizard", Boolean.TRUE );
            }
            else
            {
                String languageCode = (String) session.getAttribute( "languageCode" );
                AdminConsoleTranslationService languageMap = AdminConsoleTranslationService.getInstance();
                languageMap.toDoc( userDoc, languageCode );
                xslParams.put( "languagecode", languageCode );
            }

            addSortParamteres( "block/uid", "ascending", formItems, session, xslParams );

            try
            {
                xslParams.put( "canUpdateUser", String.valueOf( userStoreService.canUpdateUser( userStoreKey ) ) );
                xslParams.put( "canUpdateGroup", String.valueOf( userStoreService.canUpdateGroup( userStoreKey ) ) );
                final UserFormEditableFieldsResolver.FormAction formAction =
                    create == 1 ? UserFormEditableFieldsResolver.FormAction.CREATE : UserFormEditableFieldsResolver.FormAction.UPDATE;
                final boolean canCreateUserPolicy = userStoreService.canCreateUser( userStoreKey );
                final boolean canUpdateUserPolicy = userStoreService.canUpdateUser( userStoreKey );
                final UserFormEditableFieldsResolver userFormEditableFieldsResolver =
                    new UserFormEditableFieldsResolver( userStore, formAction, canCreateUserPolicy, canUpdateUserPolicy );
                userFormEditableFieldsResolver.resolveAndApply( xslParams );

            }
            catch ( final InvalidUserStoreConnectorConfigException e )
            {
                xslParams.put( "userStoreConfigError", e.getMessage() );
            }

            if ( loggedInUser.getName().equals( formItems.get( "key", "" ) ) )
            {
                xslParams.put( "profile", Boolean.TRUE );
            }

            if ( memberOfResolver.hasUserStoreAdministratorPowers( loggedInUser.getKey(), userStoreKey ) )
            {
                xslParams.put( "isadmin", "true" );
            }

            xslParams.put( "uid", loggedInUser.getName() );

            transformXML( session, response.getWriter(), xmlSource, xslSource, xslParams );
        }
        catch ( IOException e )
        {
            VerticalAdminLogger.errorAdmin( "I/O error: %t", e );
        }
        catch ( TransformerException e )
        {
            VerticalAdminLogger.errorAdmin( "XSLT error: %t", e );
        }
    }

    private void storeUserPhotoInSession( HttpSession session, ExtendedMap formItems )
    {
        FileItem item = formItems.getFileItem( UserFieldType.PHOTO.getName(), null );

        if ( item != null )
        {
            UserPhotoHolder userPhoto = new UserPhotoHolder();
            userPhoto.setPhoto( item.get() );
            session.setAttribute( SESSION_PHOTO_ITEM_KEY, userPhoto );
        }
    }

    private StreamSource buildXSL( HttpSession session, Source xslFile, Source source )
        throws VerticalAdminException
    {

        StreamSource result = null;

        HashMap<String, Object> xslParams = new HashMap<String, Object>();

        xslParams.put( "xslpath", "" );
        // xslParams.put("xslpath", AdminStore.getAdminXSLPath());

        try
        {
            StringWriter swriter = new StringWriter();
            transformXML( session, swriter, source, xslFile, xslParams );

            result = new StreamSource( new StringReader( swriter.toString() ) );
            result.setSystemId( xslFile.getSystemId() );
        }
        catch ( Exception e )
        {
            VerticalAdminLogger.errorAdmin( "XSLT error: %t", e );
        }

        return result;
    }

    private String buildUserXML( final UserFields userFields, final ExtendedMap formItems )
        throws VerticalAdminException
    {
        Document doc = XMLTool.createDocument( "user" );
        Element userElement = doc.getDocumentElement();
        userElement.setAttribute( "type", String.valueOf( UserType.NORMAL.getKey() ) );

        userElement.setAttribute( "userstorekey", formItems.getString( "userstorekey" ) );
        Element blockElement = XMLTool.createElement( doc, userElement, "block" );
        blockElement.setAttribute( "oid", "dummy" );

        XMLTool.createElement( doc, blockElement, "uid", formItems.getString( "uid_dummy", "" ) );
        XMLTool.createElement( doc, blockElement, "password", formItems.getString( "password_dummy", "" ) );
        XMLTool.createElement( doc, blockElement, "email", formItems.getString( "email", "" ) );
        XMLTool.createElement( doc, blockElement, "displayName", formItems.getString( "display_name", "" ) );

        return addUserFieldsToUserXML( XMLDocumentFactory.create( doc ), userFields );
    }

    private String addUserFieldsToUserXML( final XMLDocument xmlDoc, final UserFields userFields )
    {
        final org.jdom.Document doc = xmlDoc.getAsJDOMDocument();
        final UserFieldsXmlCreator creator = new UserFieldsXmlCreator();
        creator.addUserInfoToElement( doc.getRootElement().getChild( "block" ), userFields, true );
        return XMLTool.documentToString( doc );
    }

    public void handlerCreate( HttpServletRequest request, HttpServletResponse response, HttpSession session, AdminService admin,
                               ExtendedMap formItems )
        throws VerticalAdminException, VerticalEngineException
    {

        UserStoreKey userStoreKey = new UserStoreKey( formItems.getInt( "userstorekey" ) );

        UserStoreEntity userStore = null;
        List<UserStoreEntity> userStores = securityService.getUserStores();
        for ( UserStoreEntity userStoreEntity : userStores )
        {
            if ( userStoreEntity.getKey().equals( userStoreKey ) )
            {
                userStore = userStoreEntity;
                break;
            }
        }

        User oldUser = securityService.getLoggedInAdminConsoleUser();
        UserEntity user = securityService.getUser( oldUser );

        boolean isEnterpriseAdmin = false;
        if ( memberOfResolver.hasEnterpriseAdminPowers( user ) )
        {
            isEnterpriseAdmin = true;
        }

        boolean isUserstoreAdmin = false;
        if ( memberOfResolver.hasUserStoreAdministratorPowers( user, userStore.getKey() ) )
        {
            isUserstoreAdmin = true;
        }

        GroupKey enterpriseAdminGroupKey = securityService.getEnterpriseAdministratorGroup();

        StoreNewUserCommand command = new StoreNewUserCommand();
        command.setStorer( user.getKey() );
        command.setUserStoreKey( userStoreKey );

        boolean wizard = false;

        if ( formItems.getString( "wizard", "" ).equals( "true" ) )
        {
            wizard = true;
            String xmlData = (String) session.getAttribute( "userxml" );

            XMLDocument xmlDocument = XMLDocumentFactory.create( xmlData );
            org.jdom.Document jdomDoc = xmlDocument.getAsJDOMDocument();
            org.jdom.Element userEl = jdomDoc.getRootElement();
            org.jdom.Element blockEl = userEl.getChild( "block" );
            org.jdom.Element uidEl = blockEl.getChild( "uid" );
            org.jdom.Element passwordEl = blockEl.getChild( "password" );
            org.jdom.Element displayNameEl = blockEl.getChild( "displayName" );
            org.jdom.Element emailEl = blockEl.getChild( "email" );

            command.setUsername( uidEl.getText() );
            command.setPassword( passwordEl.getText() );
            command.setDisplayName( displayNameEl.getText() );
            command.setEmail( emailEl.getText() );

            final ExtendedMap valuesFromXml = new ExtendedMap();
            for ( org.jdom.Element userfieldEl : (List<org.jdom.Element>) blockEl.getChildren() )
            {

                String userfieldName = userfieldEl.getName();

                if ( "photo".equals( userfieldName ) )
                {
                    // Do nothing
                }
                else if ( "addresses".equals( userfieldName ) )
                {
                    int addressIndex = 0;
                    for ( org.jdom.Element addressEl : (List<org.jdom.Element>) userfieldEl.getChildren() )
                    {
                        for ( org.jdom.Element addressFieldEl : (List<org.jdom.Element>) addressEl.getChildren() )
                        {
                            String addressFieldName = "address[" + addressIndex + "]." + addressFieldEl.getName();
                            String addressFieldValue = addressFieldEl.getText();
                            valuesFromXml.put( addressFieldName, addressFieldValue );
                        }
                        addressIndex++;
                    }
                }
                else
                {
                    String userfieldValue = userfieldEl.getText();
                    valuesFromXml.put( userfieldName, userfieldValue );
                }
            }

            final UserFields userFields = parseCustomUserFieldValues( userStoreKey, valuesFromXml );

            addPhotoFromSession( session, userFields );

            command.setUserFields( userFields );
        }
        else
        {
            command.setUsername( formItems.getString( "uid_dummy", "" ) );
            command.setPassword( formItems.getString( "password_dummy", "" ) );
            command.setDisplayName( formItems.getString( "display_name", "" ) );
            command.setEmail( formItems.getString( "email", "" ) );

            final UserFields userFields = parseCustomUserFieldValues( userStoreKey, formItems );
            command.setUserFields( userFields );
        }

        // Update user with group memberships
        if ( formItems.containsKey( "member" ) )
        {
            String[] groupArray;
            if ( isArrayFormItem( formItems, "member" ) )
            {
                groupArray = (String[]) formItems.get( "member" );
            }
            else
            {
                groupArray = new String[]{formItems.getString( "member" )};
            }

            for ( String aGroupArray : groupArray )
            {
                if ( isEnterpriseAdmin )
                {
                    command.addMembership( new GroupKey( aGroupArray ) );
                }
                else if ( !isEnterpriseAdmin && isUserstoreAdmin && enterpriseAdminGroupKey.toString().equalsIgnoreCase( aGroupArray ) )
                {
                    throw new SecurityException( "No access to enterprise administrators group" );
                }
                else if ( !isEnterpriseAdmin && isUserstoreAdmin && !enterpriseAdminGroupKey.toString().equalsIgnoreCase( aGroupArray ) )
                {
                    command.addMembership( new GroupKey( aGroupArray ) );
                }
            }
        }
        else if ( wizard )
        {
            String[] groupArray = (String[]) session.getAttribute( "grouparray" );
            if ( groupArray != null )
            {
                for ( String aGroupArray : groupArray )
                {
                    if ( isEnterpriseAdmin )
                    {
                        // access to all groups/users
                        command.addMembership( new GroupKey( aGroupArray ) );
                    }
                    else if ( !isEnterpriseAdmin && isUserstoreAdmin && enterpriseAdminGroupKey.toString().equalsIgnoreCase( aGroupArray ) )
                    {
                        throw new SecurityException( "No access to enterprise administrators group" );
                    }
                    else if ( !isEnterpriseAdmin && isUserstoreAdmin &&
                        !enterpriseAdminGroupKey.toString().equalsIgnoreCase( aGroupArray ) )
                    {
                        command.addMembership( new GroupKey( aGroupArray ) );
                    }
                }
            }
        }

        UserKey newUserKey = userStoreService.storeNewUser( command );
        UserEntity newUser = userDao.findByKey( newUserKey );

        MultiValueMap queryParams = new MultiValueMap();
        if ( formItems.containsKey( "mode" ) )
        {
            queryParams.put( "mode", formItems.getString( "mode" ) );
        }
        if ( formItems.containsKey( "callback" ) )
        {
            queryParams.put( "callback", formItems.getString( "callback" ) );
        }
        if ( formItems.containsKey( "modeselector" ) )
        {
            queryParams.put( "modeselector", formItems.getString( "modeselector" ) );
        }
        if ( formItems.containsKey( "userstoreselector" ) )
        {
            queryParams.put( "userstoreselector", formItems.getString( "userstoreselector" ) );
        }
        if ( formItems.containsKey( "excludekey" ) )
        {
            queryParams.put( "excludekey", formItems.getString( "excludekey" ) );
        }

        if ( wizard )
        {
            if ( "true".equals( formItems.getString( "notification", "" ) ) )
            {
                handlerNotification( request, response, session, formItems, "sendnotification" );
            }
            else
            {
                queryParams.put( "page", formItems.get( "page" ) );
                queryParams.put( "op", "browse" );
                queryParams.put( "userstorekey", userStoreKey.toString() );
                redirectClientToAdminPath( "adminpage", queryParams, request, response );
            }
        }
        else
        {
            if ( "true".equals( formItems.getString( "notification", "" ) ) )
            {
                queryParams.put( "page", formItems.get( "page" ) );
                queryParams.put( "op", "notification" );
                queryParams.put( "userstorekey", userStoreKey.toString() );
                queryParams.put( "uid", newUser.getName() );
                redirectClientToAdminPath( "adminpage", queryParams, request, response );
            }
            else
            {
                queryParams.put( "page", formItems.get( "page" ) );
                queryParams.put( "op", "browse" );
                queryParams.put( "userstorekey", userStoreKey.toString() );
                redirectClientToAdminPath( "adminpage", queryParams, request, response );
            }
        }
    }

    private void addPhotoFromSession( HttpSession session, UserFields userFields )
    {
        UserPhotoHolder userPhoto = (UserPhotoHolder) session.getAttribute( SESSION_PHOTO_ITEM_KEY );

        if ( userPhoto != null )
        {
            userFields.setPhoto( userPhoto.getPhoto() );
            session.removeAttribute( SESSION_PHOTO_ITEM_KEY );
        }
    }

    private UserFields parseCustomUserFieldValues( final UserStoreKey userStoreKey, final ExtendedMap formItems )
    {
        return parseCustomUserFieldValues( userStoreKey, formItems, false );
    }

    private UserFields parseCustomUserFieldValues( final UserStoreKey userStoreKey, final ExtendedMap formItems,
                                                   boolean transformNullValuesToBlanksForConfiguredFields )
    {
        final UserStoreEntity userStore = userStoreService.getUserStore( userStoreKey );
        final UserStoreConfig userStoreConfig = userStore.getConfig();
        final UserFieldTransformer fieldTransformer = new UserFieldTransformer();
        if ( transformNullValuesToBlanksForConfiguredFields )
        {
            fieldTransformer.transformNullValuesToBlanksForConfiguredFields( userStoreConfig );
        }
        fieldTransformer.transformNullHtmlEmailValueToFalseIfConfigured( userStoreConfig );

        final UserFields userFields = fieldTransformer.toUserFields( formItems );

        userFields.removeReadOnlyFields( userStoreConfig );
        new RequiredUserFieldsValidator( userStoreConfig ).validateAllRequiredFieldsArePresentAndNotEmpty( userFields );
        new ReadOnlyUserFieldValidator( userStoreConfig ).validate( userFields );
        return userFields;
    }

    public void handlerRemove( HttpServletRequest request, HttpServletResponse response, HttpSession session, AdminService admin,
                               ExtendedMap formItems, String key )
        throws VerticalAdminException, VerticalEngineException
    {

        User deleter = securityService.getLoggedInAdminConsoleUser();

        GroupKey userGroupKeyToDelete = new GroupKey( formItems.getString( "key" ) );

        UserSpecification userSpec = new UserSpecification();
        userSpec.setUserGroupKey( userGroupKeyToDelete );
        userSpec.setDeletedState( UserSpecification.DeletedState.NOT_DELETED );
        UserEntity userToDelete = userDao.findSingleBySpecification( userSpec );

        UserSpecification userToDeleteSpec = new UserSpecification();
        userToDeleteSpec.setDeletedState( UserSpecification.DeletedState.NOT_DELETED );
        userToDeleteSpec.setKey( userToDelete.getKey() );
        DeleteUserCommand command = new DeleteUserCommand( deleter.getKey(), userToDeleteSpec );
        userStoreService.deleteUser( command );

        redirectClientToReferer( request, response );
    }

    public void handlerCustom( HttpServletRequest request, HttpServletResponse response, HttpSession session, AdminService admin,
                               ExtendedMap formItems, String operation )
        throws VerticalAdminException, VerticalEngineException
    {

        User user = securityService.getLoggedInAdminConsoleUser();

        if ( "remove_user".equals( operation ) )
        {
            String uid = formItems.getString( "key" );
            UserStoreKey userStoreKey = new UserStoreKey( formItems.getInt( "userstorekey" ) );

            UserSpecification userToDeleteSpec = new UserSpecification();
            userToDeleteSpec.setUserStoreKey( userStoreKey );
            userToDeleteSpec.setName( uid );
            userToDeleteSpec.setDeletedStateNotDeleted();

            DeleteUserCommand command = new DeleteUserCommand( user.getKey(), userToDeleteSpec );
            userStoreService.deleteUser( command );

            MultiValueMap queryParams = new MultiValueMap();
            queryParams.put( "page", formItems.get( "page" ) );
            queryParams.put( "userstorekey", userStoreKey.toString() );
            if ( formItems.containsKey( "searchtype" ) )
            {
                queryParams.put( "op", "searchresults" );
                queryParams.put( "searchtype", formItems.get( "searchtype" ) );
                queryParams.put( "searchtext", formItems.get( "searchtext" ) );
            }
            else
            {
                queryParams.put( "op", "browse" );
            }
            redirectClientToAdminPath( "adminpage", queryParams, request, response );
        }
        else if ( "changepassword".equals( operation ) )
        {
            changePassword( request, response, session, formItems );
        }
        else if ( "notification".equals( operation ) || "sendnotification".equals( operation ) )
        {
            handlerNotification( request, response, session, formItems, operation );
        }
        else
        {
            super.handlerCustom( request, response, session, admin, formItems, operation );
        }
    }

    public void handlerReport( HttpServletRequest request, HttpServletResponse response, HttpSession session, AdminService admin,
                               ExtendedMap formItems, String subop )
        throws VerticalAdminException, VerticalEngineException
    {

        // User user = securityService.getAdminUser();
        UserStoreKey userStoreKey = new UserStoreKey( formItems.getInt( "userstorekey" ) );
        UserStoreEntity userStore = userStoreDao.findByKey( userStoreKey );
        try
        {
            if ( subop.equals( "create" ) )
            {

                ResourceKey stylesheetKey = ResourceKey.from( formItems.getString( "stylesheetkey" ) );
                ResourceFile res = resourceService.getResourceFile( stylesheetKey );
                if ( res == null )
                {
                    throw new StylesheetNotFoundException( stylesheetKey );
                }

                Document reportDoc;

                UserXmlCreator userXmlCreator = new UserXmlCreator();
                userXmlCreator.setIncludeUserFields( true );
                userXmlCreator.wrappUserFieldsInBlockElement( false );

                String selection = formItems.getString( "selection" );
                if ( "all".equals( selection ) )
                {
                    List<UserEntity> users = securityService.findUsersByQuery( userStoreKey, null, null, true );
                    org.jdom.Document usersDoc = userXmlCreator.createUsersDocument( users, true, false );
                    reportDoc = XMLDocumentFactory.create( usersDoc ).getAsDOMDocument();
                }
                else
                {
                    String[] groupStringArray = formItems.getStringArray( "group" );

                    boolean recursive = false;
                    if ( formItems.containsKey( "recursivegroups" ) )
                    {
                        recursive = true;
                    }

                    Set<UserEntity> users = new LinkedHashSet<UserEntity>();
                    for ( String groupKey : groupStringArray )
                    {
                        // Global groups do not belong to any user store
                        GroupSpecification groupSpecification = new GroupSpecification();
                        groupSpecification.setKey( new GroupKey( groupKey ) );
                        GroupEntity groupEntity = groupDao.findSingleBySpecification( groupSpecification );

                        // Show global groups and groups in current user store
                        if ( groupEntity.getUserStore() == null || groupEntity.getUserStore().getKey().equals( userStoreKey ) )
                        {
                            Set<GroupEntity> userGroups;
                            if ( recursive )
                            {
                                userGroups = groupEntity.getAllMembersRecursively();
                            }
                            else
                            {
                                userGroups = groupEntity.getMembers( false );
                            }

                            for ( GroupEntity userGroup : userGroups )
                            {
                                if ( userGroup.isOfType( GroupType.USER, false ) )
                                {
                                    users.add( userGroup.getUser() );
                                }
                            }

                        }
                    }
                    org.jdom.Document usersDoc = userXmlCreator.createUsersDocument( new ArrayList<UserEntity>( users ), false, false );
                    reportDoc = XMLDocumentFactory.create( usersDoc ).getAsDOMDocument();
                }
                Element usersElem = reportDoc.getDocumentElement();
                Element verticaldataElem = XMLTool.createElement( reportDoc, "result" );
                reportDoc.replaceChild( verticaldataElem, usersElem );
                verticaldataElem.appendChild( usersElem );
                DOMSource reportSource = new DOMSource( reportDoc );

                XsltResource xslResource = new XsltResource( res.getDataAsXml().getAsString() );
                AdminXsltProcessor proc = xsltProcessorFactory.createProcessor( xslResource, getStylesheetURIResolver( admin ) );
                proc.setParameter( "datetoday", DateUtil.formatISODateTime( new Date() ) );

                response.setContentType( proc.getOutputMediaType() + "; charset=UTF-8" );
                response.getWriter().write( proc.process( reportSource ) );

            }
            else
            {
                Map<String, Object> xslParams = new HashMap<String, Object>();
                xslParams.put( "userstorename", userStore.getName() );
                xslParams.put( "userstorekey", String.valueOf( userStoreKey ) );
                xslParams.put( "page", formItems.getString( "page" ) );

                DOMSource xmlSource = new DOMSource( XMLTool.createDocument( "foo" ) );
                Source xslSource = AdminStore.getStylesheet( session, "user_report.xsl" );
                transformXML( session, response.getWriter(), xmlSource, xslSource, xslParams );
            }

        }
        catch ( XsltProcessorException e )
        {
            String message = "Failed to transmform XML document: %t";
            VerticalAdminLogger.errorAdmin( message, e );
        }
        catch ( TransformerException e )
        {
            VerticalAdminLogger.errorAdmin( "XSLT error: %t", e );
        }
        catch ( IOException e )
        {
            VerticalAdminLogger.errorAdmin( "I/O error: %t", e );
        }
    }

    private void handlerNotification( HttpServletRequest request, HttpServletResponse response, HttpSession session, ExtendedMap formItems,
                                      String operation )
        throws VerticalAdminException, VerticalEngineException
    {

        User user = securityService.getLoggedInAdminConsoleUser();
        UserStoreKey userStoreKey = new UserStoreKey( formItems.getInt( "userstorekey", -1 ) );

        if ( "notification".equals( operation ) )
        {
            try
            {
                final UserSpecification userSpec = new UserSpecification();
                userSpec.setName( formItems.getString( "uid" ) );
                userSpec.setUserStoreKey( userStoreKey );
                userSpec.setDeletedStateNotDeleted();
                final UserEntity userToNotify = userDao.findSingleBySpecification( userSpec );

                final UserXmlCreator userXmlCreator = new UserXmlCreator();
                final XMLDocument userDoc = XMLDocumentFactory.create( userXmlCreator.createUserDocument( userToNotify, false, false ) );

                ExtendedMap parameters = new ExtendedMap();
                parameters.put( "page", formItems.getString( "page" ) );

                parameters.put( "sender_mail", user.getEmail() );
                parameters.put( "sender_name", user.getDisplayName() );

                parameters.put( "userstorekey", String.valueOf( userStoreKey ) );
                parameters.put( "mode", formItems.getString( "mode" ) );
                if ( formItems.containsKey( "callback" ) )
                {
                    parameters.put( "callback", formItems.getString( "callback" ) );
                }
                if ( formItems.containsKey( "modeselector" ) )
                {
                    parameters.put( "modeselector", formItems.getString( "modeselector" ) );
                }
                if ( formItems.containsKey( "userstoreselector" ) )
                {
                    parameters.put( "userstoreselector", formItems.getString( "userstoreselector" ) );
                }
                if ( formItems.containsKey( "excludekey" ) )
                {
                    parameters.put( "excludekey", formItems.getString( "excludekey" ) );
                }

                final Source xslSource = AdminStore.getStylesheet( session, "user_notify.xsl" );
                transformXML( session, response.getWriter(), userDoc.getAsSource(), xslSource, parameters );
            }
            catch ( TransformerException e )
            {
                VerticalAdminLogger.errorAdmin( "XSLT error.", e );
            }
            catch ( IOException e )
            {
                VerticalAdminLogger.errorAdmin( "I/O error.", e );
            }
        }
        else if ( "sendnotification".equals( operation ) )
        {
            final SimpleMailTemplate mail = new SimpleMailTemplate();

            mail.setFrom( formItems.getString( "from_name", "" ), formItems.getString( "from_mail", "" ) );
            mail.addRecipient( formItems.getString( "to_name", "" ), formItems.getString( "to_mail" ), MailRecipientType.TO_RECIPIENT );

            if ( formItems.containsKey( "cc_mail" ) )
            {
                String[] ccRecipients = StringUtil.splitString( formItems.getString( "cc_mail" ), ";" );
                for ( String ccRecipient : ccRecipients )
                {
                    mail.addRecipient( ccRecipient.trim(), ccRecipient, MailRecipientType.CC_RECIPIENT );
                }
            }

            mail.setSubject( formItems.getString( "subject", "" ) );
            mail.setMessage( formItems.getString( "mail_body" ) );

            sendMailService.sendMail( mail );

            MultiValueMap queryParams = new MultiValueMap();
            queryParams.put( "page", formItems.get( "page" ) );
            queryParams.put( "op", "browse" );
            queryParams.put( "userstorekey", userStoreKey.toString() );
            queryParams.put( "mode", formItems.getString( "mode" ) );
            if ( formItems.containsKey( "callback" ) )
            {
                queryParams.put( "callback", formItems.getString( "callback" ) );
            }
            if ( formItems.containsKey( "modeselector" ) )
            {
                queryParams.put( "modeselector", formItems.getString( "modeselector" ) );
            }
            if ( formItems.containsKey( "userstoreselector" ) )
            {
                queryParams.put( "userstoreselector", formItems.getString( "userstoreselector" ) );
            }
            if ( formItems.containsKey( "excludekey" ) )
            {
                queryParams.put( "excludekey", formItems.getString( "excludekey" ) );
            }
            redirectClientToAdminPath( "adminpage", queryParams, request, response );
        }
    }

    private void changePassword( HttpServletRequest request, HttpServletResponse response, HttpSession session, ExtendedMap formItems )
        throws VerticalAdminException, VerticalEngineException
    {

        final User user = securityService.getLoggedInAdminConsoleUser();
        final UserStoreKey userStoreKey = new UserStoreKey( formItems.getInt( "userstorekey" ) );
        final UserStoreEntity userStore = userStoreDao.findByKey( userStoreKey );
        if ( !"submit".equals( formItems.getString( "subop", "" ) ) )
        {
            try
            {
                Source xslSource = AdminStore.getStylesheet( session, "user_changepassword.xsl" );
                ExtendedMap parameters = new ExtendedMap();
                parameters.put( "page", formItems.getString( "page" ) );
                if ( formItems.containsKey( "key" ) )
                {
                    final UserSpecification spec = new UserSpecification();
                    spec.setUserGroupKey( new GroupKey( formItems.getString( "key" ) ) );
                    spec.setType( UserType.NORMAL );
                    spec.setDeletedStateNotDeleted();

                    final UserEntity userToChangePasswordFor = userDao.findSingleBySpecification( spec );
                    parameters.put( "uid", userToChangePasswordFor.getName() );
                }
                else
                {
                    parameters.put( "uid", user.getName() );
                }

                parameters.put( "userstorekey", userStoreKey );
                parameters.put( "userstorename", userStore.getName() );

                if ( memberOfResolver.hasUserStoreAdministratorPowers( user.getKey(), userStoreKey ) )
                {
                    parameters.put( "redirect", "adminpage?page=" + formItems.getString( "page" ) + "&userstorekey=" +
                        formItems.getString( "userstorekey" ) + "&op=browse" );
                }
                else
                {
                    parameters.put( "redirect", "adminpage?page=960&op=page" );
                }

                // If user is updating himself, the old password must be submitted too
                if ( parameters.get( "uid" ).equals( user.getName() ) )
                {
                    parameters.put( "authorize", "true" );
                }

                transformXML( session, response.getWriter(), new DOMSource( XMLTool.createDocument( "dummy" ) ), xslSource, parameters );
            }
            catch ( TransformerException e )
            {
                VerticalAdminLogger.errorAdmin( "XSLT error.", e );
            }
            catch ( IOException e )
            {
                VerticalAdminLogger.errorAdmin( "I/O error.", e );
            }
        }
        else
        {
            // UserStoreKey userStoreKey = new UserStoreKey(formItems.getInt("userstorekey"));

            final String newPassword1 = formItems.getString( "password" );
            final String newPassword2 = formItems.getString( "repeatpassword" );

            if ( !newPassword1.equals( newPassword2 ) )
            {
                VerticalAdminLogger.errorAdmin( "Passwords do not match!" );
            }

            final String uid = formItems.getString( "uid" );
            final QualifiedUsername qualifiedUsername = new QualifiedUsername( userStoreKey, uid );

            // If user is updating himself, we need to verify the old password
            if ( user.getName().equals( uid ) )
            {
                final String oldPassword = formItems.getString( "oldpassword", "" );
                securityService.loginAdminUser( new LoginAdminUserCommand( qualifiedUsername, oldPassword ) );
            }

            securityService.changePassword( qualifiedUsername, newPassword1 );

            redirectClientToAdminPath( formItems.getString( "redirect" ), request, response );
        }
    }

    public void handlerUpdate( HttpServletRequest request, HttpServletResponse response, HttpSession session, AdminService admin,
                               ExtendedMap formItems )
        throws VerticalAdminException, VerticalEngineException
    {

        UserStoreKey userStoreKey = new UserStoreKey( formItems.getInt( "userstorekey" ) );

        UserEntity user = securityService.getLoggedInAdminConsoleUserAsEntity();

        GroupKey enterpriseAdminGroupKey = securityService.getEnterpriseAdministratorGroup();

        UserSpecification userSpecification = new UserSpecification();
        userSpecification.setName( formItems.getString( "uid_dummy" ) );
        userSpecification.setUserStoreKey( userStoreKey );
        userSpecification.setDeletedStateNotDeleted();

        Set<GroupKey> requestedGroupMemberships = new HashSet<GroupKey>();
        if ( formItems.containsKey( "member" ) )
        {
            String[] groupArray;
            if ( isArrayFormItem( formItems, "member" ) )
            {
                groupArray = (String[]) formItems.get( "member" );
            }
            else
            {
                groupArray = new String[]{formItems.getString( "member" )};
            }

            for ( String groupKey : groupArray )
            {
                boolean groupIsEnterpriseGroup = enterpriseAdminGroupKey.toString().equalsIgnoreCase( groupKey );
                if ( groupIsEnterpriseGroup )
                {
                    if ( memberOfResolver.hasEnterpriseAdminPowers( user ) )
                    {
                        requestedGroupMemberships.add( new GroupKey( groupKey ) );
                    }
                    else
                    {
                        throw new SecurityException( "No access to enterprise administrators group" );
                    }
                }
                else
                {
                    requestedGroupMemberships.add( new GroupKey( groupKey ) );
                }
            }
        }

        UpdateUserCommand command = new UpdateUserCommand( user.getKey(), userSpecification );
        command.setupModifyStrategy();
        command.setAllowUpdateSelf( true );
        command.setDisplayName( formItems.getString( "display_name", "" ) );
        command.setEmail( formItems.getString( "email", "" ) );
        command.setRemovePhoto( formItems.getBoolean( "remove_photo", false ) );

        final UserFields userFields = parseCustomUserFieldValues( userStoreKey, formItems, true );
        command.setUserFields( userFields );

        if ( memberOfResolver.hasUserStoreAdministratorPowers( user.getKey(), userStoreKey ) )
        {
            for ( GroupKey requestedGroupMembership : requestedGroupMemberships )
            {
                command.addMembership( requestedGroupMembership );
            }
            command.setSyncMemberships( true );


        }
        else
        {
            command.setSyncMemberships( false );

        }

        userStoreService.updateUser( command );

        MultiValueMap queryParams = new MultiValueMap();

        queryParams.put( "userstorekey", userStoreKey.toString() );

        if ( formItems.containsKey( "mode" ) )
        {
            queryParams.put( "mode", formItems.getString( "mode" ) );
        }
        if ( formItems.containsKey( "callback" ) )
        {
            queryParams.put( "callback", formItems.getString( "callback" ) );
        }
        if ( formItems.containsKey( "modeselector" ) )
        {
            queryParams.put( "modeselector", formItems.getString( "modeselector" ) );
        }
        if ( formItems.containsKey( "userstoreselector" ) )
        {
            queryParams.put( "userstoreselector", formItems.getString( "userstoreselector" ) );
        }
        if ( formItems.containsKey( "excludekey" ) )
        {
            queryParams.put( "excludekey", formItems.getString( "excludekey" ) );
        }

        if ( memberOfResolver.hasUserStoreAdministratorPowers( user.getKey(), userStoreKey ) )
        {

            queryParams.put( "page", formItems.get( "page" ) );
            queryParams.put( "userstorekey", userStoreKey.toString() );

            if ( "true".equals( formItems.getString( "notification", "" ) ) )
            {
                queryParams.put( "op", "notification" );
                queryParams.put( "uid", formItems.getString( "uid_dummy" ) );
            }
            else
            {
                queryParams.put( "op", "browse" );
            }
        }
        else  // Normal user, editing own profile.
        {
            queryParams.put( "page", "960" );
            queryParams.put( "op", "page" );
        }
        redirectClientToAdminPath( "adminpage", queryParams, request, response );
    }
}
