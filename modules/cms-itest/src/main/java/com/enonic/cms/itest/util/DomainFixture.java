/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.itest.util;

import java.sql.Types;
import java.util.List;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Component;

import com.google.common.base.Preconditions;

import com.enonic.cms.api.client.model.user.UserInfo;
import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.ContentVersionEntity;
import com.enonic.cms.core.content.ContentVersionKey;
import com.enonic.cms.core.content.RelatedContentEntity;
import com.enonic.cms.core.content.binary.BinaryDataEntity;
import com.enonic.cms.core.content.binary.BinaryDataKey;
import com.enonic.cms.core.content.category.CategoryEntity;
import com.enonic.cms.core.content.category.CategoryKey;
import com.enonic.cms.core.content.category.UnitEntity;
import com.enonic.cms.core.content.contenttype.ContentHandlerEntity;
import com.enonic.cms.core.content.contenttype.ContentHandlerKey;
import com.enonic.cms.core.content.contenttype.ContentTypeEntity;
import com.enonic.cms.core.language.LanguageEntity;
import com.enonic.cms.core.search.IndexTransactionService;
import com.enonic.cms.core.security.PortalSecurityHolder;
import com.enonic.cms.core.security.group.GroupEntity;
import com.enonic.cms.core.security.group.GroupKey;
import com.enonic.cms.core.security.group.GroupType;
import com.enonic.cms.core.security.user.User;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.security.user.UserKey;
import com.enonic.cms.core.security.user.UserType;
import com.enonic.cms.core.security.userstore.UserStoreEntity;
import com.enonic.cms.core.structure.SiteEntity;
import com.enonic.cms.core.structure.menuitem.ContentHomeEntity;
import com.enonic.cms.core.structure.menuitem.ContentHomeKey;
import com.enonic.cms.core.structure.menuitem.MenuItemEntity;
import com.enonic.cms.core.structure.page.template.PageTemplateEntity;
import com.enonic.cms.store.dao.GroupDao;
import com.enonic.cms.store.dao.UserDao;
import com.enonic.cms.store.hibernate.type.LazyInitializedJDOMDocumentUserType;

/**
 * Nov 26, 2009
 */
@Component
@Scope("prototype")
public class DomainFixture
{
    static
    {
        // prevent org.h2.jdbc.JdbcSQLException: Value too long for column "MEN_XMLDATA BINARY(255)"
        LazyInitializedJDOMDocumentUserType.JDOM_DOCUMENT_TYPE = Types.LONGVARBINARY;
    }

    public IndexTransactionService getIndexTransactionService()
    {
        return indexTransactionService;
    }

    @Autowired
    private HibernateTemplate hibernateTemplate;

    private DomainFactory factory;

    @Autowired
    private UserDao userDao;

    @Autowired
    private GroupDao groupDao;

    @Autowired
    private IndexTransactionService indexTransactionService;

    @Autowired
    private Properties systemProperties;

    public DomainFixture()
    {
        factory = new DomainFactory( this );
    }

    public DomainFactory getFactory()
    {
        return factory;
    }

    public DomainFixture( HibernateTemplate hibernateTemplate, GroupDao groupDao )
    {
        Preconditions.checkNotNull( hibernateTemplate );
        Preconditions.checkNotNull( groupDao );

        this.hibernateTemplate = hibernateTemplate;
        this.groupDao = groupDao;
    }

    public DomainFixture( HibernateTemplate hibernateTemplate )
    {
        Preconditions.checkNotNull( hibernateTemplate );

        this.hibernateTemplate = hibernateTemplate;
    }

    void setFactory( DomainFactory factory )
    {
        Preconditions.checkNotNull( factory );
        this.factory = factory;
    }

    public void initSystemData()
    {
        systemProperties.setProperty( "cms.admin.password", "password" );

        hibernateTemplate.clear();
        indexTransactionService.clearJournal();

        save( factory.createLanguage( "en" ) );
        save( factory.createLanguage( "no" ) );
        save( factory.createLanguage( "se" ) );
        save( factory.createLanguage( "dk" ) );

        if ( groupDao != null )
        {
            groupDao.invalidateCachedKeys();
        }
        save( factory.createGroup( GroupType.ENTERPRISE_ADMINS.getName(), GroupType.ENTERPRISE_ADMINS ) );
        save( factory.createGroup( GroupType.ADMINS.getName(), GroupType.ADMINS ) );
        save( factory.createGroup( GroupType.DEVELOPERS.getName(), GroupType.DEVELOPERS ) );
        save( factory.createGroup( GroupType.EXPERT_CONTRIBUTORS.getName(), GroupType.EXPERT_CONTRIBUTORS ) );
        save( factory.createGroup( GroupType.CONTRIBUTORS.getName(), GroupType.CONTRIBUTORS ) );

        UserStoreEntity testuserstore = factory.createUserStore( "testuserstore" );
        testuserstore.setDefaultStore( true );
        storeAndSetupUserStore( testuserstore );

        createAndStoreUserAndUserGroup( "anonymous", UserType.ANONYMOUS.getName(), UserType.ANONYMOUS, null );

        save( factory.createUser( User.ROOT_UID, "Enterprise Admin", UserType.ADMINISTRATOR, null ) );

        flushAndClearHibernateSession();

        PortalSecurityHolder.setAnonUser( findUserByName( "anonymous" ).getKey() );
        PortalSecurityHolder.setLoggedInUser( findUserByName( "anonymous" ).getKey() );

        userDao.resetCachedValues();
    }

    public void storeAndSetupUserStore( UserStoreEntity userStore )
    {
        save( userStore );
        save(
            factory.createGroupInUserstore( GroupType.AUTHENTICATED_USERS.getName(), GroupType.AUTHENTICATED_USERS, userStore.getName() ) );
        save( factory.createGroupInUserstore( GroupType.USERSTORE_ADMINS.getName(), GroupType.USERSTORE_ADMINS, userStore.getName() ) );
    }

    public UserEntity createAndStoreNormalUserWithUserGroup( String uid, String displayName, String userStoreName )
    {
        return createAndStoreUserAndUserGroup( uid, displayName, UserType.NORMAL, userStoreName );
    }

    public UserEntity createAndStoreNormalUserWithAllValuesAndWithUserGroup( String uid, String displayName, String userStoreName,
                                                                             UserInfo userInfo )
    {
        GroupEntity userGroup = new GroupEntity();
        userGroup.setName( uid );
        userGroup.setSyncValue( uid );
        userGroup.setDeleted( 0 );
        userGroup.setType( GroupType.resolveAssociate( UserType.NORMAL ) );
        userGroup.setRestricted( 1 );
        hibernateTemplate.save( userGroup );

        UserEntity user = factory.createUserWithAllValues( uid, displayName, UserType.NORMAL, userStoreName, userInfo );
        user.encodePassword( null );

        user.setUserGroup( userGroup );

        hibernateTemplate.save( user );
        hibernateTemplate.flush();

        return user;
    }

    public UserEntity createAndStoreUserAndUserGroup( String uid, String displayName, UserType type, String userStoreName )
    {
        return createAndStoreUserAndUserGroup( uid, null, displayName, type, userStoreName );
    }

    public UserEntity createAndStoreUserAndUserGroup( String uid, String password, String displayName, UserType type, String userStoreName )
    {
        GroupEntity userGroup = new GroupEntity();
        userGroup.setName( uid );
        userGroup.setSyncValue( uid );
        userGroup.setDeleted( 0 );
        userGroup.setType( GroupType.resolveAssociate( type ) );
        userGroup.setRestricted( 1 );
        hibernateTemplate.save( userGroup );

        UserEntity user = factory.createUser( uid, displayName, type, userStoreName );
        if ( password != null )
        {
            user.encodePassword( password );
        }
        user.setUserGroup( userGroup );

        hibernateTemplate.save( user );
        hibernateTemplate.flush();

        return user;
    }

    public List<LanguageEntity> findAllLanguage()
    {
        return typecastList( LanguageEntity.class, hibernateTemplate.find( "from LanguageEntity" ) );
    }

    public LanguageEntity findLanguageByCode( String value )
    {
        LanguageEntity example = new LanguageEntity();
        example.setCode( value );
        return (LanguageEntity) findFirstByExample( example );
    }

    public UserStoreEntity findUserStoreByName( String userStoreName )
    {
        UserStoreEntity example = new UserStoreEntity();
        example.setName( userStoreName );
        return (UserStoreEntity) findFirstByExample( example );
    }

    public int countUsersByName( String name )
    {
        UserEntity example = new UserEntity();
        example.setName( name );
        return findByExample( example ).size();
    }

    public int countUsersByType( UserType type )
    {
        UserEntity example = new UserEntity();
        example.setType( type );
        return findByExample( example ).size();
    }

    public int countNonDeletedUsersByType( UserType type )
    {
        UserEntity example = new UserEntity();
        example.setType( type );
        example.setDeleted( false );
        return findByExample( example ).size();
    }

    public int countDeletedUsersByType( UserType type )
    {
        UserEntity example = new UserEntity();
        example.setType( type );
        example.setDeleted( true );
        return findByExample( example ).size();
    }

    public UserEntity findUserByKey( String value )
    {
        return findUserByKey( new UserKey( value ) );
    }

    public UserEntity findUserByKey( UserKey value )
    {
        return (UserEntity) requireOneOrNone( hibernateTemplate.find( "from UserEntity where key = ?", value ) );
    }

    public UserEntity findUserByName( String value )
    {
        UserEntity example = new UserEntity();
        example.setName( value );
        return (UserEntity) findFirstByExample( example );
    }

    public UserEntity findUserByType( UserType userType )
    {
        UserEntity example = new UserEntity();
        example.setType( userType );
        return (UserEntity) findFirstByExample( example );
    }

    public int countGroupsByType( GroupType type )
    {
        GroupEntity example = new GroupEntity();
        example.setType( type );
        return findByExample( example ).size();
    }

    public GroupEntity findGroupByKey( String value )
    {
        return findGroupByKey( new GroupKey( value ) );
    }

    public GroupEntity findGroupByKey( GroupKey value )
    {
        return (GroupEntity) requireOneOrNone( hibernateTemplate.find( "from GroupEntity where key = ?", value ) );
    }

    public GroupEntity findGroupByName( String groupName )
    {
        GroupEntity example = new GroupEntity();
        example.setName( groupName );
        return (GroupEntity) findFirstByExample( example );
    }

    public GroupEntity findGroupByType( GroupType groupType )
    {
        GroupEntity example = new GroupEntity();
        example.setType( groupType );
        return (GroupEntity) findFirstByExample( example );
    }

    public GroupEntity findGroupByTypeAndUserstore( GroupType groupType, String userstoreName )
    {
        GroupEntity example = new GroupEntity();
        example.setType( groupType );
        example.setUserStore( findUserStoreByName( userstoreName ) );
        return (GroupEntity) findFirstByExample( example );
    }

    public ContentHandlerEntity findContentHandlerByKey( ContentHandlerKey value )
    {
        return (ContentHandlerEntity) requireOneOrNone( hibernateTemplate.find( "from ContentHandlerEntity where key = ?", value ) );
    }

    public ContentHandlerEntity findContentHandlerByClassName( String value )
    {
        ContentHandlerEntity example = new ContentHandlerEntity();
        example.setClassName( value );
        return (ContentHandlerEntity) findFirstByExample( example );
    }

    public ContentTypeEntity findContentTypeByName( String value )
    {
        ContentTypeEntity example = new ContentTypeEntity();
        example.setName( value );
        return (ContentTypeEntity) findFirstByExample( example );
    }

    public UnitEntity findUnitByName( String value )
    {
        UnitEntity example = new UnitEntity();
        example.setName( value );
        return (UnitEntity) findFirstByExample( example );
    }

    public CategoryEntity findCategoryByKey( CategoryKey value )
    {
        return (CategoryEntity) requireOneOrNone( hibernateTemplate.find( "from CategoryEntity where key = ?", value ) );
    }

    public CategoryEntity findCategoryByName( String value )
    {
        CategoryEntity example = new CategoryEntity();
        example.setName( value );
        return (CategoryEntity) findFirstByExample( example );
    }

    public ContentEntity findFirstContentByCategory( CategoryEntity category )
    {
        ContentEntity example = new ContentEntity();
        example.setCategory( category );
        return (ContentEntity) findFirstByExample( example );
    }

    public List<ContentEntity> findContentByCategory( CategoryEntity category )
    {
        ContentEntity example = new ContentEntity();
        example.setCategory( category );
        return findByExample( example );
    }

    public int countAllContent()
    {
        List<ContentEntity> list = typecastList( ContentEntity.class, hibernateTemplate.find( "from ContentEntity" ) );
        return list.size();
    }

    public List<ContentEntity> findAllContent()
    {
        return typecastList( ContentEntity.class, hibernateTemplate.find( "from ContentEntity" ) );
    }

    public ContentEntity findContentByKey( ContentKey value )
    {
        return (ContentEntity) requireOneOrNone( hibernateTemplate.find( "from ContentEntity where key = ?", value ) );
    }

    public ContentEntity findContentByName( String name )
    {
        ContentEntity example = new ContentEntity();
        example.setName( name );
        return (ContentEntity) findFirstByExample( example );
    }

    public ContentEntity findContentByPriority( int priority )
    {
        List<ContentEntity> list =
            typecastList( ContentEntity.class, hibernateTemplate.find( "from ContentEntity where priority = ?", priority ) );
        if ( list.isEmpty() )
        {
            return null;
        }
        return list.get( 0 );
    }

    public List<ContentVersionEntity> findContentVersionsByContent( ContentKey key )
    {
        return typecastList( ContentVersionEntity.class,
                             hibernateTemplate.find( "from ContentVersionEntity where content.key = ? order by key ", key ) );
    }

    public int countContentVersionsByTitle( String title )
    {
        ContentVersionEntity example = new ContentVersionEntity();
        example.setTitle( title );
        List<ContentVersionEntity> list = typecastList( ContentVersionEntity.class, findByExample( example ) );
        return list.size();
    }

    public List<ContentVersionEntity> findContentVersionsByTitle( String title )
    {
        ContentVersionEntity example = new ContentVersionEntity();
        example.setTitle( title );
        return typecastList( ContentVersionEntity.class, findByExample( example ) );
    }

    public ContentVersionEntity findFirstContentVersionByTitle( String title )
    {
        ContentVersionEntity example = new ContentVersionEntity();
        example.setTitle( title );
        return (ContentVersionEntity) findFirstByExample( example );
    }

    public ContentVersionEntity findMainContentVersionByTitle( String title )
    {
        ContentVersionEntity example = new ContentVersionEntity();
        example.setTitle( title );
        List<ContentVersionEntity> list = typecastList( ContentVersionEntity.class, findByExample( example ) );
        for ( ContentVersionEntity version : list )
        {
            if ( version.isMainVersion() )
            {
                return version;
            }
        }
        return null;
    }

    public ContentVersionEntity findContentVersionByTitle( int index, String title )
    {
        ContentVersionEntity example = new ContentVersionEntity();
        example.setTitle( title );
        List<ContentVersionEntity> list = typecastList( ContentVersionEntity.class, findByExample( example ) );
        return list.get( index );
    }

    public ContentVersionEntity findContentVersionByKey( ContentVersionKey value )
    {
        return (ContentVersionEntity) requireOneOrNone( hibernateTemplate.find( "from ContentVersionEntity where key = ?", value ) );
    }

    public ContentVersionEntity findContentVersionByContent( int index, ContentKey key )
    {
        List<ContentVersionEntity> list = typecastList( ContentVersionEntity.class, hibernateTemplate.find(
            "from ContentVersionEntity where content.key = ? order by key ", key ) );
        if ( list.isEmpty() )
        {
            return null;
        }
        return list.get( index );
    }

    public int countContentVersionsByContent( ContentKey key )
    {
        List<ContentVersionEntity> list =
            typecastList( ContentVersionEntity.class, hibernateTemplate.find( "from ContentVersionEntity where content.key = ?", key ) );
        return list.size();
    }

    public List<RelatedContentEntity> findRelatedContentsByContentVersionKey( ContentVersionKey versionKey )
    {
        return typecastList( RelatedContentEntity.class,
                             hibernateTemplate.find( "from RelatedContentEntity where key.parentContentVersionKey = ?", versionKey ) );
    }

    public BinaryDataEntity findBinaryDataByKey( BinaryDataKey value )
    {
        return (BinaryDataEntity) requireOneOrNone( hibernateTemplate.find( "from BinaryDataEntity where key = ?", value ) );
    }

    public MenuItemEntity findMenuItemByName( String name )
    {
        MenuItemEntity example = new MenuItemEntity();
        example.setName( name );
        return (MenuItemEntity) findFirstByExample( example );
    }

    public ContentHomeEntity findContentHomeByKey( ContentHomeKey value )
    {
        return (ContentHomeEntity) requireOneOrNone(
            hibernateTemplate.find( "from ContentHomeEntity where key.siteKey = ? and key.contentKey = ?",
                                    new Object[]{value.getSiteKey(), value.getContentKey()} ) );
    }

    public MenuItemEntity findMenuItemByNameAndOrder( String name, int order )
    {
        MenuItemEntity example = new MenuItemEntity();
        example.setName( name );
        example.setOrder( order );
        return (MenuItemEntity) findFirstByExample( example );
    }

    public PageTemplateEntity findPageTemplateByName( String name )
    {
        PageTemplateEntity example = new PageTemplateEntity();
        example.setName( name );
        return (PageTemplateEntity) findFirstByExample( example );
    }

    public SiteEntity findSiteByName( String value )
    {
        SiteEntity example = new SiteEntity();
        example.setName( value );
        return (SiteEntity) findFirstByExample( example );
    }

    public void save( Object... objects )
    {
        for ( Object obj : objects )
        {
            hibernateTemplate.save( obj );
        }

        flushAndClearHibernateSession();
    }

    public void saveOrUpdate( Object... objects )
    {
        for ( Object obj : objects )
        {
            hibernateTemplate.saveOrUpdate( obj );
        }

        flushAndClearHibernateSession();
    }

    public void flushAndClearHibernateSession()
    {
        hibernateTemplate.flush();
        hibernateTemplate.clear();
    }

    public void flushIndexTransaction()
    {
        if ( indexTransactionService.isActive() )
        {
            indexTransactionService.commit();
        }
    }

    private List findByExample( Object example )
    {
        return hibernateTemplate.findByExample( example );
    }

    private Object findFirstByExample( Object example )
    {
        List list = hibernateTemplate.findByExample( example );
        if ( list.isEmpty() )
        {
            return null;
        }
        if ( list.size() > 1 )
        {
            throw new IllegalStateException( "Expected only one result, was: " + list.size() );
        }
        return list.get( 0 );
    }

    private Object requireOneOrNone( List list )
    {
        if ( list.size() > 1 )
        {
            throw new IllegalStateException( "Expected only one result, was: " + list.size() );
        }
        else if ( list.size() == 0 )
        {
            return null;
        }
        else
        {
            return list.get( 0 );
        }
    }


    @SuppressWarnings("unchecked")
    private <T> List<T> typecastList( Class<T> clazz, Object list )
    {
        return (List<T>) list;
    }

}
