/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal.processor;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Preconditions;

import com.enonic.esl.servlet.http.HttpServletRequestWrapper;

import com.enonic.cms.core.Path;
import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentLocationSpecification;
import com.enonic.cms.core.content.ContentLocations;
import com.enonic.cms.core.language.LanguageEntity;
import com.enonic.cms.core.language.LanguageResolver;
import com.enonic.cms.core.portal.ContentNameMismatchException;
import com.enonic.cms.core.portal.ContentNotFoundException;
import com.enonic.cms.core.portal.ContentPath;
import com.enonic.cms.core.portal.PageRequestType;
import com.enonic.cms.core.portal.PageTemplateNotFoundException;
import com.enonic.cms.core.portal.PathToContentResolver;
import com.enonic.cms.core.portal.rendering.PageTemplateResolver;
import com.enonic.cms.core.preview.PreviewContext;
import com.enonic.cms.core.structure.SiteKey;
import com.enonic.cms.core.structure.SitePath;
import com.enonic.cms.core.structure.SitePropertyNames;
import com.enonic.cms.core.structure.menuitem.MenuItemEntity;
import com.enonic.cms.core.structure.page.template.PageTemplateEntity;

/**
 * Sep 28, 2009
 */
public class ContentRequestProcessor
    extends AbstractPageRequestProcessor
{
    public ContentRequestProcessor( final PageRequestProcessorContext context )
    {
        super( context );
    }

    public PageRequestProcessorResult process()
    {
        Preconditions.checkArgument( context.getPageRequestType() == PageRequestType.CONTENT );
        Preconditions.checkNotNull( context.getContentPath() );

        final PageRequestProcessorResult result = new PageRequestProcessorResult();

        final SitePath sitePath = context.getSitePath();

        // content from request
        final ContentEntity contentFromRequest = resolveContentFromRequest( context.getContentPath() );
        result.setContentFromRequest( contentFromRequest );

        final ContentPath contentPath = sitePath.getContentPath();
        final boolean oldStyleContentPathToBeRedirected = contentPath.isOldStyleContentPath();

        if ( oldStyleContentPathToBeRedirected )
        {
            return setRedirectForOldStyleContentPaths( result, sitePath, contentFromRequest );
        }

        final MenuItemEntity menuItemWithRequestedContent =
            contentFromRequest.getFirstDirectPlacementOnMenuItem( context.getSite().getKey() );
        if ( menuItemWithRequestedContent != null )
        {
            return resolveResultForRedirectToMenuItemWithRequestedContent( sitePath, menuItemWithRequestedContent );
        }

        if ( !contentNamesMatch( contentFromRequest, contentPath ) )
        {
            checkContentIsOnline( contentFromRequest );
            portalAccessService.checkAccessToContent( sitePath, context.getRequester(), contentFromRequest, context.getMenuItem() );

            throw new ContentNameMismatchException( contentPath.getContentKey(), contentPath.getContentName() );
        }

        if ( contentPath.isPermaLink() )
        {
            SiteKey siteKey = sitePath.getSiteKey();
            if ( !sitePropertiesService.getSiteProperties( siteKey ).getPropertyAsBoolean(
                SitePropertyNames.ENABLE_UNPUBLISHED_CONTENT_PERMALINKS ) )
            {
                ContentLocationSpecification contentLocationSpecification = new ContentLocationSpecification();
                contentLocationSpecification.setSiteKey( siteKey );
                ContentLocations contentLocations = contentFromRequest.getLocations( contentLocationSpecification );
                if ( !contentLocations.hasLocations() )
                {
                    throw new ContentNotFoundException( contentFromRequest.getKey(), contentFromRequest.getMainVersion().getTitle() );
                }
            }

            SitePath redirectSitePath = getRedirectSitePathForPermaLink( sitePath, contentFromRequest );

            if ( redirectSitePath != null )
            {
                PageRequestProcessorResult redirectResult = new PageRequestProcessorResult();
                redirectResult.setRedirectToSitePath( redirectSitePath );
                return redirectResult;
            }
        }

        checkContentIsOnline( contentFromRequest );
        portalAccessService.checkAccessToContent( sitePath, context.getRequester(), contentFromRequest, context.getMenuItem() );

        // page template
        PageTemplateEntity pageTemplate = resolvePageTemplate( contentFromRequest );
        result.setPageTemplate( pageTemplate );

        // language
        final LanguageEntity language = resolveLanguage( contentFromRequest );
        result.setLanguage( language );

        // site path
        sitePath.setParam( "key", contentFromRequest.getKey().toString() );
        sitePath.addParam( "id", context.getMenuItem().getKey().toString() );
        result.setSitePath( sitePath );

        // http request
        HttpServletRequestWrapper requestWrapper = new HttpServletRequestWrapper( context.getHttpRequest() );
        // noinspection deprecation
        requestWrapper.setParamsMasked( false );
        requestWrapper.setParameter( "key", contentFromRequest.getKey().toString() );
        requestWrapper.setParameter( "id", context.getMenuItem().getKey().toString() );
        result.setHttpRequest( requestWrapper );

        // process common stuff
        processCommonRequest( result );

        return result;
    }

    private boolean contentNamesMatch( ContentEntity contentFromRequest, ContentPath contentPath )
    {
        return contentPath.getContentName().equalsIgnoreCase( contentFromRequest.getName() );
    }

    private PageRequestProcessorResult setRedirectForOldStyleContentPaths( PageRequestProcessorResult result, SitePath sitePath,
                                                                           ContentEntity contentFromRequest )
    {
        PathToContentResolver pathToContentResolver = new PathToContentResolver( sectionContentDao );
        final Path newStyleContentLocalPath = pathToContentResolver.resolveContentUrlLocalPath( contentFromRequest, sitePath.getSiteKey() );

        // We do not want any params persisted on the menu item in the redirect, therefore we fetch the original ones from the http request
        Map originalParameters = context.getHttpRequest().getParameterMap();

        result.setRedirectToSitePath( new SitePath( sitePath.getSiteKey(), newStyleContentLocalPath, originalParameters ) );
        return result;
    }

    private SitePath getRedirectSitePathForPermaLink( SitePath sitePath, ContentEntity contentFromRequest )
    {
        PathToContentResolver pathToContentResolver = new PathToContentResolver( sectionContentDao );

        Path newStyleContentLocalPath = pathToContentResolver.resolveContentUrlLocalPathForPermalink( contentFromRequest, sitePath );

        final boolean noRedirectToBeDone =
            redirectPathIsEqualToRequestedPath( newStyleContentLocalPath.getPathAsString(), sitePath.getLocalPath().getPathAsString() );

        //final boolean noRedirectToBeDone2 = new Path( newStyleContentLocalPath.getPathAsString(), true ).equals(
        //    new Path( sitePath.getLocalPath().getPathAsString(), true ) );

        if ( noRedirectToBeDone )
        {
            return null;
        }

        // We do not want any params persisted on the menu item in the redirect, therefore we fetch the original ones from the http request
        Map originalParameters = context.getHttpRequest().getParameterMap();

        return new SitePath( sitePath.getSiteKey(), newStyleContentLocalPath, originalParameters );
    }

    private boolean redirectPathIsEqualToRequestedPath( String newLocalPath, String requestedPath )
    {
        newLocalPath = StringUtils.removeStart( newLocalPath, "/" );
        requestedPath = StringUtils.removeStart( requestedPath, "/" );

        newLocalPath = StringUtils.removeEnd( newLocalPath, "/" );
        requestedPath = StringUtils.removeEnd( requestedPath, "/" );

        return newLocalPath.equalsIgnoreCase( requestedPath );
    }

    private ContentEntity resolveContentFromRequest( final ContentPath contentPath )
    {
        ContentEntity content = contentDao.findByKey( contentPath.getContentKey() );
        if ( content == null )
        {
            throw new ContentNotFoundException( contentPath.getContentKey() );
        }

        final PreviewContext previewContext = context.getPreviewContext();
        if ( previewContext.isPreviewingContent() && previewContext.getContentPreviewContext().isContentPreviewed( content ) )
        {
            content = previewContext.getContentPreviewContext().getContentPreviewed();
        }

        return content;
    }

    private void checkContentIsOnline( ContentEntity contentFromRequest )
    {
        boolean skipOnlineCheck = false;
        final PreviewContext previewContext = context.getPreviewContext();
        if ( previewContext.isPreviewingContent() )
        {
            skipOnlineCheck = previewContext.getContentPreviewContext().isContentPreviewed( contentFromRequest ) ||
                previewContext.getContentPreviewContext().treatContentAsAvailableEvenIfOffline( contentFromRequest.getKey() );
        }

        // check access if not processing content request to the content in preview
        if ( !skipOnlineCheck )
        {
            if ( !contentFromRequest.isOnline( context.getRequestTime() ) )
            {
                throw new ContentNotFoundException( contentFromRequest.getKey(), contentFromRequest.getMainVersion().getTitle() );
            }
        }
    }

    private PageTemplateEntity resolvePageTemplate( final ContentEntity contentFromRequest )
    {
        PageTemplateResolver pageTemplateResolver = new PageTemplateResolver( pageTemplateDao );
        final PageTemplateEntity pageTemplate = pageTemplateResolver.resolvePageTemplate( context.getSite(), contentFromRequest );

        if ( pageTemplate == null )
        {
            throw new PageTemplateNotFoundException( context.getSitePath() );
        }

        return pageTemplate;
    }

    private LanguageEntity resolveLanguage( ContentEntity contentFromRequest )
    {
        LanguageEntity language;
        if ( context.getOverridingLanguage() != null )
        {
            language = context.getOverridingLanguage();
        }
        else
        {
            language = LanguageResolver.resolve( contentFromRequest, context.getSite(), context.getMenuItem() );
        }
        return language;
    }

    private PageRequestProcessorResult resolveResultForRedirectToMenuItemWithRequestedContent( SitePath sitePath,
                                                                                               MenuItemEntity menuItemWithRequestedContent )
    {

        Map originalParameters = context.getHttpRequest().getParameterMap();

        SitePath sitePathToMenuItemWithRequestedContent =
            new SitePath( context.getSite().getKey(), menuItemWithRequestedContent.getPath(), originalParameters );

        // sitePathToMenuItemWithRequestedContent.removeParam( "key" );
        PageRequestProcessorResult redirectResult = new PageRequestProcessorResult();
        redirectResult.setRedirectToSitePath( sitePathToMenuItemWithRequestedContent );
        return redirectResult;
    }


}
