/*
 * Created on Feb 18, 2005
 */
package com.openedit.blog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.collections.map.ReferenceMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.blog.archive.EntryArchive;
import org.openedit.links.Link;
import org.openedit.links.LinkTree;

import com.openedit.BaseWebPageRequest;
import com.openedit.OpenEditException;
import com.openedit.page.Page;
import com.openedit.page.Permission;
import com.openedit.page.manage.PageManager;
import com.openedit.users.User;
import com.openedit.users.UserManager;

/**
 * @author cburkey
 */
public class Blog
{
	protected PageManager fieldPageManager;
	protected LinkTree fieldLinkTree;
	protected String fieldBlogHome;
	protected String fieldArchiveRootDirectory;
	protected String fieldHostName;
	protected String fieldDescription;
	protected String fieldTitle;
	
	protected String fieldAuthor;
	protected int fieldRecentMaxCount = 5;
	protected String fieldPermission;
	protected boolean fieldAllowAnonymous;
	protected boolean fieldAutoPublishingComments;
	protected boolean fieldAutoPublishEntries;
	protected boolean fieldIsUsingNotification;
	
	protected Map fieldCache;
	protected UserManager fieldUserManager;
	protected EntryArchive fieldEntryArchive;
	protected Date fieldLastModified;
	
	Random fieldRandom;
	
	private static final Log log = LogFactory.getLog(Blog.class);

	public List getRecentEntries() throws OpenEditException
	{
		return getRecentEntries(getRecentMaxCount());
	}
	public List getVisibleEntries(int start, int end) throws OpenEditException
	{
		return getEntries(start, end, true,false);
	}
	/**
	 * Returns the given number of most recent entries.
	 * 
	 * @param inCount  The number of entries to return
	 * 
	 * @return  A {@link List} of {@link BlogEntry}s
	 */
	public List getRecentEntries(int inCount) throws OpenEditException
	{
		return getEntries(0,inCount, false,false);
	}
	
	public List getRecentVisibleEntries(int inI) throws OpenEditException
	{
		return getEntries(0,inI, true,false);
	}
	
	public List getRandomRecentEntries(int inCount) throws OpenEditException
	{
		return getEntries(0,inCount, false,true);
	}
	public List getRandomRecentVisibleEntries(int inI) throws OpenEditException
	{
		return getEntries(0,inI, true,true);
	}
	
	
	public List getEntries(int start, int end, boolean visibleOnly, boolean sortRandom) throws OpenEditException
	{
		int num;
		if( getLinkTree().getRootLink() == null)
		{
			return Collections.EMPTY_LIST;
		}
		// added "new ArrayList" because when i call more than once without it, links get empty
		List links =  new ArrayList(getLinkTree().getRootLink().getChildren());
		start = Math.min(start,links.size());
		if ( end > links.size())
		{
			end = links.size();
		}
		if ( end == 0 )  //end == 0 means get all entries
		{
			end = links.size();
		}
		//read in the links xml file and take off the top entries
		//then for each link go get the entry from the xconf file
		List entries = new ArrayList();
		for (int i = start; i < end; i++)
		{
			if (sortRandom){
				num = getRandom().nextInt(links.size());
			}else{
				num = i;
			}
			Link link = (Link)links.get(num);
			BlogEntry entry = getEntry(link.getUrl());
			if ( entry != null && (!visibleOnly || entry.isVisible()))
			{
				entries.add(entry);
				if (sortRandom){
					links.remove(num);
				}
			}
			else
			{
				if ( end < links.size())
				{
					end++;
				}
			}
		}
		return entries;
		
	}
	
	public Random getRandom()
	{
		if (fieldRandom == null)
		{
			fieldRandom = new Random();
		}
		return fieldRandom;
	}

	public void setRandom(Random inRandom)
	{
		fieldRandom = inRandom;
	}

	public void removeEntry(BlogEntry inEntry) throws OpenEditException
	{
		Link link = getLinkTree().getLink(inEntry.getId());
		
		getLinkTree().removeLink(link);
	}
	
	
	public List sortByRank(List inEntries)
	{
		return inEntries;
	}

	public LinkTree getLinkTree()
	{
		return fieldLinkTree;
	}
	public void setLinkTree(LinkTree inLinkTree)
	{
		fieldLinkTree = inLinkTree;
	}
	public PageManager getPageManager()
	{
		return fieldPageManager;
	}
	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}
	public String getLink()
	{
		return getBlogHome();
	}

	public String getBlogHome()
	{
		return fieldBlogHome;
	}
	public void setBlogHome(String inBlogRoot)
	{
		fieldBlogHome = inBlogRoot;
	}
	public String getHostName()
	{
		return fieldHostName;
	}
	public void setHostName(String inHostName)
	{
		fieldHostName = inHostName;
	}

	public String getDescription()
	{
		return fieldDescription;
	}
	public void setDescription(String inDescription)
	{
		fieldDescription = inDescription;
	}
	
	
	public String getTitle()
	{
		return fieldTitle;
	}
	public void setTitle(String inTitle)
	{
		fieldTitle = inTitle;
	}
	
	
	public int getRecentMaxCount()
	{
		return fieldRecentMaxCount;
	}
	public void setRecentMaxCount(int inRecentMaxCount)
	{
		fieldRecentMaxCount = inRecentMaxCount;
	}

	/**
	 * @param inEntryId
	 * @return
	 */
	public BlogEntry getEntry(String inPath) throws OpenEditException
	{
		Page page = getPageManager().getPage(inPath);
		if( !page.exists())
		{
			//strip off the ending and look in the archive root
			if( inPath.startsWith(getArchiveRootDirectory()))
			{
				String ending = inPath.substring(getArchiveRootDirectory().length(),inPath.length());
				//entry.setPath(ending);
				inPath = ending;
				page = getPageManager().getPage(inPath);
			}			
		}
		
		BlogEntry entry = (BlogEntry)getCache().get(inPath);
		if( entry == null)
		{
			entry = getEntryArchive().createEntry(this, page);
			getCache().put( inPath, entry);
			
		}
	
		return entry;
	}

	public BlogEntry createNewEntry(User inUser) throws OpenEditException
	{
		return getEntryArchive().createEntry(this, inUser);
	}

	
	public boolean canEdit(User inUser) throws OpenEditException
	{
		if( inUser == null)
		{
			return false;
		}
		String slink = getBlogHome() + "/permalinks.xml";

		Page linkpage = getPageManager().getPage(slink); 
		Permission filter = linkpage.getPermission("edit"); 
		BaseWebPageRequest req = new BaseWebPageRequest();
		req.setUser(inUser);
		req.setContentPage(linkpage);
		req.setPage(linkpage);
		boolean value= ((filter == null) || filter.passes( req ));

		return value;
	}
	
	public String getAuthor()
	{
		return fieldAuthor;
	}
	public void setAuthor(String inAuthor)
	{
		fieldAuthor = inAuthor;
	}
	public String getEditPermission()
	{
		return fieldPermission;
	}
	public void setEditPermission(String inEditors)
	{
		fieldPermission = inEditors;
	}
	public boolean getAllowAnonymous() 
	{
		return fieldAllowAnonymous;
	}
	public void setAllowAnonymous(boolean inAllowAnonymous) 
	{
		fieldAllowAnonymous = inAllowAnonymous;
	}
	public boolean isAutoPublishingComments()
	{
		return fieldAutoPublishingComments;
	}
	public void setAutoPublishingComments(boolean inAutoPublishingComments)
	{
		fieldAutoPublishingComments = inAutoPublishingComments;
	}
	
	public BlogComment createNewComment(User inAuthor, String inContent)
	{
		BlogComment comment = new BlogComment();
		comment.setAuthor(inAuthor.getShortDescription());
		comment.setUser(inAuthor);
		comment.setContent(inContent);
		comment.setDateTime(new Date());
		comment.setId(String.valueOf(System.currentTimeMillis()));
		comment.setVisible(isAutoPublishingComments());
		return comment;
	}

	public boolean isAutoPublishEntries()
	{
		return fieldAutoPublishEntries;
	}
	public void setAutoPublishEntries(boolean inAutoPublishEntries)
	{
		fieldAutoPublishEntries = inAutoPublishEntries;
	}
	public String getArchiveRootDirectory()
	{
		return fieldArchiveRootDirectory;
	}
	public void setArchiveRootDirectory(String inArchiveRootDirectory)
	{
		fieldArchiveRootDirectory = inArchiveRootDirectory;
	}
	public Map getCache()
	{
		if( fieldCache == null)
		{
			fieldCache = new ReferenceMap(ReferenceMap.HARD,ReferenceMap.HARD);
		}
		return fieldCache;
	}
	public void setCache(Map inCache)
	{
		fieldCache = inCache;
	}
	public UserManager getUserManager()
	{
		return fieldUserManager;
	}
	public void setUserManager(UserManager inUserManager)
	{
		fieldUserManager = inUserManager;
	}
	public boolean isUseNotification()
	{
		return fieldIsUsingNotification;
	}
	public void setUseNotification( boolean inBol)
	{
		fieldIsUsingNotification = inBol;
	}
	public EntryArchive getEntryArchive()
	{
		return fieldEntryArchive;
	}
	public void setEntryArchive(EntryArchive inEntryArchive)
	{
		fieldEntryArchive = inEntryArchive;
	}
	public Date getLastModified()
	{
		return fieldLastModified;
	}
	public void setLastModified(Date inLastModified)
	{
		fieldLastModified = inLastModified;
	}
}
