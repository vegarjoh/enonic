/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.content;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.enonic.vertical.adminweb.handlers.xmlbuilders.ContentBaseXMLBuilder;

import com.enonic.cms.framework.util.JDOMUtil;

import com.enonic.cms.core.content.access.ContentAccessEntity;
import com.enonic.cms.core.content.binary.BinaryDataKey;
import com.enonic.cms.core.content.category.CategoryEntity;
import com.enonic.cms.core.content.category.CategoryKey;
import com.enonic.cms.core.content.contentdata.ContentData;
import com.enonic.cms.core.content.contentdata.ContentDataParser;
import com.enonic.cms.core.content.contenttype.ContentTypeEntity;
import com.enonic.cms.core.language.LanguageEntity;
import com.enonic.cms.core.language.LanguageKey;
import com.enonic.cms.core.security.group.GroupEntity;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.security.user.UserKey;
import com.enonic.cms.store.dao.CategoryDao;
import com.enonic.cms.store.dao.ContentDao;
import com.enonic.cms.store.dao.ContentVersionDao;
import com.enonic.cms.store.dao.GroupDao;
import com.enonic.cms.store.dao.LanguageDao;
import com.enonic.cms.store.dao.UserDao;


public class ContentParser
{
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern( "yyyy-MM-dd HH:mm" );

    /**
     * Whether to parse contentdata from xml data or not. If false, contentdata is loaded from storage instead. Default is true.
     */
    private boolean parseContentData = true;

    private boolean parseContentCreatedAt = false;

    private ContentDao contentDao;

    private ContentVersionDao contentVersionDao;

    private CategoryDao categoryDao;

    private UserDao userDao;

    private GroupDao groupDao;

    private LanguageDao languageDao;

    private Element contentEl;

    private List<BinaryDataKey> binaryDatas;

    private static final String COMMENT_XML_KEY = "comment";

    public ContentParser( ContentDao contentDao, ContentVersionDao contentVersionDao, CategoryDao categoryDao, GroupDao groupDao,
                          UserDao userDao, LanguageDao languageDao, String contentXml )
    {
        this.contentDao = contentDao;
        this.contentVersionDao = contentVersionDao;
        this.userDao = userDao;
        this.categoryDao = categoryDao;
        this.groupDao = groupDao;
        this.languageDao = languageDao;

        if ( contentXml != null )
        {
            Document doc = getContentElement( contentXml );
            this.contentEl = doc.getRootElement();
            if ( !"content".equals( this.contentEl.getName() ) )
            {
                throw new IllegalArgumentException(
                    "Root element in content XML expected to be named 'content': " + this.contentEl.getName() );
            }
        }
    }

    public void setBinaryDatas( List<BinaryDataKey> list )
    {
        this.binaryDatas = list;
    }

    public void setParseContentData( boolean value )
    {
        this.parseContentData = value;
    }

    public void setParseContentCreatedAt( boolean parseContentCreatedAt )
    {
        this.parseContentCreatedAt = parseContentCreatedAt;
    }

    public ContentAndVersion parseContentAndVersion( Element contentEl )
    {
        this.contentEl = contentEl;
        return parseContentAndVersion();
    }

    public ContentAndVersion parseContentAndVersion()
    {
        ContentEntity content = parseContent();

        ContentVersionEntity version = parseContentVersion( content );

        return new ContentAndVersion( content, version );
    }

    private ContentEntity parseContent()
    {
        ContentEntity content = new ContentEntity();
        content.setName( parseContentName() );
        content.setKey( parseContentKey() );
        content.setAvailableFrom( parsePublishFrom() );
        content.setAvailableTo( parsePublishTo() );
        content.setPriority( parsePriority() );

        CategoryEntity category = parseCategory();
        if ( category == null )
        {
            throw new IllegalArgumentException( "Given contentXml did not define category" );
        }
        content.setCategory( category );

        LanguageEntity language = parseLanguage();
        if ( language == null )
        {
            // set language to category's language if not given in xml
            language = category.getLanguage();
        }
        content.setLanguage( language );

        content.setOwner( parseOwner() );

        content.setAssignee( parseAssignee() );
        content.setAssignmentDueDate( parseDueDate() );
        content.setAssignmentDescription( parseAssignmentComment() );
        content.setAssigner( parseAssigner() );

        content.addContentAccessRights( parseContentAccessRights() );

        if ( parseContentCreatedAt )
        {
            content.setCreatedAt( parseCreatedAt() );
        }

        return content;
    }

    private String parseAssignmentComment()
    {
        Element assignmentCommentElement = contentEl.getChild( ContentBaseXMLBuilder.ASSIGNMENT_DESCRIPTION_XML_KEY );
        if ( assignmentCommentElement == null )
        {
            return null;
        }
        String assignmentComment = assignmentCommentElement.getValue();
        if ( StringUtils.isBlank( assignmentComment ) )
        {
            return "";
        }

        return assignmentComment;
    }

    private String parseContentName()
    {
        Element contentNameElement = contentEl.getChild( "name" );
        if ( contentNameElement == null )
        {
            return null;
        }
        String contentName = contentNameElement.getValue();
        if ( StringUtils.isBlank( contentName ) )
        {
            return "";
        }

        return contentName;
    }

    private String parseComment()
    {
        Element contentNameElement = contentEl.getChild( ContentBaseXMLBuilder.COMMENT_XML_KEY );
        if ( contentNameElement == null )
        {
            return null;
        }

        String description = contentNameElement.getValue();
        if ( StringUtils.isBlank( description ) )
        {
            return "";
        }

        return description;
    }


    public ContentVersionEntity parseContentVersion( ContentEntity content )
    {
        ContentVersionEntity version = new ContentVersionEntity();
        version.setKey( parseContentVersionKey() );
        version.setContent( content );
        version.setStatus( ContentStatus.get( parseStatus() ) );
        version.setModifiedBy( parseModifier() );
        version.setChangeComment( parseComment() );

        ContentData contentData = parseContentData( content, version );
        version.setContentData( contentData );

        Set<ContentKey> relatedcontentKeys = contentData.resolveRelatedContentKeys();
        for ( ContentKey relatedContentKey : relatedcontentKeys )
        {
            ContentEntity relatedContent = contentDao.findByKey( relatedContentKey );
            if ( relatedContent != null )
            {
                version.addRelatedChild( relatedContent );
            }
        }

        return version;
    }

    private ContentData parseContentData( ContentEntity content, ContentVersionEntity version )
    {
        ContentData contentData = null;

        if ( parseContentData )
        {
            contentData = parseContentData( content.getCategory().getContentType() );
        }
        else if ( version.getKey() != null )
        {
            ContentVersionEntity persistedVersion = contentVersionDao.findByKey( version.getKey() );
            if ( persistedVersion != null )
            {
                contentData = persistedVersion.getContentData();
            }
        }

        return contentData;
    }

    public List<ContentAccessEntity> parseContentAccessRights()
    {
        List<ContentAccessEntity> list = new ArrayList<ContentAccessEntity>();
        Element accessrightsEl = contentEl.getChild( "accessrights" );
        if ( accessrightsEl == null )
        {
            return list;
        }

        //noinspection unchecked
        List<Element> accessrighElList = accessrightsEl.getChildren( "accessright" );
        for ( Element accessrightEl : accessrighElList )
        {
            ContentAccessEntity accessRight = parseContentAccessRight( accessrightEl );
            if ( accessRight != null )
            {
                list.add( accessRight );
            }
        }

        return list;
    }

    private ContentAccessEntity parseContentAccessRight( Element accessRightEl )
    {
        GroupEntity group = parseGroup( accessRightEl.getAttributeValue( "groupkey" ) );
        if ( group == null )
        {
            return null;
        }

        ContentAccessEntity contentAccess = new ContentAccessEntity();
        contentAccess.setGroup( group );
        contentAccess.setReadAccess( parseBoolean( accessRightEl.getAttributeValue( "read" ), false ) );
        contentAccess.setUpdateAccess( parseBoolean( accessRightEl.getAttributeValue( "update" ), false ) );
        contentAccess.setDeleteAccess( parseBoolean( accessRightEl.getAttributeValue( "delete" ), false ) );

        return contentAccess;
    }

    private GroupEntity parseGroup( String groupKeyStr )
    {
        return groupDao.find( groupKeyStr );
    }

    private ContentData parseContentData( ContentTypeEntity contentType )
    {
        Element contentdataEl = contentEl.getChild( "contentdata" );
        if ( contentdataEl == null )
        {
            return null;
        }

        Document doc = new Document( (Element) contentdataEl.detach() );
        return ContentDataParser.parse( doc, contentType, binaryDatas );
    }

    private CategoryEntity parseCategory()
    {
        Element categorynameEl = contentEl.getChild( "categoryname" );
        if ( categorynameEl == null )
        {
            return null;
        }
        String categoryKeyStr = categorynameEl.getAttributeValue( "key" );
        if ( categoryKeyStr == null )
        {
            return null;
        }
        CategoryKey categoryKey = new CategoryKey( categoryKeyStr );
        return categoryDao.findByKey( categoryKey );
    }

    private UserEntity parseOwner()
    {
        Element ownerEl = contentEl.getChild( "owner" );
        if ( ownerEl == null )
        {
            return null;
        }
        String ownerKeyStr = ownerEl.getAttributeValue( "key" );
        if ( ownerKeyStr == null )
        {
            return null;
        }
        UserKey userKey = new UserKey( ownerKeyStr );
        return userDao.findByKey( userKey.toString() );
    }

    private UserEntity parseAssigner()
    {
        Element assignerEl = contentEl.getChild( ContentBaseXMLBuilder.ASSIGNER_XML_KEY );
        if ( assignerEl == null )
        {
            return null;
        }
        String assignerKeyString = assignerEl.getAttributeValue( "key" );
        if ( assignerKeyString == null )
        {
            return null;
        }
        UserKey userKey = new UserKey( assignerKeyString );
        return userDao.findByKey( userKey );
    }

    private UserEntity parseAssignee()
    {
        Element assigneeEl = contentEl.getChild( ContentBaseXMLBuilder.ASSIGNEE_XML_KEY );
        if ( assigneeEl == null )
        {
            return null;
        }
        String assigneeKeyString = assigneeEl.getAttributeValue( "key" );
        if ( assigneeKeyString == null )
        {
            return null;
        }
        UserKey userKey = new UserKey( assigneeKeyString );
        return userDao.findByKey( userKey );
    }

    private UserEntity parseModifier()
    {
        Element modifierEl = contentEl.getChild( "modifier" );
        if ( modifierEl == null )
        {
            return null;
        }
        String modifierKeyStr = modifierEl.getAttributeValue( "key" );
        if ( modifierKeyStr == null )
        {
            return null;
        }
        UserKey userKey = new UserKey( modifierKeyStr );
        return userDao.findByKey( userKey.toString() );
    }

    private LanguageEntity parseLanguage()
    {
        String languageKeyStr = contentEl.getAttributeValue( "languagekey" );

        if ( StringUtils.isBlank( languageKeyStr ) )
        {
            return null;
        }

        return languageDao.findByKey( new LanguageKey( languageKeyStr ) );
    }

    private ContentKey parseContentKey()
    {
        String string = contentEl.getAttributeValue( "key" );
        Integer intValue = parseInteger( string, null );
        if ( intValue == null )
        {
            return null;
        }
        return new ContentKey( intValue );
    }

    private ContentVersionKey parseContentVersionKey()
    {
        String string = contentEl.getAttributeValue( "versionkey" );
        Integer intValue = parseInteger( string, null );
        if ( intValue == null )
        {
            return null;
        }
        return new ContentVersionKey( intValue );
    }

    private Date parseCreatedAt()
    {
        String dateString = contentEl.getAttributeValue( "created" );
        return parseDate( dateString );
    }

    private Date parsePublishFrom()
    {
        String dateString = contentEl.getAttributeValue( "publishfrom" );
        return parseDate( dateString );
    }

    private Date parsePublishTo()
    {
        String dateString = contentEl.getAttributeValue( "publishto" );
        return parseDate( dateString );
    }

    private Date parseDueDate()
    {
        Element dueDate = contentEl.getChild( ContentBaseXMLBuilder.ASSIGNMENT_DUEDATE_XML_KEY );

        if ( dueDate != null )
        {
            String dateString = dueDate.getValue();
            return parseDate( dateString );
        }

        return null;
    }

    private Integer parseStatus()
    {
        String statusString = contentEl.getAttributeValue( "status" );
        return parseInteger( statusString, null );
    }

    private Integer parsePriority()
    {
        String priorityStr = contentEl.getAttributeValue( "priority" );
        return parseInteger( priorityStr, 0 );
    }

    private Date parseDate( String dateString )
    {
        if ( dateString == null || dateString.trim().length() == 0 )
        {
            return null;
        }
        final DateTime date = DATE_TIME_FORMATTER.parseDateTime( dateString );
        return date.toDate();
    }

    private Integer parseInteger( String str, Integer defaultValue )
    {
        if ( str == null || str.trim().length() == 0 )
        {
            return defaultValue;
        }
        return Integer.valueOf( str );
    }

    private Boolean parseBoolean( String value, Boolean defaultValue )
    {
        if ( value == null )
        {
            return defaultValue;
        }
        else if ( "true".equals( value ) )
        {
            return Boolean.TRUE;
        }
        else if ( "false".equals( value ) )
        {
            return Boolean.FALSE;
        }

        return defaultValue;
    }

    private static Document getContentElement( String contentXml )
    {
        try
        {
            return JDOMUtil.parseDocument( contentXml );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Failed to parse contentXml", e );
        }
    }

}
