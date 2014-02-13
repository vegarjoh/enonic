/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.mail;

import com.enonic.cms.core.security.user.QualifiedUsername;

public interface SendMailService
{
    public void sendMail( AbstractMailTemplate mailTemplate );

    public void sendChangePasswordMail( QualifiedUsername userName, String newPassword );

    public void sendChangePasswordMail( QualifiedUsername userName, String newPassword, MessageSettings settings );
}
