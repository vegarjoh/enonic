/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.portal.datasource.handler.util;

import javax.annotation.Nonnull;

public final class GetFormattedDateParams
{
    public int offset = 0;

    public String dateFormat = "EEEE d. MMMM yyyy";

    @Nonnull
    public String language;

    @Nonnull
    public String country;
}
