/*
 * Created on Feb 18, 2005
 */
package com.openedit.blog;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.openedit.OpenEditException;
import com.openedit.comments.Comment;
import com.openedit.users.User;

/**
 * A blog comment, attached to a blog entry.
 * 
 * @author cburkey
 */
public class BlogComment extends Comment
{
	protected String fieldAuthor; //friendly name
	protected String fieldId;
	protected boolean fieldVisible;
	
	public String getAuthor()
	{
		return fieldAuthor;
	}
	public void setAuthor(String inAuthor)
	{
		fieldAuthor = inAuthor;
	}
	
	/**
	 * @deprecated
	 * @return
	 */
	public String getContent()
	{
		return getComment();
	}
	
	/**
	 * @deprecated
	 * @param inContent
	 */
	public void setContent(String inContent)
	{
		setComment(inContent);
	}
	
	/**
	 * @deprecated
	 * @return
	 */
	public Date getDateTime()
	{
		return getDate();
	}
	/**
	 * @deprecated
	 * @param inDateTime
	 */
	public void setDateTime(Date inDateTime)
	{
		setDate(inDateTime);
	}
	public String published(String inFormat)
	{
		SimpleDateFormat format = new SimpleDateFormat(inFormat);
		return format.format(getDateTime());
	}
	public String getId() 
	{
		return fieldId;
	}
	public void setId(String inId)
	{
		fieldId = inId;
	}
	public boolean isVisible()
	{
		return fieldVisible;
	}
	public void setVisible(boolean inVisible)
	{
		fieldVisible = inVisible;
	}
	
	public boolean canEdit(User inUser) throws OpenEditException
	{
		if ( inUser != null )
		{
			if( inUser.getShortDescription().equals( getAuthor() ) )
			{
				return true;
			}
			if( inUser.hasPermission("oe.administration"))
			{
				return true;
			}
		}
		return false;
	}
}
