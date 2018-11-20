package org.tensorflow.demo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class History extends AppCompatActivity {
    ImageView imgSearch,imgData;
    DatabaseHelper databaseHelper;
    ListView listView;
    ArrayList<HistoryActivity> activities;
    HistoryAdapter historyAdapter;
    int image;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_history);
        Init();
        imgSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(History.this,DetectorActivity.class);
                startActivity(intent);
            }
        });


        listView = (ListView) findViewById(R.id.lvHistory);
        activities = new ArrayList<>();


        databaseHelper = new DatabaseHelper(this, "database.sqlite",null, 1);


//
//        Cursor dataHistory = databaseHelper.GetData("SELECT * FROM History");
//        activities.clear();
//        dataHistory.moveToLast();
//
//
//        for (int i = dataHistory.getCount() ; i > 0 ; i--){
//
//            String ten = dataHistory.getString(1);
//            int image = dataHistory.getInt(2);
//            String time = dataHistory.getString(3);
//            int id=dataHistory.getInt(0);
//            activities.add(new HistoryActivity(id,ten, image, time,null));
//            dataHistory.moveToPrevious();
//        }



        ListHistoryData();
       // Log.d("dddd",activities.get(3).getName()+activities.get(3).getImage());
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                HistoryActivity hist = new HistoryActivity();
                hist = activities.get(i);
                Log.d("histq+",hist.getImage()+"");
                // image=hist.getImage();
              //  String name_SqLite=hist.getName();
              //  String time_SqLite=hist.getTime();
               DialogDetailHistory(hist);
//                Log.d("ddd",image +""+name_SqLite);
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                int position_SqLite=activities.get(i).getId();
                DialogDeleteHistory(position_SqLite);
                return true;
            }
        });
        

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater=getMenuInflater();
        menuInflater.inflate(R.menu.menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId()==R.id.delete_history){
            DialogDeleteAllHistory();
        }
        return super.onOptionsItemSelected(item);
    }

    private void DialogDeleteAllHistory() {
        final AlertDialog.Builder dialogDelete = new AlertDialog.Builder(this);
        dialogDelete.setMessage("Bạn có chắc muốn xóa tất cả dòng lịch sử này");
        dialogDelete.setPositiveButton("Có", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                databaseHelper.QueryData("DELETE FROM History");
                Toast.makeText(History.this, "Đã Xóa", Toast.LENGTH_SHORT).show();
                ListHistoryData();
            }
        });
        dialogDelete.setNegativeButton("Không", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        dialogDelete.show();
    }

    private void DialogDeleteHistory(final int position_sqLite) {
        final AlertDialog.Builder dialogDelete = new AlertDialog.Builder(this);
        dialogDelete.setMessage("Bạn có chắc muốn xóa dòng lịch sử này");
        dialogDelete.setPositiveButton("Có", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                databaseHelper.QueryData("DELETE FROM History WHERE Id = '"+ position_sqLite +"'");
                Toast.makeText(History.this, "Đã Xóa", Toast.LENGTH_SHORT).show();
                ListHistoryData();
            }
        });
        dialogDelete.setNegativeButton("Không", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        dialogDelete.show();
    }

    private void ListHistoryData() {
        Cursor dataHistory = databaseHelper.GetData("SELECT * FROM History");
        activities.clear();
        dataHistory.moveToLast();


        for (int i = dataHistory.getCount() ; i > 0 ; i--){

            String ten = dataHistory.getString(1);
            int image = dataHistory.getInt(2);
            String time = dataHistory.getString(3);
            int id=dataHistory.getInt(0);
            activities.add(new HistoryActivity(id,ten, image, time,null));
            //Log.d("ddd",image+ "");
            dataHistory.moveToPrevious();
        }


        historyAdapter = new HistoryAdapter(this, R.layout.custom_history_view, activities);
        listView.setAdapter(historyAdapter);
        historyAdapter.notifyDataSetChanged();
    }

    private void DialogDetailHistory(HistoryActivity a) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_detail_history);

        databaseHelper = new DatabaseHelper(this, "database.sqlite",null, 1);
//        Cursor dataHistory = database.GetData("SELECT * FROM History WHERE Id = "+ postion_Sqlite +"");

        TextView txtTen = (TextView) dialog.findViewById(R.id.history_name_detail);
        TextView txtDescription = (TextView) dialog.findViewById(R.id.history_description);
        //ImageView imgDetail= (ImageView) findViewById(R.id.history_image_detail_dan);

        txtTen.setText(a.getName());
        txtDescription.setText(a.getTime());
        String imgNameDemo=a.getName().replaceAll("[^u0000-u007F]+","");
        String imgName = imgNameDemo.toLowerCase();
        //Log.d("ddd",imgName);
        int imageID=getRawResIdByName(imgName);
//        Log.d("chodan",imageID+":"+imgName);
//        imgDetail.setImageResource(imageID);



        dialog.show();
    }

    private void Init() {
        imgSearch= (ImageView) findViewById(R.id.imgSearch);
        imgData= (ImageView) findViewById(R.id.imgData);
    }

    private int getRawResIdByName(String resName) {
        String pkgName=getApplicationContext().getPackageName();
        return this.getResources().getIdentifier(resName,"drawable",pkgName);
    }


}
