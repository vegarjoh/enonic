/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.mail;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.vertical.adminweb.AdminHelper;

import com.enonic.cms.core.AdminConsoleTranslationService;
import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.servlet.ServletRequestAccessor;

/**
 * Created by IntelliJ IDEA.
 * User: rmh
 * Date: May 6, 2010
 * Time: 2:40:17 PM
 */
public abstract class AbstractMailTemplate
{
    protected final static Logger LOG = LoggerFactory.getLogger( AbstractMailTemplate.class );

    private List<MailRecipient> mailRecipients = new ArrayList<MailRecipient>();

    private Map<String, InputStream> attachments = new LinkedHashMap<String, InputStream>();

    private MailRecipient from;

    protected static final DateTimeFormatter DATE_FORMAT = DateTimeFormat.forPattern( "dd.MM.yyyy HH:mm" );

    protected String getTranslation( String key, String languageCode )
    {
        if ( !isLanguageCodeAvailable( languageCode ) )
        {
            languageCode = AdminConsoleTranslationService.getInstance().getDefaultLanguageCode();
        }

        AdminConsoleTranslationService languageMap = AdminConsoleTranslationService.getInstance();
        Map<String, String> translationMap = languageMap.getTranslationMap( languageCode );

        String phrase = translationMap.get( key );
        if ( phrase != null && !phrase.equalsIgnoreCase( "missing" ) )
        {
            return phrase;
        }
        else
        {
            translationMap = languageMap.getTranslationMap( AdminConsoleTranslationService.getInstance().getDefaultLanguageCode() );
            return translationMap.get( key );
        }
    }

    private boolean isLanguageCodeAvailable( String languageCode )
    {
        try
        {
            AdminConsoleTranslationService.getInstance().getTranslationMap( languageCode );
            return true;
        }
        catch ( IllegalArgumentException e )
        {
            LOG.warn( "Languagecode " + languageCode + " is not available, reverting to default language" );
            return false;
        }
    }

    protected String getAdminUrl( final ContentKey contentKey )
    {
        HttpServletRequest request = ServletRequestAccessor.getRequest();
        String adminUrl = AdminHelper.getAdminPath( request, false );
        if ( adminUrl != null )
        {
            adminUrl += "/adminpage?page=0&editContent=" + contentKey.toString();
        }
        return adminUrl;
    }

    public void addRecipient( MailRecipient recipient )
    {
        mailRecipients.add( recipient );
    }

    public void addAttachment( final String filename, final InputStream inputStream )
    {
        attachments.put( filename, inputStream );
    }

    public void addRecipient( UserEntity recipient )
    {
        mailRecipients.add( new MailRecipient( recipient ) );
    }

    public List<MailRecipient> getMailRecipients()
    {
        return mailRecipients;
    }

    public void setMailRecipients( List<MailRecipient> mailRecipients )
    {
        this.mailRecipients = mailRecipients;
    }

    public void addMailRecipients( List<UserEntity> mailRecipients )
    {
        for ( UserEntity recipient : mailRecipients )
        {
            this.mailRecipients.add( new MailRecipient( recipient.getDisplayName(), recipient.getEmail() ) );
        }
    }

    public MailRecipient getFrom()
    {
        return from;
    }

    public void setFrom( MailRecipient from )
    {
        this.from = from;
    }

    public abstract String getBody();

    public abstract String getSubject();

    protected void addNewLine( StringBuffer buffer )
    {
        buffer.append( "\n" );
    }

    protected String createUserName( UserEntity user )
    {
        StringBuffer buffer = new StringBuffer();

        buffer.append( user.getDisplayName() );
        buffer.append( " (" );
        buffer.append( user.getQualifiedName() );
        buffer.append( ")" );

        return buffer.toString();
    }

    public boolean isHtml()
    {
        return false;
    }

    public Map<String, InputStream> getAttachments()
    {
        return attachments;
    }
}
