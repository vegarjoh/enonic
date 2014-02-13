/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.server.service.admin.mvc.controller;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.util.UrlPathHelper;

import com.enonic.cms.framework.blob.BlobRecord;
import com.enonic.cms.framework.util.HttpServletRangeUtil;
import com.enonic.cms.framework.util.HttpServletUtil;

import com.enonic.cms.core.Path;
import com.enonic.cms.core.PathAndParams;
import com.enonic.cms.core.RequestParameters;
import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.ContentVersionEntity;
import com.enonic.cms.core.content.ContentVersionKey;
import com.enonic.cms.core.content.access.ContentAccessResolver;
import com.enonic.cms.core.content.binary.AttachmentNotFoundException;
import com.enonic.cms.core.content.binary.AttachmentRequest;
import com.enonic.cms.core.content.binary.AttachmentRequestResolver;
import com.enonic.cms.core.content.binary.BinaryDataEntity;
import com.enonic.cms.core.content.binary.BinaryDataKey;
import com.enonic.cms.core.content.binary.ContentBinaryDataEntity;
import com.enonic.cms.core.content.binary.InvalidBinaryPathException;
import com.enonic.cms.core.security.SecurityService;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.store.dao.BinaryDataDao;
import com.enonic.cms.store.dao.ContentDao;
import com.enonic.cms.store.dao.GroupDao;

@Controller("adminAttachmentController")
public class AttachmentController
    extends AbstractController
    implements InitializingBean
{
    private static final Logger LOG = LoggerFactory.getLogger( AttachmentController.class );

    private BinaryDataDao binaryDataDao;

    private ContentDao contentDao;

    private GroupDao groupDao;

    private SecurityService securityService;

    private AttachmentRequestResolver attachmentRequestResolver;

    private UrlPathHelper urlEncodingUrlPathHelper;


    public void afterPropertiesSet()
        throws Exception
    {
        this.urlEncodingUrlPathHelper = new UrlPathHelper();
        this.urlEncodingUrlPathHelper.setUrlDecode( true );

        attachmentRequestResolver = new AttachmentRequestResolver()
        {
            @Override
            protected BinaryDataKey getBinaryData( ContentEntity content, String label )
            {
                BinaryDataEntity binaryData;
                if ( label == null )
                {
                    binaryData = content.getMainVersion().getOneAndOnlyBinaryData();
                }
                else
                {
                    binaryData = content.getMainVersion().getBinaryData( label );
                }

                if ( "source".equals( label ) && binaryData == null )
                {
                    binaryData = content.getMainVersion().getOneAndOnlyBinaryData();
                }

                if ( binaryData != null )
                {
                    return new BinaryDataKey( binaryData.getKey() );
                }
                return null;
            }

            @Override
            protected ContentEntity getContent( ContentKey contentKey )
            {
                return contentDao.findByKey( contentKey );
            }
        };
    }

    public final ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        final UserEntity loggedInUser = securityService.getLoggedInAdminConsoleUserAsEntity();

        try
        {
            final PathAndParams pathAndParams = resolvePathAndParams( request );
            final AttachmentRequest attachmentRequest = attachmentRequestResolver.resolveBinaryDataKey( pathAndParams );

            final ContentEntity content = resolveContent( attachmentRequest, pathAndParams );
            checkContentAccess( loggedInUser, content, pathAndParams );

            final ContentVersionEntity contentVersion = resolveContentVersion( content, request, pathAndParams );
            final ContentBinaryDataEntity contentBinaryData = resolveContentBinaryData( contentVersion, attachmentRequest, pathAndParams );
            final BinaryDataEntity binaryData = contentBinaryData.getBinaryData();

            boolean download = "true".equals( request.getParameter( "download" ) );
            download = download || "true".equals( request.getParameter( "_download" ) );

            final BlobRecord blob = binaryDataDao.getBlob( binaryData.getBinaryDataKey() );

            if ( blob == null )
            {
                throw AttachmentNotFoundException.notFound( binaryData.getBinaryDataKey() );
            }

            putBinaryOnResponse( download, request, response, binaryData, blob );
            return null;
        }
        catch ( InvalidBinaryPathException e )
        {
            LOG.warn( e.getMessage() );
            response.sendError( HttpServletResponse.SC_NOT_FOUND );
        }
        catch ( AttachmentNotFoundException e )
        {
            LOG.warn( e.getMessage() );
            response.sendError( HttpServletResponse.SC_NOT_FOUND );
        }
        return null;
    }

    private PathAndParams resolvePathAndParams( HttpServletRequest request )
    {
        @SuppressWarnings({"unchecked"}) Map<String, String[]> parameterMap = request.getParameterMap();

        RequestParameters requestParameters = new RequestParameters( parameterMap );
        String pathAsString = urlEncodingUrlPathHelper.getRequestUri( request );
        Path path = new Path( pathAsString );

        return new PathAndParams( path, requestParameters );
    }

    private void putBinaryOnResponse( boolean download, final HttpServletRequest request, HttpServletResponse response, BinaryDataEntity binaryData, BlobRecord blob )
        throws IOException
    {
        final File file = blob.getAsFile();

        final String mimeType = HttpServletUtil.resolveMimeType( getServletContext(), binaryData.getName() );

        if ( file != null )
        {
            HttpServletRangeUtil.processRequest( request, response, binaryData.getName(), mimeType, file );
        }
        else
        {
            HttpServletUtil.setContentDisposition( response, download, binaryData.getName() );

            response.setContentType( mimeType );
            response.setContentLength( (int) blob.getLength() );

            HttpServletUtil.copyNoCloseOut( blob.getStream(), response.getOutputStream() );
        }

    }

    private ContentEntity resolveContent( AttachmentRequest attachmentRequest, PathAndParams pathAndParams )
    {
        final ContentEntity content = contentDao.findByKey( attachmentRequest.getContentKey() );
        if ( content == null || content.isDeleted() )
        {
            throw AttachmentNotFoundException.notFound( pathAndParams.getPath().toString() );
        }
        return content;
    }

    private ContentVersionEntity resolveContentVersion( ContentEntity content, HttpServletRequest request, PathAndParams pathAndParams )
    {
        String versionParam = request.getParameter( "_version" );
        if ( StringUtils.isNotBlank( versionParam ) )
        {
            ContentVersionEntity contentVersion = content.getVersion( new ContentVersionKey( versionParam ) );
            if ( contentVersion == null )
            {
                throw AttachmentNotFoundException.notFound( pathAndParams.getPath().toString() );
            }
            return contentVersion;
        }

        return content.getMainVersion();
    }

    private ContentBinaryDataEntity resolveContentBinaryData( ContentVersionEntity contentVersion, AttachmentRequest attachmentRequest,
                                                              PathAndParams pathAndParams )
    {
        final ContentBinaryDataEntity contentBinaryData = contentVersion.getContentBinaryData( attachmentRequest.getBinaryDataKey() );
        if ( contentBinaryData == null )
        {
            throw AttachmentNotFoundException.notFound( pathAndParams.getPath().toString() );
        }
        return contentBinaryData;
    }

    private void checkContentAccess( UserEntity loggedInUser, ContentEntity content, PathAndParams pathAndParams )
    {
        if ( !new ContentAccessResolver( groupDao ).hasReadContentAccess( loggedInUser, content ) )
        {
            throw AttachmentNotFoundException.notFound( pathAndParams.getPath().toString() );
        }
    }

    @Autowired
    public void setBinaryDataDao( BinaryDataDao binaryDataDao )
    {
        this.binaryDataDao = binaryDataDao;
    }

    @Autowired
    public void setContentDao( ContentDao dao )
    {
        contentDao = dao;
    }

    @Autowired
    public void setSecurityService( SecurityService value )
    {
        this.securityService = value;
    }

    @Autowired
    public void setGroupDao( GroupDao groupDao )
    {
        this.groupDao = groupDao;
    }
}