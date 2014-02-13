/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.web.portal.services;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Component;

import com.enonic.esl.containers.ExtendedMap;
import com.enonic.vertical.engine.VerticalEngineException;

import com.enonic.cms.core.content.CreateContentException;
import com.enonic.cms.core.content.command.CreateContentCommand;
import com.enonic.cms.core.content.contentdata.ContentDataParserException;
import com.enonic.cms.core.content.contentdata.ContentDataParserInvalidDataException;
import com.enonic.cms.core.content.contentdata.ContentDataParserUnsupportedTypeException;
import com.enonic.cms.core.content.contentdata.InvalidContentDataException;
import com.enonic.cms.core.content.contentdata.MissingRequiredContentDataException;
import com.enonic.cms.core.portal.httpservices.UserServicesException;
import com.enonic.cms.core.security.user.User;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.service.UserServicesService;
import com.enonic.cms.core.structure.SiteKey;

@Component
public final class ContentSendMailServicesProcessor
    extends SendMailServicesBase
{
    private final static int ERR_MISSING_CATEGORY_KEY = 150;

    public ContentSendMailServicesProcessor()
    {
        super( "content_sendmail" );
    }

    protected void handlerCustom( HttpServletRequest request, HttpServletResponse response, HttpSession session, ExtendedMap formItems,
                                  UserServicesService userServices, SiteKey siteKey, String operation )
        throws VerticalUserServicesException, VerticalEngineException, IOException, ClassNotFoundException, IllegalAccessException,
        InstantiationException
    {
        if ( operation.equals( "send" ) )
        {

            User oldUser = securityService.getLoggedInPortalUser();
            int categoryKey = formItems.getInt( "categorykey", -1 );

            if ( categoryKey == -1 )
            {
                String message = "Category key not specified.";
                VerticalUserServicesLogger.warn( message );
                redirectToErrorPage( request, response, formItems, ERR_MISSING_CATEGORY_KEY );
                return;
            }

            CreateContentCommand createContentCommand;
            try
            {
                createContentCommand = parseCreateContentCommand( formItems );
            }
            catch ( ContentDataParserInvalidDataException e )
            {
                String message = e.getMessage();
                VerticalUserServicesLogger.warn( message );
                redirectToErrorPage( request, response, formItems, ERR_PARAMETERS_INVALID );
                return;
            }
            catch ( ContentDataParserException e )
            {
                VerticalUserServicesLogger.error( e.getMessage(), e );
                throw new UserServicesException( ERR_OPERATION_BACKEND );
            }
            catch ( ContentDataParserUnsupportedTypeException e )
            {
                VerticalUserServicesLogger.error( e.getMessage(), e );
                throw new UserServicesException( ERR_OPERATION_BACKEND );
            }

            UserEntity runningUser = securityService.getUser( oldUser );

            createContentCommand.setAccessRightsStrategy( CreateContentCommand.AccessRightsStrategy.INHERIT_FROM_CATEGORY );

            createContentCommand.setCreator( runningUser );

            try
            {
                contentService.createContent( createContentCommand );
            }
            catch ( CreateContentException e )
            {
                RuntimeException cause = e.getRuntimeExceptionCause();

                if ( cause instanceof MissingRequiredContentDataException )
                {
                    String message = e.getMessage();
                    VerticalUserServicesLogger.warn( message );
                    redirectToErrorPage( request, response, formItems, ERR_PARAMETERS_MISSING );
                    return;
                }
                else if ( cause instanceof InvalidContentDataException )
                {
                    String message = e.getMessage();
                    VerticalUserServicesLogger.warn( message );
                    redirectToErrorPage( request, response, formItems, ERR_PARAMETERS_INVALID );
                    return;
                }
                else
                {
                    throw cause;
                }
            }
        }

        // call parent method to ensure inherited functionality
        super.handlerCustom( request, response, session, formItems, userServices, siteKey, operation );
    }
}
