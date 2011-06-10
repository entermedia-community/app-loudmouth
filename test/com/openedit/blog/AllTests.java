/*
 * Created on Feb 18, 2005
 */
package com.openedit.blog;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author cburkey
 *
 */
public class AllTests
{

	public static Test suite()
	{
		TestSuite suite = new TestSuite("Test for com.openedit.blog");
		//$JUnit-BEGIN$
		suite.addTestSuite(BlogTest.class);
		suite.addTestSuite(PageCommentTest.class);
		//$JUnit-END$
		return suite;
	}
}
