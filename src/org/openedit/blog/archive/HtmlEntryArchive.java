/*
 * Created on Sep 25, 2006
 */
package org.openedit.blog.archive;

import org.openedit.links.LinkTree;
import org.openedit.links.PageLink;

import com.openedit.OpenEditException;
import com.openedit.blog.Blog;

public class HtmlEntryArchive extends EntryArchive
{

	public void saveLinks(Blog inBlog) throws OpenEditException
	{
//
//		String slink = inBlog.getBlogHome() + "/links.xml";
//
//		Page linkpage = getPageManager().getPage(slink); 
//		Writer out = new StringWriter();
//		try
//		{
//			XmlLinkLoader loader = new XmlLinkLoader();
//			loader.saveLinks(inBlog.getLinkTree(), out, linkpage.getCharacterEncoding());
//		}
//		finally
//		{
//			FileUtils.safeClose(out);
//		}
//		StringItem item = new StringItem(linkpage.getPath(), out.toString(), linkpage.getCharacterEncoding());
//		linkpage.setContentItem(item);
//		getPageManager().putPage(linkpage);

	}

	/**
	 * 
	 */
	public void loadLinks(Blog inBlog) throws OpenEditException
	{
//		String slink = inBlog.getBlogHome() + "/links.xml";
//		Page linkpage = getPageManager().getPage(slink ); 
//		if ( !linkpage.exists())
//		{
//			throw new OpenEditException("could not find " + slink);
//		}

//		XmlLinkLoader loader = new XmlLinkLoader();
		LinkTree tree = new LinkTree();
		PageLink root = new PageLink();
		root.setId("");		
		root.setPath("/");
		root.setPageManager(getPageManager());
		tree.setRootLink(root);
		inBlog.setLinkTree(tree);
	}

	
}
