/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.itest.content;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.access.ContentAccessResolver;
import com.enonic.cms.core.content.access.ContentAccessRightsAccumulated;
import com.enonic.cms.core.security.group.GroupEntity;
import com.enonic.cms.core.security.group.GroupType;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.security.user.UserType;
import com.enonic.cms.itest.AbstractSpringTest;
import com.enonic.cms.itest.util.DomainFactory;
import com.enonic.cms.itest.util.DomainFixture;
import com.enonic.cms.store.dao.GroupEntityDao;

import static org.junit.Assert.*;

public class ContentAccessResolverTest
    extends AbstractSpringTest
{
    @Autowired
    private GroupEntityDao groupEntityDao;

    private DomainFactory factory;

    @Autowired
    private DomainFixture fixture;

    @Before
    public void before()
    {
        factory = fixture.getFactory();
        fixture.initSystemData();
    }

    @Test
    public void root_user_get_accumulated_all_rights()
    {
        ContentEntity content = new ContentEntity();
        UserEntity rootUser = fixture.findUserByType( UserType.ADMINISTRATOR );

        ContentAccessResolver contentAccessResolver = new ContentAccessResolver( groupEntityDao );

        ContentAccessRightsAccumulated accumulated = contentAccessResolver.getAccumulatedAccessRights( rootUser, content );
        assertTrue( accumulated.isReadAccess() );
        assertTrue( accumulated.isUpdateAccess() );
        assertTrue( accumulated.isDeleteAccess() );
    }

    @Test
    public void user_memberOf_enterpriseadminsgroup_get_accumulated_all_rights()
    {
        ContentEntity content = new ContentEntity();
        UserEntity user = fixture.createAndStoreNormalUserWithUserGroup( "myuser", "My User", "testuserstore" );
        GroupEntity enterpriseAdminsGroup = fixture.findGroupByType( GroupType.ENTERPRISE_ADMINS );
        user.getUserGroup().addMembership( enterpriseAdminsGroup );

        ContentAccessResolver contentAccessResolver = new ContentAccessResolver( groupEntityDao );

        ContentAccessRightsAccumulated accumulated = contentAccessResolver.getAccumulatedAccessRights( user, content );
        assertTrue( accumulated.isReadAccess() );
        assertTrue( accumulated.isUpdateAccess() );
        assertTrue( accumulated.isDeleteAccess() );
    }

    @Test
    public void anonymous_gets_accumulated_right_from_anonymous()
    {
        ContentEntity content = new ContentEntity();
        UserEntity anonymousUser = fixture.findUserByType( UserType.ANONYMOUS );

        content.addContentAccessRight( factory.createContentAccess( "read, update", anonymousUser.getUserGroup(), content ) );

        ContentAccessResolver contentAccessResolver = new ContentAccessResolver( groupEntityDao );
        ContentAccessRightsAccumulated accumulated = contentAccessResolver.getAccumulatedAccessRights( anonymousUser, content );
        assertTrue( accumulated.isReadAccess() );
        assertTrue( accumulated.isUpdateAccess() );
        assertFalse( accumulated.isDeleteAccess() );
    }

    @Test
    public void user_gets_accumulated_right_from_anonymous()
    {
        ContentEntity content = new ContentEntity();
        UserEntity user = fixture.createAndStoreNormalUserWithUserGroup( "myuser", "My User", "testuserstore" );

        GroupEntity anonymousUsersGroup = fixture.findGroupByType( GroupType.ANONYMOUS );
        content.addContentAccessRight( factory.createContentAccess( "read", anonymousUsersGroup, content ) );

        ContentAccessResolver contentAccessResolver = new ContentAccessResolver( groupEntityDao );
        ContentAccessRightsAccumulated accumulated = contentAccessResolver.getAccumulatedAccessRights( user, content );
        assertTrue( accumulated.isReadAccess() );
        assertFalse( accumulated.isUpdateAccess() );
        assertFalse( accumulated.isDeleteAccess() );
    }

    @Test
    public void user_gets_accumulated_right_from_usergroup()
    {
        ContentEntity content = new ContentEntity();
        UserEntity user = fixture.createAndStoreNormalUserWithUserGroup( "myuser", "My User", "testuserstore" );

        content.addContentAccessRight( factory.createContentAccess( "read", user.getUserGroup(), content ) );

        ContentAccessResolver contentAccessResolver = new ContentAccessResolver( groupEntityDao );
        ContentAccessRightsAccumulated accumulated = contentAccessResolver.getAccumulatedAccessRights( user, content );
        assertTrue( accumulated.isReadAccess() );
        assertFalse( accumulated.isUpdateAccess() );
        assertFalse( accumulated.isDeleteAccess() );
    }

    @Test
    public void user_gets_accumulated_right_from_autenticated_users_group()
    {
        ContentEntity content = new ContentEntity();
        UserEntity user = fixture.createAndStoreNormalUserWithUserGroup( "myuser", "My User", "testuserstore" );

        GroupEntity authenticatedUsersGroup = fixture.findGroupByTypeAndUserstore( GroupType.AUTHENTICATED_USERS, "testuserstore" );
        content.addContentAccessRight( factory.createContentAccess( "read", authenticatedUsersGroup, content ) );

        ContentAccessResolver contentAccessResolver = new ContentAccessResolver( groupEntityDao );
        ContentAccessRightsAccumulated accumulated = contentAccessResolver.getAccumulatedAccessRights( user, content );
        assertTrue( accumulated.isReadAccess() );
        assertFalse( accumulated.isUpdateAccess() );
        assertFalse( accumulated.isDeleteAccess() );
    }

    @Test
    public void user_gets_accumulated_right_from_indirect_membership()
    {
        ContentEntity content = new ContentEntity();
        UserEntity user = fixture.createAndStoreNormalUserWithUserGroup( "myuser", "My User", "testuserstore" );

        GroupEntity group1 = factory.createGlobalGroup( "Group-1" );
        user.getUserGroup().addMembership( group1 );
        fixture.save( group1 );

        GroupEntity group1_group2 = factory.createGlobalGroup( "Group-1-2" );
        group1.addMembership( group1_group2 );
        fixture.save( group1_group2 );

        GroupEntity group1_group2_group3 = factory.createGlobalGroup( "Group-1-2-3" );
        group1_group2.addMembership( group1_group2_group3 );
        fixture.save( group1_group2_group3 );

        content.addContentAccessRight( factory.createContentAccess( "read", group1_group2_group3, content ) );

        ContentAccessResolver contentAccessResolver = new ContentAccessResolver( groupEntityDao );
        ContentAccessRightsAccumulated accumulated = contentAccessResolver.getAccumulatedAccessRights( user, content );
        assertTrue( accumulated.isReadAccess() );
        assertFalse( accumulated.isUpdateAccess() );
        assertFalse( accumulated.isDeleteAccess() );
    }

    @Test
    public void user_gets_accumulated_rights_from_different_indirect_memberships()
    {
        ContentEntity content = new ContentEntity();
        UserEntity user = fixture.createAndStoreNormalUserWithUserGroup( "myuser", "My User", "testuserstore" );

        GroupEntity group1 = factory.createGlobalGroup( "Group-1" );
        user.getUserGroup().addMembership( group1 );
        fixture.save( group1 );

        GroupEntity group1_group2 = factory.createGlobalGroup( "Group-1-2" );
        group1.addMembership( group1_group2 );
        fixture.save( group1_group2 );

        GroupEntity group1_group2_group3 = factory.createGlobalGroup( "Group-1-2-3" );
        group1_group2.addMembership( group1_group2_group3 );
        fixture.save( group1_group2_group3 );

        content.addContentAccessRight( factory.createContentAccess( "read", group1, content ) );
        content.addContentAccessRight( factory.createContentAccess( "update", group1_group2, content ) );
        content.addContentAccessRight( factory.createContentAccess( "delete", group1_group2_group3, content ) );

        ContentAccessResolver contentAccessResolver = new ContentAccessResolver( groupEntityDao );
        ContentAccessRightsAccumulated accumulated = contentAccessResolver.getAccumulatedAccessRights( user, content );
        assertTrue( accumulated.isReadAccess() );
        assertTrue( accumulated.isUpdateAccess() );
        assertTrue( accumulated.isDeleteAccess() );
    }

    @Test
    public void user_gets_accumulated_rights_from_indirect_memberships_in_different_branches()
    {
        ContentEntity content = new ContentEntity();
        UserEntity user = fixture.createAndStoreNormalUserWithUserGroup( "myuser", "My User", "testuserstore" );

        GroupEntity group1 = factory.createGlobalGroup( "Group-1" );
        fixture.save( group1 );
        user.getUserGroup().addMembership( group1 );

        GroupEntity group1_group2a = factory.createGlobalGroup( "Group-1-2a" );
        fixture.save( group1_group2a );
        group1.addMembership( group1_group2a );

        GroupEntity group1_group2b = factory.createGlobalGroup( "Group-1-2b" );
        fixture.save( group1_group2b );
        group1.addMembership( group1_group2b );

        GroupEntity group1_group2a_group3 = factory.createGlobalGroup( "Group-1-2a-3" );
        fixture.save( group1_group2a_group3 );
        group1_group2a.addMembership( group1_group2a_group3 );

        GroupEntity group1_group2b_group3 = factory.createGlobalGroup( "Group-1-2b-3" );
        fixture.save( group1_group2b_group3 );
        group1_group2b.addMembership( group1_group2b_group3 );

        fixture.flushAndClearHibernateSession();

        content.addContentAccessRight( factory.createContentAccess( "read", user.getUserGroup(), content ) );
        content.addContentAccessRight( factory.createContentAccess( "update", group1_group2a_group3, content ) );
        content.addContentAccessRight( factory.createContentAccess( "delete", group1_group2b_group3, content ) );

        ContentAccessResolver contentAccessResolver = new ContentAccessResolver( groupEntityDao );
        ContentAccessRightsAccumulated accumulated = contentAccessResolver.getAccumulatedAccessRights( user, content );
        assertTrue( accumulated.isReadAccess() );
        assertTrue( accumulated.isUpdateAccess() );
        assertTrue( accumulated.isDeleteAccess() );
    }


}
