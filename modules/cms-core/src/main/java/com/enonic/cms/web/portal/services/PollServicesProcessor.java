/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.web.portal.services;

import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.enonic.esl.containers.ExtendedMap;
import com.enonic.esl.containers.MultiValueMap;
import com.enonic.esl.servlet.http.CookieUtil;
import com.enonic.esl.xml.XMLTool;

import com.enonic.cms.core.DeploymentPathResolver;
import com.enonic.cms.core.security.user.User;
import com.enonic.cms.core.service.UserServicesService;
import com.enonic.cms.core.structure.SiteKey;

@Component
public final class PollServicesProcessor
    extends ContentServicesBase
{
    // error codes
    public final static int ERR_UNKNOWN_POLL_SELECTION = 100;

    public PollServicesProcessor()
    {
        super( "poll" );
    }

    @Override
    protected void buildContentTypeXML( UserServicesService userServices, Element contentdataElem, ExtendedMap formItems,
                                        boolean skipElements )
        throws VerticalUserServicesException
    {
    }

    protected void handlerUpdate( HttpServletRequest request, HttpServletResponse response, HttpSession session, ExtendedMap formItems,
                                  UserServicesService userServices, SiteKey siteKey )
    {

        int contentKey = formItems.getInt( "key" );
        User user = securityService.getLoggedInPortalUser();

        Document doc = userServices.getContent( user, contentKey, true, 0, 0, 0 ).getAsDOMDocument();
        Element contentsElement = doc.getDocumentElement();
        Element contentElement = XMLTool.getElement( contentsElement, "content" );
        Element contentDataElement = XMLTool.getElement( contentElement, "contentdata" );
        Element alternativesElement = XMLTool.getElement( contentDataElement, "alternatives" );

        // Find out if the user has already polled:
        String tmp = "poll" + String.valueOf( contentKey );
        Cookie cookie = CookieUtil.getCookie( request, tmp );
        if ( cookie != null && cookie.getValue().equals( "done" ) )
        {
            MultiValueMap queryParams = new MultiValueMap();
            queryParams.put( "status", "alreadyanswered" );
            redirectToPage( request, response, formItems, queryParams );
            return;
        }

        boolean voted = false;

        String multipleChoiceStr = alternativesElement.getAttribute( "multiplechoice" );
        boolean multipleChoice = ( "yes".equals( multipleChoiceStr ) );
        if ( !multipleChoice )
        {
            String selected = formItems.getString( "choice" );
            VerticalUserServicesLogger.info( "the selection was: {0}", selected );

            Map alternativesMap = XMLTool.filterElementsWithAttributeAsKey( alternativesElement.getChildNodes(), "id" );
            Element alternativeElem = (Element) alternativesMap.get( selected );
            if ( alternativeElem != null )
            {
                tmp = alternativeElem.getAttribute( "count" );
                if ( tmp.length() > 0 )
                {
                    alternativeElem.setAttribute( "count", String.valueOf( Integer.parseInt( tmp ) + 1 ) );
                }
                else
                {
                    alternativeElem.setAttribute( "count", String.valueOf( 1 ) );
                }
                voted = true;
            }
            else
            {
                redirectToErrorPage( request, response, formItems, ERR_UNKNOWN_POLL_SELECTION );
                return;
            }
        }
        else
        {
            Element[] alternatives = XMLTool.getElements( alternativesElement );
            for ( int i = 0; i < alternatives.length; i++ )
            {
                String id = alternatives[i].getAttribute( "id" );
                if ( String.valueOf( i ).equals( formItems.get( "poll" + id, null ) ) )
                {
                    tmp = alternatives[i].getAttribute( "count" );
                    if ( tmp.length() > 0 )
                    {
                        alternatives[i].setAttribute( "count", String.valueOf( Integer.parseInt( tmp ) + 1 ) );
                    }
                    else
                    {
                        alternatives[i].setAttribute( "count", "1" );
                    }
                    voted = true;
                }
            }
        }

        if ( voted )
        {
            // Increment the user counter:
            tmp = alternativesElement.getAttribute( "count" );
            if ( tmp.length() > 0 )
            {
                alternativesElement.setAttribute( "count", String.valueOf( Integer.parseInt( tmp ) + 1 ) );
            }
            else
            {
                alternativesElement.setAttribute( "count", "1" );
            }

            // Update the poll:
            Document newdoc = XMLTool.createDocument();
            newdoc.appendChild( newdoc.importNode( contentElement, true ) );
            String xmlData = XMLTool.documentToString( newdoc );
            updateContent( user, xmlData, null, null, false );

            // Set cookie to prevent user from polling a second time:
            String deploymentPath = DeploymentPathResolver.getSiteDeploymentPath( request );
            CookieUtil.setCookie( response, "poll" + String.valueOf( contentKey ), "done", SECONDS_IN_WEEK, deploymentPath );
        }

        // redirect
        redirectToPage( request, response, formItems );
    }
}
