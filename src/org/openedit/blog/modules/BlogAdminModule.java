/*
 * Created on Jan 13, 2006
 */
package org.openedit.blog.modules;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermedia.email.PostMail;
import org.entermedia.email.TemplateWebEmail;

import com.openedit.BaseWebPageRequest;
import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.WebServer;
import com.openedit.blog.BlogEntry;
import com.openedit.blog.Notify;
import com.openedit.modules.BaseModule;
import com.openedit.modules.html.Tidy;
import com.openedit.page.Page;
import com.openedit.page.PageRequestKeys;
import com.openedit.page.PageStreamer;
import com.openedit.util.URLUtilities;
public class BlogAdminModule extends BaseModule
{
	private static final Log log = LogFactory.getLog(BlogAdminModule.class);
	private PostMail postMail;
	public void cancelNotification( WebPageRequest inReq)
	{
		Notify notify = getNotify(inReq);
		notify.cancel();
		inReq.removeSessionValue("notify");
		log.info("Canceled job");
	}
	
	public Notify getNotify(WebPageRequest inReq)
	{
		Notify notify = (Notify)inReq.getSessionValue("notify");
		if ( notify == null)
		{
			notify = new Notify();
			notify.setUserManager(getUserManager());
			notify.setRootDirectory(getRoot());
			notify.setPageManager(getPageManager());
			inReq.putSessionValue("notify",notify);
		}
		return notify;
	}
	
	public void sendNotification(WebPageRequest inReq) throws OpenEditException, IOException
	{
		Notify notify = getNotify(inReq);
		if ( notify.isKeepRunning() )
		{
			//already running
			return;
		}
		
		/* Warning: this should be group ids, not names */
		String[] groupnames = inReq.getRequestParameters("groupnames");
		if ( groupnames != null && groupnames.length > 0)
		{
			notify.setGroupNames(groupnames);
			
			TemplateWebEmail mailer = postMail.getTemplateWebEmail();
			mailer.setPageManager(getPageManager());
			
			String from = inReq.getRequestParameter("author");
			String server = inReq.getRequestParameter("server");
			String subject = inReq.getRequestParameter("title");
			//String path = inReq.getRequestParameter("editPath");
			BlogModule module = (BlogModule)getModule("BlogModule");
			BlogEntry entry = module.getEntry(inReq);
			mailer.setFrom(from);
			mailer.setSubject(subject);
			mailer.setMailTemplatePath(entry.getPath());
			Page content = getPageManager().getPage(entry.getPath());

			String uselayout = cleanLink( inReq.getRequestParameter("uselayout"), inReq );
			if (uselayout != null )
			{
				String results = renderUserLayout(inReq, content, uselayout);
				mailer.setMessage(results);
				
				String addplaintext = inReq.getRequestParameter("addplaintext");
				if ( addplaintext != null && addplaintext.equalsIgnoreCase("true"))
				{
					String alternative = content.getContent();
					alternative = new Tidy().removeHtml(alternative);
					mailer.setAlternativeMessage(alternative);
				}
			}
			else
			{
				mailer.setMailTemplatePage(content);
			}
			
			//mailer.setMailTemplatePage(template);	
			notify.sendEmail(mailer, inReq.getWriter() );
		}		
		else
		{
			inReq.getWriter().write("Error: No groups selected");
		}
	}
	private String cleanLink(String inRequestParameter, WebPageRequest inReq)
	{
		if ( inRequestParameter != null)
		{
			String home = (String)inReq.getPageValue("home");
			if ( home != null && home.length() > 0)
			{
				inRequestParameter = inRequestParameter.substring(home.length(),inRequestParameter.length());
			}
		}
		return inRequestParameter;
	}

	public void renderPreview(WebPageRequest inReq ) throws OpenEditException
	{
		String uselayout = cleanLink( inReq.getRequestParameter("uselayout"), inReq );
		BlogModule module = (BlogModule)getModule("BlogModule");
		BlogEntry entry = module.getEntry(inReq);

		Page content = getPageManager().getPage(entry.getPath());
		String preview = null;
		
		if ( uselayout != null)
		{
			preview = renderUserLayout(inReq,content,uselayout);
		}
		else
		{
			preview = content.getContent(); //TODO: Use a notify render for a true preview
		}
		inReq.putPageValue("preview",preview);
	}
	protected String renderUserLayout(WebPageRequest inReq, Page content, String uselayout) throws OpenEditException
	{
		WebServer webserver = (WebServer)getBeanFactory().getBean("WebServer");
		BaseWebPageRequest req = (BaseWebPageRequest)inReq.copy(content);
		req.putPageValue(PageRequestKeys.LAYOUTOVERRIDE,uselayout);
		req.putPageValue(PageRequestKeys.INNERLAYOUTOVERRIDE,Page.BLANK_LAYOUT); //TODO: Find nicer way
		req.putPageValue(PageRequestKeys.CONTENT,content);
		req.putProtectedPageValue(PageRequestKeys.PAGE,content);
		req.setEditable(false);
		req.putPageValue(PageRequestKeys.OUTPUT_WRITER, new StringWriter());
		PageStreamer stream = webserver.getOpenEditEngine().createPageStreamer(content,req);
		stream.render();
		String fcontent = req.getWriter().toString();
		return fcontent;
	}
	public void fixLinks(WebPageRequest inReq) throws OpenEditException
	{
		URLUtilities utils = (URLUtilities)inReq.getPageValue(PageRequestKeys.URL_UTILITIES);
		if ( utils != null)
		{
			String base = utils.siteRoot() + utils.relativeHomePrefix();
			Notify notify = getNotify(inReq);
			String path = inReq.getRequestParameter("editPath");
			Page template = getPageManager().getPage(path);
			notify.fixLinks(template,base);
			inReq.putPageValue("message","Links have been set to " + base);
		}

	}

	public PostMail getPostMail() {
		return postMail;
	}

	public void setPostMail(PostMail postMail) {
		this.postMail = postMail;
	}
}
