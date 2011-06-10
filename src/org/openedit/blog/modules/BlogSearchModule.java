package org.openedit.blog.modules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.openedit.WebPageRequest;
import com.openedit.blog.Blog;
import com.openedit.config.Configuration;
import com.openedit.modules.BaseModule;
import com.openedit.page.PageRequestKeys;
import com.openedit.util.URLUtilities;

public class BlogSearchModule extends BaseModule
{
	private static final Log log = LogFactory.getLog(BlogSearchModule.class);
	public void loadMergedBlogEntries(WebPageRequest inReq) throws Exception
	{
		BlogModule mod = (BlogModule)getModule("BlogModule");
		List entries = new ArrayList();
		Blog blog = mod.getBlog(inReq);
		List recent = blog.getRecentVisibleEntries(5);
		entries.addAll(recent);
		for (Iterator iterator = inReq.getCurrentAction().getConfig().getChildIterator("blog"); iterator.hasNext();)
		{
			Configuration conf = (Configuration) iterator.next();
			String home = conf.getValue();
			blog =  mod.getBlog(home);

			
			if (blog.getHostName() == null) {
				log.info("no hostname specified in blogsettings.xml, attempting to auto configure");
				String hostname = inReq.findValue("hostName");
				if (hostname == null) {
					log.info("no value for hostName specified in properties");
					URLUtilities utils = (URLUtilities) inReq
							.getPageValue(PageRequestKeys.URL_UTILITIES);
					if (utils != null) {
						 hostname = utils.buildAppRoot();
					}
				}
				blog.setHostName(hostname);
				log.info("configured hostname: " + hostname);
			}
			
			
			recent = blog.getRecentVisibleEntries(5);
			entries.addAll(recent);
		}
		Collections.sort(entries);
		Collections.reverse(entries);
		if( entries.size() > 6)
		{
			entries = entries.subList(0,5);
		}
		inReq.putPageValue("entries", entries);
	}	
	
	public void searchBlogEntries(WebPageRequest inReq) throws Exception
	{
	BlogModule mod = (BlogModule)getModule("BlogModule");
	Blog blog = mod.getBlog(inReq);
	List entries = new ArrayList();
	List recent;
	
	String count = inReq.findValue("showcount");
	String random = inReq.findValue("randomposting");
	
	if (Boolean.parseBoolean(random)){
		recent = blog.getRandomRecentVisibleEntries(Integer.parseInt(count));
	}
	else{
		recent = blog.getRecentVisibleEntries(Integer.parseInt(count));
	}
	
	entries.addAll(recent);
	inReq.putPageValue("entries", entries);
	}
}
