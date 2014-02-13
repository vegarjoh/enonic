/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.portal.datasource.handler.util;

import javax.annotation.Nonnull;

public final class GetCalendarParams
{
    public boolean relative = false;

    public Integer year;

    public Integer month;

    public int count = 12;

    public boolean includeWeeks = false;

    public boolean includeDays = false;

    @Nonnull
    public String language;

    @Nonnull
    public String country;
}
