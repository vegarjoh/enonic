/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.vertical.adminweb;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import com.enonic.esl.containers.ExtendedMap;
import com.enonic.esl.containers.MultiValueMap;
import com.enonic.esl.net.URL;

import com.enonic.cms.framework.util.MimeTypeResolver;

import com.enonic.cms.core.content.ContentParserService;
import com.enonic.cms.core.content.ContentService;
import com.enonic.cms.core.content.category.CategoryService;
import com.enonic.cms.core.content.imports.ImportJobFactory;
import com.enonic.cms.core.content.imports.ImportService;
import com.enonic.cms.core.country.CountryService;
import com.enonic.cms.core.locale.LocaleService;
import com.enonic.cms.core.log.LogService;
import com.enonic.cms.core.mail.SendMailService;
import com.enonic.cms.core.portal.cache.PageCacheService;
import com.enonic.cms.core.portal.rendering.PageRendererFactory;
import com.enonic.cms.core.preview.PreviewService;
import com.enonic.cms.core.resolver.deviceclass.DeviceClassResolverService;
import com.enonic.cms.core.resolver.locale.LocaleResolverService;
import com.enonic.cms.core.resource.ResourceService;
import com.enonic.cms.core.resource.access.ResourceAccessResolver;
import com.enonic.cms.core.search.query.ContentIndexService;
import com.enonic.cms.core.security.AdminConsoleLoginAccessResolver;
import com.enonic.cms.core.security.SecurityService;
import com.enonic.cms.core.security.userstore.MemberOfResolver;
import com.enonic.cms.core.security.userstore.UserStoreService;
import com.enonic.cms.core.security.userstore.connector.synchronize.SynchronizeUserStoreJobFactory;
import com.enonic.cms.core.service.AdminService;
import com.enonic.cms.core.service.KeyService;
import com.enonic.cms.core.structure.SitePropertiesService;
import com.enonic.cms.core.structure.SiteService;
import com.enonic.cms.core.structure.menuitem.MenuItemService;
import com.enonic.cms.core.structure.page.template.PageTemplateService;
import com.enonic.cms.core.time.TimeService;
import com.enonic.cms.core.timezone.TimeZoneService;
import com.enonic.cms.core.xslt.admin.AdminXsltProcessorFactory;
import com.enonic.cms.store.dao.CategoryDao;
import com.enonic.cms.store.dao.ContentDao;
import com.enonic.cms.store.dao.ContentHandlerDao;
import com.enonic.cms.store.dao.ContentTypeDao;
import com.enonic.cms.store.dao.ContentVersionDao;
import com.enonic.cms.store.dao.GroupDao;
import com.enonic.cms.store.dao.LanguageDao;
import com.enonic.cms.store.dao.MenuItemDao;
import com.enonic.cms.store.dao.PageTemplateDao;
import com.enonic.cms.store.dao.PortletDao;
import com.enonic.cms.store.dao.SiteDao;
import com.enonic.cms.store.dao.UnitDao;
import com.enonic.cms.store.dao.UserDao;
import com.enonic.cms.store.dao.UserStoreDao;
import com.enonic.cms.upgrade.UpgradeService;

public abstract class AbstractAdminwebServlet
    implements Controller, ServletContextAware, ApplicationContextAware
{
    @Autowired
    protected CategoryDao categoryDao;

    @Autowired
    protected ContentDao contentDao;

    @Autowired
    protected ContentIndexService contentIndexService;

    @Autowired
    protected ContentHandlerDao contentHandlerDao;

    @Autowired
    protected ContentTypeDao contentTypeDao;

    @Autowired
    protected ContentVersionDao contentVersionDao;

    @Autowired
    protected GroupDao groupDao;

    protected LanguageDao languageDao;

    @Autowired
    protected MenuItemDao menuItemDao;

    @Autowired
    protected PageTemplateDao pageTemplateDao;

    @Autowired
    protected PortletDao portletDao;

    @Autowired
    protected SiteDao siteDao;

    @Autowired
    protected UnitDao unitDao;

    @Autowired
    protected UserDao userDao;

    protected UserStoreDao userStoreDao;

    // Services:

    @Autowired
    protected AdminService adminService;

    @Autowired
    protected ContentService contentService;

    @Autowired
    protected ContentParserService contentParserService;

    @Autowired
    protected CountryService countryService;

    @Autowired
    protected DeviceClassResolverService deviceClassResolverService;

    @Autowired
    protected ImportService importService;

    @Autowired
    protected KeyService keyService;

    @Autowired
    protected LocaleService localeService;

    @Autowired
    protected LocaleResolverService localeResolverService;

    @Autowired
    protected LogService logService;

    @Autowired
    protected MenuItemService menuItemService;

    @Autowired
    protected ResourceService resourceService;

    protected SecurityService securityService;

    @Autowired
    protected CategoryService categoryService;

    @Autowired
    protected PageTemplateService pageTemplateService;

    @Autowired
    protected SendMailService sendMailService;

    @Autowired
    protected SiteService siteService;

    @Autowired
    protected PageCacheService pageCacheService;

    @Autowired
    protected SitePropertiesService sitePropertiesService;

    @Autowired
    protected TimeService timeService;

    @Autowired
    protected PreviewService previewService;

    @Autowired
    protected TimeZoneService timeZoneService;

    @Autowired
    protected UpgradeService upgradeService;

    protected UserStoreService userStoreService;

    // Factories:

    @Autowired
    protected ImportJobFactory importJobFactory;

    @Autowired
    protected PageRendererFactory pageRendererFactory;

    @Autowired
    protected SynchronizeUserStoreJobFactory synchronizeUserStoreJobFactory;

    // Resolvers:

    @Autowired
    protected AdminConsoleLoginAccessResolver adminConsoleLoginAccessResolver;

    protected MemberOfResolver memberOfResolver;

    @Autowired
    protected ResourceAccessResolver resourceAccessResolver;

    @Autowired
    protected MimeTypeResolver mimeTypeResolver;

    protected AdminXsltProcessorFactory xsltProcessorFactory;

    private ServletContext servletContext;

    public final ServletContext getServletContext()
    {
        return this.servletContext;
    }

    public final void setServletContext( final ServletContext servletContext )
    {
        this.servletContext = servletContext;
    }

    protected ApplicationContext applicationContext;

    public void setApplicationContext( ApplicationContext applicationContext )
        throws BeansException
    {
        this.applicationContext = applicationContext;
    }

    public ModelAndView handleRequest( final HttpServletRequest request, final HttpServletResponse response )
        throws Exception
    {
        if ( request.getMethod().equalsIgnoreCase( "GET" ) )
        {
            doGet( request, response );
        }
        else if ( request.getMethod().equalsIgnoreCase( "POST" ) )
        {
            doPost( request, response );
        }
        else
        {
            response.sendError( HttpServletResponse.SC_METHOD_NOT_ALLOWED );
        }

        return null;
    }

    protected void doGet( final HttpServletRequest request, final HttpServletResponse response )
        throws Exception
    {
        response.sendError( HttpServletResponse.SC_METHOD_NOT_ALLOWED );
    }

    protected void doPost( final HttpServletRequest request, final HttpServletResponse response )
        throws Exception
    {
        response.sendError( HttpServletResponse.SC_METHOD_NOT_ALLOWED );
    }

    protected AdminService lookupAdminBean()
    {
        return adminService;
    }

    protected boolean isRequestForAdminPath( String path, HttpServletRequest request )
    {
        if ( path == null )
        {
            throw new NullPointerException( path );
        }
        if ( !path.startsWith( "/" ) )
        {
            throw new IllegalArgumentException( "Expected a path that starts with a forward slash" );
        }

        return request.getRequestURI().endsWith( path );
    }

    protected void redirectClientToAdminPath( String adminPath, HttpServletRequest request, HttpServletResponse response )
        throws VerticalAdminException
    {
        redirectClientToAdminPath( adminPath, (MultiValueMap) null, request, response );
    }

    protected void redirectClientToAdminPath( String adminPath, ExtendedMap formItems, HttpServletRequest request,
                                              HttpServletResponse response )
        throws VerticalAdminException
    {
        MultiValueMap mv = null;
        if ( formItems != null )
        {
            mv = new MultiValueMap( formItems );
        }
        redirectClientToAdminPath( adminPath, mv, request, response );
    }

    protected void redirectClientToAdminPath( String adminPath, String parameterName, String parameterValue, HttpServletRequest request,
                                              HttpServletResponse response )
        throws VerticalAdminException
    {
        MultiValueMap queryParams = new MultiValueMap();
        queryParams.put( parameterName, parameterValue );

        redirectClientToAdminPath( adminPath, queryParams, request, response );
    }

    protected void redirectClientToAdminPath( String adminPath, MultiValueMap queryParams, HttpServletRequest request,
                                              HttpServletResponse response )
        throws VerticalAdminException
    {
        AdminHelper.redirectClientToAdminPath( request, response, adminPath, queryParams );
    }

    protected void redirectClientToReferer( HttpServletRequest request, HttpServletResponse response )
    {
        AdminHelper.redirectClientToReferer( request, response );
    }

    protected void redirectClientToURL( URL url, HttpServletResponse response )
    {
        AdminHelper.redirectToURL( url, response );
    }

    protected void redirectClientToAbsoluteUrl( String url, HttpServletResponse response )
    {
        AdminHelper.redirectClientToAbsoluteUrl( url, response );
    }


    protected void forwardRequest( String adminPath, HttpServletRequest request, HttpServletResponse response )
        throws VerticalAdminException
    {
        int length = adminPath != null ? adminPath.length() : 0;
        length += "/admin".length();

        StringBuffer newUrl = new StringBuffer( length );
        newUrl.append( "/admin" );
        newUrl.append( adminPath );

        try
        {
            RequestDispatcher dispatcher = request.getRequestDispatcher( newUrl.toString() );
            dispatcher.forward( request, response );
        }
        catch ( IOException ioe )
        {
            String message = "Failed to forward request to \"{0}\": %t";
            VerticalAdminLogger.errorAdmin( message, adminPath, ioe );
        }
        catch ( ServletException se )
        {
            String message = "Failed to forward request to \"{0}\": %t";
            VerticalAdminLogger.errorAdmin( message, adminPath, se );
        }
    }

    @Autowired
    public void setUserStoreDao( UserStoreDao value )
    {
        this.userStoreDao = value;
    }

    @Autowired
    public void setSecurityService( SecurityService value )
    {
        this.securityService = value;
    }

    @Autowired
    public void setUserStoreService( UserStoreService value )
    {
        this.userStoreService = value;
    }

    @Autowired
    public void setMemberOfResolver( MemberOfResolver value )
    {
        this.memberOfResolver = value;
    }

    public AdminXsltProcessorFactory getXsltProcessorFactory()
    {
        return xsltProcessorFactory;
    }

    @Autowired
    public void setXsltProcessorFactory( final AdminXsltProcessorFactory xsltProcessorFactory )
    {
        this.xsltProcessorFactory = xsltProcessorFactory;
    }
}
