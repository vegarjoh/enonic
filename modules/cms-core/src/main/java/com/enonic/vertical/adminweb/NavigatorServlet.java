/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.vertical.adminweb;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;

import com.enonic.esl.containers.ExtendedMap;
import com.enonic.esl.servlet.http.CookieUtil;
import com.enonic.esl.xml.XMLTool;

import com.enonic.cms.core.AdminConsoleTranslationService;
import com.enonic.cms.core.DeploymentPathResolver;
import com.enonic.cms.core.security.user.User;
import com.enonic.cms.core.security.userstore.UserStoreKey;
import com.enonic.cms.core.security.userstore.connector.config.InvalidUserStoreConnectorConfigException;
import com.enonic.cms.core.service.AdminService;

public final class NavigatorServlet
    extends AdminHandlerBaseServlet
{

    private static final int COOKIE_TIMEOUT = 60 * 60 * 24 * 365 * 50;   // 50 years

    public void handlerCustom( HttpServletRequest request, HttpServletResponse response, HttpSession session, AdminService admin,
                               ExtendedMap formItems, String operation )
        throws VerticalAdminException
    {

        User user = securityService.getLoggedInAdminConsoleUser();

        AdminConsoleTranslationService languageMap = AdminConsoleTranslationService.getInstance();
        String languageCode = formItems.getString( "lang", null );
        if ( languageCode != null )
        {
            String deploymentPath = DeploymentPathResolver.getAdminDeploymentPath( request );
            CookieUtil.setCookie( response, "languageCode", languageCode, COOKIE_TIMEOUT, deploymentPath );
            session.setAttribute( "languageCode", languageCode );
            user.setSelectedLanguageCode( languageCode );
        }
        else
        {
            languageCode = (String) session.getAttribute( "languageCode" );
            if ( languageCode == null )
            {
                languageCode = languageMap.getDefaultLanguageCode();
            }
        }

        try
        {
            Document doc = XMLTool.createDocument( "data" );

            ExtendedMap parameters = new ExtendedMap();
            parameters.put( "usergroupkey", user.hasUserGroup() ? user.getUserGroupKey().toString() : null );
            parameters.put( "userkey", user.getKey() );
            String displayName = user.getDisplayName();
            if ( displayName == null )
            {
                displayName = user.getName();
            }

            parameters.put( "userfullname", displayName );
            parameters.put( "hasphoto", user.getUserFields().getPhoto() != null );

            final UserStoreKey userStoreKey = user.getUserStoreKey();
            if ( userStoreKey != null )
            {
                parameters.put( "userstorekey", String.valueOf( userStoreKey ) );
                try
                {
                    parameters.put( "canUpdate", String.valueOf( userStoreService.canUpdateUser( userStoreKey ) ) );
                    parameters.put( "canUpdatePassword", String.valueOf( userStoreService.canUpdateUserPassword( userStoreKey ) ) );
                }
                catch ( final InvalidUserStoreConnectorConfigException e )
                {
                    parameters.put( "userStoreConfigError", e.getMessage() );
                }
            }

            parameters.put( "languagecode", languageCode );
            languageMap.toDoc( doc, languageCode );
            DOMSource xmlSource = new DOMSource( doc );
            Source xslSource = AdminStore.getStylesheet( session, "navigator.xsl" );
            transformXML( session, response.getWriter(), xmlSource, xslSource, parameters );
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
}
