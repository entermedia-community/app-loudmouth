/*
 * Created on Sep 25, 2006
 */
package com.openedit.blog;

import java.util.Date;
import java.util.List;

import org.openedit.blog.modules.BlogModule;

import com.openedit.BaseTestCase;
import com.openedit.WebPageRequest;

public class PageCommentTest extends BaseTestCase
{

	public PageCommentTest(String inName)
	{
		super( inName);
	}

	public void xtestComment() throws Exception
	{
 		BlogModule blogmodule = (BlogModule)getBean("BlogModule");
 		//list last five entries
 		//System.out.println(SimpleDateFormat.getDateTimeInstance().format(new Date()));
 		WebPageRequest req = getFixture().createPageRequest("/index.html");
 		String name = req.getPage().getProperty("blogarchivename");
 		assertEquals("DynamicBlogArchive",name);
 		Blog blog = blogmodule.getBlog(req); //blog home would be set to /

 		BlogEntry entry = blog.getEntry("/sub/link.html");
 		assertNotNull(entry);
 		
 		List list = entry.getComments();
 		assertNotNull(list);
 		
 		BlogComment comment = new BlogComment();
 		comment.setAuthor("admin");
 		comment.setContent("This is a test");
 		comment.setId("dsdfdfdfdf");
 		comment.setDateTime(new Date());
 		comment.setVisible(true);
 		
 		entry.addComment(comment);
 		
 		
 		
 		
	}
	public void testComment() throws Exception
	{
 	 		
 		
 		
	}
}
