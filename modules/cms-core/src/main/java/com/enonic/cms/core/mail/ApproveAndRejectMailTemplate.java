/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.mail;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.ContentService;
import com.enonic.cms.core.content.query.ContentByContentQuery;
import com.enonic.cms.core.content.resultset.ContentResultSet;
import com.enonic.cms.core.security.user.UserEntity;

public class ApproveAndRejectMailTemplate
    extends AbstractMailTemplate
{
    @Autowired
    private ContentService contentService;

    private String originalBody;

    private ContentKey contentKey;

    private UserEntity user;

    private ContentEntity content;

    private boolean reject;

    /**
     * Creates a new instance of ApproveAndRejectMailFormatter
     *
     * @param originalBody
     * @param contentKey
     * @param user
     */
    public ApproveAndRejectMailTemplate( String originalBody, ContentKey contentKey, UserEntity user )
    {
        this.originalBody = originalBody;
        this.contentKey = contentKey;
        this.user = user;

        content = getContent( contentKey, user );

    }

    private ContentEntity getContent( final ContentKey contentKey, final UserEntity user )
    {
        List<ContentKey> keyList = new ArrayList<ContentKey>();
        keyList.add( contentKey );

        ContentByContentQuery contentByContentQuery = new ContentByContentQuery();
        contentByContentQuery.setUser( user );
        contentByContentQuery.setContentKeyFilter( keyList );

        ContentResultSet resultSet = contentService.queryContent( contentByContentQuery );
        if ( resultSet.getLength() < 1 )
        {
            throw new RuntimeException( "Content does not is exist" );
        }
        if ( resultSet.getLength() > 1 )
        {
            throw new RuntimeException( "getContent returned multiple contents for single content key" );
        }
        return resultSet.getContent( 0 );
    }

    @Override
    public String getBody()
    {
        String contentPath = content.getPathAsString();

        String body = "";
        if ( originalBody != null )
        {
            body = originalBody;
        }

        if ( contentPath != null )
        {
            String infoText = getTranslation( "%approveRejectContentMailText%", getLanguageCode() );
            if ( infoText != null )
            {
                body = body + "\n\n" + infoText + ":\n" + contentPath;
            }
        }

        String adminUrl = getAdminUrl( contentKey );
        if ( adminUrl != null )
        {
            body = body + "\n\n" + adminUrl;
        }

        return body;
    }

    @Override
    public String getSubject()
    {
        return reject ? createRejectSubject() : createApprovalSubject();
    }

    private String createRejectSubject()
    {
        return getTranslation( "%contentRejectedSubject%", getLanguageCode() ) + ": " + content.getMainVersion().getTitle();
    }

    private String createApprovalSubject()
    {
        return getTranslation( "%contentWaitingForApprovalSubject%", getLanguageCode() ) + ": " + content.getMainVersion().getTitle();
    }

    private String getLanguageCode()
    {
        return content.getLanguage().getCode();
    }

    public boolean isReject()
    {
        return reject;
    }

    public void setReject( boolean reject )
    {
        this.reject = reject;
    }
}
