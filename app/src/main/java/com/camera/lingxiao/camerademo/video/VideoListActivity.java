package com.camera.lingxiao.camerademo.video;

import android.Manifest;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import com.camera.lingxiao.camerademo.BaseActivity;
import com.camera.lingxiao.camerademo.R;
import com.chad.library.adapter.base.BaseQuickAdapter;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import butterknife.BindView;
import pub.devrel.easypermissions.EasyPermissions;

public class VideoListActivity extends BaseActivity {

    @BindView(R.id.recycerView)
    RecyclerView mRecycerView;
    @BindView(R.id.swipeLayout)
    SwipeRefreshLayout mRefreshLayout;
    private List<VideoModel> listImage = new ArrayList<>();
    private VideoAdapter mAdapter;

    private String[] projection = {MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATA};
    private String orderBy = MediaStore.Video.Media.DISPLAY_NAME;
    private Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
    private static final String TAG = VideoListActivity.class.getSimpleName();

    @Override
    protected int getContentLayoutId() {
        return R.layout.activity_video_list;
    }

    @Override
    protected void initWidget() {
        super.initWidget();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("本地视频列表");
        }
        LinearLayoutManager manager = new LinearLayoutManager(this);
        mRecycerView.setLayoutManager(manager);
        mAdapter = new VideoAdapter(R.layout.video_item,listImage);
        mRecycerView.setAdapter(mAdapter);
        mRefreshLayout.setRefreshing(true);

        String[] perms = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
        if (!EasyPermissions.hasPermissions(this, perms)) {
            EasyPermissions.requestPermissions(this, "需要权限",100, perms);
        }else {
            getContentProvider(uri,projection, orderBy);
        }

        mAdapter.setOnItemChildClickListener((BaseQuickAdapter adapter, View view, int position)-> {
            Intent intent = new Intent(getApplicationContext(),PlayActivity.class);
            intent.putExtra("path",listImage.get(position).getPath());
            startActivity(intent);
        });
        mRefreshLayout.setOnRefreshListener(() -> {
            listImage.clear();
            getContentProvider(uri,projection, orderBy);
        });
        mRefreshLayout.setColorSchemeColors(
                getResources().getColor(R.color.colorPrimary),
                getResources().getColor(R.color.colorAccent));
    }


    /**
     * 获取ContentProvider
     * @param projection
     * @param orderBy
     */
    public void getContentProvider(Uri uri, String[] projection, String orderBy) {
        // TODO Auto-generated method stub
        Cursor cursor = getContentResolver().query(uri, projection, null,
                null, orderBy);
        if (null == cursor) {
            return;
        }
        if (cursor.moveToFirst()){
            do {
                VideoModel model = new VideoModel();
                model.setId(cursor.getLong(0));
                model.setName(cursor.getString(1));
                model.setSize(cursor.getInt(2));
                model.setPath(cursor.getString(3));
                listImage.add(model);
            }while (cursor.moveToNext());
        }
        cursor.close();
        mAdapter.notifyDataSetChanged();
        mRefreshLayout.setRefreshing(false);
        Log.d(TAG, "getContentProvider: "+listImage.toString());
    }
}
