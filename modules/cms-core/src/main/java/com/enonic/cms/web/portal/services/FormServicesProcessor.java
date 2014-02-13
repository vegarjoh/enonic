/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.web.portal.services;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.enonic.esl.containers.ExtendedMap;
import com.enonic.esl.io.FileUtil;
import com.enonic.esl.xml.XMLTool;
import com.enonic.vertical.engine.VerticalCreateException;
import com.enonic.vertical.engine.VerticalSecurityException;

import com.enonic.cms.core.content.ContentAndVersion;
import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.ContentVersionEntity;
import com.enonic.cms.core.content.binary.BinaryData;
import com.enonic.cms.core.content.binary.BinaryDataAndBinary;
import com.enonic.cms.core.content.command.CreateContentCommand;
import com.enonic.cms.core.mail.MailRecipientType;
import com.enonic.cms.core.mail.SendMailService;
import com.enonic.cms.core.mail.SimpleMailTemplate;
import com.enonic.cms.core.portal.PrettyPathNameCreator;
import com.enonic.cms.core.portal.VerticalSession;
import com.enonic.cms.core.security.user.User;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.security.user.UserType;
import com.enonic.cms.core.service.UserServicesService;
import com.enonic.cms.core.servlet.ServletRequestAccessor;
import com.enonic.cms.core.structure.SiteKey;

@Component
public final class FormServicesProcessor
    extends ContentServicesBase
{
    private final static int ERR_MISSING_REQ = 1;

    private final static String ERR_MSG_MISSING_REQ = "Mandatory field is missing.";

    private final static int ERR_VALIDATION_FAILED = 2;

    private final static String ERR_MSG_VALIDATION_FAILED = "Validation failed.";

    private String adminEmail;

    private SendMailService sendMailService;

    class FormException
        extends VerticalUserServicesException
    {
        Document doc = null;

        Integer[] errorCodes = null;

        FormException( Document doc, Integer[] errorCodes )
        {
            super( "" );
            this.doc = doc;
            this.errorCodes = errorCodes;
        }
    }

    private final static int contentTypeKey = 50;

    public FormServicesProcessor()
    {
        super( "form" );
    }

    @Override
    protected void buildContentTypeXML( UserServicesService userServices, Element contentdataElem, ExtendedMap formItems,
                                        boolean skipElements )
        throws VerticalUserServicesException
    {

        int menuItemKey = formItems.getInt( "_form_id" );

        // Elements in the old form XML are prefixed with an underscore
        Element _formElement = (Element) formItems.get( "__form" );

        Document doc = contentdataElem.getOwnerDocument();
        Element formElement = XMLTool.createElement( doc, contentdataElem, "form" );
        formElement.setAttribute( "categorykey", _formElement.getAttribute( "categorykey" ) );

        // Set title element:
        Element _titleElement = XMLTool.getElement( _formElement, "title" );
        XMLTool.createElement( doc, formElement, "title", XMLTool.getElementText( _titleElement ) );

        // There may be multiple error states/codes, so we have to keep track of them.
        // When errors occurr, XML is inserted into the resulting document, and sent
        // back to the user client.
        // TIntArrayList errorCodes = new TIntArrayList(5);
        List<Integer> errorCodes = new ArrayList<Integer>( 5 );

        // The people that will recieve the form mail:
        Element recipientsElem = XMLTool.getElement( _formElement, "recipients" );
        if ( recipientsElem != null )
        {
            formElement.appendChild( doc.importNode( recipientsElem, true ) );
        }

        // Loop all form items and insert the data from the form:
        int fileattachmentCount = 0;
        Element[] _formItems = XMLTool.getElements( _formElement, "item" );
        for ( int i = 0; i < _formItems.length; i++ )
        {
            String formName = menuItemKey + "_form_" + ( i + 1 );

            Element itemElement = (Element) doc.importNode( _formItems[i], true );
            formElement.appendChild( itemElement );

            String type = itemElement.getAttribute( "type" );

            if ( "text".equals( type ) )
            {
                // Remove default data:
                Element tmpElement = XMLTool.getElement( itemElement, "data" );
                if ( tmpElement != null )
                {
                    itemElement.removeChild( tmpElement );
                }
            }

            if ( "text".equals( type ) || "textarea".equals( type ) || "checkbox".equals( type ) )
            {
                String value = formItems.getString( formName, "" );

                // If a regular expression is specified, it should be verified that
                // the data entered in the form conforms to this:
                String regexp = itemElement.getAttribute( "validation" );
                if ( "text".equals( type ) && regexp != null && regexp.length() > 0 )
                {
                    final boolean valueIsNonEmpty = value.length() > 0;
                    if ( !value.matches( regexp ) && valueIsNonEmpty )
                    {
                        XMLTool.createElement( doc, itemElement, "error", ERR_MSG_VALIDATION_FAILED ).setAttribute( "id", String.valueOf(
                            ERR_VALIDATION_FAILED ) );

                        if ( !errorCodes.contains( ERR_VALIDATION_FAILED ) )
                        {
                            errorCodes.add( ERR_VALIDATION_FAILED );
                        }
                    }
                }

                // If the form element is required, we must test that the user actually
                // entered data:
                if ( itemElement.getAttribute( "required" ).equals( "true" ) && value.length() == 0 )
                {
                    XMLTool.createElement( doc, itemElement, "error", ERR_MSG_MISSING_REQ ).setAttribute( "id", String.valueOf(
                        ERR_MISSING_REQ ) );

                    if ( !errorCodes.contains( ERR_MISSING_REQ ) )
                    {
                        errorCodes.add( ERR_MISSING_REQ );
                    }
                }

                XMLTool.createElement( doc, itemElement, "data", value );
            }
            else if ( "checkbox".equals( type ) )
            {
                String value;
                if ( formItems.getString( formName, "" ).equals( "on" ) )
                {
                    value = "1";
                }
                else
                {
                    value = "0";
                }

                XMLTool.createElement( doc, itemElement, "data", value );
            }
            else if ( "radiobuttons".equals( type ) || "dropdown".equals( type ) )
            {
                String value = formItems.getString( formName, null );

                boolean selected = false;
                Element tmpElement = XMLTool.getElement( itemElement, "data" );
                Element[] options = XMLTool.getElements( tmpElement, "option" );
                for ( Element option : options )
                {
                    tmpElement = option;
                    if ( tmpElement.getAttribute( "value" ).equals( value ) )
                    {
                        tmpElement.setAttribute( "selected", "true" );
                        selected = true;
                        break;
                    }
                }

                // If the form element is required, we must test that the user actually
                // checked on of the radiobuttons:
                if ( itemElement.getAttribute( "required" ).equals( "true" ) && !selected )
                {
                    XMLTool.createElement( doc, itemElement, "error", ERR_MSG_MISSING_REQ ).setAttribute( "id", String.valueOf(
                        ERR_MISSING_REQ ) );

                    if ( !errorCodes.contains( ERR_MISSING_REQ ) )
                    {
                        errorCodes.add( ERR_MISSING_REQ );
                    }
                }
            }
            else if ( "checkboxes".equals( type ) )
            {
                String[] values = formItems.getStringArray( formName );

                Element tmpElement = XMLTool.getElement( itemElement, "data" );
                Element[] options = XMLTool.getElements( tmpElement, "option" );
                for ( Element option : options )
                {
                    tmpElement = option;

                    for ( String currentValue : values )
                    {
                        if ( tmpElement.getAttribute( "value" ).equals( currentValue ) )
                        {
                            tmpElement.setAttribute( "selected", "true" );
                            break;
                        }
                    }
                }
            }
            else if ( "fileattachment".equals( type ) )
            {
                FileItem fileItem = formItems.getFileItem( formName, null );

                // If the form element is required, we must test that the user actually
                // entered data:
                if ( "true".equals( itemElement.getAttribute( "required" ) ) && fileItem == null )
                {
                    XMLTool.createElement( doc, itemElement, "error", ERR_MSG_MISSING_REQ ).setAttribute( "id", String.valueOf(
                        ERR_MISSING_REQ ) );

                    if ( !errorCodes.contains( ERR_MISSING_REQ ) )
                    {
                        errorCodes.add( ERR_MISSING_REQ );
                    }
                }
                else if ( fileItem != null )
                {
                    String fileName = FileUtil.getFileName( fileItem );
                    Element binaryDataElem = XMLTool.createElement( doc, itemElement, "binarydata", fileName );
                    binaryDataElem.setAttribute( "key", "%" + fileattachmentCount++ );
                }
            }

        }

        HttpServletRequest request = ServletRequestAccessor.getRequest();

        Boolean captchaOk = captchaService.validateCaptcha( formItems, request, "form", "create" );
        if ( ( captchaOk != null ) && ( !captchaOk ) )
        {
            errorCodes.add( ERR_INVALID_CAPTCHA );
        }

        // If one or more errors occurred, an exception is thrown, containing the errorcodes
        // and the resulting document (that now should include error XML):
        if ( errorCodes.size() > 0 )
        {
            throw new FormException( doc, errorCodes.toArray( new Integer[errorCodes.size()] ) );
        }
    }

    protected void handlerCreate( HttpServletRequest request, HttpServletResponse response, HttpSession session, ExtendedMap formItems,
                                  UserServicesService userServices, SiteKey siteKey )
        throws VerticalUserServicesException, VerticalCreateException, VerticalSecurityException, RemoteException
    {

        User user = securityService.getLoggedInPortalUser();
        VerticalSession vsession = (VerticalSession) session.getAttribute( VerticalSession.VERTICAL_SESSION_OBJECT );
        if ( vsession == null )
        {
            vsession = new VerticalSession();
            session.setAttribute( VerticalSession.VERTICAL_SESSION_OBJECT, vsession );
        }

        try
        {
            // get anonymous user if user is not logged in:
            if ( user == null )
            {
                user = userServices.getAnonymousUser();
            }

            // Get the form config XML
            int menuItemKey = formItems.getInt( "_form_id" );
            Document doc = userServices.getMenuItem( user, menuItemKey ).getAsDOMDocument();
            Element rootElement = doc.getDocumentElement();

            Element formElement = (Element) XMLTool.selectNode( rootElement, "/menuitems/menuitem/data/form" );
            formItems.put( "__form", formElement );

            // Find the title element
            String contentTitle = XMLTool.getElementText( XMLTool.getElement( formElement, "title" ) );
            Element[] itemElems = XMLTool.getElements( formElement, "item" );
            for ( int i = 0; i < itemElems.length; i++ )
            {
                if ( "true".equals( itemElems[i].getAttribute( "title" ) ) )
                {
                    String titleFormName = menuItemKey + "_form_" + ( i + 1 );
                    String subTitle = formItems.getString( titleFormName, null );
                    if ( subTitle != null && subTitle.length() > 0 )
                    {
                        contentTitle += ": " + subTitle;
                    }
                    break;
                }
            }

            // Find the category key
            int categoryKey = -1;
            String categoryKeyStr = formElement.getAttribute( "categorykey" );
            if ( categoryKeyStr != null && categoryKeyStr.length() > 0 )
            {
                categoryKey = Integer.parseInt( categoryKeyStr );
            }

            // Find email recipients
            Element recipientsElem = XMLTool.getElement( formElement, "recipients" );
            Element[] emailElems = XMLTool.getElements( recipientsElem, "e-mail" );
            ArrayList<String> emailAddresses = new ArrayList<String>();
            for ( Element emailElem : emailElems )
            {
                String email = XMLTool.getElementText( emailElem );
                if ( email != null && email.length() > 0 )
                {
                    emailAddresses.add( email );
                }
            }

            // Add the category key to formItems so buildXML method works (even if it is -1)
            formItems.put( "categorykey", String.valueOf( categoryKey ) );

            // Build the content XML and check for errors:
            String xmlData = buildXML( userServices, user, formItems, siteKey, contentTypeKey, contentTitle, false );

            // int contentKey = -1;

            int contentReference = -1;

            if ( categoryKey != -1 )
            {
                UserEntity runningUser = securityService.getUser( user );

                BinaryData[] binaries = null;
                if ( formItems.hasFileItems() )
                {
                    FileItem[] fileItems = getFileItems( formItems, menuItemKey );
                    binaries = new BinaryData[fileItems.length];
                    for ( int i = 0; i < fileItems.length; i++ )
                    {
                        binaries[i] = createBinaryData( fileItems[i] );
                    }
                }
                List<BinaryDataAndBinary> binaryDataAndBinaries = BinaryDataAndBinary.createNewFrom( binaries );

                boolean parseContentData = true; // always parse content data when creating content
                ContentAndVersion parsedContentAndVersion = contentParserService.parseContentAndVersion( xmlData, null, parseContentData );
                ContentEntity parsedContent = parsedContentAndVersion.getContent();
                ContentVersionEntity parsedVersion = parsedContentAndVersion.getVersion();

                CreateContentCommand createCommand = new CreateContentCommand();
                createCommand.setAccessRightsStrategy( CreateContentCommand.AccessRightsStrategy.INHERIT_FROM_CATEGORY );

                createCommand.populateCommandWithContentValues( parsedContent );
                createCommand.populateCommandWithContentVersionValues( parsedVersion );

                createCommand.setCreator( runningUser );

                createCommand.setBinaryDatas( binaryDataAndBinaries );
                createCommand.setUseCommandsBinaryDataToAdd( true );

                createCommand.setContentName(
                    new PrettyPathNameCreator( transliterate ).generatePrettyPathName( parsedVersion.getTitle() ) );

                ContentKey key = contentService.createContent( createCommand );

                contentReference = key.toInt();

            }

            // Mail the form posting:
            if ( emailAddresses.size() > 0 )
            {
                String[] emailAddr = emailAddresses.toArray( new String[emailAddresses.size()] );

                String subject = contentTitle;
                if ( contentReference > 0 )
                {
                    subject += " #" + contentReference;
                }

                mailForm( subject, user, menuItemKey, emailAddr, formElement, formItems );
            }

            Element sendReceiptElem = XMLTool.selectElement( formElement, "receipt/sendreceipt" );
            String sendReceipt = XMLTool.getElementText( sendReceiptElem );
            if ( sendReceipt != null && sendReceipt.equalsIgnoreCase( "yes" ) )
            {
                // mail reciept to person who submitted form
                mailReciept( menuItemKey, formElement, formItems, contentReference );
            }

            // Remove the error XML if content creation and/or e-mail dispatch was successfull
            vsession.removeAttribute( "error_form_create" );

            redirectToPage( request, response, formItems );
        }
        catch ( FormException e )
        {
            vsession.setAttribute( "error_form_create", e.doc );
            int[] tmp = new int[e.errorCodes.length];
            for ( int i = 0; i < e.errorCodes.length; i++ )
            {
                tmp[i] = e.errorCodes[i];
            }
            redirectToErrorPage( request, response, formItems, tmp, null );
        }
    }

    /**
     * Get fileItems in the same order as in buildContentTypeXML()
     *
     * @param formItems   Input from the user form.
     * @param menuItemKey The current menu item.
     * @return The selected file items.
     */
    private FileItem[] getFileItems( ExtendedMap formItems, int menuItemKey )
    {
        ArrayList<FileItem> fileItems = new ArrayList<FileItem>();

        // Elements in the old form XML are prefixed with an underscore
        Element formElement = (Element) formItems.get( "__form" );
        Element[] formItemsFromForm = XMLTool.getElements( formElement, "item" );

        for ( int i = 0; i < formItemsFromForm.length; i++ )
        {
            String formName = menuItemKey + "_form_" + ( i + 1 );
            String type = formItemsFromForm[i].getAttribute( "type" );
            if ( "fileattachment".equals( type ) )
            {
                FileItem fileItem = formItems.getFileItem( formName, null );

                if ( fileItem != null )
                {
                    fileItems.add( fileItem );
                }
            }
        }

        return fileItems.toArray( new FileItem[fileItems.size()] );
    }

    protected void mailForm( String subject, User user, int menuItemKey, String[] recipients, Element formElement, ExtendedMap formItems )
        throws VerticalUserServicesException
    {

        // don't waste time here if there are no recipients
        if ( recipients == null || recipients.length == 0 )
        {
            return;
        }

        StringBuffer body = createMailBody( menuItemKey, formElement, formItems );

        String fromEmail = null;
        Element fromEmailElem = XMLTool.selectElement( formElement, "item[@fromemail = 'true']" );
        if ( fromEmailElem != null )
        {
            int itemIndex = XMLTool.getElementIndex( fromEmailElem ) + 1;
            String formName = menuItemKey + "_form_" + itemIndex;
            fromEmail = formItems.getString( formName, null );
        }

        String fromName = null;
        Element fromNameElem = XMLTool.selectElement( formElement, "item[@fromname = 'true']" );
        if ( fromNameElem != null )
        {
            int itemIndex = XMLTool.getElementIndex( fromNameElem ) + 1;
            String formName = menuItemKey + "_form_" + itemIndex;
            fromName = formItems.getString( formName, null );
        }

        createAndSendMail( subject, user, recipients, formItems, body, fromEmail, fromName, true );
    }

    private void mailReciept( int menuItemKey, Element formElement, ExtendedMap formItems, int contentReference )
        throws VerticalUserServicesException
    {

        String toEmail = null;
        Element toEmailElem = XMLTool.selectElement( formElement, "item[@fromemail = 'true']" );
        if ( toEmailElem != null )
        {
            int itemIndex = XMLTool.getElementIndex( toEmailElem ) + 1;
            String formName = menuItemKey + "_form_" + itemIndex;
            toEmail = formItems.getString( formName, null );
        }

        if ( toEmail == null || toEmail.trim().equals( "" ) )
        {
            // If there are no e-mail recipient specified, check if there is a logged in user who can recieve a reciept
            UserEntity loggedInUser = securityService.getLoggedInPortalUserAsEntity();
            if ( loggedInUser != null )
            {
                toEmail = loggedInUser.getEmail();
            }
        }

        if ( toEmail == null || toEmail.trim().equals( "" ) )
        {
            // don't waste time here if there are no recipients
            return;
        }

        String[] recipient = new String[]{toEmail};

        Element receipt = XMLTool.getElement( formElement, "receipt" );
        String fromName = XMLTool.getElementText( XMLTool.getElement( receipt, "name" ) );
        String fromEmail = XMLTool.getElementText( XMLTool.getElement( receipt, "email" ) );
        String message = XMLTool.getElementText( XMLTool.getElement( receipt, "message" ) );
        String subject = XMLTool.getElementText( XMLTool.getElement( receipt, "subject" ) );
        if ( contentReference > 0 )
        {
            subject += " #" + contentReference;
        }
        String formData = XMLTool.getElementText( XMLTool.getElement( receipt, "includeform" ) );
        boolean includeFormData = true;
        if ( "no".equalsIgnoreCase( formData ) )
        {
            includeFormData = false;
        }

        StringBuffer body = new StringBuffer();
        if ( message != null )
        {
            body.append( HtmlUtils.htmlUnescape( message ) );
        }

        if ( includeFormData )
        {
            body.append( "\n\n" ).append( createMailBody( menuItemKey, formElement, formItems ) );
        }

        createAndSendMail( HtmlUtils.htmlUnescape( subject ), null, recipient, formItems, body, fromEmail, fromName, false );
    }


    private void createAndSendMail( String subject, User user, String[] recipients, ExtendedMap formItems, StringBuffer body,
                                    String fromEmail, String fromName, boolean addAttachment )
    {
        final SimpleMailTemplate formMail = new SimpleMailTemplate();
        if ( fromEmail != null )
        {
            formMail.setFrom( fromName, fromEmail );
        }
        else
        {
            if ( user.getType() != UserType.ANONYMOUS )
            {
                formMail.setFrom( user.getDisplayName(), user.getEmail() );
            }
            else
            {
                formMail.setFrom( "Anonymous", adminEmail );
            }
        }

        formMail.setSubject( subject );
        formMail.setMessage( body.toString() );
        for ( String recipient : recipients )
        {
            formMail.addRecipient( null, recipient, MailRecipientType.TO_RECIPIENT );
        }

        if ( addAttachment )
        {
            if ( formItems.hasFileItems() )
            {
                FileItem[] fileItems = formItems.getFileItems();
                for ( FileItem fileItem : fileItems )
                {
                    try
                    {
                        formMail.addAttachment( fileItem.getName(), fileItem.getInputStream() );
                    }
                    catch ( IOException e )
                    {
                        throw new RuntimeException( e );
                    }
                }
            }
        }
        sendMailService.sendMail( formMail );
    }

    private StringBuffer createMailBody( int menuItemKey, Element formElement, ExtendedMap formItems )
    {
        // build mail body
        StringBuffer body = new StringBuffer();
        Element[] items = XMLTool.getElements( formElement, "item" );
        for ( int i = 0; i < items.length; i++ )
        {
            String formName = menuItemKey + "_form_" + ( i + 1 );
            Element itemElement = items[i];
            String type = itemElement.getAttribute( "type" );

            if ( type.equals( "separator" ) )
            {
                body.append( "\n" );
            }
            else
            {
                body.append( itemElement.getAttribute( "label" ) ).append( ": " );
                if ( type.equals( "text" ) )
                {
                    body.append( formItems.getString( formName ) );
                    body.append( "\n" );
                }
                else if ( type.equals( "textarea" ) )
                {
                    body.append( "\n" );
                    body.append( formItems.getString( formName ) );
                    body.append( "\n" );
                }
                else if ( type.equals( "checkbox" ) )
                {
                    if ( formItems.getString( formName, "" ).equals( "on" ) )
                    {
                        body.append( "true" );
                    }
                    else
                    {
                        body.append( "false" );
                    }

                    body.append( "\n" );
                }
                else if ( type.equals( "radiobuttons" ) )
                {
                    body.append( formItems.getString( formName, "" ) );
                    body.append( "\n" );
                }
                else if ( type.equals( "dropdown" ) )
                {
                    body.append( formItems.getString( formName, "" ) );
                    body.append( "\n" );
                }
                else if ( type.equals( "checkboxes" ) )
                {
                    String[] values = formItems.getStringArray( formName );
                    for ( int j = 0; j < values.length; j++ )
                    {
                        String string = values[j];
                        if ( j > 0 )
                        {
                            body.append( ", " );
                        }
                        body.append( string );
                    }

                    body.append( "\n" );
                }
                else if ( type.equals( "fileattachment" ) )
                {
                    FileItem fileItem = formItems.getFileItem( formName, null );

                    if ( fileItem != null )
                    {
                        String fileName = FileUtil.getFileName( fileItem );
                        if ( fileName != null )
                        {
                            body.append( fileName );
                        }

                    }

                    body.append( "\n" );
                }
            }
        }
        return body;
    }

    @Value("${cms.admin.email}")
    public void setAdminEmail( final String adminEmail )
    {
        this.adminEmail = adminEmail;
    }

    @Autowired
    public void setSendMailService( final SendMailService sendMailService )
    {
        this.sendMailService = sendMailService;
    }
}
