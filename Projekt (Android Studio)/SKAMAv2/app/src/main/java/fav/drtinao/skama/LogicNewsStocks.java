package fav.drtinao.skama;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.skamav2.R;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;

/**
 * Contains logic for retrieving news for stocks part of the application. Multiple sources of news are supported.
 */
public class LogicNewsStocks extends AsyncTask<Void, Void, Void>{
    /* variables assigned in constructor - START */
    private Activity appActivity; /* reference to Activity of the application */
    private boolean noInternet; /* true when internet connection is not available */
    /* variables assigned in constructor - END */

    /* variables which reflects user preferences - START */
    private int sourceNumPref; /* news source number - default source num is 0 */
    /* variables which reflects user preferences - END */

    /* important variables assigned during parsing process - START */
    private ArrayList<LogicNewsCrypto.PieceNews> newsList; /* news */
    /* important variables assigned during parsing process - END */

    /* constants which define part of web pages addresses, from which can stocks news be retrieved - START */
    private final String PAGE_PREF0 = "https://www.akcie.cz/zpravy/vse?offset=0"; /* CZ, without pictures */
    private final String PAGE_PREF1 = "https://www.w4t.cz/akcie/1/"; /* CZ, with pictures */
    private final String PAGE_PREF2 = "https://zpravy.aktualne.cz/akcie/l~i:keyword:540/?offset=0"; /* CZ, with pictures */
    private final String PAGE_PREF3 = "https://www.irozhlas.cz/zpravy-tag/akcie?page=0"; /* CZ, with pictures */

    private final String PAGE_PREF4 = "https://www.fool.com/investing-news/?page=1"; /* EN, with pictures */
    private final String PAGE_PREF5 = "https://www.investing.com/news/stock-market-news/1"; /* EN, with pictures */
    private final String PAGE_PREF6 = "https://www.cnbctv18.com/market/stocks/page-1/"; /* EN, with pictures */
    private final String PAGE_PREF7 = "https://markets.businessinsider.com/news?p=1"; /* EN, without pictures */
    /* constants which define part of web pages addresses, from which can stocks news be retrieved - END */

    /* other variables & objects used during parsing process - START */
    private ProgressDialog parsingPd; /* ProgressDialog object; is displayed on top of the activity and tells user, that the application is busy */
    private Document pageDOM; /* DOM of web page (contains info regarding wallet), which will be later parsed */
    /* other variables & objects used during parsing process - END */

    /* references to objects, which are in activity regarding stocks news part of the application - START */
    private ListView newsStocksLV; /* reference to news_stocks_lv; ListView object */
    /* references to objects, which are in activity regarding stocks news part of the application - END */

    /**
     * Constructor takes just application context, which is needed for working with UI elements.
     * @param appContext application context
     */
    public LogicNewsStocks(Context appContext){
        this.appActivity = (Activity) appContext;
        this.noInternet = false;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        loadPreferences();
        acquireRefViewsInfo();

        newsList = new ArrayList<>();

        parsingPd = new ProgressDialog(appActivity);
        parsingPd.setMessage(appActivity.getResources().getString(R.string.logic_retrieving));
        parsingPd.setCancelable(false);
        parsingPd.show();
    }

    /**
     * Method is used to perform backgrounded tasks and cannot operate with UI elements (by Android OS definition).
     * Here is used for downloading web page source, which is parsed afterwards.
     * @param voids not used here
     * @return not used
     */
    @Override
    protected Void doInBackground(Void... voids) {
        parseInfo();
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        if(noInternet){ /* lost internet connection - display alert */
            parsingPd.dismiss();

            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(appActivity);
            alertBuilder.setTitle(appActivity.getResources().getString(R.string.no_internet_alert_title));
            alertBuilder.setMessage(appActivity.getResources().getString(R.string.no_internet_alert_mes));
            alertBuilder.setPositiveButton(appActivity.getResources().getString(R.string.no_internet_alert_ok),null);
            alertBuilder.show();

            ((AppCompatActivity)appActivity).getSupportFragmentManager().beginTransaction().replace(R.id.frame_nav_id, new FragmentWalletCrypto()).addToBackStack(null).commit();
            return;
        }

        parsingPd.dismiss();

        showRetrievedInfo();
    }

    /**
     * Parses code of web page which is used for getting information about news regarding stocks.
     * Parsing provides pieces of information, which are then stored in respective variables (like titles of articles and links to respective articles).
     */
    private void parseInfo(){
        retrievePageDOM();
        if(pageDOM == null){
            noInternet = true;
            return;
        }

        switch(sourceNumPref){
            case 0:
                retrieveInfo0();
                break;

            case 1:
                retrieveInfo1();
                break;

            case 2:
                retrieveInfo2();
                break;

            case 3:
                retrieveInfo3();
                break;

            case 4:
                retrieveInfo4();
                break;

            case 5:
                retrieveInfo5();
                break;

            case 6:
                retrieveInfo6();
                break;

            case 7:
                retrieveInfo7();
                break;
        }
    }

    /**
     * Downloads source code of wanted page and builds DOM from it (contains information about stocks news).
     */
    private void retrievePageDOM(){
        try{
            switch(sourceNumPref){
                case 0:
                    pageDOM = Jsoup.connect(PAGE_PREF0).timeout(12000).ignoreContentType(true).get();
                    break;

                case 1:
                    pageDOM = Jsoup.connect(PAGE_PREF1).timeout(12000).ignoreContentType(true).get();
                    break;

                case 2:
                    pageDOM = Jsoup.connect(PAGE_PREF2).timeout(12000).ignoreContentType(true).get();
                    break;

                case 3:
                    pageDOM = Jsoup.connect(PAGE_PREF3).timeout(12000).ignoreContentType(true).get();
                    break;

                case 4:
                    pageDOM = Jsoup.connect(PAGE_PREF4).timeout(12000).ignoreContentType(true).get();
                    break;

                case 5:
                    pageDOM = Jsoup.connect(PAGE_PREF5).timeout(12000).ignoreContentType(true).get();
                    break;

                case 6:
                    pageDOM = Jsoup.connect(PAGE_PREF6).timeout(12000).ignoreContentType(true).get();
                    break;

                case 7:
                    pageDOM = Jsoup.connect(PAGE_PREF7).timeout(12000).ignoreContentType(true).get();
                    break;
            }
        } catch (Exception e) {
            noInternet = true;
            e.printStackTrace();
        }
    }

    /**
     * Parses downloaded DOM of the web page which contains news regarding stocks and gets needed information like title link.
     */
    private void retrieveInfo0(){
        /* retrieve news visible on actual page - START */
        Element otherNewsOuterEl = pageDOM.getElementsByClass("content").first();
        Elements otherNewsAllEl = otherNewsOuterEl.getElementsByClass("anote"); /* contains also some unwanted information, go on */

        for(int i = 0; i < otherNewsAllEl.size(); i++){ /* retrieve title and link for every article */
            Element pieceOfNews = otherNewsAllEl.get(i).getElementsByTag("a").first(); /* get one piece of news */
            String titleNews = pieceOfNews.text();
            String linkNews = "https://www.akcie.cz/" + pieceOfNews.attr("href");

            /* get picture link - START */
            String pictureNews = "PICTURE_NOT_PRESENT";
            newsList.add(new LogicNewsCrypto.PieceNews(titleNews, linkNews, pictureNews));
            /* get picture link - END */
        }
        /* retrieve news visible on actual page - END */
    }

    /**
     * Parses downloaded DOM of the web page which contains news regarding stocks and gets needed information like title link.
     */
    private void retrieveInfo1(){
        /* retrieve news visible on actual page - START */
        Element otherNewsOuterEl = pageDOM.getElementsByClass("col-md-8").first().getElementsByClass("articles-list").first();
        Elements otherNewsAllEl = otherNewsOuterEl.getElementsByClass("article"); /* contains also some unwanted information, go on */

        for(int i = 0; i < otherNewsAllEl.size(); i++){ /* retrieve title, link and picture for every article */
            Element pieceOfNews = otherNewsAllEl.get(i); /* get one piece of news */
            Element infoTitleLink = pieceOfNews.getElementsByTag("a").first(); /* continue with parsing for title and link */

            String titleNews = infoTitleLink.text();
            String linkNews = infoTitleLink.attr("href");

            /* get picture link - START */
            String pictureNews = pieceOfNews.getElementsByTag("img").first().attr("data-src");
            newsList.add(new LogicNewsCrypto.PieceNews(titleNews, linkNews, pictureNews));
            /* get picture link - END */
        }
        /* retrieve news visible on actual page - END */
    }

    /**
     * Parses downloaded DOM of the web page which contains news regarding stocks and gets needed information like title link.
     */
    private void retrieveInfo2(){
        /* retrieve news visible on actual page - START */
        Element otherNewsOuterEl = pageDOM.getElementsByClass("left-column").first();
        Elements otherNewsAllEl = otherNewsOuterEl.getElementsByClass("small-box--article"); /* add classic articles + articles with photo gallery */

        for(int i = 0; i < otherNewsAllEl.size(); i++){ /* retrieve title, link and picture for every article */
            Element pieceOfNews = otherNewsAllEl.get(i); /* get one piece of news */

            String titleNews = pieceOfNews.getElementsByTag("h3").first().text();
            String linkNews = "https://www.aktualne.cz/" + pieceOfNews.getElementsByTag("a").first().attr("href");

            /* get picture link - START */
            String pictureNews = pieceOfNews.getElementsByTag("img").first().attr("src");
            pictureNews = "https:" + pictureNews;
            newsList.add(new LogicNewsCrypto.PieceNews(titleNews, linkNews, pictureNews));
            /* get picture link - END */
        }
        /* retrieve news visible on actual page - END */
    }

    /**
     * Parses downloaded DOM of the web page which contains news regarding stocks and gets needed information like title link.
     */
    private void retrieveInfo3(){
        /* retrieve news visible on actual page - START */
        Element newsOuterEl = pageDOM.getElementsByClass("col--main").first().getElementsByClass("c-articles__list").first();
        Elements newsAllEl = newsOuterEl.getElementsByTag("article");

        for(int i = 0; i < newsAllEl.size(); i++){ /* retrieve title, link and picture for every article */
            Element pieceOfNews = newsAllEl.get(i); /* get one piece of news */
            Element newsTitleLink = pieceOfNews.getElementsByTag("h3").first().getElementsByTag("a").first(); /* continue with parsing for title and link */

            String titleNews = newsTitleLink.text();
            String linkNews = "https://www.irozhlas.cz/" + newsTitleLink.attr("href");

            /* get picture link - START */
            String pictureNews = pieceOfNews.getElementsByTag("img").first().attr("src");
            newsList.add(new LogicNewsCrypto.PieceNews(titleNews, linkNews, pictureNews));
            /* get picture link - END */
        }
        /* retrieve news visible on actual page - END */
    }

    /**
     * Parses downloaded DOM of the web page which contains news regarding stocks and gets needed information like title link.
     */
    private void retrieveInfo4(){
        /* retrieve news visible on actual page - START */
        Element newsOuterEl = pageDOM.getElementById("article_listing").getElementsByClass("list-content").first(); /* news under "recent articles" contains all news (sorted by date) */
        Elements newsAllEl = newsOuterEl.getElementsByAttributeValue("data-id", "article-list");

        for(int i = 0; i < newsAllEl.size(); i++){ /* retrieve title, link and picture for every article */
            Element pieceOfNews = newsAllEl.get(i); /* get one piece of news */

            String titleNews = pieceOfNews.getElementsByClass("text").first().getElementsByTag("h4").first().text();
            String linkNews = "https://www.fool.com" + pieceOfNews.attr("href");

            /* get picture link - START */
            String pictureNews = pieceOfNews.getElementsByTag("img").first().attr("data-src");
            newsList.add(new LogicNewsCrypto.PieceNews(titleNews, linkNews, pictureNews));
            /* get picture link - END */
        }
        /* retrieve news visible on actual page - END */
    }

    /**
     * Parses downloaded DOM of the web page which contains news regarding stocks and gets needed information like title link.
     */
    private void retrieveInfo5(){
        /* retrieve news visible on actual page - START */
        Element newsOuterEl = pageDOM.getElementById("leftColumn").getElementsByClass("largeTitle").first();
        Elements newsAllEl = newsOuterEl.getElementsByAttribute("data-id");

        for(int i = 0; i < newsAllEl.size(); i++){ /* retrieve title, link and picture for every article */
            Element pieceOfNews = newsAllEl.get(i); /* get one piece of news */
            Element newsTitleLink = pieceOfNews.getElementsByClass("textDiv").first().getElementsByTag("a").first(); /* continue with parsing for title and link */

            String titleNews = newsTitleLink.attr("title");
            String linkNews;
            if(newsTitleLink.attr("href").charAt(0) == '/'){
                linkNews = "https://www.investing.com" + newsTitleLink.attr("href");
            }else{
                linkNews = newsTitleLink.attr("href");
            }

            /* get picture link - START */
            String pictureNews = pieceOfNews.getElementsByTag("img").first().attr("data-src");
            newsList.add(new LogicNewsCrypto.PieceNews(titleNews, linkNews, pictureNews));
            /* get picture link - END */
        }
        /* retrieve news visible on actual page - END */
    }

    /**
     * Parses downloaded DOM of the web page which contains news regarding stocks and gets needed information like title link.
     */
    private void retrieveInfo6(){
        /* retrieve news visible on actual page - START */
        Element newsOuterEl = pageDOM.getElementsByClass("article-list").first();
        Elements newsAllEl = newsOuterEl.getElementsByClass("article-holder");

        for(int i = 0; i < newsAllEl.size(); i++){ /* retrieve title, link and picture for every article */
            Element pieceOfNews = newsAllEl.get(i); /* get one piece of news */
            Element newsTitleLink = pieceOfNews.getElementsByClass("list_title").first().getElementsByTag("a").first(); /* continue with parsing for title and link */

            String titleNews = newsTitleLink.ownText();
            String linkNews = newsTitleLink.attr("href");

            /* get picture link - START */
            String pictureNews = pieceOfNews.getElementsByTag("img").first().attr("data-src");
            newsList.add(new LogicNewsCrypto.PieceNews(titleNews, linkNews, pictureNews));
            /* get picture link - END */
        }
        /* retrieve news visible on actual page - END */
    }

    /**
     * Parses downloaded DOM of the web page which contains news regarding stocks and gets needed information like title link.
     */
    private void retrieveInfo7(){
        /* retrieve news visible on actual page - START */
        Elements newsAllEl = pageDOM.getElementsByClass("further-news-container");

        for(int i = 0; i < newsAllEl.size(); i++){ /* retrieve title, link and picture for every article */
            Element pieceOfNews = newsAllEl.get(i); /* get one piece of news */
            Element newsTitleLink = pieceOfNews.getElementsByClass("news-link").first(); /* continue with parsing for title and link */

            String titleNews = newsTitleLink.attr("title");
            String linkNews = "https://markets.businessinsider.com" + newsTitleLink.attr("href");

            /* get picture link - START */
            String pictureNews = "PICTURE_NOT_PRESENT";
            newsList.add(new LogicNewsCrypto.PieceNews(titleNews, linkNews, pictureNews));
            /* get picture link - END */
        }
        /* retrieve news visible on actual page - END */
    }

    /**
     * Method gets references to some objects which are later used to display retrieved news.
     */
    private void acquireRefViewsInfo(){
        newsStocksLV = appActivity.findViewById(R.id.news_stocks_lv);
    }

    /**
     * Shows information regarding news in respective ListView object.
     */
    private void showRetrievedInfo(){
        NewsAdapter newsAdapter = new NewsAdapter(appActivity, newsList);
        newsStocksLV.setAdapter(newsAdapter);
        newsStocksLV.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent goLink = new Intent(Intent.ACTION_VIEW);

                goLink.setData(Uri.parse(newsList.get(position).getNewsLink()));
                appActivity.startActivity(goLink);
            }
        });
    }

    /**
     * Loads preferences regarding news. In this case just number of source, from which user wants to get news.
     */
    private void loadPreferences(){
        /* use SharedPreferences for user preferences store -> get instance (name of the save file is "preferences", file is private - only for use with this app) */
        SharedPreferences sharedPref = appActivity.getSharedPreferences(appActivity.getResources().getString(R.string.sharedpref_name), 0);
        sourceNumPref = sharedPref.getInt(appActivity.getResources().getString(R.string.settings_sharedpref_stocks_news_pref), 0);
    }
}
