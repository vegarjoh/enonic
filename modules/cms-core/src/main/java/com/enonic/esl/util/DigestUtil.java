/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.esl.util;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * This class implements the digest utilities.
 */
public final class DigestUtil
{
    /**
     * Generate MD5 digest string.
     */
    public static String generateMD5( String value )
    {
        return DigestUtils.md5Hex(value).toUpperCase();
    }

    /**
     * Generate SHA digest string.
     */
    public static String generateSHA( byte[] bytes )
    {
        return DigestUtils.shaHex(bytes).toUpperCase();
    }

    /**
     * Generate MD5 digest string.
     */
    public static String generateSHA( String value )
    {
        return DigestUtils.shaHex(value).toUpperCase();
    }
}
