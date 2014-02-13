/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal.rendering.portalfunctions;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.enonic.cms.core.SiteURLResolver;
import com.enonic.cms.core.captcha.CaptchaService;
import com.enonic.cms.core.localization.LocalizationService;
import com.enonic.cms.core.portal.image.ImageService;
import com.enonic.cms.core.portal.livetrace.LivePortalTraceService;
import com.enonic.cms.core.resolver.locale.LocaleResolverService;
import com.enonic.cms.core.security.SecurityService;
import com.enonic.cms.core.servlet.ServletRequestAccessor;
import com.enonic.cms.core.structure.SitePropertiesService;
import com.enonic.cms.store.dao.ContentBinaryDataDao;
import com.enonic.cms.store.dao.ContentDao;
import com.enonic.cms.store.dao.MenuItemDao;
import com.enonic.cms.store.dao.PortletDao;

@Component
public class PortalFunctionsFactory
{

    private static PortalFunctionsFactory instance;

    private SiteURLResolver siteURLResolver;

    private ContentDao contentDao;

    @Autowired
    private MenuItemDao menuItemDao;

    @Autowired
    private PortletDao portletDao;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private LocalizationService localizeService;

    @Autowired
    private LocaleResolverService localeResolverService;

    @Autowired
    private ContentBinaryDataDao contentBinaryDataDao;

    @Autowired
    private ImageService imageService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private CreateAttachmentUrlFunction createAttachmentUrlFunction;

    @Autowired
    private IsWindowEmptyFunction isWindowEmptyFunction;

    @Autowired
    private LivePortalTraceService livePortalTraceService;

    private SitePropertiesService sitePropertiesService;

    private final ThreadLocal<PortalFunctionsContext> context = new ThreadLocal<PortalFunctionsContext>();

    public static PortalFunctionsFactory get()
    {
        return instance;
    }

    public PortalFunctionsFactory()
    {
        instance = this;
    }

    public void setContext( PortalFunctionsContext value )
    {
        context.set( value );
    }

    public PortalFunctionsContext getContext()
    {
        return context.get();
    }

    public void removeContext()
    {
        context.remove();
    }

    public PortalFunctions createPortalFunctions()
    {
        HttpServletRequest httpRequest = ServletRequestAccessor.getRequest();

        PortalFunctions portalFunctions = new PortalFunctions();
        if ( getContext().getSiteURLResolver() != null )
        {
            portalFunctions.setSiteURLResolver( getContext().getSiteURLResolver() );
        }
        else
        {
            portalFunctions.setSiteURLResolver( siteURLResolver );
        }
        portalFunctions.setCaptchaService( captchaService );
        portalFunctions.setContentBinaryDataDao( contentBinaryDataDao );
        portalFunctions.setContentDao( contentDao );
        portalFunctions.setContext( getContext() );
        portalFunctions.setCreateAttachmentUrlFunction( createAttachmentUrlFunction );
        portalFunctions.setEncodeURIs( getContext().isEncodeURIs() );
        portalFunctions.setImageService( imageService );
        portalFunctions.setIsWindowEmptyFunction( isWindowEmptyFunction );
        portalFunctions.setLivePortalTraceService( livePortalTraceService );
        portalFunctions.setLocaleResolvingService( localeResolverService );
        portalFunctions.setLocalizeService( localizeService );
        portalFunctions.setMenuItemDao( menuItemDao );
        portalFunctions.setPortletDao( portletDao );
        portalFunctions.setRequest( httpRequest );
        portalFunctions.setSecurityService( securityService );
        portalFunctions.setSitePropertiesService( sitePropertiesService );

        return portalFunctions;
    }


    @Autowired
    public void setSiteURLResolver( SiteURLResolver value )
    {
        this.siteURLResolver = value;
    }

    @Autowired
    public void setContentDao( ContentDao contentDao )
    {
        this.contentDao = contentDao;
    }

    @Autowired
    public void setSitePropertiesService( SitePropertiesService sitePropertiesService )
    {
        this.sitePropertiesService = sitePropertiesService;
    }

}
