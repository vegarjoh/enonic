/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.captcha;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.junit.Test;

import junit.framework.Assert;

public class CaptchaRepositoryImplTest
{
    @Test
    public void testGetImageChallengeForID()
        throws Exception
    {
        final File file = File.createTempFile( "image-", ".png" );
        final CaptchaRepository captchaRepository = new CaptchaRepositoryImpl();
        final BufferedImage challenge = captchaRepository.getImageChallengeForID( null, null );
        ImageIO.write( challenge, "png", file );
        Assert.assertTrue( "file must exists!", file.exists() );
    }
}
