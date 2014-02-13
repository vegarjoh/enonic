/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal.rendering;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.language.LanguageEntity;
import com.enonic.cms.core.portal.PageRequestType;
import com.enonic.cms.core.portal.VerticalSession;
import com.enonic.cms.core.portal.datasource.executor.DataSourceInvocationCache;
import com.enonic.cms.core.preview.PreviewContext;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.structure.SiteEntity;
import com.enonic.cms.core.structure.SitePath;
import com.enonic.cms.core.structure.menuitem.MenuItemEntity;
import com.enonic.cms.core.structure.page.Regions;
import com.enonic.cms.core.structure.page.template.PageTemplateEntity;

/**
 * Apr 21, 2009
 */
public class WindowRendererContext
{
    private PageRequestType pageRequestType;

    private String originalUrl;

    private UserEntity renderer;

    private SiteEntity site;

    private MenuItemEntity menuItem;

    private ContentEntity contentFromRequest;

    private LanguageEntity language;

    private PageTemplateEntity pageTemplate;

    private Regions regionsInPage;

    private DataSourceInvocationCache invocationCache;

    private PreviewContext previewContext;

    private boolean forceNoCacheUsage = false;

    private boolean encodeURIs;

    private Locale locale;

    private String deviceClass;

    private SitePath sitePath;

    private SitePath originalSitePath;

    private VerticalSession verticalSession;

    private String profile;

    private HttpServletRequest httpRequest;

    private boolean isRenderedInline = true;

    private String ticketId;

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

    public SiteEntity getSite()
    {
        return site;
    }

    public MenuItemEntity getMenuItem()
    {
        return menuItem;
    }

    public ContentEntity getContentFromRequest()
    {
        return contentFromRequest;
    }

    public PageTemplateEntity getPageTemplate()
    {
        return pageTemplate;
    }

    public DataSourceInvocationCache getInvocationCache()
    {
        return invocationCache;
    }

    public PreviewContext getPreviewContext()
    {
        return previewContext;
    }

    public void setPreviewContext( PreviewContext previewContext )
    {
        this.previewContext = previewContext;
    }

    public boolean forceNoCacheUsage()
    {
        return forceNoCacheUsage;
    }

    public String getOriginalUrl()
    {
        return originalUrl;
    }

    public boolean isEncodeURIs()
    {
        return encodeURIs;
    }

    public Locale getLocale()
    {
        return locale;
    }

    public String getDeviceClass()
    {
        return deviceClass;
    }

    public void setRenderer( UserEntity renderer )
    {
        this.renderer = renderer;
    }

    public void setSite( SiteEntity site )
    {
        this.site = site;
    }

    public void setMenuItem( MenuItemEntity menuItem )
    {
        this.menuItem = menuItem;
    }

    public void setContentFromRequest( ContentEntity contentFromRequest )
    {
        this.contentFromRequest = contentFromRequest;
    }

    public void setPageTemplate( PageTemplateEntity pageTemplate )
    {
        this.pageTemplate = pageTemplate;
    }

    public void setInvocationCache( DataSourceInvocationCache invocationCache )
    {
        this.invocationCache = invocationCache;
    }

    public void setForceNoCacheUsage( boolean forceNoCacheUsage )
    {
        this.forceNoCacheUsage = forceNoCacheUsage;
    }

    public void setOriginalUrl( String originalUrl )
    {
        this.originalUrl = originalUrl;
    }

    public void setEncodeURIs( boolean encodeURIs )
    {
        this.encodeURIs = encodeURIs;
    }

    public void setLocale( Locale locale )
    {
        this.locale = locale;
    }

    public void setOriginalSitePath( SitePath originalSitePath )
    {
        this.originalSitePath = originalSitePath;
    }

    public SitePath getOriginalSitePath()
    {
        return originalSitePath;
    }

    public SitePath getSitePath()
    {
        return sitePath;
    }

    public void setSitePath( SitePath sitePath )
    {
        this.sitePath = sitePath;
    }

    public void setDeviceClass( String deviceClass )
    {
        this.deviceClass = deviceClass;
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

    public boolean isRenderedInline()
    {
        return isRenderedInline;
    }

    public void setRenderedInline( boolean renderedInline )
    {
        isRenderedInline = renderedInline;
    }

    public String getTicketId()
    {
        return ticketId;
    }

    public void setTicketId( String ticketId )
    {
        this.ticketId = ticketId;
    }

    public Boolean getOverridingSitePropertyCreateUrlAsPath()
    {
        return overridingSitePropertyCreateUrlAsPath;
    }

    public void setOverridingSitePropertyCreateUrlAsPath( final Boolean value )
    {
        this.overridingSitePropertyCreateUrlAsPath = value;
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

