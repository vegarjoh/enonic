/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal.processor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.enonic.cms.core.portal.PageRequestType;
import com.enonic.cms.core.portal.PortalAccessService;
import com.enonic.cms.core.resolver.deviceclass.DeviceClassResolverService;
import com.enonic.cms.core.resolver.locale.LocaleResolverService;
import com.enonic.cms.core.structure.SitePropertiesService;
import com.enonic.cms.store.dao.ContentDao;
import com.enonic.cms.store.dao.PageTemplateDao;
import com.enonic.cms.store.dao.SectionContentDao;

/**
 * Sep 29, 2009
 */
@Component
public class PageRequestProcessorFactory
{
    @Autowired
    private ContentDao contentDao;

    @Autowired
    private SectionContentDao sectionContentDao;

    @Autowired
    private PageTemplateDao pageTemplateDao;

    @Autowired
    private PortalAccessService portalAccessService;

    @Autowired
    private LocaleResolverService localeResolverService;

    @Autowired
    private DeviceClassResolverService deviceClassResolverService;

    @Autowired
    private SitePropertiesService sitePropertiesService;

    public AbstractPageRequestProcessor create( PageRequestProcessorContext context )
    {
        final AbstractPageRequestProcessor pageRequestProcessor;

        final PageRequestType pageRequestType = context.getPageRequestType();

        if ( PageRequestType.CONTENT.equals( pageRequestType ) )
        {
            pageRequestProcessor = new ContentRequestProcessor( context );

        }
        else if ( PageRequestType.MENUITEM.equals( pageRequestType ) )
        {
            pageRequestProcessor = new PageRequestProcessor( context );
        }
        else
        {
            throw new IllegalArgumentException( "PageRequestType not supported: " + pageRequestType );
        }

        pageRequestProcessor.setContentDao( contentDao );
        pageRequestProcessor.setPageTemplateDao( pageTemplateDao );
        pageRequestProcessor.setPortalAccessService( portalAccessService );
        pageRequestProcessor.setDeviceClassResolverService( deviceClassResolverService );
        pageRequestProcessor.setLocaleResolverService( localeResolverService );
        pageRequestProcessor.setSectionContentDao( sectionContentDao );
        pageRequestProcessor.setSitePropertiesService( sitePropertiesService );

        return pageRequestProcessor;
    }
}
