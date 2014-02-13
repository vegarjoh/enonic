/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.user.remote;

import org.junit.Assert;
import org.junit.Test;

import com.enonic.cms.api.plugin.ext.userstore.RemoteGroup;
import com.enonic.cms.api.plugin.ext.userstore.RemotePrincipal;
import com.enonic.cms.api.plugin.ext.userstore.RemoteUser;

public class RemotePrincipalTest
{
    @Test
    public void testEquals()
    {
        RemotePrincipal user = new RemoteUser( "myuser" );
        RemotePrincipal group = new RemoteGroup( "mygroup" );

        Assert.assertTrue( user.equals( user ) );
        Assert.assertTrue( group.equals( group ) );

        Assert.assertFalse( user.equals( group ) );
        Assert.assertFalse( group.equals( user ) );

        Assert.assertFalse( user.equals( new RemoteGroup( "myuser" ) ) );
        Assert.assertFalse( group.equals( new RemoteUser( "mygroup" ) ) );
    }
}
