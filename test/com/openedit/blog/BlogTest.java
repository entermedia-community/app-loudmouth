/*
 * Created on Feb 18, 2005
 */
package com.openedit.blog;

import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.openedit.blog.archive.BlogArchive;
import org.openedit.blog.modules.BlogModule;

import com.openedit.BaseTestCase;
import com.openedit.WebPageRequest;
import com.openedit.modules.admin.users.Question;
import com.openedit.users.User;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.SyndFeedOutput;

/**
 * @author cburkey
 *
 */
public class BlogTest extends BaseTestCase
{
	/**
	 * 
	 */
	public BlogTest(String inName)
	{
		super( inName);
	}
	public void testReadExistingBlog() throws Exception
	{
 		BlogModule blogmodule = (BlogModule)getBean("BlogModule");
 		//list last five entries
 		//System.out.println(SimpleDateFormat.getDateTimeInstance().format(new Date()));
 		WebPageRequest req = getFixture().createPageRequest("/yetanotherblog/index.html");
 		blogmodule.getBlog(req);
 		Blog blog = (Blog)req.getPageValue("blog");
 		List recent = blog.getRecentEntries(5);
 		assertTrue(recent.size()>0);
 		BlogEntry entry = (BlogEntry)recent.get(0);
 		// FIXME: Actually test something here.
 		//assertEquals("A blog entry",entry.getTitle());
 		
 		//assertEquals(1,entry.getCategories().size() );
 		
/* 		assertTrue(entry.getDescription().getValue().length() > 100);
 		assertEquals(entry.getLink(),"http://localhost:8080/yetanotherblog/permalink/2005/2/17/112302.html");
 		assertEquals(entry.getPath(),"/yetanotherblog/2005/2/17/112302.html");
 		assertEquals(entry.getId(),"yetanotherblog2005217112302");
 		
 		int count = entry.countComments();
 		assertEquals(1,count);
*/ 		
	}
	
	public void testPermalink() throws Exception
	{
 		BlogModule blogmodule = (BlogModule)getBean("BlogModule");
 		WebPageRequest req = getFixture().createPageRequest("/yetanotherblog/permalink/2005/2/17/112302.html");
 		blogmodule.loadPermalink(req);
 		BlogEntry entry = (BlogEntry)req.getPageValue("entry");
		assertNotNull(entry);
	}
	public void testAtomFeed() throws Exception
	{
		//this makes a blog
		SyndFeed feed = new SyndFeedImpl();
		feed.setFeedType("atom_0.3");
		feed.setTitle("Sample Feed (created with Rome)");
		feed.setLink("http://rome.dev.java.net");
		feed.setDescription("This feed has been created using Rome (Java syndication utilities");
		 
		List entries = new ArrayList();
		SyndEntry entry;
		SyndContent description;
		
		entry = new SyndEntryImpl();
		entry.setTitle("Rome v1.0");
		entry.setLink("http://wiki.java.net/bin/view/Javawsxml/Rome01");
		entry.setPublishedDate(new SimpleDateFormat("MM/dd/yyyy").parse("1/1/2004"));
		description = new SyndContentImpl();
		description.setType("text/plain");
		description.setValue("Initial release of Rome");
		entry.setDescription(description);
		entries.add(entry);
		
		entry = new BlogEntry();
		entry.setTitle("Rome v3.0");
		entry.setLink("http://wiki.java.net/bin/view/Javawsxml/Rome03");
		entry.setPublishedDate(new SimpleDateFormat("MM/dd/yyyy").parse("1/1/2004"));
		description = new SyndContentImpl();
		description.setType("text/html");
		String content = "<p>More Bug fixes, mor API changes, some new &nbsp; features and some Unit testing</p>"+
		              "<p>For details chec the <a href=\"http://wiki.java.net/bin/view/Javawsxml/RomeChangesLog#RomeV03\">Changes Log</a></p>";
		//content = content.replaceAll("&nbsp;","&#160;");

		description.setValue( content );
		entry.setDescription(description);
		entries.add(entry);
			
		feed.setEntries(entries);
		//blog.setFeed(feed);
		
		 Writer writer = new StringWriter();
         SyndFeedOutput output = new SyndFeedOutput();
         output.output(feed,writer);
         writer.close();
         assertTrue(writer.toString().length() > 200);
	}
	
	public void testEditing() throws Exception 
	{
 		BlogModule blogmodule = (BlogModule)getBean("BlogModule");
 		//list last five entries
 		//System.out.println(SimpleDateFormat.getDateTimeInstance().format(new Date()));
 		WebPageRequest req = getFixture().createPageRequest("/yetanotherblog/comments.html");
 		
 		req.setRequestParameter("username","admin");
		req.setRequestParameter("password","admin");
		User admin = (User)req.getPageValue("user");
		req.setUser(admin);
 		getFixture().getEngine().executePageActions(req);
 		//add entry
 		blogmodule.addNewEntry(req);
 		BlogEntry entry = (BlogEntry)req.getPageValue("entry");
 		assertNotNull(entry);

 		//test new entry
 		req.setRequestParameter("title.value","New Entry");
 		req.setRequestParameter("author.value","New Entry");
 		
 		blogmodule.saveEntry(req);
 		req.setRequestParameter("entryId",entry.getId());
 		blogmodule.getEntry(req);
 		BlogEntry blogentry = (BlogEntry)req.getPageValue("entry");
 		assertNotNull(blogentry);
 		assertEquals("New Entry", blogentry.getTitle());

 		Blog blog = blogmodule.getBlog(req);
 		Question q = new Question();//blog.getQuestion("1");
 		q.setId("1");
 		q.setAnswer("9");
 		req.putSessionValue("question", q);
 		
 		//add comments
 		req.setRequestParameter("entryId",entry.getId());
 		req.setRequestParameter("author","jimbob");
 		req.setRequestParameter("content","This is a comment");
 		
 		req.setRequestParameter("questionid","1");
 		req.setRequestParameter("answerid","9");
 		
 		blogmodule.addNewComment(req);
 		req.setRequestParameter("entryId",entry.getId());
 		blogmodule.getEntry(req);
 		blogentry = (BlogEntry)req.getPageValue("entry");
 		assertEquals(1,blogentry.getComments().size());
 		BlogComment comment = (BlogComment) blogentry.getComments().get(0);
 		assertTrue(comment.isVisible());
 		
 		req.setRequestParameter("entryId",entry.getId());
 		req.setRequestParameter("commentId", comment.getId());
 		req.setRequestParameter("actiontext","flip");
 		blogmodule.changeCommentVisibility(req);

 		BlogComment blogcomment = (BlogComment) blogentry.getComments().get(0);
 		assertFalse(blogcomment.isVisible());

 		
 		assertFalse(entry.isVisible());
 		req.setRequestParameter("entryId",entry.getId());
 		blogmodule.changeEntryVisibility(req);
 		
 		req.setRequestParameter("entryId",entry.getId());
 		blogmodule.getEntry(req);
 		blogentry = (BlogEntry)req.getPageValue("entry");

 		assertTrue(blogentry.isVisible());
	}
	
	public void testNotificationList(){
		
	 	BlogModule blogmodule = (BlogModule)getBean("BlogModule");
 		WebPageRequest req = getFixture().createPageRequest("/yetanotherblog/index.html");
	 	blogmodule.getBlog(req);
	 	Blog blog = (Blog)req.getPageValue("blog");
	 		List recent = blog.getRecentEntries(5);
	 		assertTrue(recent.size()>0);
	 		BlogEntry entry = (BlogEntry)recent.get(0);
	 		assertTrue(entry.getNotify().size()==0);
	 		entry.addNotify(req.getUser());
	 		BlogArchive archive = blogmodule.getArchive("/yetanotherblog/index.html");
	 		archive.saveEntry(blog, entry);
	 		req.removePageValue("blog");
	 		blogmodule.getBlog(req);
	 		entry = (BlogEntry)recent.get(0);
	 		assertTrue(entry.getNotify().size()>0);
	 		entry.setNotify(null);
	 		archive.saveEntry(blog, entry);
	 		
	 		
	 	}	
	
}
