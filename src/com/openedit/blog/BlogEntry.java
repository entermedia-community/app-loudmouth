/*
 * Created on Feb 18, 2005
 */
package com.openedit.blog;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.blog.archive.BlogCommentArchive;
import org.openedit.links.LinkTree;
import org.openedit.util.DateStorageUtil;

import com.openedit.OpenEditException;
import com.openedit.OpenEditRuntimeException;
import com.openedit.page.manage.PageManager;
import com.openedit.users.User;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntryImpl;

/**
 * @author cburkey
 * 
 */
public class BlogEntry extends SyndEntryImpl implements Serializable, Comparable, Data {
	protected String fieldPath;
	protected List fieldComments;
	transient protected PageManager fieldPageManager;
	protected LinkTree fieldLinkTree;
	protected boolean fieldVisible = false;
	protected static SimpleDateFormat fieldGmtStandard;
	protected User fieldUser;
	protected List fieldNotify;
	protected BlogCommentArchive fieldCommentArchive;
	private static final Log log = LogFactory.getLog(BlogEntry.class);
	protected Map fieldProperties;

	public BlogEntry() {
	}

	public String getPath() {
		return fieldPath;
	}

	/**
	 * A path is just the end part of a link
	 * 
	 * @param inPath
	 */

	public void setPath(String inPath) {
		fieldPath = inPath;
	}

	public String published(String inFormat) {
		SimpleDateFormat format = new SimpleDateFormat(inFormat);
		return format.format(getPublishedDate());
	}

	public String getId() {
		return getPath();
	}

	/**
	 * Returns the total number of comments on this blog entry, both visible
	 * (published) and invisible (unpublished).
	 * 
	 * @return The number of comments
	 * 
	 * @throws OpenEditException
	 *             If the comments needed to be loaded and could not be
	 */
	public int countComments() throws OpenEditException {
		return getComments().size();
	}

	/**
	 * Returns the number of visible (published) comments on this blog entry.
	 * 
	 * @return The number of visible comments
	 * 
	 * @throws OpenEditException
	 *             If the comments needed to be loaded and could not be
	 */
	public int countVisibleComments() throws OpenEditException {
		return getVisibleComments().size();
	}

	/**
	 * Returns all the comments on this blog, published or unpublished.
	 * 
	 * @return A {@link List} of {@link BlogComment}s
	 * 
	 * @throws OpenEditException
	 *             If the comments needed to be loaded and could not be
	 */
	public List getComments() throws OpenEditException {
		if (fieldComments == null) {
			fieldComments = new ArrayList();
			getCommentArchive().loadComments(this);
		}
		return fieldComments;
	}

	/**
	 * Returns all comments on this blog entry that are visible (published).
	 * 
	 * @return A {@link List} of {@link BlogComment}s
	 * 
	 * @throws OpenEditException
	 *             If the comments needed to be loaded and could not be
	 */
	public List getVisibleComments() throws OpenEditException {
		List comments = getComments();
		List visibleComments = new ArrayList();
		for (Iterator iter = comments.iterator(); iter.hasNext();) {
			BlogComment comment = (BlogComment) iter.next();
			if (comment.isVisible()) {
				visibleComments.add(comment);
			}
		}
		return visibleComments;
	}

	public Collection getNotificationList() {
		Set notify = new HashSet();
		for (Iterator iterator = getComments().iterator(); iterator.hasNext();) {
			BlogComment lastcomment = (BlogComment) iterator.next();
			User user = lastcomment.getUser();
			if (user != null) // legacy comments
			{

				notify.add(user);

			}
		}
		List subscribers = getNotify();
		log.info(getId());
		notify.addAll(subscribers);
		notify.add(getUser());
		return notify;
	}

	public boolean isBeingNotified(User inUser) {
		// notification list is an aggregate of people who have commented and
		// people who have subscribed
		Collection list = getNotificationList();
		return list.contains(inUser);
	}

	public boolean hasCommented(User inUser) {
		Set notify = new HashSet();
		for (Iterator iterator = getComments().iterator(); iterator.hasNext();) {
			BlogComment lastcomment = (BlogComment) iterator.next();
			User user = lastcomment.getUser();
			if (user != null) // legacy comments
			{
				if (user.equals(inUser)) {
					return true;
				}

			}
		}
		return false;
	}

	public List getAllowedComments(User inUser) throws OpenEditException {
		List comments = getComments();
		List allowedComments = new ArrayList();
		for (Iterator iter = comments.iterator(); iter.hasNext();) {
			BlogComment comment = (BlogComment) iter.next();
			if (comment.isVisible() || comment.canEdit(inUser)) {
				allowedComments.add(comment);
			}
		}
		return allowedComments;
	}

	public PageManager getPageManager() {
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager) {
		fieldPageManager = inPageManager;
	}

	public LinkTree getLinkTree() {
		return fieldLinkTree;
	}

	public void setLinkTree(LinkTree inLinkTree) {
		fieldLinkTree = inLinkTree;
	}

	/**
	 * Adds a comment to this blog entry. The comment's data, including the ID,
	 * must already be populated.
	 * 
	 * @param inComment
	 *            The new comment
	 */
	public void addComment(BlogComment inComment) throws OpenEditException {
		// TODO: check for dups
		getComments().add(inComment);
	}

	/**
	 * Returns the comment with the given comment ID.
	 * 
	 * @param inId
	 *            The comment ID
	 * 
	 * @return The comment, or <code>null</code> if this entry does not have a
	 *         comment with the given ID
	 */
	public BlogComment getComment(String inId) throws OpenEditException {
		for (Iterator iter = getComments().iterator(); iter.hasNext();) {
			BlogComment comment = (BlogComment) iter.next();
			if (comment.getId().equals(inId)) {
				return comment;
			}
		}
		return null;
	}

	public void removeComment(String inId) throws OpenEditException {
		BlogComment comment = getComment(inId);
		removeComment(comment);
	}

	public void removeComment(BlogComment inComment) throws OpenEditException {
		if (inComment != null) {
			getComments().remove(inComment);
		}
	}

	/**
	 * @param inContent
	 */
	public void setDescription(String inContent) {
		SyndContentImpl content = new SyndContentImpl();
		content.setType("text/html");
		content.setValue(inContent);
		setDescription(content);
	}

	public boolean isVisible() {
		return fieldVisible;
	}

	public void setVisible(boolean invisible) {
		this.fieldVisible = invisible;
	}

	public SimpleDateFormat getGmtStandard() {
		if (fieldGmtStandard == null) {
			fieldGmtStandard = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		}
		return fieldGmtStandard;
	}

	public Date parse(String inDate) {
		try {
			if (inDate.startsWith("2")) {
				return getGmtStandard().parse(inDate);
			} else {
				return SimpleDateFormat.getDateTimeInstance().parse(inDate);
			}
		} catch (ParseException ex) {
			throw new OpenEditRuntimeException(ex);
		}
	}

	public void setGmtStandard(SimpleDateFormat inGmtStandard) {
		fieldGmtStandard = inGmtStandard;
	}

	public User getUser() {
		return fieldUser;
	}

	public void setUser(User inUser) {
		fieldUser = inUser;
	}

	public List getNotify() {
		if (fieldNotify == null) {
			fieldNotify = new ArrayList();

		}

		return fieldNotify;
	}

	public void setNotify(List inNotify) {
		fieldNotify = inNotify;
	}

	public BlogCommentArchive getCommentArchive() {
		return fieldCommentArchive;
	}

	public void setCommentArchive(BlogCommentArchive inCommentArchive) {
		fieldCommentArchive = inCommentArchive;
	}

	public int compareTo(Object inEntry) {
		BlogEntry in = (BlogEntry) inEntry;
		return getPublishedDate().compareTo(in.getPublishedDate());
	}

	public Map getProperties() {
		if (fieldProperties == null) {
			fieldProperties = new HashMap();

		}

		return fieldProperties;
	}

	public String getTitle() {
		String title = super.getTitle();
		return title;
	}

	public void setProperties(Map fieldProperties) {
		this.fieldProperties = fieldProperties;
	}

	public void addProperty(String key, String value) {
		getProperties().put(key, value);
	}

	public String getProperty(String key) {
		if ("title".equals(key)) {
			return getTitle();
		}
		if ("content".equals(key)) {
			return getDescription().getValue();
		}
		if ("date".equals(key)) {
			return DateStorageUtil.getStorageUtil().formatForStorage(getPublishedDate());
		}
		return (String) getProperties().get(key);
	}

	public String get(String key) {
		return getProperty(key);
	}

	public void addNotify(User inUser) {
		getNotify().add(inUser);

	}

	public void removeNotify(User inUser) {
		getNotify().remove(inUser);

	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSourcePath() {
		return getLink();
	}

	@Override
	public void setId(String inNewid) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setName(String inName) {
		setProperty("name", inName);

	}

	public void setProperty(String inId, String inValue) {
		addProperty(inId, inValue);

	}

	public void setSourcePath(String inSourcepath) {
		// TODO Auto-generated method stub

	}

	public int getAge() {
		Date now = new Date();
		Date published = getPublishedDate();
		return daysBetween(published, now);
	}
	 public int daysBetween(Date d1, Date d2){
	     return (int)( (d2.getTime() - d1.getTime()) / (1000 * 60 * 60 * 24));
	         }
}
