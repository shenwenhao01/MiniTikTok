package com.example.project_mini_tiktok;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.project_mini_tiktok.model.VideoMessage;
import com.example.project_mini_tiktok.model.VideoMessageListResponse;
import com.example.project_mini_tiktok.recycler.LinearItemDecoration;
import com.example.project_mini_tiktok.recycler.MyAdapter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;
import static com.example.project_mini_tiktok.Constants.token;

public class ZhuyeActivity_Taren extends AppCompatActivity implements FeedAdapter.IOnItemClickListener {
    private RecyclerView recyclerView;
    private MyAdapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private GridLayoutManager gridLayoutManager;

    private static final String TAG = "PlaceholderFragement";
    private FeedAdapter adapter = new FeedAdapter();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gerenzhuye_taren);

        final Intent it = getIntent();
        String userName = it.getStringExtra("userName");
        String student_id = it.getStringExtra("student_id");

        //????????????
        recyclerView = findViewById(R.id.taren_video_recycler);
        //?????????????????????????????????
        recyclerView.setHasFixedSize(true);
        //???????????????????????????
        layoutManager = new LinearLayoutManager(ZhuyeActivity_Taren.this);
        //???????????????????????????
        gridLayoutManager = new GridLayoutManager(ZhuyeActivity_Taren.this, 2);
        //?????????????????????
        recyclerView.setLayoutManager(layoutManager);
        //??????Adapter
//        mAdapter = new MyAdapter(TestDataSet.getData());
        adapter = new FeedAdapter();
        adapter.setOnItemClickListener((FeedAdapter.IOnItemClickListener)ZhuyeActivity_Taren.this);
        //??????Adapter
//        recyclerView.setAdapter(mAdapter);
        recyclerView.setAdapter(adapter);
        //?????????
        LinearItemDecoration itemDecoration = new LinearItemDecoration(Color.BLUE);
        //recyclerView.addItemDecoration(itemDecoration);
        recyclerView.addItemDecoration(new DividerItemDecoration(ZhuyeActivity_Taren.this, LinearLayoutManager.VERTICAL));
        //??????
        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setAddDuration(3000);
        recyclerView.setItemAnimator(animator);

        TextView taren_username = findViewById(R.id.taren_username);
        taren_username.setText(userName);

        ImageView imageView = findViewById(R.id.taren_portrait);
        RequestOptions cropOptions = new RequestOptions();
        cropOptions.centerCrop().circleCrop();
        Glide.with(this)
                .load(R.mipmap.tang)
                .apply(cropOptions)
                .transition(withCrossFade())
                .into(imageView);


        getData(student_id, null);

    }


    //TODO 2
    // ???HttpUrlConnection????????????????????????????????????Gson?????????????????????UI?????????adapter.setData()?????????
    // ?????????????????????UI???????????????????????????????????????
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public List<VideoMessage> baseDetReposFromRemote(String studentId, String userName){
        String Query = "";
        if(studentId != null && !studentId.isEmpty()){
            Query = String.format("?student_id=%s", studentId);
        }
//        if(userName != null && !userName.isEmpty()){
//            Query = String.format("?user_name=%s", userName);
//        }
        String urlStr = String.format("https://api-android-camp.bytedance.com/zju/invoke/video%s", Query);
        VideoMessageListResponse result = null;
        try{
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setConnectTimeout(6000);

            conn.setRequestMethod("GET");

            conn.setRequestProperty("token", token);

            if(conn.getResponseCode() == 200){
                InputStream in = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                result = new Gson().fromJson(reader, new TypeToken<VideoMessageListResponse>(){}.getType());
                reader.close();
                in.close();
            }else{
                // ????????????
                Log.d(TAG, "HttpURLConnection Failed with code: " + conn.getResponseCode());
                Toast.makeText(ZhuyeActivity_Taren.this, "??????????????????????????????" + conn.getResponseCode(), Toast.LENGTH_SHORT);
            }
            conn.disconnect(); // very important! Do not forget it!
        }catch(Exception e){
            e.printStackTrace();
            Toast.makeText(ZhuyeActivity_Taren.this, "????????????" + e.toString(), Toast.LENGTH_SHORT).show();
        }

        return result.feeds;
    }

    private void getData(String studentId, String userName){
        new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void run() {
                List<VideoMessage> MessageList = baseDetReposFromRemote(studentId, userName);
                if(MessageList != null && !MessageList.isEmpty()){
                    new Handler(getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            adapter.setData(MessageList);
                        }
                    });
                }
            }
        }).start();
    }

    @Override
    public void onItemCLick(int position, VideoMessage data){
        Toast.makeText(ZhuyeActivity_Taren.this, "Item Clicked", Toast.LENGTH_SHORT).show();
        Intent it = new Intent();
        it.setClass(ZhuyeActivity_Taren.this, VideoDetailActivity.class);
        it.putExtra("VideoURL", data.getVideoUrl());
        it.putExtra("position", position);
        it.putExtra("userName", data.getUserName());
        ZhuyeActivity_Taren.this.startActivity(it);
    }

    @Override
    public void onItemLongCLick(int position, VideoMessage data) {
        // Toast.makeText(MainActivity.this, "????????????" + position + "???", Toast.LENGTH_SHORT).show();
//        mAdapter.removeData(position);
    }

}