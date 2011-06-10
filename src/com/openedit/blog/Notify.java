/*
 * Created on Jan 14, 2006
 */
package com.openedit.blog;

import java.io.File;
import java.io.FileWriter;
import java.io.Serializable;
import java.io.Writer;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermedia.email.Recipient;
import org.entermedia.email.TemplateWebEmail;
import org.openedit.repository.filesystem.StringItem;

import com.openedit.OpenEditException;
import com.openedit.hittracker.HitTracker;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.users.Group;
import com.openedit.users.User;
import com.openedit.users.UserManager;
import com.openedit.util.FileUtils;

public class Notify implements Serializable
{
	private static final Log log = LogFactory.getLog(Notify.class);
	transient protected UserManager fieldUserManager;
	transient protected PageManager fieldPageManager;
	
	protected File fieldRootDirectory;
	protected String[] fieldGroupNames;
	protected boolean fieldKeepRunning;
	
	public UserManager getUserManager()
	{
		return fieldUserManager;
	}

	public void setUserManager(UserManager inUserManager)
	{
		fieldUserManager = inUserManager;
	}
	
	public void sendEmail( TemplateWebEmail inEmail, Writer inLog ) throws OpenEditException
	{
		fieldKeepRunning = true;
		FileWriter out = null;
		int grandTotal = 0;
		try
		{
			Set duplicate = new HashSet();
			inLog.write("<pre>");
			File logFile = getLogFile(inEmail.getMailTemplatePath());
			if ( logFile.exists() )
			{
				out = new FileWriter(logFile,true);
			}
			else
			{
				out = new FileWriter( logFile);
			}
			out.write("Starting Sending " + new Date() + "\n");
						
			for (int i = 0; i < getGroupNames().length && isKeepRunning(); i++)
			{
				Group group = getUserManager().getGroup(getGroupNames()[i]);
				inLog.write("Status, "+ group.getName() + " group starting to send\n" );
				out.write("Status, "+ group.getName() + " group starting to send\n" );
				int countSent = 0;
				int countSkipped = 0;
				int logCount = 0;
				HitTracker users = getUserManager().getUsersInGroup(group);
				for (Iterator iter = users.iterator(); iter.hasNext();)
				{
					User user = (User) iter.next();
					String email = user.getEmail();
					if (email != null && email.length() > 4 && !duplicate.contains(email.toLowerCase()))
					{
						Recipient rec = new Recipient();
						rec.setEmailAddress(user.getEmail());
						rec.setLastName(user.getLastName());
						rec.setFirstName(user.getFirstName());
						try
						{
							inEmail.setRecipient(rec);
							inEmail.send();
							out.write("Sent, email: " + rec.getEmailAddress() + ", username:" + user.getUserName() + "\n");
							duplicate.add( email.toLowerCase());
							countSent++;
						}
						catch (Exception ex)
						{
							inLog.write("Error, email: " + rec.getEmailAddress() + ", username:" + user.getUserName() +  ", error: " + ex.toString() + "\n" );
							out.write("Error, email: " + rec.getEmailAddress() + ", username:" + user.getUserName() +  ", error: " + ex.toString() + "\n" );
							log.error( ex);
						}
					}
					else
					{
						inLog.write("Skipping, " + email + " looks invalid or a duplicate\n");
						out.write("Skipping, " + email + " looks invalid or a duplicate\n");
	
						countSkipped++;
					}
					logCount++;
					grandTotal++;
					if ( logCount == 100 )
					{
						inLog.write("Status, Processed 100 more emails and " + grandTotal + " total\n");
						logCount = 0;
					}
					inLog.flush();
					out.flush();
				}
				inLog.write("Status, completed. sent:" + countSent + " skipped: " + countSkipped + "\n" );
				out.write("Status, completed. sent:" + countSent + " skipped: " + countSkipped + "\n" );
				inLog.write("</pre>");
				inLog.flush();
				setKeepRunning(false);
			}
		}
		catch ( Exception ex)
		{
			setKeepRunning(false);
			if ( ex instanceof OpenEditException)
			{
				throw (OpenEditException)ex;
			}
			throw new OpenEditException(ex);
		}
		finally
		{
			FileUtils.safeClose(out);			
		}
	}

	public void fixLinks(Page inMailTemplatePage, String base) throws OpenEditException
	{
		String content = inMailTemplatePage.getContent();
		//String base = inUrl.buildRoot() + inUrl.relativeHomePrefix();
		
		content = content.replaceAll("src=\\\"/","src=\"" + base + "/");
		content = content.replaceAll("src=/","src=" + base + "/");
		content = content.replaceAll("href=\\\"/","href=\"" + base + "/");
		content = content.replaceAll("href=/","href=" + base + "/");
		content = content.replaceAll("url\\('/","url('" + base + "/");
		content = content.replaceAll("\\$home",base );
		
		StringItem item = new StringItem(inMailTemplatePage.getPath(), content, inMailTemplatePage.getCharacterEncoding());
		inMailTemplatePage.setContentItem(item);
		
		getPageManager().putPage(inMailTemplatePage);
		
	}

	protected File getLogFile(String inPath)
	{
		File f = new File( getRootDirectory(), inPath + ".log");
		return f;
	}

	public boolean hasLog(String inPath)
	{
		File f = getLogFile(inPath);
		return f.exists();
	}
	public String[] getGroupNames()
	{
		return fieldGroupNames;
	}

	public void setGroupNames(String[] inGroupNames)
	{
		fieldGroupNames = inGroupNames;
	}

	public void cancel()
	{
		fieldKeepRunning = false;
	}

	public boolean isKeepRunning()
	{
		return fieldKeepRunning;
	}

	public void setKeepRunning(boolean inKeepRunning)
	{
		fieldKeepRunning = inKeepRunning;
	}

	public File getRootDirectory()
	{
		return fieldRootDirectory;
	}

	public void setRootDirectory(File inRootDirectory)
	{
		fieldRootDirectory = inRootDirectory;
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}
	
}