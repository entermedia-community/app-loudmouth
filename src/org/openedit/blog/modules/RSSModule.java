package org.openedit.blog.modules;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;


public class RSSModule {

	
	
	public void loadRomeFeed(WebPageRequest inReq) throws OpenEditException {
		String url = inReq.findValue("feed");
		try {
			URL feedUrl = new URL(url);
				SyndFeedInput input = new SyndFeedInput();
				  SyndFeed feed = input.build(new XmlReader(feedUrl));
				  inReq.putPageValue("rss", feed);
				List entries = feed.getEntries();
				System.out.println("hello");
		
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FeedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
