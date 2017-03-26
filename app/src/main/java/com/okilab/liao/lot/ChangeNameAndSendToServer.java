package com.okilab.liao.lot;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Dongyang Liao on 2017/2/3.
 */

public class ChangeNameAndSendToServer {

    private List<String> mResultspath, mPreparetoSendpath;
    private int ListNumber;
    private Context context;
    private String key;
    //public static MainActivity mactivity;
    private static FileInputStream fi = null;
    private static FileOutputStream fo = null;
    private static FileChannel in = null;
    private static FileChannel out = null;
    private String uploadUrl = "http://192.168.2.1/PictureUpLoad.php";
    private String typesam; // con or sig


    ChangeNameAndSendToServer(List<String> mResults,int ListNumber,String key,String typesam) throws IOException, InterruptedException {
        this.mResultspath = mResults;
        this.ListNumber = ListNumber;
        this.key = key;
        this.typesam = typesam;

        mPreparetoSendpath = new ArrayList<>();
        if (ListNumber != 0) {
            ChangeTheName();
            Upload();
            //enddelete();
            //Thread.sleep(15000);
            //enddelete(); //in the botton of delete

        } else {
            Toast.makeText(MainActivity.mactivity, "No file or image to send", Toast.LENGTH_SHORT).show();
        }
    }

    private void ChangeTheName() throws IOException {

        for (int i=0; i<ListNumber; i++){
            String storefolder = "/storage/sdcard0/tmpfiletobesent/";
            File storefoldercheck = new File(storefolder);
            if (!storefoldercheck.exists()){
                storefoldercheck.mkdir();
            }

            File filein=new File(mResultspath.get(i).toString());
            String filename = filein.getName();
            String storepath = storefolder+filename;
            File fileout = new File(storepath);
            fi = new FileInputStream(filein);
            fo = new FileOutputStream(fileout);
            in = fi.getChannel();
            out = fo.getChannel();
            in.transferTo(0, in.size(), out);
            fi.close();
            in.close();
            fo.close();
            out.close();

            File fileneedchangenamepath = new File(storepath);
            UUID uuid = UUID.randomUUID();
            String Uid =  uuid.toString();
            String fileneedname=fileneedchangenamepath.getName();
            String prefix=fileneedname.substring(filename.lastIndexOf("."));
            //String newfilename = key +"_"+Uid+prefix;
            String newfilename = key +"_"+typesam +"_"+ Uid + prefix;
            String newfilepath = storefolder+newfilename;
            fileneedchangenamepath.renameTo(new File(newfilepath));
            mPreparetoSendpath.add(newfilepath);
        }
        //for (int i = 0; i < mPreparetoSendpath.size(); i++){
        //    Toast.makeText(MainActivity.mactivity,mPreparetoSendpath.get(i).toString(),Toast.LENGTH_SHORT).show();
        //}
    }

    private void Upload() {
        for(int i=0;i<ListNumber; i++){

         final String uploadfilepath = mPreparetoSendpath.get(i).toString();
            Toast.makeText(MainActivity.mactivity,uploadfilepath,Toast.LENGTH_SHORT).show();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    uploadFile(uploadUrl,uploadfilepath);
                }
            }).start();
        }
    }

    private void uploadFile(String uploadUrl,String uploadfilepath) {
        //mUpdateHandler.sendEmptyMessage(EVENT_POST_START);
        String end = "\r\n";
        String twoHyphens = "--";
        String boundary = "******";
        try {
            URL url = new URL(uploadUrl);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url
                    .openConnection();
            // 允许输入输出流
            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setUseCaches(false);
            // POST
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setChunkedStreamingMode(20*1024);
            httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
            httpURLConnection.setRequestProperty("Charset", "UTF-8");
            httpURLConnection.setRequestProperty("Content-Type",
                    "multipart/form-data;boundary=" + boundary);

            DataOutputStream dos = new DataOutputStream(
                    httpURLConnection.getOutputStream());
            dos.writeBytes(twoHyphens + boundary + end);
            dos.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\"; filename=\""
                    + uploadfilepath.substring(uploadfilepath.lastIndexOf("/") + 1)
                    + "\""
                    + end);
            dos.writeBytes(end);

            FileInputStream fis = new FileInputStream(uploadfilepath);
            byte[] buffer = new byte[1024]; // 1k
            int count = 0;
            // read file
            while ((count = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, count);
            }
            fis.close();

            dos.writeBytes(end);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + end);
            dos.flush();

            InputStream is = httpURLConnection.getInputStream();
            InputStreamReader isr = new InputStreamReader(is, "utf-8");
            BufferedReader br = new BufferedReader(isr);
            String result = br.readLine();
            Log.d("OK", result);

            //mUpdateHandler.sendEmptyMessage(EVENT_POST_SUCCESS);

            dos.close();
            is.close();

            enddelete(uploadfilepath);//add a delete

        } catch (Exception e) {
            //mUpdateHandler.sendEmptyMessage(EVENT_POST_FAILED);
            e.printStackTrace();
        }
    }

    private void enddelete(String uploadfilepath){
        //for(int i=0;i<ListNumber; i++){
        //    final String deletefilepath = mPreparetoSendpath.get(i).toString();
        //    File file = new File(deletefilepath);
              File todeletefile = new File(uploadfilepath);
              todeletefile.delete();
        //}
    }

}
