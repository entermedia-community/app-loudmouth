/*
 * Created on Oct 18, 2006
 */
package org.openedit.blog.archive;

import com.openedit.page.manage.PageManager;
import com.openedit.users.UserManager;
import com.openedit.util.XmlUtil;

public class BaseArchive
{
	protected UserManager fieldUserManager;
	protected PageManager fieldPageManager;
	protected XmlUtil fieldXmlUtil;

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	public UserManager getUserManager()
	{
		return fieldUserManager;
	}

	public void setUserManager(UserManager inUserManager)
	{
		fieldUserManager = inUserManager;
	}

	public XmlUtil getXmlUtil()
	{
		if (fieldXmlUtil == null)
		{
			fieldXmlUtil = new XmlUtil();
		}
		return fieldXmlUtil;
	}

	public void setXmlUtil(XmlUtil inXmlUtil)
	{
		fieldXmlUtil = inXmlUtil;
	}

}
