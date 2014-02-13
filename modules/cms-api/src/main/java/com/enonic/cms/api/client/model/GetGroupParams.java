/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.api.client.model;

public class GetGroupParams
    extends AbstractParams
{

    private static final long serialVersionUID = -8677566277616296562L;

    /**
     * Specify group either by qualified group name (&lt;userStoreKey&gt;:&lt;group name&gt;) or key. When specifying a key, prefix with a
     * hash (group = #xxx).
     */
    public String group;

    /**
     *  Set this property to <code>true</code> to include a list of all members of this group, in the result XML.  Default is false.
     */
    public boolean includeMembers = false;

    /**
     * Set this property to <code>true</code> to include a list of all groups this group is a member of, in the result XML.  Default is false.
     */
    public boolean includeMemberships = false;

    /**
     * If set to false; only direct memeberships are listed. If set to true; indirect memberships are also listed.
     * This property have no effect, unless <code>includeMemberships</code> is set to <code>true</code>.
     */
    public boolean normalizeGroups = false;
}