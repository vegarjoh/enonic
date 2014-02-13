/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.structure;

public enum SitePropertyNames
{
    PAGE_CACHE( "cms.site.pageCache" ),
    PAGE_CACHE_HEADERS_ENABLED( "cms.site.page.http.cacheHeadersEnabled" ),
    PAGE_CACHE_HEADERS_FORCENOCACHE( "cms.site.page.http.forceNoCache" ),
    PAGE_CACHE_TIMETOLIVE( "cms.site.cache.page.timeToLive" ),

    ATTACHMENT_CACHE_HEADERS_ENABLED( "cms.site.attachment.http.cacheHeadersEnabled" ),
    ATTACHMENT_CACHE_HEADERS_FORCENOCACHE( "cms.site.attachment.http.forceNoCache" ),
    ATTACHMENT_CACHE_HEADERS_MAXAGE( "cms.site.attachment.http.cache.maxAge" ),

    RESOURCE_CACHE_HEADERS_ENABLED( "cms.site.resource.http.cacheHeadersEnabled" ),
    RESOURCE_CACHE_HEADERS_FORCENOCACHE( "cms.site.resource.http.forceNoCache" ),
    RESOURCE_CACHE_HEADERS_MAXAGE( "cms.site.resource.http.cache.maxAge" ),

    IMAGE_CACHE_HEADERS_ENABLED( "cms.site.image.http.cacheHeadersEnabled" ),
    IMAGE_CACHE_HEADERS_FORCENOCACHE( "cms.site.image.http.forceNoCache" ),
    IMAGE_CACHE_HEADERS_MAXAGE( "cms.site.image.http.cache.maxAge" ),

    AUTOLOGIN_HTTP_REMOTE_USER_ENABLED( "cms.site.login.autologin.httpRemoteUserEnabled" ),
    AUTOLOGIN_REMEMBER_ME_COOKIE_ENABLED( "cms.site.login.autologin.rememberMeCookieEnabled" ),

    ENABLE_UNPUBLISHED_CONTENT_PERMALINKS( "cms.site.page.enableUnpublishedContentPermalinks" ),

    SITE_URL( "cms.site.url" ),
    CREATE_URL_AS_PATH_PROPERTY( "cms.site.createUrlAsPath" ),

    LOGGING_AUTHENTICATION( "cms.site.logging.authentication" ),

    HTTP_SERVICES_ALLOW_PROPERTY( "cms.site.httpServices.allow" ),
    HTTP_SERVICES_DENY_PROPERTY( "cms.site.httpServices.deny" ),

    SITE_PROPERTY_CAPTCHA_ENABLE( "cms.site.httpServices.captchaEnabled" );    // .form, .content, .gurba

    private final String keyName;

    SitePropertyNames( final String keyName )
    {
        this.keyName = keyName;
    }

    public String getKeyName()
    {
        return keyName;
    }
}