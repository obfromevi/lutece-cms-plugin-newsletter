/*
 * Copyright (c) 2002-2012, Mairie de Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.newsletter.util;

import fr.paris.lutece.plugins.document.business.Document;
import fr.paris.lutece.plugins.newsletter.business.NewsLetterTemplate;
import fr.paris.lutece.plugins.newsletter.business.NewsLetterTemplateHome;
import fr.paris.lutece.portal.service.html.EncodingService;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.template.AppTemplateService;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPathService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import fr.paris.lutece.util.ReferenceList;
import fr.paris.lutece.util.html.HtmlTemplate;
import fr.paris.lutece.util.string.StringUtil;
import fr.paris.lutece.util.url.UrlItem;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * This classe provides utility methods for newsletters.
 */
public final class NewsletterUtils
{
    // This class implements the Singleton design pattern.
    private static NewsletterUtils _singleton;
    private static final String MARK_VIRTUAL_HOSTS = "virtual_hosts";
    private static final String PROPERTY_VIRTUAL_HOST = "virtualHost.";
    private static final String SUFFIX_BASE_URL = ".baseUrl";

    /**
     * Constructor
     */
    private NewsletterUtils(  )
    {
        if ( _singleton == null )
        {
            _singleton = this;
        }
    }

    /**
     * Fetches the singleton instance
     *
     * @return The singleton instance
     */
    public static NewsletterUtils getInstance(  )
    {
        if ( _singleton == null )
        {
            _singleton = new NewsletterUtils(  );
        }

        return _singleton;
    }

    /**
     * Retrieve the html template for the given template id
     *
     * @param nTemplateId the id of the template to retrieve
     * @param plugin the plugin
     * @return the html template to use of null if no NewsletterTemplate found for this Id
     */
    public static String getHtmlTemplatePath( int nTemplateId, Plugin plugin )
    {
        NewsLetterTemplate newsletterTemplate = NewsLetterTemplateHome.findByPrimaryKey( nTemplateId, plugin );

        if ( ( newsletterTemplate == null ) || ( newsletterTemplate.getFileName(  ) == null ) ||
                newsletterTemplate.getFileName(  ).equals( "" ) )
        {
            return null;
        }

        String strTemplatePathName = AppPropertiesService.getProperty( plugin.getName(  ) +
                NewsLetterConstants.PROPERTY_PATH_FILE_NEWSLETTER_TEMPLATE );
        strTemplatePathName += "/";
        strTemplatePathName += newsletterTemplate.getFileName(  );

        return strTemplatePathName;
    }

    /**
     * Cleans a string in order to make it usable in a javascript script
     *
     * @param strIn the string to clean
     * @return the javascript escaped String
     */
    public static String convertForJavascript( String strIn )
    {
        // Convert problem characters to JavaScript Escaped values
        if ( strIn == null )
        {
            return "";
        }

        String strOut = strIn;

        strOut = StringUtil.substitute( strOut, "\\\\", "\\" ); // replace backslash with \\

        strOut = StringUtil.substitute( strOut, "\\\'", "'" ); // replace an single quote with \'

        strOut = StringUtil.substitute( strOut, "\\\"", "\"" ); // replace a double quote with \"

        strOut = StringUtil.substitute( strOut, "\\r", "\r\n" ); // replace CR // with \r;

        strOut = StringUtil.substitute( strOut, "\\n", "\n" ); // replace LF with \n;

        return strOut;
    }

    /**
     * Fills a given document template with the document data
     *
     * @return the html code corresponding to the document data
     * @param strBaseUrl The base url of the portal
     * @param strTemplatePath The path of the template file
     * @param document the object gathering the document data
     * @param nPortletId the portlet id
     * @param plugin the plugin needed to retrieve properties
     * @param locale the locale used to build the template
     */
    public static String fillTemplateWithDocumentInfos( String strTemplatePath, Document document, int nPortletId,
        Plugin plugin, Locale locale, String strBaseUrl )
    {
        Map<String, Object> model = new HashMap<String, Object>(  );
        model.put( NewsLetterConstants.MARK_DOCUMENT, document );

        try
        {
            if ( AppPathService.getAvailableVirtualHosts(  ) != null )
            {
                ReferenceList hostKeysList = AppPathService.getAvailableVirtualHosts(  );
                ReferenceList list = new ReferenceList(  );

                for ( int i = 0; i < hostKeysList.size(  ); i++ )
                {
                    list.addItem( hostKeysList.get( i ).getName(  ),
                        AppPropertiesService.getProperty( PROPERTY_VIRTUAL_HOST + hostKeysList.get( i ).getCode(  ) +
                            SUFFIX_BASE_URL ) );
                }

                model.put( MARK_VIRTUAL_HOSTS, list );
            }
        }
        catch ( NullPointerException e )
        {
            AppLogService.debug( e );
        }

        model.put( NewsLetterConstants.MARK_BASE_URL, strBaseUrl );
        model.put( NewsLetterConstants.MARK_DOCUMENT_THUMBNAIL, document.getThumbnail(  ) );
        model.put( NewsLetterConstants.MARK_DOCUMENT_PORTLET_ID, nPortletId );

        HtmlTemplate template = AppTemplateService.getTemplate( strTemplatePath, locale, model );

        return template.getHtml(  );
    }

    /**
     * Rewrite relatives url to absolutes urls
     *
     * @param strContent The content to analyze
     * @param strBaseUrl The base url
     * @return The converted content
     */
    public static String rewriteUrls( String strContent, String strBaseUrl )
    {
        HtmlDocumentNewsletter doc = new HtmlDocumentNewsletter( strContent, strBaseUrl );
        doc.convertAllRelativesUrls( HtmlDocumentNewsletter.ELEMENT_IMG );
        doc.convertAllRelativesUrls( HtmlDocumentNewsletter.ELEMENT_A );
        doc.convertAllRelativesUrls( HtmlDocumentNewsletter.ELEMENT_FORM );
        doc.convertAllRelativesUrls( HtmlDocumentNewsletter.ELEMENT_CSS );
        doc.convertAllRelativesUrls( HtmlDocumentNewsletter.ELEMENT_JAVASCRIPT );

        return doc.getContent(  );
    }
    
	/**
	 * Rewrite secured omg urls to absolutes urls
	 * 
	 * @param strContent The content to analyze
	 * @param strBaseUrl The base url
	 * @return The converted content
	 */
	public static String rewriteImgUrls( String strContent, String strBaseUrl, String strUnsecuredBaseUrl, String strUnsecuredFolderPath, String strUnsecuredFolder )
	{
		HtmlDocumentNewsletter doc = new HtmlDocumentNewsletter( strContent, strBaseUrl );
		doc.convertUrlsToUnsecuredUrls( HtmlDocumentNewsletter.ELEMENT_IMG, strUnsecuredBaseUrl, strUnsecuredFolderPath, strUnsecuredFolder );
		doc.convertUrlsToUnsecuredUrls( HtmlDocumentNewsletter.ELEMENT_A, strUnsecuredBaseUrl, strUnsecuredFolderPath, strUnsecuredFolder );
		return doc.getContent();
	}

    /**
     * Encode a string for passage in parameter in URL
     *
     * @param strEntry the string entry
     * @return the string encoding
     */
    public static String encodeForURL( String strEntry )
    {
        return EncodingService.encodeUrl( strEntry );
    }

    /**
     * Addition of information as header of the http response
     *
     * @param request The Http Request
     * @param response The Http Response
     * @param strFileName THe filename of the file
     * @param strFileExtension The file extension
     */
    public static void addHeaderResponse( HttpServletRequest request, HttpServletResponse response, String strFileName,
        String strFileExtension )
    {
        response.setHeader( "Content-Disposition", "attachment ;filename=\"" + strFileName + "\"" );

        if ( strFileExtension.equals( ".csv" ) )
        {
            response.setContentType( "application/csv" );
        }
        else
        {
            String strMimeType = request.getSession(  ).getServletContext(  ).getMimeType( strFileName );

            if ( strMimeType != null )
            {
                response.setContentType( strMimeType );
            }
            else
            {
                response.setContentType( "application/octet-stream" );
            }
        }

        response.setHeader( "Pragma", "public" );
        response.setHeader( "Expires", "0" );
        response.setHeader( "Cache-Control", "must-revalidate,post-check=0,pre-check=0" );
    }

    /**
     * Adds all parameter values to the urlItem
     * @param urlItem the urlItem
     * @param strParameterName the name of the parameter which has multiple values
     * @param values parameter values
     */
    public static void addParameters( UrlItem urlItem, String strParameterName, String[] values )
    {
        for ( String strParameterValue : values )
        {
            urlItem.addParameter( strParameterName, strParameterValue );
        }
    }
}
