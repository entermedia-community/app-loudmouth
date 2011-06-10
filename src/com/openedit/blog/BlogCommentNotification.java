/*
 * Created on Oct 15, 2006
 */
package com.openedit.blog;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermedia.email.PostMail;
import org.entermedia.email.TemplateWebEmail;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.users.User;
import com.openedit.users.UserManager;
public class BlogCommentNotification
{
	protected PageManager fieldPageManager;
	protected UserManager fieldUserManager;
	private static final Log log = LogFactory.getLog(BlogCommentNotification.class);
	private PostMail postMail;
	public PostMail getPostMail() {
		return postMail;
	}

	public void setPostMail(PostMail postMail) {
		this.postMail = postMail;
	}

	public void commentAdded( WebPageRequest inReq, Blog inBlog, BlogEntry inEntry, BlogComment inComment) throws OpenEditException
	{
		if( !inBlog.isUseNotification() )
		{
			return;
		}
		
//		if( inReq.getUser() == inComment.getUser())
//		{
//			return;
//		}
		//notify the previous poster. If no previous poster then notify the author of the entry
		Set notify = new HashSet();

		if( inEntry.getUser() != null)
		{
			String email = inEntry.getUser().getEmail();
			if( email != null)
			{
				notify.add(email);
			}
		}

		for (Iterator iterator = inEntry.getComments().iterator(); iterator.hasNext();)
		{
			BlogComment lastcomment = (BlogComment)iterator.next();
			User user = lastcomment.getUser();
			if( user != null) //legacy comments
			{
				String email = user.getEmail();
				if( email != null)
				{
					notify.add(email);
				}
			}
		}
		if( inReq.getUser() != null && inReq.getUser().getEmail() != null)
		{
			notify.remove(inReq.getUser().getEmail()); //Dont email yourself
		}
		
		if( notify.size() > 0 )
		{
			//send email out
			inReq.putPageValue("comment", inComment);
			inReq.putPageValue("entry", inEntry);
			inReq.putPageValue("blog", inBlog);
			
			try
			{
				Page page = getPageManager().getPage( inBlog.getBlogHome() + "/layout/commentemail.html");
				TemplateWebEmail template = postMail.getTemplateWebEmail();
				template.setMailTemplatePage(page);
				template.setWebPageContext(inReq.copy(page));
				template.loadSettings(template.getWebPageContext());
				String sub = template.getSubject();
				sub = "[" + inBlog.getHostName() + "] " + sub;
				template.setSubject(sub);
				for (Iterator iterator = notify.iterator(); iterator.hasNext();)
				{
					String email = (String) iterator.next();
					template.setTo(email);						
					template.send();
				}
				//new PostMail().postMail(email, "Blog Comment", "A new comment has been posted \n" + inEntry.getLink(), "support@openedit.org");
			}
			catch ( Exception ex)
			{
				log.error(ex);
			}
		}
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	public UserManager getUserManager()
	{
		return fieldUserManager;
	}

	public void setUserManager(UserManager inUserManager)
	{
		fieldUserManager = inUserManager;
	}
}
