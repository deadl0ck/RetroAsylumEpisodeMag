package com.deadlock.retroasylum.ra_episodes_mag;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfWriter;

public class MagGenerator
{
    public final static String START_URL		= "http://retroasylum.com/category/all-posts/podcasts/page/";
    public final static int NUM_PAGES			= 67;
    public final static String DIV_TAG			= "div";
    public final static String PARAGRAPH_TAG	= "p";
    public final static String PP_CLASS			= "previous-posts-container";
    public final static String TITLE_CLASS		= "post-block-editorial-title";
    public final static String ANCHOR_TAG		= "a";
    public final static String IMAGE_TAG		= "img";
    public final static String HREF				= "href";
    public final static String SOURCE			= "src";
    public final static String PDF_NAME			= "/Users/martinstephenson/Desktop/RA Episode Guide.pdf";
    public final static double IMAGE_SCALE		= 0.85;
    private boolean done						= false;

	private int currentPage	= NUM_PAGES;

    public static void main(String[] args) throws IOException, DocumentException
    {
    	System.out.println(MagGenerator.getTimestamp() + "Starting to process....");
        MagGenerator gen = new MagGenerator();
        gen.process();
        System.out.println(MagGenerator.getTimestamp() + "Processing complete - PDF is available at: " + PDF_NAME);
    }

    public static String getTimestamp()
    {
    	DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    	Date date = new Date();
    	return "[" + dateFormat.format(date) + "]";
    }

    private void process() throws IOException, DocumentException
    {
        // Create the PDF
        com.itextpdf.text.Document pdfDoc = new com.itextpdf.text.Document();
        pdfDoc.setPageSize(new Rectangle(PageSize.A4.getWidth(), PageSize.A4.getHeight()));
        PdfWriter.getInstance(pdfDoc, new FileOutputStream(PDF_NAME));
        pdfDoc.open();

        ArrayList<String> fullUrlList = new ArrayList<String>();

        for (int i = 0; i < NUM_PAGES; i++)
        {
            // Episodes are in reverse order in each page, so we store them and then process in reverse
            List<String> episodeUrlList = new ArrayList<String>();

            String html = this.getHtml(this.getCurrentPageUrl());
            this.decrementPage();

            // Get the main body for the page
            Document doc = Jsoup.parse(html);
            Elements body = doc.body().children();

            // Get the tag and class we are interested in
            Element previousPosts = this.getTagAndClassAndChildren(body, DIV_TAG, PP_CLASS);
            if (previousPosts != null)
            {
                List<String> pages = this.getLinkedPages(previousPosts);
                for (String url: pages)
                {
                    if (!url.isEmpty())
                    	episodeUrlList.add(url);
                }
            }

            // Process in reverse
            for (int j = (episodeUrlList.size() - 1); j >=0; j--)
            	fullUrlList.add(episodeUrlList.get(j));
//            	this.processPage(episodeUrlList.get(j), pdfDoc);
        }

        // Process all pages in reverse
        for (int j = (fullUrlList.size() - 1); j >=0; j--)
        	this.processPage(fullUrlList.get(j), pdfDoc);

        pdfDoc.close();
    }

    private void processPage(String url, com.itextpdf.text.Document pdfDoc) throws IOException, DocumentException
    {
        System.out.println(MagGenerator.getTimestamp() + "Processing Page: " + url);
        String html = this.getHtml(url);
        Document doc = Jsoup.parse(html);
        Elements body = doc.body().children();

        Element title = this.getTagAndClassAndChildren(body, DIV_TAG, TITLE_CLASS);

        String titleText = title.ownText();
        String pictureUrl = "";

        Elements paragraphs = doc.select(PARAGRAPH_TAG);

        int currentIndex = 0;

        // This is horrible hard-coded shit but I'm too busy right now to figure it out
        if ((url.indexOf("episode-71-funstock-co-uk") != -1) && !this.isDone())
        {
        	this.setDone(true);
        	pictureUrl = "http://retroasylum.com/wp-content/uploads/2014/02/RA_Episode_71.jpg";
        }
        else if (url.indexOf("episode-202-preservation") != -1)
        	pictureUrl = "http://retroasylum.com/wp-content/uploads/2019/04/202a.png";
        else
        {

	        // Paragraph 1 is the pic (most of the time!!)
	        Element picElement = paragraphs.get(currentIndex++);
	        if (picElement.toString().equals("<p>&nbsp;</p>"))
	            picElement = paragraphs.get(currentIndex++);

//	        if (url.indexOf("episode-71-funstock-co-uk") != -1)
//	        	System.out.println("Here");

	        Elements images = picElement.select(IMAGE_TAG);
	        pictureUrl = images.get(0).attr(SOURCE);
        }

        // Add to the document
        Font titleFont = FontFactory.getFont(FontFactory.TIMES_ROMAN, 18, BaseColor.BLACK);
        Chunk titleChunk = new Chunk(titleText, titleFont);

        URL picUrl = new URL(pictureUrl);
        Image img = Image.getInstance(picUrl);

        // Scale image and centre it
        img.scaleToFit((float)(PageSize.A4.getWidth() * IMAGE_SCALE), (float)(PageSize.A4.getHeight() * IMAGE_SCALE));
        float x = (PageSize.A4.getWidth() - img.getScaledWidth()) / 2;
        float y = (PageSize.A4.getHeight() - img.getScaledHeight()) / 2;
        img.setAbsolutePosition(x, y);

        // Create the episode link
        Font linkFont = FontFactory.getFont(FontFactory.TIMES_ROMAN, 10, BaseColor.BLACK);
        Chunk linkChunk = new Chunk("Full Episode Details and listen", linkFont);
        linkChunk.setAnchor(url);

        pdfDoc.newPage();

        Paragraph titleParagraph = new Paragraph();
        titleParagraph.setAlignment(Paragraph.ALIGN_CENTER);
        titleParagraph.add(titleChunk);

        Paragraph linkParagraph = new Paragraph();
        linkParagraph.setAlignment(Paragraph.ALIGN_CENTER);
        linkParagraph.add(linkChunk);

        pdfDoc.add(titleParagraph);
        pdfDoc.add(linkParagraph);
        pdfDoc.add(img);

    }

    private List<String> getLinkedPages(Element e)
    {
        ArrayList<String> pages = new ArrayList<String>();
        Elements children = e.children();
        for (Element child: children)
            if (child.tagName().equals(ANCHOR_TAG))
                pages.add(child.attr(HREF));
        return pages;
    }

    private Element getTagAndClassAndChildren(Elements elements, String tagName, String className)
    {
        for (Element e: elements)
        {
            if (e.tagName().equals(tagName))
            {
                if (e.className().equals(className))
                    return e;
            }
            Element el = this.getTagAndClassAndChildren(e.children(), tagName, className);
            if (el != null)
                return el;
        }
        return null;
    }

    private String getHtml(String url) throws IOException
    {
        return Jsoup.connect(url).get().html();
    }

    private String getCurrentPageUrl()
    {
        return START_URL + Integer.toString(this.getCurrentPage()) + "/";
    }

    private void decrementPage()
    {
        this.setCurrentPage(this.getCurrentPage() - 1);
    }

    protected int getCurrentPage()
    {
        return currentPage;
    }

    protected void setCurrentPage(int currentPage)
    {
        this.currentPage = currentPage;
    }
    protected boolean isDone()
    {
		return done;
	}

	protected void setDone(boolean done)
	{
		this.done = done;
	}

}
