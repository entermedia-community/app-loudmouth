/*
 * Created on Oct 18, 2006
 */
package org.openedit.blog.archive;

import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.openedit.links.LinkTree;
import org.openedit.links.XmlLinkLoader;
import org.openedit.repository.ContentItem;
import org.openedit.repository.filesystem.StringItem;

import com.openedit.OpenEditException;
import com.openedit.blog.Blog;
import com.openedit.blog.BlogEntry;
import com.openedit.config.Configuration;
import com.openedit.page.Page;
import com.openedit.users.User;
import com.openedit.util.FileUtils;
import com.sun.syndication.feed.synd.SyndCategoryImpl;
import com.sun.syndication.feed.synd.SyndContentImpl;

public class EntryArchive extends BaseArchive
{
	protected BlogCommentArchive fieldCommentArchive;
	
	public BlogEntry createEntry(Blog inBlog, Page inPath) throws OpenEditException
	{
//		if( !inPath.exists() )
//		{
//			return null;
//		}
		BlogEntry entry = new BlogEntry();
		entry.setCommentArchive(getCommentArchive());
		entry.setPath(inPath.getPath());
		entry.setLink(inBlog.getHostName() + inPath);

		entry.setPageManager(getPageManager());
		entry.setLinkTree(inBlog.getLinkTree());

		Configuration config = inPath.getPageSettings().getUserDefined("blog");
		if (config != null) {
			entry.setTitle(config.getAttribute("title"));
			entry.setAuthor(config.getAttribute("author"));

			String username = config.getAttribute("username");
			if (username != null) {
				User user = getUserManager().getUser(username);
				entry.setUser(user);
			}
			String pubdate = config.getChildValue("publishdate");
			String visible = config.getChildValue("visible");
			for (Iterator iter = config.getChildIterator("property"); iter.hasNext();) {
				Configuration element = (Configuration) iter.next();
				String value = element.getValue();
				String key = element.getAttribute("id");
				if (key != null && value != null) {
					entry.addProperty(key, value);
				}
			}
			config.getChildIterator("property");
			Configuration notificationslist = config.getChild("notifications");
			if (notificationslist != null) {
				for (Iterator iter = notificationslist.getChildIterator("user"); iter.hasNext();) {
					Configuration element = (Configuration) iter.next();
					String key = element.getValue();
					User user = getUserManager().getUser(key);
					entry.addNotify(user);

				}
			}
			entry.setVisible(visible == null || visible.equals("true"));
			entry.setPublishedDate(entry.parse(pubdate));
			List categories = new ArrayList();
			for (Iterator iter = config.getChildren("category").iterator(); iter.hasNext();) {
				Configuration element = (Configuration) iter.next();
				SyndCategoryImpl cat = new SyndCategoryImpl();
				cat.setName(element.getValue());
				categories.add(cat);
			}
			entry.setCategories(categories);
			entry.setDescription(inPath.getContent());
		} else {
			entry.setTitle(inPath.get("title"));
			entry.setPublishedDate(inPath.getLastModified());
			ContentItem item = getPageManager().getLatestVersion(entry.getPath());
			if (item != null) {
				entry.setAuthor(item.getAuthor());
				User user = getUserManager().getUser(item.getAuthor());
				entry.setUser(user);
			}
		}
		return entry;
	}

	/**
	 * 
	 */
	public void saveLinks(Blog inBlog) throws OpenEditException {

		String slink = inBlog.getBlogHome() + "/permalinks.xml";

		Page linkpage = getPageManager().getPage(slink);
		Writer out = new StringWriter();
		try {
			XmlLinkLoader loader = new XmlLinkLoader();
			loader.saveLinks(inBlog.getLinkTree(), out, linkpage.getCharacterEncoding());
		} finally {
			FileUtils.safeClose(out);
		}
		StringItem item = new StringItem(linkpage.getPath(), out.toString(), linkpage.getCharacterEncoding());
		linkpage.setContentItem(item);
		getPageManager().putPage(linkpage);

	}

	/**
	 * 
	 */
	public void loadLinks(Blog inBlog) throws OpenEditException {
		String slink = inBlog.getBlogHome() + "/permalinks.xml";
		Page linkpage = getPageManager().getPage(slink);
		if (!linkpage.exists()) {
			slink = inBlog.getBlogHome() + "/links.xml";
			linkpage = getPageManager().getPage(slink);
			if (!linkpage.exists()) {
				throw new OpenEditException("could not find " + slink);
			}
		}

		XmlLinkLoader loader = new XmlLinkLoader();
		LinkTree tree = loader.loadLinks(linkpage,null);
		inBlog.setLinkTree(tree);
	}
	public BlogCommentArchive getCommentArchive()
	{
		return fieldCommentArchive;
	}
	public void setCommentArchive(BlogCommentArchive inCommentArchive)
	{
		fieldCommentArchive = inCommentArchive;
	}

	public BlogEntry createEntry(Blog inBlog, User inUser) {
		BlogEntry entry = new BlogEntry();
		//
		entry.setCommentArchive(getCommentArchive());
		Date now = new Date();
		entry.setPublishedDate(now);

		SimpleDateFormat format = new SimpleDateFormat("yyyy/M/d/HHmmss");
		String path = inBlog.getArchiveRootDirectory() + "/" + format.format(now) + ".html";

		entry.setPath(path);

		entry.setLink(inBlog.getHostName() + path);

		entry.setLinkTree(inBlog.getLinkTree());
		entry.setPageManager(getPageManager());
		SyndContentImpl content = new SyndContentImpl();
		content.setType("text/html");
		entry.setDescription(content);

		entry.setUser(inUser);
		return entry;
	}

}
