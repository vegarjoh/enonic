/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.content;

import java.util.Date;

import com.enonic.esl.containers.ExtendedMap;
import com.enonic.esl.util.DateUtil;
import com.enonic.vertical.adminweb.VerticalAdminLogger;

/**
 * Created by IntelliJ IDEA.
 * User: rmh
 * Date: Oct 25, 2010
 * Time: 8:58:44 AM
 */
public class AssignmentDataParser
{

    private static final String DUEDATE_DATE_FORMITEM_KEY = "date_assignment_duedate";

    private static final String DUEDATE_TIME_FORMITEM_KEY = "time_assignment_duedate";

    private static final String DEFAULT_ASSIGNMENT_DUEDATE_HHMM = "23:59";

    private static final String ASSIGNMENT_DESCRIPTION_FORMITEM_KEY = "_assignment_description";

    private static final String ASSIGNEE_FORMITEM_KEY = "_assignee";

    private static final String ASSIGNER_FORMITEM_KEY = "_assigner";

    private ExtendedMap formItems;

    public AssignmentDataParser( ExtendedMap formItems )
    {
        this.formItems = formItems;
    }

    public String getAssigneeKey()
    {
        return formItems.getString( ASSIGNEE_FORMITEM_KEY, null );
    }

    public String getAssignerKey()
    {
        return formItems.getString( ASSIGNER_FORMITEM_KEY, null );
    }

    public String getAssignmentDescription()
    {
        return formItems.getString( ASSIGNMENT_DESCRIPTION_FORMITEM_KEY, null );
    }

    public Date getAssignmentDueDate()
    {
        if ( formItems.containsKey( DUEDATE_DATE_FORMITEM_KEY ) )
        {
            StringBuffer date = new StringBuffer( formItems.getString( DUEDATE_DATE_FORMITEM_KEY ) );
            date.append( ' ' );
            date.append( formItems.getString( DUEDATE_TIME_FORMITEM_KEY, DEFAULT_ASSIGNMENT_DUEDATE_HHMM ) );

            try
            {
                return DateUtil.parseDateTime( date.toString() );
            }
            catch ( final Exception e )
            {
                VerticalAdminLogger.errorAdmin("Error parsing assignment due date: %t", e );
            }
        }

        return null;
    }

}
