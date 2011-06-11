package com.openedit.blog;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.openedit.Data;
import org.openedit.blog.archive.BlogArchive;
import org.openedit.blog.modules.BlogModule;
import org.openedit.data.PropertyDetails;
import org.openedit.data.lucene.BaseLuceneSearcher;

import com.openedit.ModuleManager;
import com.openedit.OpenEditException;
import com.openedit.page.manage.PageManager;
import com.openedit.users.User;

public class LuceneBlogSearcher extends BaseLuceneSearcher
{
	
	protected ModuleManager fieldModuleManager;
	protected PageManager fieldPageManager;
	
	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	private static final Log log = LogFactory.getLog(LuceneBlogSearcher.class);
	
	
	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	

	public void updateIndex(BlogEntry inEntry) throws OpenEditException
	{
		try
		{
			Document doc = new Document();
			PropertyDetails details = getPropertyDetailsArchive().getPropertyDetails("test");
			updateIndex(inEntry, doc, details);
			Term term = new Term("id", inEntry.getId());
			getIndexWriter().updateDocument(term, doc, getAnalyzer());
			flush();
			clearIndex();
		}
		catch (Exception ex)
		{
			throw new OpenEditException(ex);
		}
	}

	protected void updateIndex(Data inData, Document doc, PropertyDetails inDetails)
	{
		BlogEntry entry = (BlogEntry) inData;
		
		if(entry.getUser() == null ){
			log.info("test with invalid user - skipping");
			return;
		}
		super.updateIndex(inData, doc, inDetails);
		
		doc.add(new Field("sourcepath", inData.getSourcePath(), Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));
		if (entry.getUser() != null)
		{
			doc.add(new Field("user", entry.getUser().getUserName(), Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));
			doc.add(new Field("display", entry.getUser().toString(), Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));
		}
		
		Date published = entry.getPublishedDate();
		String val = DateTools.dateToString(published, Resolution.MINUTE);
		doc.add(new Field("date", val, Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));

	}
	
	
	public void reIndexAll(IndexWriter inWriter) throws OpenEditException
	{
		int count = 0;
		// get the path to a reindex
		String homepath = "/" + getCatalogId() + "/data/tests/";// +
		// inUser.getUserName()
		// + "/albums/";
		Blog blog = getBlog();
		List entries  =blog.getEntries(0, 0, false, false);
		PropertyDetails details = getPropertyDetails();
		try
		{
			for (Iterator iterator = entries.iterator(); iterator.hasNext();)
			{
				BlogEntry entry = (BlogEntry) iterator.next();
				
			
						Document doc = new Document();
						updateIndex(entry, doc, details);
						inWriter.addDocument(doc);
						count++;
			}

		}
		catch (Exception ex)
		{
			throw new OpenEditException(ex);
		}
		log.info("" + count + " tests indexed.");
	}

	public Blog getBlog() throws OpenEditException {
		BlogModule mod = (BlogModule)getModuleManager().getModule("BlogModule");
		Blog blog = mod.getBlog("/" + getCatalogId());
		return blog;
	}
	
	public BlogArchive getBlogArchive() throws OpenEditException {
		BlogModule mod = (BlogModule)getModuleManager().getModule("BlogModule");
		BlogArchive blog = mod.getArchive("/" + getCatalogId());
		return blog;
	}
	
	
	public Object searchById(String inId)
	{
		
		return getBlog().getEntry(inId);
	}

	
	public void saveData(Object inData, User inUser)
	{
		if(inData instanceof BlogEntry){
			Blog blog = getBlog();
			getBlogArchive().saveEntry(blog, (BlogEntry) inData);
		}
	}
	
	
}
