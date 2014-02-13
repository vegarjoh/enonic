/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal.rendering;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.joda.time.DateTime;

import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.language.LanguageEntity;
import com.enonic.cms.core.portal.PageRequestType;
import com.enonic.cms.core.portal.VerticalSession;
import com.enonic.cms.core.preview.PreviewContext;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.structure.SiteEntity;
import com.enonic.cms.core.structure.SitePath;
import com.enonic.cms.core.structure.menuitem.MenuItemEntity;
import com.enonic.cms.core.structure.page.Regions;

/**
 * Apr 22, 2009
 */
public class PageRendererContext
{
    private PageRequestType pageRequestType;

    private DateTime requestTime;

    private String ticketId;

    private String originalUrl;

    private UserEntity renderer;

    private UserEntity runAsUser;

    private SiteEntity site;

    private MenuItemEntity menuItem;

    private Regions regionsInPage;

    /**
     * Requested content.
     */
    private ContentEntity contentFromRequest;

    private LanguageEntity language;

    private PreviewContext previewContext;

    private String deviceClass;

    private Locale locale;

    private boolean encodeURIs;

    private SitePath originalSitePath;

    private SitePath sitePath;

    private VerticalSession verticalSession;

    private String profile;

    private HttpServletRequest httpRequest;

    private boolean forceNoCacheUsage;

    private Boolean overridingSitePropertyCreateUrlAsPath;

    public PageRequestType getPageRequestType()
    {
        return pageRequestType;
    }

    public void setPageRequestType( PageRequestType pageRequestType )
    {
        this.pageRequestType = pageRequestType;
    }

    public UserEntity getRenderer()
    {
        return renderer;
    }

    public void setRenderer( UserEntity renderer )
    {
        this.renderer = renderer;
    }

    public SiteEntity getSite()
    {
        return site;
    }

    public void setSite( SiteEntity site )
    {
        this.site = site;
    }

    public MenuItemEntity getMenuItem()
    {
        return menuItem;
    }

    public void setMenuItem( MenuItemEntity menuItem )
    {
        this.menuItem = menuItem;
    }

    public ContentEntity getContentFromRequest()
    {
        return contentFromRequest;
    }

    public void setContentFromRequest( ContentEntity contentFromRequest )
    {
        this.contentFromRequest = contentFromRequest;
    }

    public Locale getLocale()
    {
        return locale;
    }

    public String getDeviceClass()
    {
        return deviceClass;
    }

    public boolean isEncodeURIs()
    {
        return encodeURIs;
    }

    public PreviewContext getPreviewContext()
    {
        return previewContext;
    }

    public void setPreviewContext( PreviewContext previewContext )
    {
        this.previewContext = previewContext;
    }

    public String getOriginalUrl()
    {
        return originalUrl;
    }

    public void setOriginalUrl( String originalUrl )
    {
        this.originalUrl = originalUrl;
    }

    public void setLocale( Locale locale )
    {
        this.locale = locale;
    }

    public void setDeviceClass( String deviceClass )
    {
        this.deviceClass = deviceClass;
    }

    public void setEncodeURIs( boolean encodeURIs )
    {
        this.encodeURIs = encodeURIs;
    }

    public SitePath getOriginalSitePath()
    {
        return originalSitePath;
    }

    public void setOriginalSitePath( SitePath originalSitePath )
    {
        this.originalSitePath = originalSitePath;
    }

    public SitePath getSitePath()
    {
        return sitePath;
    }

    public void setSitePath( SitePath sitePath )
    {
        this.sitePath = sitePath;
    }

    public UserEntity getRunAsUser()
    {
        return runAsUser;
    }

    public void setRunAsUser( UserEntity runAsUser )
    {
        this.runAsUser = runAsUser;
    }

    public String getTicketId()
    {
        return ticketId;
    }

    public void setTicketId( String ticketId )
    {
        this.ticketId = ticketId;
    }

    public VerticalSession getVerticalSession()
    {
        return verticalSession;
    }

    public void setVerticalSession( VerticalSession verticalSession )
    {
        this.verticalSession = verticalSession;
    }

    public LanguageEntity getLanguage()
    {
        return language;
    }

    public void setLanguage( LanguageEntity language )
    {
        this.language = language;
    }

    public String getProfile()
    {
        return profile;
    }

    public void setProfile( String profile )
    {
        this.profile = profile;
    }

    public HttpServletRequest getHttpRequest()
    {
        return httpRequest;
    }

    public void setHttpRequest( HttpServletRequest httpRequest )
    {
        this.httpRequest = httpRequest;
    }

    public boolean forceNoCacheUsage()
    {
        return forceNoCacheUsage;
    }

    public void setForceNoCacheUsage( boolean value )
    {
        this.forceNoCacheUsage = value;
    }

    public DateTime getRequestTime()
    {
        return requestTime;
    }

    public void setRequestTime( DateTime requestTime )
    {
        this.requestTime = requestTime;
    }

    public void setOverridingSitePropertyCreateUrlAsPath( final Boolean value )
    {
        this.overridingSitePropertyCreateUrlAsPath = value;
    }

    public Boolean getOverridingSitePropertyCreateUrlAsPath()
    {
        return overridingSitePropertyCreateUrlAsPath;
    }

    public Regions getRegionsInPage()
    {
        return regionsInPage;
    }

    public void setRegionsInPage( Regions regionsInPage )
    {
        this.regionsInPage = regionsInPage;
    }
}
