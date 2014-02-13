/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.content.mail;

import org.apache.commons.lang.StringUtils;

import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentVersionEntity;
import com.enonic.cms.core.mail.MailRecipient;

/**
 * Created by IntelliJ IDEA.
 * User: rmh
 * Date: May 6, 2010
 * Time: 2:45:14 PM
 */
public class AssignedContentMailTemplate
    extends AbstractAssignmentMailTemplate
{

    public AssignedContentMailTemplate( ContentEntity content, ContentVersionEntity contentVersion )
    {
        super( contentVersion, content );
    }

    @Override
    public String getBody()
    {
        StringBuffer body = new StringBuffer();

        if ( StringUtils.isNotBlank( assignmentDescription ) )
        {
            body.append( assignmentDescription );
        }

        if ( assigner != null )
        {
            addNewLine( body );
            body.append( " - " );
            body.append( createUserName( assigner ) );
        }

        addNewLine( body );
        addNewLine( body );

        body.append( createAssignmentMailInfoElement() );

        return body.toString();
    }

    @Override
    public MailRecipient getFrom()
    {
        return new MailRecipient( assigner.getDisplayName(), assigner.getEmail() );
    }

    @Override
    public String getSubject()
    {
        return getTranslation( "%contentAssignedSubject%", getLanguageCode() ) + ": " + contentVersion.getTitle();
    }

}
