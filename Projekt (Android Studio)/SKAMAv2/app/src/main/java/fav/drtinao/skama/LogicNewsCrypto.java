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
import android.util.Log;
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
 * Contains logic for retrieving news for cryptocurrency part of the application. Multiple sources of news are supported.
 */
public class LogicNewsCrypto extends AsyncTask<Void, Void, Void> {
    /* variables assigned in constructor - START */
    private Activity appActivity; /* reference to Activity of the application */
    private boolean noInternet; /* true when internet connection is not available */
    /* variables assigned in constructor - END */

    /* variables which reflects user preferences - START */
    private int sourceNumPref; /* news source number - default source num is 0 */
    /* variables which reflects user preferences - END */

    /* important variables assigned during parsing process - START */
    private ArrayList<PieceNews> newsList; /* news */
    /* important variables assigned during parsing process - END */

    /* constants which define part of web pages addresses, from which can info about wallet content be retrieved - START */
    private final String PAGE_PREF0 = "https://cryptosvet.cz/category/novinky/"; /* CZ, rss is not available */
    private final String PAGE_PREF1 = "https://www.fxstreet.cz/kryptomeny-zpravodajstvi.html?active_page=1"; /* CZ/SK, rss not available */
    private final String PAGE_PREF2 = "https://www.kryptonovinky.com/novinky-2/"; /* CZ, rss is not available */
    private final String PAGE_PREF3 = "https://www.coinmagazin.cz/category/zpravy/page/1/"; /* CZ, rss is not available */
    private final String PAGE_PREF4 = "https://kryptomagazin.cz/category/zpravy/"; /* rss exists - but does not contain much info for unknown reason */

    private final String PAGE_PREF5 = "https://www.ccn.com/crypto/";
    private final String PAGE_PREF6 = "https://nulltx.com/category/news/crypto/page/1/";
    private final String PAGE_PREF7 = "https://cryptoslate.com/news/";
    private final String PAGE_PREF8 = "https://www.coinspeaker.com/news/crypto/page/1/";
    private final String PAGE_PREF9 = "https://themerkle.com/category/news/crypto/page/1/";
    /* constants which define part of web pages addresses, from which can info about wallet content be retrieved - END */

    /* other variables & objects used during parsing process - START */
    private ProgressDialog parsingPd; /* ProgressDialog object; is displayed on top of the activity and tells user, that the application is busy */
    private Document pageDOM; /* DOM of web page (contains info regarding wallet), which will be later parsed */
    /* other variables & objects used during parsing process - END */

    /* references to objects, which are in activity regarding cryptocurrency news part of the application - START */
    private ListView newsCryptoLV; /* reference to news_crypto_lv; ListView object */
    /* references to objects, which are in activity regarding cryptocurrency news part of the application - END */

    /**
     * Constructor takes just application context, which is needed for working with UI elements.
     * @param appContext application context
     */
    public LogicNewsCrypto(Context appContext){
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
     * Parses code of web page which is used for getting information about news regarding cryptocurrencies.
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

            case 8:
                retrieveInfo8();
                break;

            case 9:
                retrieveInfo9();
                break;
        }
    }

    /**
     * Downloads source code of wanted page and builds DOM from it (contains information about cryptocurrency news).
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

                case 8:
                    pageDOM = Jsoup.connect(PAGE_PREF8).timeout(12000).ignoreContentType(true).get();
                    break;

                case 9:
                    pageDOM = Jsoup.connect(PAGE_PREF9).timeout(12000).ignoreContentType(true).get();
                    break;
            }
        } catch (Exception e) {
            noInternet = true;
            e.printStackTrace();
        }
    }

    /**
     * Parses downloaded DOM of the web page which contains news regarding cryptocurrencies and gets needed information like title link.
     */
    private void retrieveInfo0(){
        Element mainNewsOuterEl = pageDOM.getElementsByClass("jeg_heroblock_wrapper").first(); /* inside are 4 pieces of actual news (visible on every page - save just once) */

        /* Elements which are same for title + picture + link - START */
        Element firstMainNewsInnerEl = mainNewsOuterEl.getElementsByClass("jeg_hero_item_1").first(); /* first piece of news */
        Element secondMainNewsInnerEl = mainNewsOuterEl.getElementsByClass("jeg_hero_item_2").first(); /* second piece of news */
        Element thirdMainNewsInnerEl = mainNewsOuterEl.getElementsByClass("jeg_hero_item_3").first(); /* third piece of news */
        Element fourthMainNewsInnerEl = mainNewsOuterEl.getElementsByClass("jeg_hero_item_4").first(); /* fourth piece of news */
        /* Elements which are same for title + picture + link - END */

        /* continue with finding Elements specific for title + link - START */
        Element firstMainNewsPostEl = firstMainNewsInnerEl.getElementsByClass("jeg_post_title").first();
        Element secondMainNewsPostEl = secondMainNewsInnerEl.getElementsByClass("jeg_post_title").first();
        Element thirdMainNewsPostEl = thirdMainNewsInnerEl.getElementsByClass("jeg_post_title").first();
        Element fourthMainNewsPostEl = fourthMainNewsInnerEl.getElementsByClass("jeg_post_title").first();
        /* continue with finding Elements specific for title + link - END */

        /* retrieve actual news which are visible on top of the page - START */
        String firstMainNewsTitle = firstMainNewsPostEl.getElementsByAttribute("href").html();
        String firstMainNewsLink = firstMainNewsPostEl.getElementsByTag("a").attr("href");
        String secondMainNewsTitle = secondMainNewsPostEl.getElementsByAttribute("href").html();
        String secondMainNewsLink = secondMainNewsPostEl.getElementsByTag("a").attr("href");
        String thirdMainNewsTitle = thirdMainNewsPostEl.getElementsByAttribute("href").html();
        String thirdMainNewsLink = thirdMainNewsPostEl.getElementsByTag("a").attr("href");
        String fourthMainNewsTitle = fourthMainNewsPostEl.getElementsByAttribute("href").html();
        String fourthMainNewsLink = fourthMainNewsPostEl.getElementsByTag("a").attr("href");
        /* retrieve actual news which are visible on top of the page - END */

        /* get actual news picture links - START */
        String firstMainNewsPic = firstMainNewsInnerEl.getElementsByClass("thumbnail-container").attr("data-src");
        String secondMainNewsPic = secondMainNewsInnerEl.getElementsByClass("thumbnail-container").attr("data-src");
        String thirdMainNewsPic = thirdMainNewsInnerEl.getElementsByClass("thumbnail-container").attr("data-src");
        String fourthMainNewsPic = fourthMainNewsInnerEl.getElementsByClass("thumbnail-container").attr("data-src");
        /* get actual news picture links - END */

        /* add main news to news ArrayList - START */
        newsList.add(new PieceNews(firstMainNewsTitle, firstMainNewsLink, firstMainNewsPic));
        newsList.add(new PieceNews(secondMainNewsTitle, secondMainNewsLink, secondMainNewsPic));
        newsList.add(new PieceNews(thirdMainNewsTitle, thirdMainNewsLink, thirdMainNewsPic));
        newsList.add(new PieceNews(fourthMainNewsTitle, fourthMainNewsLink, fourthMainNewsPic));
        /* add main news to news ArrayList - END */

        /* retrieve other news visible on actual page - START */
        Element otherNewsOuterEl = pageDOM.getElementsByClass("jeg_main_content").first(); /* every page has <= 10 news of standard type */
        Elements otherNewsAllEl = otherNewsOuterEl.getElementsByClass("jeg_pl_md_5"); /* contains also some unwanted information, go on */
        for(int i = 0; i < otherNewsAllEl.size(); i++){ /* retrieve title and link for every article */
            Element pieceOfNews = otherNewsAllEl.get(i).getElementsByClass("jeg_post_title").first(); /* get one piece of news */
            String titleNews = pieceOfNews.getElementsByAttribute("href").html();
            String linkNews = pieceOfNews.getElementsByTag("a").attr("href");

            /* get picture link - START */
            String pictureNews = otherNewsAllEl.get(i).getElementsByTag("img").first().attr("src");
            newsList.add(new PieceNews(titleNews, linkNews, pictureNews));
            /* get picture link - END */
        }
        /* retrieve other news visible on actual page - END */
    }

    /**
     * Parses downloaded DOM of the web page which contains news regarding cryptocurrencies and gets needed information like title link.
     */
    private void retrieveInfo1(){
        /* retrieve news visible on actual page - START */
        Elements newsOuterEl = pageDOM.getElementsByClass("article_list");

        for(int i = 0; i < newsOuterEl.size(); i++){ /* retrieve title + link + picture for every article */
            Element pieceOfNews = newsOuterEl.get(i).getElementsByClass("info").first(); /* get one piece of news */
            String titleNews = pieceOfNews.getElementsByTag("h2").first().attr("title");
            String linkNews = "https://www.fxstreet.cz" + pieceOfNews.getElementsByTag("a").first().attr("href");

            /* get picture link - START */
            Element pictureNewsEl = newsOuterEl.get(i).getElementsByClass("article_img").first().getElementsByTag("img").first();
            String pictureNews = "https://www.fxstreet.cz" + pictureNewsEl.attr("src");

            /* get picture link - END */
            newsList.add(new PieceNews(titleNews, linkNews, pictureNews));
        }
        /* retrieve news visible on actual page - END */
    }

    /**
     * Parses downloaded DOM of the web page which contains news regarding cryptocurrencies and gets needed information like title link.
     */
    private void retrieveInfo2(){
        /* retrieve news visible on actual page - START */
        Element newsOuterEl = pageDOM.getElementsByClass("mvp-blog-story-list").first(); /* contains also some unwanted information, go on */
        Elements newsInnerEl = newsOuterEl.getElementsByClass("mvp-blog-story-wrap");

        for(int i = 0; i < newsInnerEl.size(); i++){ /* retreive title and link for every article */
            Element pieceOfNews = newsInnerEl.get(i); /* get one piece of news */
            String titleNews = pieceOfNews.getElementsByTag("h2").first().html();
            String linkNews = pieceOfNews.getElementsByTag("a").first().attr("href");

            /* get picture link - START */
            Element pictureNewsEl = pieceOfNews.getElementsByClass("mvp-blog-story-img").first().getElementsByTag("img").first();
            String pictureNews = pictureNewsEl.attr("data-src");

            /* get picture link - END */
            newsList.add(new PieceNews(titleNews, linkNews, pictureNews));
        }
        /* retrieve news visible on actual page - END */
    }

    /**
     * Parses downloaded DOM of the web page which contains news regarding cryptocurrencies and gets needed information like title link.
     */
    private void retrieveInfo3(){
        /* retrieve actual news which are visible on right side of the page - START */
        Element mainNewsOuterEl = pageDOM.getElementsByClass("td_block_1").first();
        Elements mainNewsInnerEl = mainNewsOuterEl.getElementsByClass("td-module-thumb"); /* inside are 3 pieces of actual news (visible on every page - save just once) */

        for(int i = 0; i < mainNewsInnerEl.size(); i++){
            Element pieceOfNews = mainNewsInnerEl.get(i).getElementsByTag("a").first(); /* get one piece of news */

            String titleNews = pieceOfNews.attr("title");
            String linkNews = pieceOfNews.attr("href");

            /* get picture link - START */
            Element pictureNewsEl = pieceOfNews.getElementsByTag("img").first();
            String pictureNews = pictureNewsEl.attr("src");

            /* get picture link - END */
            newsList.add(new PieceNews(titleNews, linkNews, pictureNews));
        }
        /* retrieve actual news which are visible on right side of the page - END */

        /* retrieve actual news which are visible on top of the page - START */
        mainNewsOuterEl = pageDOM.getElementsByClass("td-big-grid-wrapper").first();
        mainNewsInnerEl = mainNewsOuterEl.getElementsByClass("td-module-thumb"); /* inside are 5 pieces of actual news (visible on every page - save just once) */

        for(int i = 0; i < mainNewsInnerEl.size(); i++){
            Element pieceOfNews = mainNewsInnerEl.get(i).getElementsByTag("a").first();

            String titleNews = pieceOfNews.attr("title");
            String linkNews = pieceOfNews.attr("href");

            /* get picture link - START */
            Element pictureNewsEl = pieceOfNews.getElementsByTag("img").first();
            String pictureNews = pictureNewsEl.attr("src");

            /* get picture link - END */
            newsList.add(new PieceNews(titleNews, linkNews, pictureNews));
        }
        /* retrieve actual news which are visible on top of the page - END */

        /* retrieve other news which are presented on the page - START */
        Element newsOuterEl = pageDOM.getElementsByClass("td-ss-main-content").first();
        Elements newsInnerEl = newsOuterEl.getElementsByClass("td-module-thumb"); /* inside are 5 pieces of actual news (visible on every page - save just once) */

        for(int i = 0; i < newsInnerEl.size(); i++){
            Element pieceOfNews = newsInnerEl.get(i).getElementsByTag("a").first();
            String titleNews = pieceOfNews.attr("title");
            String linkNews = pieceOfNews.attr("href");

            /* get picture link - START */
            Element pictureNewsEl = pieceOfNews.getElementsByTag("img").first();
            String pictureNews = pictureNewsEl.attr("src");

            /* get picture link - END */
            newsList.add(new PieceNews(titleNews, linkNews, pictureNews));
        }
        /* retrieve other news which are presented on the page - END */
    }

    /**
     * Parses downloaded DOM of the web page which contains news regarding cryptocurrencies and gets needed information like title link.
     */
    private void retrieveInfo4(){
        /* retrieve the most recent piece of news, located on the right side of the page - START */
        Element recentNewsOuterEl = pageDOM.getElementsByClass("td-category-pos-").first();
        Element recentNewsInnerEl = recentNewsOuterEl.getElementsByClass("td-module-thumb").first().getElementsByTag("a").first();

        String recentNewsTitle = recentNewsInnerEl.attr("title");
        String recentNewsLink = recentNewsInnerEl.attr("href");

        /* get picture link - START */
        Element recentPictureNewsEl = recentNewsInnerEl.getElementsByTag("span").first();
        String recentPictureNews = recentPictureNewsEl.attr("data-img-url");

        /* get picture link - END */
        newsList.add(new PieceNews(recentNewsTitle, recentNewsLink, recentPictureNews));
        /* retrieve the most recent piece of news, located on the right side of the page - END */

        /* retrieve other news which are presented on the page - START */
        Element newsOuterEl = pageDOM.getElementsByClass("tdb-category-loop-posts").first();
        Elements newsInnerEl = newsOuterEl.getElementsByClass("td-module-thumb");

        for(int i = 0; i < newsInnerEl.size(); i++){
            Element pieceOfNews = newsInnerEl.get(i).getElementsByTag("a").first();

            String titleNews = pieceOfNews.attr("title");
            String linkNews = pieceOfNews.attr("href");

            /* get picture link - START */
            Element pictureNewsEl = pieceOfNews.getElementsByTag("span").first();
            String pictureNews = pictureNewsEl.attr("style");
            /* get picture link - END */
            String[] splitPic = pictureNews.split("ground-image: url\\(");
            pictureNews = splitPic[1].substring(0, splitPic[1].length() - 1);

            newsList.add(new PieceNews(titleNews, linkNews, pictureNews));
        }
        /* retrieve other news which are presented on the page - END */
    }

    /**
     * Parses downloaded DOM of the web page which contains news regarding cryptocurrencies and gets needed information like title link.
     */
    private void retrieveInfo5(){
        /* retrieve news which are presented on the page - START */
        Element newsOuterEl = pageDOM.getElementsByClass("arch-psts").first();
        Elements newsInnerEl = newsOuterEl.getElementsByClass("fsp");

        for(int i = 0; i < newsInnerEl.size(); i++){
            Element pieceOfNews = newsInnerEl.get(i).getElementsByClass("image-container").first().getElementsByTag("a").first();

            String titleNews = pieceOfNews.attr("title");
            String linkNews = pieceOfNews.attr("href");

            /* get picture link - START */
            Element pictureNewsEl = pieceOfNews.getElementsByTag("amp-img").first();
            String pictureNews = pictureNewsEl.attr("src");

            /* get picture link - END */
            newsList.add(new PieceNews(titleNews, linkNews, pictureNews));
        }
        /* retrieve news which are presented on the page - END */
    }

    /**
     * Parses downloaded DOM of the web page which contains news regarding cryptocurrencies and gets needed information like title link.
     */
    private void retrieveInfo6(){
        /* retrieve news visible on to of the page - START */
        Element otherNewsOuterEl = pageDOM.getElementsByClass("td-big-grid-wrapper").first();
        Elements otherNewsAllEl = otherNewsOuterEl.getElementsByClass("td-module-thumb");

        for(int i = 0; i < otherNewsAllEl.size(); i++){ /* retrieve title and link for every article */
            Element pieceOfNews = otherNewsAllEl.get(i).getElementsByTag("a").first(); /* get one piece of news */
            String titleNews = pieceOfNews.attr("title");
            String linkNews = pieceOfNews.attr("href");

            /* get picture link - START */
            String pictureNews = pieceOfNews.getElementsByTag("img").first().attr("src");
            newsList.add(new PieceNews(titleNews, linkNews, pictureNews));
            /* get picture link - END */
        }
        /* retrieve news visible on to of the page - END */

        /* retrieve other news which are presented on the page - START */
        Element newsOuterEl = pageDOM.getElementsByClass("td-ss-main-content").first();
        Elements newsInnerEl = newsOuterEl.getElementsByClass("td-module-thumb");

        for(int i = 0; i < newsInnerEl.size(); i++){
            Element pieceOfNews = newsInnerEl.get(i).getElementsByTag("a").first();

            String titleNews = pieceOfNews.attr("title");
            String linkNews = pieceOfNews.attr("href");

            /* get picture link - START */
            Element pictureNewsEl = pieceOfNews.getElementsByTag("img").first();
            String pictureNews = pictureNewsEl.attr("src");

            /* get picture link - END */
            newsList.add(new PieceNews(titleNews, linkNews, pictureNews));
        }
        /* retrieve other news which are presented on the page - END */
    }

    /**
     * Parses downloaded DOM of the web page which contains news regarding cryptocurrencies and gets needed information like title link.
     */
    private void retrieveInfo7(){
        /* retrieve news which are presented on the page - START */
        Element newsOuterEl = pageDOM.getElementsByClass("list-feed").first();
        Elements newsInnerEl = newsOuterEl.getElementsByClass("list-post");

        for(int i = 0; i < newsInnerEl.size(); i++){
            Element pieceOfNews = newsInnerEl.get(i).getElementsByTag("a").first();

            String titleNews = pieceOfNews.attr("title");
            String linkNews = pieceOfNews.attr("href");

            /* get picture link - START */
            Element pictureNewsEl = pieceOfNews.getElementsByTag("img").first();
            String pictureNews = pictureNewsEl.attr("src");
            /* get picture link - END */
            newsList.add(new PieceNews(titleNews, linkNews, pictureNews));
        }
        /* retrieve news which are presented on the page - END */
    }

    /**
     * Parses downloaded DOM of the web page which contains news regarding cryptocurrencies and gets needed information like title link.
     */
    private void retrieveInfo8(){
        /* retrieve actual news which are visible on top of the page - START */
        Element mainNewsOuterEl = pageDOM.getElementsByClass("top-news-preview").first();
        Elements mainNewsInnerEl = mainNewsOuterEl.getElementsByTag("article"); /* inside are 3 pieces of most recent news (different on every page) */

        for(int i = 0; i < mainNewsInnerEl.size(); i++){
            Element pieceOfNews = mainNewsInnerEl.get(i).getElementsByClass("title").first().getElementsByTag("a").first();

            String titleNews = pieceOfNews.html();
            String linkNews = pieceOfNews.attr("href");

            /* get picture link - START */
            Element pictureNewsEl = mainNewsInnerEl.get(i).getElementsByTag("img").first();
            String pictureNews = pictureNewsEl.attr("data-src");
            /* get picture link - END */
            newsList.add(new PieceNews(titleNews, linkNews, pictureNews));
        }
        /* retrieve actual news which are visible on top of the page - END */

        /* retrieve other news which are presented on the page - START */
        Element newsOuterEl = pageDOM.getElementsByClass("content-news").first();
        Elements newsInnerEl = newsOuterEl.getElementsByTag("article");

        for(int i = 0; i < newsInnerEl.size(); i++){
            Element pieceOfNews = newsInnerEl.get(i).getElementsByClass("news-preview_title").first().getElementsByTag("a").first();

            String titleNews = pieceOfNews.html();
            String linkNews = pieceOfNews.attr("href");

            /* get picture link - START */
            Element pictureNewsEl = newsInnerEl.get(i).getElementsByTag("img").first();
            String pictureNews = pictureNewsEl.attr("data-src");

            /* get picture link - END */
            newsList.add(new PieceNews(titleNews, linkNews, pictureNews));
        }
        /* retrieve other news which are presented on the page - END */
    }

    /**
     * Parses downloaded DOM of the web page which contains news regarding cryptocurrencies and gets needed information like title link.
     */
    private void retrieveInfo9(){
        /* retrieve news visible on actual page - START */
        Element newsOuterEl = pageDOM.getElementById("content_box"); /* contains also some unwanted information, go on */
        Elements newsInnerEl = newsOuterEl.getElementsByTag("article");

        for(int i = 0; i < newsInnerEl.size(); i++){ /* retrieve title and link for every article */
            Element pieceOfNews = newsInnerEl.get(i).getElementsByClass("post-image").first(); /* get one piece of news */
            String titleNews = pieceOfNews.attr("title");
            String linkNews = pieceOfNews.attr("href");

            /* get picture link - START */
            Element pictureNewsEl = pieceOfNews.getElementsByTag("img").first();
            String pictureNews = pictureNewsEl.attr("src");

            /* get picture link - END */
            newsList.add(new PieceNews(titleNews, linkNews, pictureNews));
        }
        /* retrieve news visible on actual page - END */
    }

    /**
     * Method gets references to some objects which are later used to display retrieved news.
     */
    private void acquireRefViewsInfo(){
        newsCryptoLV = appActivity.findViewById(R.id.news_crypto_lv);
    }

    /**
     * Shows information regarding news in respective ListView object.
     */
    private void showRetrievedInfo(){
        NewsAdapter newsAdapter = new NewsAdapter(appActivity, newsList);
        newsCryptoLV.setAdapter(newsAdapter);
        newsCryptoLV.setOnItemClickListener(new AdapterView.OnItemClickListener(){
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
        sourceNumPref = sharedPref.getInt(appActivity.getResources().getString(R.string.settings_sharedpref_crypto_news_pref), 0);
    }

    /**
     * Inner data class, carries data regarding piece of news (news title, news link, link to respective picture).
     */
    public static class PieceNews{
        private String newsTitle; /* news title */
        private String newsLink; /* news link */
        private String pictureNewsLink; /* picture related to news */

        /**
         * Constructor is used just for value initialization.
         * @param newsTitle news title
         * @param newsLink news link
         * @param pictureNewsLink link to picture, which is related to news
         */
        public PieceNews(String newsTitle, String newsLink, String pictureNewsLink){
            this.newsTitle = newsTitle;
            this.newsLink = newsLink;
            this.pictureNewsLink = pictureNewsLink;
        }

        /**
         * Getter for news title.
         * @return news title
         */
        public String getNewsTitle() {
            return newsTitle;
        }

        /**
         * Getter for news link.
         * @return news link
         */
        public String getNewsLink() {
            return newsLink;
        }

        /**
         * Getter for picture news link.
         * @return picture news link
         */
        public String getPictureNewsLink() {
            return pictureNewsLink;
        }
    }
}
