/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal.processor;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.springframework.util.Assert;

import com.enonic.cms.core.language.LanguageEntity;
import com.enonic.cms.core.portal.rendering.RegionsResolver;
import com.enonic.cms.core.resolver.ResolverContext;
import com.enonic.cms.core.resolver.deviceclass.DeviceClassResolverService;
import com.enonic.cms.core.resolver.locale.LocaleResolverService;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.structure.SiteEntity;
import com.enonic.cms.core.structure.SitePath;
import com.enonic.cms.core.structure.SitePropertiesService;
import com.enonic.cms.core.structure.menuitem.MenuItemEntity;
import com.enonic.cms.core.structure.page.Regions;
import com.enonic.cms.core.structure.page.template.PageTemplateEntity;
import com.enonic.cms.store.dao.ContentDao;
import com.enonic.cms.store.dao.PageTemplateDao;
import com.enonic.cms.store.dao.SectionContentDao;

/**
 * Sep 28, 2009
 */
public abstract class AbstractPageRequestProcessor
    extends AbstractBasePortalRequestProcessor
{
    protected PageRequestProcessorContext context;

    protected ContentDao contentDao;

    protected PageTemplateDao pageTemplateDao;

    protected LocaleResolverService localeResolverService;

    protected DeviceClassResolverService deviceClassResolverService;

    protected SectionContentDao sectionContentDao;

    protected SitePropertiesService sitePropertiesService;

    protected AbstractPageRequestProcessor( final PageRequestProcessorContext context )
    {
        this.context = context;
    }

    public abstract PageRequestProcessorResult process();

    protected void processCommonRequest( final PageRequestProcessorResult result )
    {
        final LanguageEntity resolvedLanguage = result.getLanguage();
        Assert.notNull( resolvedLanguage != null, "expected language to already be set on result" );

        final PageTemplateEntity resolvedPageTemplate = result.getPageTemplate();
        Assert.notNull( resolvedPageTemplate, "expected page template to already be set on result" );

        final SitePath sitePath = result.getSitePath();
        Assert.notNull( sitePath != null, "expected site path to already be set on result" );

        final UserEntity requester = context.getRequester();
        final MenuItemEntity menuItem = context.getMenuItem();
        final SiteEntity site = context.getSite();

        // run-as-user 
        UserEntity runAsUser = menuItem.resolveRunAsUser( requester, true );
        if ( runAsUser == null )
        {
            runAsUser = requester;
        }
        result.setRunAsUser( runAsUser );

        // http request
        HttpServletRequest httpRequest = context.getHttpRequest();
        if ( result.getHttpRequest() != null )
        {
            httpRequest = result.getHttpRequest();
        }

        ResolverContext resolverContext = new ResolverContext( httpRequest, site, menuItem, resolvedLanguage );
        resolverContext.setUser( requester );

        // locale
        final Locale locale = localeResolverService.getLocale( resolverContext );
        result.setLocale( locale );

        // device class
        final String deviceClass = deviceClassResolverService.getDeviceClass( resolverContext );
        result.setDeviceClass( deviceClass );

        // regions in page
        final Regions regionsInPage =
            RegionsResolver.resolveRegionsForPageRequest( menuItem, resolvedPageTemplate, context.getPageRequestType() );
        result.setRegionsInPage( regionsInPage );

    }

    public void setContentDao( ContentDao value )
    {
        this.contentDao = value;
    }

    public void setPageTemplateDao( PageTemplateDao value )
    {
        this.pageTemplateDao = value;
    }

    public void setLocaleResolverService( LocaleResolverService value )
    {
        this.localeResolverService = value;
    }

    public void setDeviceClassResolverService( DeviceClassResolverService value )
    {
        this.deviceClassResolverService = value;
    }

    public void setSectionContentDao( SectionContentDao sectionContentDao )
    {
        this.sectionContentDao = sectionContentDao;
    }

    public void setSitePropertiesService( final SitePropertiesService sitePropertiesService )
    {
        this.sitePropertiesService = sitePropertiesService;
    }
}
