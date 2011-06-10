/*
 * Created on Feb 21, 2006
 */
package org.openedit.blog.archive;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.openedit.links.Link;
import org.openedit.repository.filesystem.StringItem;

import com.openedit.OpenEditException;
import com.openedit.blog.Blog;
import com.openedit.blog.BlogEntry;
import com.openedit.config.Configuration;
import com.openedit.config.XMLConfiguration;
import com.openedit.page.Page;
import com.openedit.users.User;

public class BlogArchive extends BaseArchive
{
	protected EntryArchive fieldEntryArchive;
	
	private static final Log log = LogFactory.getLog(BlogArchive.class);
	
	public boolean hasChanged(Blog inBlog) throws OpenEditException
	{
		Page settings = getPageManager().getPage(inBlog.getBlogHome() + "/blogsettings.xml");
		Date date = settings.getLastModified();
		
		boolean same = inBlog.getLastModified().equals(date);
		return !same;
		
	}

	public void loadBlog(Blog inBlog) throws OpenEditException
	{
		Page settings = getPageManager().getPage(inBlog.getBlogHome() + "/blogsettings.xml");
		//read in some XML
		Reader read = settings.getReader();

		inBlog.setPageManager(getPageManager());
		inBlog.setUserManager(getUserManager());
		inBlog.setEntryArchive(getEntryArchive());
		Element root = null;
			root = getXmlUtil().getXml(read, "UTF-8");
		
		inBlog.setTitle( root.elementText("title") );
		inBlog.setHostName(root.elementText("hostname"));
		inBlog.setAuthor(root.elementText("author"));
		inBlog.setDescription(root.elementText("description"));
		
		inBlog.setAllowAnonymous("true".equals(root.elementText("allow-anonymous-comments")));
	
		String dir = root.elementText("archive-root-directory");
		if( dir != null)
		{
			if( dir.endsWith("/"))
			{
				dir = dir.substring(0,dir.length() - 1);
			}
			inBlog.setArchiveRootDirectory(dir);
		}
		else
		{
			inBlog.setArchiveRootDirectory(inBlog.getBlogHome() + "/permalink");
		}
		
		//inBlog.setBlogRoot(root.elementText("blogroot"));
		String autoPublishCommentsStr = root.elementText("auto-publish-comments");
		if ( autoPublishCommentsStr == null )
		{
			inBlog.setAutoPublishingComments( true );
		}
		else
		{
			inBlog.setAutoPublishingComments(Boolean.parseBoolean(autoPublishCommentsStr));
		}		
		inBlog.setAutoPublishEntries(Boolean.parseBoolean( root.elementText("auto-publish-entries") ) );
		inBlog.setUseNotification(Boolean.parseBoolean( root.elementText("use-notification") ) );
			
//		List questions = new ArrayList();
//		for (Iterator iter = root.elementIterator("question"); iter.hasNext();)
//		{
//			Element q = (Element) iter.next();
//			Question ques = new Question();
//			ques.setId(q.attributeValue("id"));
//			ques.setDescription(q.attributeValue("description"));
//			ques.setAnswer(q.attributeValue("answer"));
//			questions.add(ques);
//		}
//		inBlog.setQuestions(questions);
		inBlog.setLastModified(settings.getLastModified());
		getEntryArchive().loadLinks(inBlog);
	}
	
	public void saveBlog(Blog inBlog, Writer inWriter, String inEncoding) throws IOException
	{
		Element root = DocumentHelper.createElement("blog");
		root.addElement("title").setText(inBlog.getTitle());
		root.addElement("hostname").setText(inBlog.getHostName());
		root.addElement("author").setText(inBlog.getAuthor());
		root.addElement("description").setText(inBlog.getDescription());
		//root.addElement("blogroot").setText(inBlog.getBlogRoot());
		root.addElement("allow-anonymous-comments").setText(String.valueOf(inBlog.getAllowAnonymous()));
		root.addElement("auto-publish-comments").setText(String.valueOf(inBlog.isAutoPublishingComments()));
		root.addElement("auto-publish-entries").setText(String.valueOf(inBlog.isAutoPublishEntries()));
		//auto-publish-entries
		//use-notification
		root.addElement("archive-root-directory").setText(inBlog.getArchiveRootDirectory());
		
		Document doc = DocumentHelper.createDocument( root );
		
		getXmlUtil().saveXml( doc, inWriter,inEncoding);
	}
	
	public void saveEntry(Blog inBlog, BlogEntry inEntry) throws OpenEditException
	{
		Page page = getPageManager().getPage(inEntry.getPath());
		String content = "";
		if ( inEntry.getDescription() != null)
		{
			content = inEntry.getDescription().getValue();
		}
		StringItem item = new StringItem(inEntry.getPath(),content,"UTF-8");
		page.setContentItem(item);
		getPageManager().putPage(page);
		
		//save the xconf
		XMLConfiguration pageconfig = new XMLConfiguration("page");
		XMLConfiguration  blog = (XMLConfiguration)pageconfig.addChild("blog");
		blog.setAttribute("author",inEntry.getAuthor());
		if(inEntry.getUser() != null){
		blog.setAttribute("username",inEntry.getUser().getUserName());
		}
		blog.setAttribute("title",inEntry.getTitle());
		blog.addChild("publishdate").setValue(inEntry.getGmtStandard().format(inEntry.getPublishedDate()));
		blog.addChild("visible").setValue(String.valueOf(inEntry.isVisible()));
	
		for (Iterator iter = inEntry.getProperties().keySet().iterator(); iter.hasNext();) {
			String key = (String) iter.next();
			String value = inEntry.getProperty(key);
			if(key != null && value != null){
				XMLConfiguration c = (XMLConfiguration)blog.addChild("property");
				c.setAttribute("id", key);
				c.setValue(value);				
			}			
		}
		Configuration notifications = blog.addChild("notifications");
		for (Iterator iterator = inEntry.getNotify().iterator(); iterator.hasNext();) {
			User user = (User) iterator.next();
			notifications.addChild("user").setValue(user.getUserName());
		}
		
		for (Iterator iter = inEntry.getProperties().keySet().iterator(); iter.hasNext();) {
			String key = (String) iter.next();
			String value = inEntry.getProperty(key);
			if(key != null && value != null){
				XMLConfiguration c = (XMLConfiguration)blog.addChild("property");
				c.setAttribute("id", key);
				c.setValue(value);				
			}			
		}
	
		StringItem xconf = new StringItem(page.getPageSettings().getXConf().getPath(),pageconfig.toXml("UTF-8"),"UTF-8");
		getPageManager().getPageSettingsManager().saveSetting(xconf);
		getPageManager().clearCache(page);
		
		Link existing = inEntry.getLinkTree().getLink(inEntry.getId());
		if ( existing == null)
		{
			//add it to the link.xml file
			Link link = new Link();
			link.setId(inEntry.getId());
			link.setPath(inEntry.getPath());
			//getLinkTree().removeLink(link);
			inEntry.getLinkTree().insertLink("index",link);
		}
		getEntryArchive().saveLinks(inBlog);

	
	}

	public EntryArchive getEntryArchive()
	{
		return fieldEntryArchive;
	}

	public void setEntryArchive(EntryArchive inEntryArchive)
	{
		fieldEntryArchive = inEntryArchive;
	}

}
