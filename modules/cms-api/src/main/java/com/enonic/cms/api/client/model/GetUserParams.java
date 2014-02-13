/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.api.client.model;

public class GetUserParams
    extends AbstractParams
{

    private static final long serialVersionUID = 2525364117810761893L;

    /**
     * Specify user either by qualified name ([userStoreKey:]&lt;user name&gt;) or key. If null the logged in user is used.
     * When specifying a key, prefix with a hash (user = "#xxx").
     */
    public String user;

    /**
     * If set to false, group memberships are not listed
     */
    public boolean includeMemberships = true;

    /**
     * If set to false; only direct memeberships are listed. If set to true; indirect memberships are also listed.
     */
    public boolean normalizeGroups = false;

    /**
     * If set to true, additional attributes for the user will be included.
     */
    public boolean includeCustomUserFields = false;
}
