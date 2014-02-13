/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.itest.core.mail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import com.enonic.cms.core.home.HomeDir;
import com.enonic.cms.core.mail.MailRecipientType;
import com.enonic.cms.core.mail.SendMailService;
import com.enonic.cms.core.mail.SimpleMailTemplate;
import com.enonic.cms.itest.AbstractSpringTest;

@TransactionConfiguration(defaultRollback = true)
@DirtiesContext
@Transactional
public class AbstractSendMailServiceTest
    extends AbstractSpringTest
{
    @Autowired
    private SendMailService sendMailService;

    @Autowired
    private HomeDir homeDir;

    @Test
    @Ignore // requires running SMTP server. mocking is senseless.
    public void testAttachment()
        throws Exception
    {
        final SimpleMailTemplate formMail = new SimpleMailTemplate();
        formMail.setFrom( "hza", "hza@enonic.com" );
        formMail.setSubject( "mail test" );
        formMail.setMessage( "test message" );
        formMail.addRecipient( null,  "hza@enonic.com" , MailRecipientType.TO_RECIPIENT );

        final File file = new File( this.homeDir.toFile(), "config/cms.properties" );

        try
        {
            formMail.addAttachment( "cms.properties", new FileInputStream( file ) );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }

        sendMailService.sendMail( formMail );
    }
}
