/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.api.client.model;

import com.enonic.cms.api.client.model.preference.PreferenceScope;

public class SetPreferenceParams
    extends AbstractParams
{
    private static final long serialVersionUID = 8835663063064609797L;

    public PreferenceScope scope;

    public String key;

    public String value;
}
