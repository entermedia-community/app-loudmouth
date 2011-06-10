/*
 * Created on Oct 18, 2006
 */
package org.openedit.blog.archive;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.openedit.repository.filesystem.FileItem;

import com.openedit.OpenEditException;
import com.openedit.blog.Blog;
import com.openedit.blog.BlogComment;
import com.openedit.blog.BlogEntry;
import com.openedit.page.Page;
import com.openedit.users.User;
import com.openedit.users.UserManager;
import com.openedit.util.FileUtils;
import com.openedit.util.PathUtilities;

//TODO: Extend CommentArchive

public class BlogCommentArchive extends BaseArchive
{
	private static final Log log = LogFactory.getLog(BlogCommentArchive.class);
	
	protected BlogComment loadComment(BlogEntry inEntry, Element inCommentElement) throws ParseException
	{
		BlogComment comment = new BlogComment();
		comment.setAuthor(inCommentElement.attributeValue("author"));
		
		String username = inCommentElement.attributeValue("username");
		if( username != null)
		{
			User user = getUserManager().getUser(username);
			comment.setUser(user);
		}
		
		String id = inCommentElement.attributeValue("id");
		comment.setId(id);
		comment.setContent(inCommentElement.getTextTrim());
		Date date = inEntry.parse( inCommentElement.attributeValue("date"));
		comment.setDateTime(date);
		String visible = inCommentElement.attributeValue("visible");
		if ( visible == null )
		{
			visible = "true";
		}
		comment.setVisible(Boolean.parseBoolean(visible));
		return comment;
	}

	public void loadComments(BlogEntry inEntry) throws OpenEditException
	{
		Page page = commentsPage(inEntry);
		
		if ( page.exists() )
		{
			log.debug( "Loading comments for entry " + inEntry.getId() + " from " + page.getPath() );
			Reader reader = page.getReader();
			try
			{
				Element root = getXmlUtil().getXml(reader, page.getCharacterEncoding());
				for (Iterator iter = root.elementIterator("comment"); iter.hasNext();)
				{
					Element element = (Element) iter.next();
					BlogComment comment = loadComment(inEntry, element);
					inEntry.addComment(comment);
				}
			}
			catch ( Exception ex)
			{
				throw new OpenEditException(ex);
			}
			finally
			{
				FileUtils.safeClose(reader);
			}
		}
	}

	public void setUserManager(UserManager inUserManager)
	{
		fieldUserManager = inUserManager;
	}
	protected Page commentsPage(BlogEntry inEntry) throws OpenEditException
	{
		String dir = PathUtilities.extractPagePath(inEntry.getPath());
		String path = dir + "-comments.xml";

		Page page = getPageManager().getPage(path);
		return page;
	}


	public void saveComments(Blog inBlog, BlogEntry inEntry) throws OpenEditException
	{
		Page page = commentsPage(inEntry);
		Element root = DocumentHelper.createDocument().addElement("comments");
		for (Iterator iter = inEntry.getComments().iterator(); iter.hasNext();)
		{
			BlogComment com = (BlogComment) iter.next(); //	<comment author="admin" date="Feb 18, 2005 2:42:29 PM">This is a snide remark</comment>

			Element comment = root.addElement("comment");
			comment.addAttribute("author",com.getAuthor());
			if( com.getUser() != null)
			{
				comment.addAttribute("username",com.getUser().getUserName());
			}
			
			comment.addAttribute("date",inEntry.getGmtStandard().format(com.getDateTime()));
			comment.addAttribute("id",com.getId());
			comment.addAttribute("visible", String.valueOf(com.isVisible()));
			comment.setText(com.getContent());
		}
		try
		{
			//TODO: Add locking
			File tmp = File.createTempFile("blog", "junk");
			getXmlUtil().saveXml(root.getDocument(), tmp);
			FileItem item = new FileItem();
			item.setFile(tmp);
			item.setPath(page.getPath());
			Page tmpPage = new Page(page);
			tmpPage.setContentItem(item);

			getPageManager().copyPage(tmpPage, page); //Copy over as a tmp file in case there is a problem
			tmp.delete();
		}
		catch (IOException ex)
		{
			throw new OpenEditException(ex);
		}
	}

	
}
