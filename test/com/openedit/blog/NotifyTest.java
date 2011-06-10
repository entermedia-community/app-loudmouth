/*
 * Created on Jan 13, 2006
 */
package com.openedit.blog;

import org.openedit.blog.modules.BlogAdminModule;

import com.openedit.BaseTestCase;
import com.openedit.WebPageRequest;
import com.openedit.page.Page;

public class NotifyTest extends BaseTestCase
{
	public void XtestNotify() throws Exception
	{
		BlogAdminModule module = (BlogAdminModule)getFixture().getModuleManager().getModule("BlogAdminModule");
		WebPageRequest req = getFixture().createPageRequest();
		module.sendNotification(req);

		String output = req.getWriter().toString();
		assertTrue(output.length() > 100);
	}
	
	public void testFixContent() throws Exception
	{
		Notify notify = new Notify();
		notify.setRootDirectory(getFixture().getWebServer().getRootDirectory());
		notify.setPageManager(getFixture().getPageManager());
		notify.setUserManager(getFixture().getUserManager());

		//copy blank story
		Page good = getFixture().getPageManager().getPage("/yetanotherblog/permalink/2005/2/17/112302clean.html");
		Page page = getFixture().getPageManager().getPage("/yetanotherblog/permalink/2005/2/17/112302.html");
		getFixture().getPageManager().copyPage(good, page);
		notify.fixLinks(page,"http://localhost:8080");
		page = getFixture().getPageManager().getPage("/yetanotherblog/permalink/2005/2/17/112302.html");
		String content = page.getContent();
		assertEquals( -1,content.indexOf("<img src=/yetanotherblog/images/"));
		assertTrue( content.indexOf("<img src=http://localhost:8080/yetanotherblog/images/") > 0);
		assertTrue( content.indexOf("<img src=\"http://localhost:8080/yetanotherblog/images/logobar.gif\"") > 0);
		assertTrue( content.indexOf("url('http://localhost:8080/yetanotherblog/images/logobar.gif')") > 0);
		
		assertTrue( content.indexOf("<img src=\"http://localhost:8080/yetanotherblog/images/home.gif\"") > 0);

		
		
	}
}
