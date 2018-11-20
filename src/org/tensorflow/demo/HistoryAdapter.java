package org.tensorflow.demo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class HistoryAdapter extends BaseAdapter {
    private Context context;
    private int layout;
    private List<HistoryActivity> historyActivities;

    public HistoryAdapter(Context context, int layout, List<HistoryActivity> historyActivities) {
        this.context = context;
        this.layout = layout;
        this.historyActivities = historyActivities;
    }

    @Override

    public int getCount() {
        return historyActivities.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = inflater.inflate(layout, null);

        TextView txtName = (TextView) view.findViewById(R.id.history_name);
        TextView txtTime = (TextView) view.findViewById(R.id.history_location);
        ImageView imgHistory = (ImageView) view.findViewById(R.id.history_image);

        HistoryActivity history = historyActivities.get(i);

        txtName.setText(history.getName());
        txtTime.setText(history.getTime());
//        imgHistory.setImageResource(Integer.parseInt(history.getImage()));
        imgHistory.setImageResource(history.getImage());
        return view;
    }

}
