package fav.drtinao.skama;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.skamav2.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Class basically serves as a bridge between ListView object and information, that should be presented in ListView (in this case news title + picture).
 */
public class NewsAdapter extends BaseAdapter {
    /* variables assigned in constructor - START */
    private Activity appActivity; /* reference to Activity of the application */
    private ArrayList<LogicNewsCrypto.PieceNews> newsList; /* news */
    /* variables assigned in constructor - END */

    /**
     * Constructor takes reference to Activity of the application and reference to ArrayList with news data.
     * @param appActivity application Activity
     * @param newsList ArrayList with news data
     */
    public NewsAdapter(Activity appActivity, ArrayList<LogicNewsCrypto.PieceNews> newsList) {
        this.appActivity = appActivity;
        this.newsList = newsList;
    }

    /**
     * Returns number of items, which are currently presented in the ListView object (number of news displayed in this case)
     * @return number of visible pieces of news
     */
    @Override
    public int getCount() {
        return newsList.size();
    }

    @Override
    public Object getItem(int position) {
        return newsList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Method is called for every item passed to this adapter - sets layout for each item.
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder viewHolder = new ViewHolder();

        if(convertView == null) { /* if layout not yet inflated, inflate it */
            LayoutInflater inflater = appActivity.getLayoutInflater();
            convertView = inflater.inflate(R.layout.news_item_lv, parent, false);

            viewHolder.newsImage = convertView.findViewById(R.id.news_item_image_id);
            viewHolder.newsTitle = convertView.findViewById(R.id.news_item_tv_id);
            convertView.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder) convertView.getTag();
        }

        //viewHolder.newsImage.setImageResource(R.drawable.ic_converter_nav);
        viewHolder.newsTitle.setText(newsList.get(position).getNewsTitle());

        if(newsList.get(position).getPictureNewsLink() != null && !newsList.get(position).getPictureNewsLink().isEmpty())
        Picasso.get().load(newsList.get(position).getPictureNewsLink()).into(viewHolder.newsImage);

        return convertView;
    }

    /**
     * Inner class used just for smoother work with ListView - stores components (=> no need to repeatedly use findViewById).
     */
    static class ViewHolder{
        ImageView newsImage;
        TextView newsTitle;
    }
}
