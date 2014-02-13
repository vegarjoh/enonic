/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal.processor;

import com.enonic.esl.servlet.http.HttpServletRequestWrapper;

import com.enonic.cms.core.language.LanguageEntity;
import com.enonic.cms.core.language.LanguageResolver;
import com.enonic.cms.core.portal.PageTemplateNotFoundException;
import com.enonic.cms.core.structure.SitePath;
import com.enonic.cms.core.structure.menuitem.MenuItemEntity;
import com.enonic.cms.core.structure.page.PageEntity;
import com.enonic.cms.core.structure.page.template.PageTemplateEntity;


public class PageRequestProcessor
    extends AbstractPageRequestProcessor
{
    public PageRequestProcessor( final PageRequestProcessorContext context )
    {
        super( context );
    }

    public PageRequestProcessorResult process()
    {
        final PageRequestProcessorResult result = new PageRequestProcessorResult();

        final SitePath sitePath = context.getSitePath();
        final MenuItemEntity menuItem = context.getMenuItem();

        portalAccessService.checkAccessToPage( menuItem, sitePath, context.getRequester() );

        // page template
        PageTemplateEntity pageTemplate = resolvePageTemplate();
        result.setPageTemplate( pageTemplate );

        // language
        final LanguageEntity language = resolveLanguage();
        result.setLanguage( language );

        // site path
        sitePath.addParam( "id", menuItem.getKey().toString() );
        result.setSitePath( sitePath );

        // http request
        HttpServletRequestWrapper requestWrapper = new HttpServletRequestWrapper( context.getHttpRequest() );
        // noinspection deprecation
        requestWrapper.setParamsMasked( false );
        requestWrapper.setParameter( "id", menuItem.getKey().toString() );
        result.setHttpRequest( requestWrapper );

        processCommonRequest( result );

        return result;
    }

    private PageTemplateEntity resolvePageTemplate()
    {
        final PageEntity page = context.getMenuItem().getPage();

        if ( page == null || page.getTemplate() == null )
        {
            throw new PageTemplateNotFoundException( context.getSitePath() );
        }

        return page.getTemplate();
    }

    private LanguageEntity resolveLanguage()
    {
        if ( context.getOverridingLanguage() != null )
        {
            return context.getOverridingLanguage();
        }
        else
        {
            return LanguageResolver.resolve( context.getSite(), context.getMenuItem() );
        }
    }
}
