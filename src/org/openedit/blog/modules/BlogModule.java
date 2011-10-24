/*
 * Created on Feb 18, 2005
 */
package org.openedit.blog.modules;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.blog.archive.BlogArchive;
import org.openedit.repository.filesystem.StringItem;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.blog.Blog;
import com.openedit.blog.BlogComment;
import com.openedit.blog.BlogCommentNotification;
import com.openedit.blog.BlogEntry;
import com.openedit.blog.LuceneBlogSearcher;
import com.openedit.modules.BaseModule;
import com.openedit.modules.admin.users.Question;
import com.openedit.modules.html.EditorSession;
import com.openedit.page.Page;
import com.openedit.page.PageRequestKeys;
import com.openedit.users.Group;
import com.openedit.users.User;
import com.openedit.users.filesystem.FileSystemUser;
import com.openedit.util.URLUtilities;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.SyndFeedOutput;

/**
 * @author cburkey
 * 
 */
public class BlogModule extends BaseModule {
	protected Map fieldBlogs;
	private static final Log log = LogFactory.getLog(BlogModule.class);
	protected BlogCommentNotification fieldCommentNotification;

	public Blog getBlog(WebPageRequest req) throws OpenEditException {
		Page inPath = req.getPage();

		String catalogid = inPath.get("blogid");
		if (catalogid == null) {
			catalogid = inPath.get("bloghome");
			catalogid = catalogid.substring(1);
		}
		String home = "/" + catalogid;
		if (home == null) {
			Blog blog = (Blog) req.getPageValue("blog"); // already loaded once
			if (blog != null) {
				return blog;
			}

			throw new OpenEditException("Need to define bloghome page property");
		}

		Blog blog = getBlog(home);
		if (blog.getHostName() == null) {
			log.info("no hostname specified in blogsettings.xml, attempting to auto configure");
			String hostname = req.findValue("hostName");
			if (hostname == null) {
				log.info("no value for hostName specified in properties");
				URLUtilities utils = (URLUtilities) req.getPageValue(PageRequestKeys.URL_UTILITIES);
				if (utils != null) {
					hostname = utils.buildAppRoot();
				}
			}
			blog.setHostName(hostname);
			log.info("configured hostname: " + hostname);
		}

		req.putPageValue("blog", blog);
		req.putPageValue("blogid", catalogid);
		req.putPageValue("bloghome", blog.getBlogHome());
		return blog;
	}

	public Blog getBlog(String home) throws OpenEditException {

		if (home.endsWith("/")) // This will probably not happen
		{
			home = home.substring(0, home.length() - 1);
		}
		Blog blog = (Blog) getBlogs().get(home);
		BlogArchive archive = getArchive(home + "/index.html");
		boolean changed = false;
		if (blog == null) {
			changed = true;
		} else {
			// check the time stamp on the link file
			changed = archive.hasChanged(blog);
		}

		if (changed) {
			blog = new Blog();
			blog.setBlogHome(home);
			archive.loadBlog(blog);
			getBlogs().put(home, blog);
		}
		return blog;
	}

	public void writeBlogSettings(WebPageRequest inReq) throws Exception {
		Blog blog = getBlog(inReq);
		Page settings = getPageManager().getPage(blog.getBlogHome() + "/blogsettings.xml");
		// read in some XML

		String parameter = inReq.getRequestParameter("blogtitle");
		blog.setTitle(parameter);
		parameter = inReq.getRequestParameter("bloghostname");
		blog.setHostName(parameter);
		parameter = inReq.getRequestParameter("blogauthor");
		blog.setAuthor(parameter);
		parameter = inReq.getRequestParameter("blogdescription");
		blog.setDescription(parameter);
		parameter = inReq.getRequestParameter("bloganonymous");
		if (parameter == null) {
			parameter = "false";
		}
		blog.setAllowAnonymous("true".equals(parameter));

		parameter = inReq.getRequestParameter("blogautopublishentries");
		blog.setAutoPublishEntries(Boolean.parseBoolean(parameter));

		parameter = inReq.getRequestParameter("blogautopublishcomments");
		blog.setAutoPublishingComments(Boolean.parseBoolean(parameter));

		StringWriter writer = new StringWriter();
		try {
			new BlogArchive().saveBlog(blog, writer, settings.getCharacterEncoding());
		} finally {
			writer.close();
		}

		// lets write to a file
		StringItem item = new StringItem(settings.getPath(), writer.toString(), settings.getCharacterEncoding());
		item.setMessage("Edited blog settings via admin interface");
		item.setAuthor(inReq.getUser().getUserName());
		settings.setContentItem(item);
		getPageManager().putPage(settings);
	}

	public void generateFeed(WebPageRequest req) throws Exception {
		SyndFeed feed = new SyndFeedImpl();
		String feedtype = req.getPage().get("feedtype");
		if (feedtype == null) {
			feedtype = "atom_0.3";
		}
		feed.setFeedType(feedtype);

		Blog blog = getBlog(req);

		feed.setTitle(blog.getTitle());

		feed.setLink(blog.getHostName());

		feed.setDescription(blog.getDescription());

		List recent = blog.getRecentVisibleEntries(blog.getRecentMaxCount());
		feed.setEntries(recent);

		Writer writer = new StringWriter();
		SyndFeedOutput output = new SyndFeedOutput();
		output.output(feed, writer);
		writer.close();
		req.putPageValue("feedresults", writer.toString());
	}

	public Map getBlogs() {
		if (fieldBlogs == null) {
			fieldBlogs = new HashMap();
		}
		return fieldBlogs;
	}

	public void setBlogs(Map inBlogs) {
		fieldBlogs = inBlogs;
	}

	/**
	 * @param inReq
	 */
	public BlogEntry getEntry(WebPageRequest inReq) throws OpenEditException {
		Blog blog = getBlog(inReq);
		String entryId = inReq.getRequestParameter("entryId");
		BlogEntry entry = blog.getEntry(entryId);
		inReq.putPageValue("entry", entry);
		return entry;
	}

	/**
	 * @param inReq
	 */
	public void subscribeToEntry(WebPageRequest inReq) throws OpenEditException {
		User user = inReq.getUser();
		if (user != null && !user.isVirtual()) {
			Blog blog = getBlog(inReq);
			String entryId = inReq.getRequestParameter("entryId");
			BlogEntry entry = blog.getEntry(entryId);
			entry.addNotify(user);
			BlogArchive archive = getArchive(inReq.getPath());
			archive.saveEntry(blog, entry);
		}
	}

	public void toggleEntrySubscription(WebPageRequest inReq) throws OpenEditException {
		User user = inReq.getUser();

		String userid = inReq.getRequestParameter("userid");
		if (userid != null) {
			user = getUserManager().getUser(userid);
		}
		if (user != null && !user.isVirtual()) {
			Blog blog = getBlog(inReq);
			String entryId = inReq.getRequestParameter("entryId");
			BlogEntry entry = blog.getEntry(entryId);
			if (entry.isBeingNotified(user)) {
				entry.removeNotify(user);
			} else {
				entry.addNotify(user);
			}
			BlogArchive archive = getArchive(inReq.getPath());
			archive.saveEntry(blog, entry);

		}
	}

	/**
	 * @param inReq
	 */
	public void unsubcribeFromEntry(WebPageRequest inReq) throws OpenEditException {
		User user = inReq.getUser();
		if (user != null && !user.isVirtual()) {
			Blog blog = getBlog(inReq);
			String entryId = inReq.getRequestParameter("entryId");
			BlogEntry entry = blog.getEntry(entryId);
			entry.removeNotify(user);
			BlogArchive archive = getArchive(inReq.getPath());
			archive.saveEntry(blog, entry);
		}
	}

	public void editEntry(WebPageRequest inReq) throws Exception {
		Blog blog = getBlog(inReq);
		String entryId = inReq.getRequestParameter("entryId");
		if (entryId == null) {
			return;
		}
		BlogEntry entry = blog.getEntry(entryId);
		inReq.putPageValue("blog", blog);
		inReq.putSessionValue("entry", entry);
		inReq.setRequestParameter("editPath", entry.getPath());
		inReq.setRequestParameter("originalPath", blog.getBlogHome());

	}

	public void loadPermalink(WebPageRequest inReq) throws Exception {
		// some page is being requested
		Blog blog = getBlog(inReq);
		String path = inReq.getPath();

		// get entry will check two places for the path.
		BlogEntry entry = blog.getEntry(path);
		if (entry != null) {
			inReq.putPageValue("entry", entry);
		} else {
			log.error("Null entry" + path);
		}
	}

	// public void loadLink(WebPageRequest inReq) throws Exception
	// {
	// //some page is being requested
	// String path = inReq.getPath();
	// Blog blog = getBlog(inReq);
	// String id =
	// path.substring(blog.getArchiveRootDirectory().length(),path.length());
	// id = blog.getBlogHome() + id;
	// BlogEntry entry = blog.getEntry(id);
	// inReq.putPageValue("entry",entry);
	// }
	public void loadAdminLink(WebPageRequest inReq) throws Exception {
		// some page is being requested
		String path = inReq.getPath();
		Blog blog = getBlog(inReq);
		path = "admin/" + path;
		BlogEntry entry = blog.getEntry(path);
		inReq.putPageValue("entry", entry);
	}

	/**
	 * @param inReq
	 */
	public void addNewEntry(WebPageRequest inReq) throws Exception {
		Blog blog = getBlog(inReq);
		BlogEntry entry = blog.createNewEntry(inReq.getUser());

		inReq.putPageValue("blog", blog);
		inReq.putSessionValue("entry", entry);

		inReq.setRequestParameter("editPath", entry.getPath());
		inReq.setRequestParameter("originalURL", blog.getBlogHome());

	}

	public void saveEntry(WebPageRequest inReq) throws Exception {
		Blog blog = getBlog(inReq);
		if (!blog.canEdit(inReq.getUser())) {
			throw new OpenEditException("User cannot edit blog");
		}

		BlogEntry entry = (BlogEntry) inReq.getSessionValue("entry");
		if (entry == null) {
			entry = blog.createNewEntry(inReq.getUser());
			inReq.putSessionValue("entry", entry);
			inReq.putPageValue("newpost", "true");
		}

		// inReq.getPageStreamer().entry.getLink();

		String content = inReq.getRequestParameter("content");
		if (content == null) {
			content = inReq.getRequestParameter("content.value");
		}
		if (content != null) {
			EditorSession session = new EditorSession();
			content = session.stripBody(content);
			// atom does not like XHTML entities NEW VERSION FIXES THIS
			// content.replaceAll("&trade;","&#8482;");
			// content.replaceAll("&reg;","&#174;");
			// content.replaceAll("&copy;","&#169;");
			// content.replaceAll("&nbsp;","&#160;");

			// replace &nbsp;
			/*
			 * //<!ENTITY sharp "&#35;"> <!ENTITY trade "&#8482;"> <!ENTITY reg
			 * "&#174;"> <!ENTITY copy "&#169;"> <!ENTITY nbsp "&#160;">
			 */

		}

		String author = inReq.getRequestParameter("author.value");

		entry.setAuthor(author);

		String title = inReq.getRequestParameter("title.value");
		if (title == null) {
			title = inReq.getRequestParameter("title");
		}
		entry.setTitle(title);
		entry.setDescription(content);

		String[] properties = inReq.getRequestParameters("field");
		if (properties != null) {
			for (int i = 0; i < properties.length; i++) {
				String value = inReq.getRequestParameter(properties[i] + ".value");
				if (value != null) {
					entry.addProperty(properties[i], value);
				}
			}
		}
		entry.setVisible(blog.isAutoPublishEntries());

		BlogArchive archive = getArchive(inReq.getPath());

		archive.saveEntry(blog, entry);
		archive.getEntryArchive().saveLinks(blog);
		String catid = inReq.findValue("blogid");
		LuceneBlogSearcher searcher = (LuceneBlogSearcher) getSearcherManager().getSearcher(catid, "blog");
		searcher.updateIndex(entry);
	}

	public void removeEntry(WebPageRequest inReq) throws Exception {
		Blog blog = getBlog(inReq);

		if (!blog.canEdit(inReq.getUser())) {
			throw new OpenEditException("User cannot edit blog");
		}

		String entryId = inReq.getRequestParameter("entryId");
		BlogEntry entry = blog.getEntry(entryId);

		blog.removeEntry(entry);
		BlogArchive archive = getArchive(blog.getBlogHome());
		archive.getEntryArchive().saveLinks(blog);
	}

	public BlogArchive getArchive(String inPath) throws OpenEditException {
		Page path = getPageManager().getPage(inPath);
		return getArchive(path);
	}

	public BlogArchive getArchive(Page inPath) throws OpenEditException {
		// get the bloghome archive id
		String name = inPath.getProperty("blogarchivename");
		if (name == null) {
			name = "BlogArchive";
		}
		BlogArchive archive = (BlogArchive) getBeanFactory().getBean(name);
		return archive;
	}

	/**
	 * @param inReq
	 */
	public void addNewComment(WebPageRequest inReq) throws Exception {
		String content = inReq.getRequestParameter("content");
		String entryId = inReq.getRequestParameter("entryId");
		if (content == null || entryId == null) {
			return;
		}
		Blog blog = getBlog(inReq);
		if (checkQuestion(inReq)) {
			BlogEntry entry = blog.getEntry(entryId);

			String author = inReq.getRequestParameter("username");
			User user = getUserManager().getUser(author);
			if (user == null) {
				if ("anonymous".equals(author) && blog.getAllowAnonymous()) {
					user = new FileSystemUser();
					user.setUserName(author);
				} else {
					throw new OpenEditException("Anonymous user not allowed");
				}
			}
			BlogComment comment = blog.createNewComment(user, content);
			entry.addComment(comment);
			BlogArchive archive = getArchive(inReq.getPage());
			archive.getEntryArchive().getCommentArchive().saveComments(blog, entry);
			getCommentNotification(inReq.findValue("blogid")).commentAdded(inReq, blog, entry, comment);
		}
		// inReq.removeSessionValue("question");
	}

	public boolean checkQuestion(WebPageRequest inReq) throws OpenEditException {
		if (inReq.getSessionValue("answer") != null || inReq.getUser() != null) {
			return true;
		}

		String answer = inReq.getRequestParameter("answerid");
		Blog blog = getBlog(inReq);
		Question q = (Question) inReq.getSessionValue("question");
		if (q == null || !q.checkAnswer(answer)) {
			inReq.redirect(blog.getBlogHome() + "/questionerror.html");
			// //inReq.putPageValue("error",
			// "Answer did not match this session.");
			return false;
		}
		inReq.putSessionValue("answer", answer);
		return true;
	}

	public void removeComment(WebPageRequest inReq) throws Exception {
		Blog blog = getBlog(inReq);
		String entryId = inReq.getRequestParameter("entryId");
		BlogEntry entry = blog.getEntry(entryId);
		String commentId = inReq.getRequestParameter("commentId");
		BlogComment comment = entry.getComment(commentId);
		User user = (User) inReq.getSessionValue("user");

		if (comment.canEdit(user)) {
			entry.removeComment(comment);
			BlogArchive archive = getArchive(inReq.getPage());
			archive.getEntryArchive().getCommentArchive().saveComments(blog, entry);
		}
		redirectToOrig(inReq);
	}

	public void changeCommentVisibility(WebPageRequest inReq) throws Exception {
		String entryId = inReq.getRequestParameter("entryId");
		if (entryId != null) {
			Blog blog = getBlog(inReq);
			String commentId = inReq.getRequestParameter("commentId");
			BlogEntry entry = blog.getEntry(entryId);
			BlogComment comment = entry.getComment(commentId);
			if (comment != null) {
				if (comment.canEdit(inReq.getUser()) || blog.canEdit(inReq.getUser())) {
					comment.setVisible(!comment.isVisible());
					BlogArchive archive = getArchive(inReq.getPage());
					archive.getEntryArchive().getCommentArchive().saveComments(blog, entry);
				}
			}
			redirectToOrig(inReq);
		}
	}

	private void redirectToOrig(WebPageRequest inReq) {
		String origURL = inReq.getRequestParameter("origURL");
		if (origURL != null && origURL.length() > 0) {
			inReq.redirect(origURL);
		}
	}

	public void login(WebPageRequest inReq) throws Exception {
		String username = inReq.getRequestParameter("username");
		User user = getUserManager().getUser(username);
		String password = inReq.getRequestParameter("password");
		if (getUserManager().authenticate(user, password)) {
			inReq.putSessionValue("user", user);
		}
	}

	/*
	 * public void registerNewUser(WebPageRequest inReq) throws Exception {
	 * //loop over all the properties String email =
	 * inReq.getRequestParameter("property-email"); User user =
	 * getUserManager().getUserByEmail(email); if ( user != null) {
	 * inReq.putPageValue
	 * ("oe-error","Duplicate user. email address already in system"); return; }
	 * String password = inReq.getRequestParameter("password1"); String
	 * password2 = inReq.getRequestParameter("password2"); if ( password == null
	 * || password2 == null || !password.equals(password2) || password.length()
	 * == 0) { inReq.putPageValue("oe-error","Passwords do not match"); return;
	 * } User newuser = getUserManager().createUser(null,password); for
	 * (Iterator iter = inReq.getParameterMap().keySet().iterator();
	 * iter.hasNext();) { String keyId = (String) iter.next(); if (
	 * keyId.indexOf("property-") == 0) {
	 * newuser.put(keyId.substring("property-"
	 * .length()),inReq.getRequestParameter(keyId)); } }
	 * getUserManager().saveUser(newuser);
	 * inReq.putSessionValue("user",newuser); String commentlink =
	 * inReq.getRequestParameter("loginokpage"); if ( commentlink != null &&
	 * commentlink.length() > 0) { inReq.redirect(commentlink); } }
	 */
	public void changeEntryVisibility(WebPageRequest inReq) throws Exception {
		Blog blog = getBlog(inReq);

		String entryId = inReq.getRequestParameter("entryId");
		BlogEntry entry = blog.getEntry(entryId);

		if (!blog.canEdit(inReq.getUser()) && !entry.getUser().getUserName().equals(inReq.getUserName())) {
			throw new OpenEditException("User cannot edit blog");
		}

		entry.setVisible(!entry.isVisible());
		BlogArchive archive = getArchive(inReq.getPage());
		archive.saveEntry(blog, entry);
	}

	public BlogCommentNotification getCommentNotification(String inCatalogid) {
	
		return (BlogCommentNotification) getModuleManager().getBean(inCatalogid, "BlogCommentNotification");
	}

	public void setCommentNotification(BlogCommentNotification inCommentNotification) {
		fieldCommentNotification = inCommentNotification;
	}

	public void notifyOnPost(WebPageRequest inReq) throws Exception {
		boolean onpost = Boolean.parseBoolean(inReq.findValue("sendnotificationonpost"));
	
		log.info("Notify on post was: " + onpost);
		if (onpost) {
			BlogEntry entry = (BlogEntry) inReq.getSessionValue("entry");
			Blog blog = (Blog) inReq.getPageValue("blog");
			String notificationgroup = inReq.findValue("notificationgroup");
			log.info("Notify group was: " + notificationgroup);
			Group group = getUserManager().getGroup(notificationgroup);
			Collection users = getUserManager().getUsersInGroup(group);
			// only send each mail once.
			HashSet send = new HashSet();
			send.addAll(users);
			getCommentNotification(inReq.findValue("blogid")).blogPostAdded(inReq, blog, entry, send);
		}
	}
}
