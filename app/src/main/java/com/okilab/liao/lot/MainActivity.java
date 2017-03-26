package com.okilab.liao.lot;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.openlib.photopicker.PhotoPickerActivity;
import com.openlib.photopicker.utils.ImageLoader;
import com.openlib.photopicker.utils.OtherUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_PHOTO = 1;
    private GridView mGrideView;
    private EditText mRequestNum;
    private LinearLayout mRequestNumLayout;
    private int mColumnWidth;
    private List<String> mResults;
    private List<String> mKeylist = new ArrayList<>();
    private List<String> mKeylisttoshow = new ArrayList<>();
    private GridAdapter mAdapter;
    public static MainActivity mactivity;
    private String Finalkey = null;
    private String KEYspinner = "KEY";
    private int keylength = 16;
    private String tmpkey = "AAAAAAAAAAAAAAAA";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mactivity=this;
        mKeylisttoshow = mKeylist;
        //String tmpkey = "AAAAAAAAAAAAAAAA";
        mKeylisttoshow.add(tmpkey);
        //int screenWidth = OtherUtils.getWidthInPx(getApplicationContext());
        mRequestNum = (EditText) findViewById(R.id.request_num);
        mRequestNumLayout = (LinearLayout) findViewById(R.id.num_layout);
        mGrideView = (GridView) findViewById(R.id.gridview);
        int screenWidth = OtherUtils.getWidthInPx(getApplicationContext());
        mColumnWidth = (screenWidth - OtherUtils.dip2px(getApplicationContext(), 4))/3;

        //FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        //fab.setOnClickListener(new View.OnClickListener() {
        //    @Override
        //    public void onClick(View view) {
        //        Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
        //                .setAction("Action", null).show();
        //    }
        //});

        //botton: choose photo
        findViewById(R.id.picker_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int maxNum = PhotoPickerActivity.DEFAULT_NUM;
                if(!TextUtils.isEmpty(mRequestNum.getText())){
                    maxNum = Integer.valueOf(mRequestNum.getText().toString());
                }
                boolean showCamera = false;
                int selectedMode = PhotoPickerActivity.MODE_MULTI;
                //Toast.makeText(MainActivity.this,"Choose Photo",Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, PhotoPickerActivity.class);
                intent.putExtra(PhotoPickerActivity.EXTRA_MAX_MUN,maxNum);
                intent.putExtra(PhotoPickerActivity.EXTRA_SHOW_CAMERA, showCamera);
                intent.putExtra(PhotoPickerActivity.EXTRA_SELECT_MODE, selectedMode);
                startActivityForResult(intent, PICK_PHOTO);
                //PhotoPicker();
            }
        });

        //botto to clear key
        findViewById(R.id.key_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mKeylisttoshow.clear();
            }
        });


        //botton to add key
        findViewById(R.id.key_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                    //Toast.makeText(MainActivity.this,mResults.get(i).toString(),Toast.LENGTH_SHORT).show();
                    final EditText keyinputedit = new EditText(MainActivity.this);
                    AlertDialog.Builder inputdialog = new AlertDialog.Builder(MainActivity.this);
                    inputdialog.setTitle("Please input new Key").setView(keyinputedit);
                    inputdialog.setPositiveButton("Comfirm", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(MainActivity.this,keyinputedit.getText().toString(),Toast.LENGTH_SHORT).show();
                            //int m = mKeylist.get(2).length();
                            //String mm = ""+m;
                            //Toast.makeText(MainActivity.this,mm,Toast.LENGTH_SHORT).show();
                            String keynextinput = keyinputedit.getText().toString();
                            if(keynextinput.length() == keylength ){
                                mKeylisttoshow.add(keynextinput);
                            }else {
                                Toast.makeText(MainActivity.this,"The key is not allowed to use, Please re-input",Toast.LENGTH_SHORT).show();
                            }

                        }
                    }).show();

            }
        });


        //spinner to choose key
        Spinner keyoutputto = (Spinner) findViewById(R.id.keyoutput);
        //String[] mItems = getResources().getStringArray(R.array.SpinnerInput);
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_spinner_item, mKeylisttoshow);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        keyoutputto.setAdapter(adapter);
        keyoutputto.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //String[] languages = getResources().getStringArray(R.array.SpinnerInput);
                String keychoose = adapter.getItem(position);
                    Finalkey = keychoose;
                Toast.makeText(MainActivity.this, Finalkey, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //GridView
        mGrideView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String mResultsChoose = mAdapter.getItem(position);
                Toast.makeText(MainActivity.this,mResultsChoose,Toast.LENGTH_SHORT).show();
            }
        });

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //Only one to be sent in a exp
        if (id == R.id.Sendcon){
            //Toast.makeText(MainActivity.this,"OK",Toast.LENGTH_SHORT).show();
            //String key = "5RGP9A5F4PSMXZNQ";
            String con = "con";
            if(mResults != null && Finalkey != KEYspinner && Finalkey != null ) {
                try {
                    new ChangeNameAndSendToServer(mResults, mResults.size(), Finalkey, con); //
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }else {
                Toast.makeText(MainActivity.this,"No file to be sent OR key is incorrect",Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        if (id == R.id.Sendsig){
            //Toast.makeText(MainActivity.this,"OK",Toast.LENGTH_SHORT).show();
            //String key = "5RGP9A5F4PSMXZNQ";
            String sig = "sig";
            if(mResults != null && Finalkey != KEYspinner && Finalkey != null) {
                try {
                    new ChangeNameAndSendToServer(mResults, mResults.size(), Finalkey, sig); //
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }else {
                Toast.makeText(MainActivity.this,"No file to be sent OR key is incorrect",Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_PHOTO){
            if(resultCode == RESULT_OK){
                ArrayList<String> result = data.getStringArrayListExtra(PhotoPickerActivity.KEY_RESULT);
                showResult(result);
            }
        }
    }

    private void showResult(ArrayList<String> paths){
        if(mResults == null){
            mResults = new ArrayList<>();
        }
        mResults.clear();
        mResults.addAll(paths);

        if(mAdapter == null){
            mAdapter = new GridAdapter(mResults);
            mGrideView.setAdapter(mAdapter);
        }else {
            mAdapter.setPathList(mResults);
            mAdapter.notifyDataSetChanged();
        }
    }

    private class GridAdapter extends BaseAdapter {
        private List<String> pathList;

        public GridAdapter(List<String> listUrls) {
            this.pathList = listUrls;
        }

        @Override
        public int getCount() {
            return pathList.size();
        }

        @Override
        public String getItem(int position) {
            return pathList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public void setPathList(List<String> pathList) {
            this.pathList = pathList;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if(convertView == null){
                convertView = getLayoutInflater().inflate(R.layout.item_image, null);
                imageView = (ImageView) convertView.findViewById(R.id.imageView);
                convertView.setTag(imageView);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(mColumnWidth, mColumnWidth);
                imageView.setLayoutParams(params);
            }else {
                imageView = (ImageView) convertView.getTag();
            }
            ImageLoader.getInstance().display(getItem(position), imageView, mColumnWidth, mColumnWidth);
            return convertView;
        }
    }


    protected void onPause(){
        SharedPreferences KeyListStore = getSharedPreferences("KeyListStore",0);
        SharedPreferences.Editor editor = KeyListStore.edit();
        editor.clear();
        mKeylisttoshow.remove(tmpkey);


        //editor.putString("0","KEY"); //2
        if (mKeylisttoshow !=null) {
            int mKeylistsize = mKeylisttoshow.size(); //2
            int todeletekeytoone = 0;
            for (int i= 0; i<mKeylistsize; i++){

                if (mKeylisttoshow.get(i).toString() == KEYspinner){
                    todeletekeytoone++;
                }
                if (todeletekeytoone>1){
                    mKeylisttoshow.remove(KEYspinner);
                    todeletekeytoone--;
                }
                //if (todeletekeytoone>1){
                //    mKeylisttoshow.remove(KEYspinner);
                //}
            }
            String mKeylistsizem = String.valueOf(mKeylistsize);
            editor.putString("0",mKeylistsizem);
            for (int i= 0; i<mKeylistsize; i++){
                int ikey = i+1;
                //String ikeystore=""+ikey;
                String ikeystore = String.valueOf(ikey);
                editor.putString(ikeystore,mKeylisttoshow.get(i).toString());
            }
        }
        editor.commit();
        mKeylisttoshow.clear();
        mKeylist.clear();
        super.onPause();
    }


    protected  void  onResume(){
        super.onResume();

        SharedPreferences KeyListStore = getSharedPreferences("KeyListStore",0);
        mKeylist.clear();

        int mKeylistsize =1;
        if(KeyListStore.getString("0", "") == null  ){
            mKeylistsize = 1;
        }else if(KeyListStore.getString("0", "") == "") {
            mKeylistsize = 1;

        }else{
            String mKeylistsizem = KeyListStore.getString("0", "") ;//3 0 size
            //mKeylistsize = Integer.parseInt(mKeylistsizem.trim());
            mKeylistsize = Integer.valueOf(mKeylistsizem).intValue();
        }

        String mkey = null;
        if(mKeylistsize != 1) {  //2
            for (int i = 0; i<mKeylistsize; i++){
                int keyv = i+1;
                String keyvv = String.valueOf(keyv);
                mkey = KeyListStore.getString(keyvv,null);
                mKeylist.add(mkey);
            }
        }
        int keynum = 0;

        int keydeletetoone = 0;
        for(int i=0; i<mKeylist.size(); i++){

            if(mKeylist.get(i).toString() == KEYspinner){
                keydeletetoone++;
                if (keydeletetoone>1){
                    mKeylist.remove(KEYspinner);
                    keydeletetoone--;
                }
                //if (keydeletetoone>1){
                //    mKeylist.remove(KEYspinner);
                //}
            }
        }
        if(mKeylist.size() == 0) {
            mKeylist.add(KEYspinner);
        }
    }





}
