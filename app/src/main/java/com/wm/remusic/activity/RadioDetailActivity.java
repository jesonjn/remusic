package com.wm.remusic.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.binaryresource.BinaryResource;
import com.facebook.binaryresource.FileBinaryResource;
import com.facebook.cache.common.CacheKey;
import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.cache.DefaultCacheKeyFactory;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.core.ImagePipelineFactory;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.github.ksoichiro.android.observablescrollview.ObservableRecyclerView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.google.gson.JsonArray;
import com.nineoldandroids.view.ViewHelper;
import com.wm.remusic.MainApplication;
import com.wm.remusic.R;
import com.wm.remusic.downmusic.Down;
import com.wm.remusic.fragment.MoreFragment;
import com.wm.remusic.fragment.NetMoreFragment;
import com.wm.remusic.handler.HandlerUtil;
import com.wm.remusic.info.MusicInfo;
import com.wm.remusic.json.AlbumInfo;
import com.wm.remusic.json.MusicDetailInfo;
import com.wm.remusic.json.RadioInfo;
import com.wm.remusic.net.BMA;
import com.wm.remusic.net.HttpUtil;
import com.wm.remusic.net.NetworkUtils;
import com.wm.remusic.service.MusicPlayer;
import com.wm.remusic.uitl.CommonUtils;
import com.wm.remusic.uitl.IConstants;
import com.wm.remusic.uitl.ImageUtils;
import com.wm.remusic.widget.DividerItemDecoration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by wm on 2016/4/15.
 */

//歌单
public class RadioDetailActivity extends BaseActivity implements ObservableScrollViewCallbacks {

    private String albumId;
    private String albumPath, albumName, albumDes, artistName;
    private ArrayList<RadioInfo> mList = new ArrayList<>();
    private ArrayList<MusicInfo> adapterList = new ArrayList<>();

    private SimpleDraweeView albumArtSmall;
    private ImageView albumArt;
    private TextView albumTitle, tryAgain;

    private PlaylistDetailAdapter mAdapter;
    private Toolbar toolbar;
    private SparseArray<MusicDetailInfo> sparseArray = new SparseArray<MusicDetailInfo>();
    private FrameLayout loadFrameLayout;
    private int musicCount;
    private Handler mHandler;
    private int tryCount;
    private View loadView;
    private int mFlexibleSpaceImageHeight;
    private ActionBar actionBar;
    private int mActionBarSize;
    private int mStatusSize;
    private TextView playlistCountView;
    private String albumListenCount;
    private FrameLayout headerViewContent;
    private RelativeLayout headerDetail;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        if (getIntent().getExtras() != null) {
            albumId = getIntent().getStringExtra("albumid");
            albumPath = getIntent().getStringExtra("albumart");
            albumName = getIntent().getStringExtra("albumname");
            albumDes = getIntent().getStringExtra("albumdetail");
            artistName = getIntent().getStringExtra("artistname");
        }
        setContentView(R.layout.activity_playlist);
        loadFrameLayout = (FrameLayout) findViewById(R.id.state_container);

        headerViewContent = (FrameLayout) findViewById(R.id.headerview);
        headerDetail = (RelativeLayout) findViewById(R.id.headerdetail);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        mHandler = HandlerUtil.getInstance(this);

        mFlexibleSpaceImageHeight = getResources().getDimensionPixelSize(R.dimen.flexible_space_image_height);
        mActionBarSize = CommonUtils.getActionBarHeight(this);
        mStatusSize = CommonUtils.getStatusHeight(this);


        tryAgain = (TextView) findViewById(R.id.try_again);

        setUpEverything();

    }

    private void setUpEverything() {
        setupToolbar();
        setHeaderView();
        setAlbumart();
        setList();
        loadAllLists();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setHomeAsUpIndicator(R.drawable.actionbar_back);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("歌单");
        toolbar.setPadding(0, mStatusSize, 0, 0);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        toolbar.setSubtitle(albumDes);

    }

    private void setHeaderView() {
        albumArt = (ImageView) findViewById(R.id.album_art);
        albumTitle = (TextView) findViewById(R.id.album_title);
        albumArtSmall = (SimpleDraweeView) findViewById(R.id.playlist_art);

        LinearLayout downAll = (LinearLayout) headerViewContent.findViewById(R.id.playlist_down);
        downAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(RadioDetailActivity.this).setTitle("要下载音乐吗").
                        setPositiveButton(RadioDetailActivity.this.getString(R.string.sure), new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                int len = mList.size();
                                for (int i = 0; i < len; i++) {
                                    Down.downMusic(MainApplication.context, mList.get(i).getSong_id(), mList.get(i).getSong_name());
                                }
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(RadioDetailActivity.this, "已加入到下载", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                dialog.dismiss();
                            }
                        }).
                        setNegativeButton(RadioDetailActivity.this.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).show();
            }
        });
        headerDetail.setVisibility(View.GONE);

    }


    private void setList() {
        ObservableRecyclerView recyclerView = (ObservableRecyclerView) findViewById(R.id.recyclerview);
        recyclerView.setScrollViewCallbacks(RadioDetailActivity.this);
        recyclerView.setLayoutManager(new LinearLayoutManager(RadioDetailActivity.this));
        recyclerView.setHasFixedSize(false);
        mAdapter = new PlaylistDetailAdapter(RadioDetailActivity.this, adapterList);
        recyclerView.setAdapter(mAdapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(RadioDetailActivity.this, DividerItemDecoration.VERTICAL_LIST));
    }


    protected void updateViews(int scrollY, boolean animated) {
        // If it's ListView, onScrollChanged is called before ListView is laid out (onGlobalLayout).
        // This causes weird animation when onRestoreInstanceState occurred,
        // so we check if it's laid out already.
//        if (!mReady) {
//            return;
//        }

        // Translate header
        ViewHelper.setTranslationY(headerViewContent, getHeaderTranslationY(scrollY));

    }

    protected float getHeaderTranslationY(int scrollY) {
        final int headerHeight = headerViewContent.getHeight();
        Log.e("hei", "  " + headerHeight);
        int headerTranslationY = mActionBarSize + mStatusSize - headerHeight;
        if (mActionBarSize + mStatusSize <= -scrollY + headerHeight) {
            headerTranslationY = -scrollY;
        }
        Log.e("headerY", "  " + headerTranslationY);
        return headerTranslationY;
    }


    private void loadAllLists() {


        if (NetworkUtils.isConnectInternet(this)) {
            tryAgain.setVisibility(View.GONE);
            loadView = LayoutInflater.from(this).inflate(R.layout.loading, loadFrameLayout, false);
            loadFrameLayout.addView(loadView);
            new LoadNetPlaylistInfo().execute();

        } else {
            tryAgain.setVisibility(View.VISIBLE);
            tryAgain.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    loadAllLists();
                }
            });
        }

    }


    class LoadNetPlaylistInfo extends AsyncTask<Void, Void, Void> {
        AlbumInfo albumInfo;

        @Override
        protected Void doInBackground(final Void... unused) {
            try {
                JsonArray jsonArray = HttpUtil.getResposeJsonObject(BMA.Lebo.albumInfo(albumId, 10)).get("result").getAsJsonObject()
                        .get("latest_song").getAsJsonArray();

                musicCount = jsonArray.size();

                for (int i = 0; i < musicCount; i++) {
                    RadioInfo geDanGeInfo = MainApplication.gsonInstance().fromJson(jsonArray.get(i), RadioInfo.class);
                    mList.add(geDanGeInfo);
                    // PlaylistPlayInfoGet.get(new MusicDetailInfoGet(geDanGeInfo.getSong_id(), i, sparseArray));
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (albumInfo != null) {
                if (albumDes == null) {
                    albumDes = albumInfo.getInfo();
                    toolbar.setSubtitle(albumDes);
                }
            }

            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    for (int i = 0; i < mList.size(); i++) {
                        MusicInfo musicInfo = new MusicInfo();
                        musicInfo.songId = Integer.parseInt(mList.get(i).getSong_id());
                        musicInfo.musicName = mList.get(i).getSong_name();
                        musicInfo.islocal = false;
                        musicInfo.albumId = Integer.parseInt(mList.get(i).getSong_duration());
                        musicInfo.albumData = albumPath;
                        adapterList.add(musicInfo);
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    Log.e("mlist", mList.toString());
                    loadFrameLayout.removeAllViews();
                    mAdapter.updateDataSet(adapterList);
                    headerDetail.setVisibility(View.VISIBLE);
                }
            }.execute();

        }
    }


//    Runnable showPlaylistView = new Runnable() {
//        @Override
//        public void run() {
//            if (sparseArray.size() != musicCount && tryCount < 36) {
//                mHandler.postDelayed(showPlaylistView, 200);
//                tryCount++;
//            } else {
//                new AsyncTask<Void, Void, Void>() {
//                    @Override
//                    protected Void doInBackground(Void... params) {
//                        for (int i = 0; i < mList.size(); i++) {
//                            MusicInfo musicInfo = new MusicInfo();
//                            musicInfo.songId = Integer.parseInt(mList.get(i).getSong_id());
//                            musicInfo.musicName = mList.get(i).getSong_name();
//                            musicInfo.islocal = false;
//                            musicInfo.albumId = Integer.parseInt(mList.get(i).getSong_duration());
//                            musicInfo.albumData = albumPath;
//                            adapterList.add(musicInfo);
//                        }
//                        return null;
//                    }
//
//                    @Override
//                    protected void onPostExecute(Void aVoid) {
//                        Log.e("mlist", mList.toString());
//                        loadFrameLayout.removeAllViews();
//                        mAdapter.updateDataSet(adapterList);
//                    }
//                }.execute();
//            }
//        }
//    };


    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }


    private void setAlbumart() {
        albumTitle.setText(albumName);
        albumArtSmall.setImageURI(Uri.parse(albumPath));
        try {
            ImageRequest imageRequest = ImageRequest.fromUri(albumPath);
            CacheKey cacheKey = DefaultCacheKeyFactory.getInstance()
                    .getEncodedCacheKey(imageRequest);
            BinaryResource resource = ImagePipelineFactory.getInstance()
                    .getMainDiskStorageCache().getResource(cacheKey);
            File file = ((FileBinaryResource) resource).getFile();
            if (file != null) {
                new setBlurredAlbumArt().execute(ImageUtils.getArtworkQuick(file, 300, 300));
                return;
            }

            imageRequest = ImageRequestBuilder.newBuilderWithSource(Uri.parse(albumPath))
                    .setProgressiveRenderingEnabled(true).build();
            ImagePipeline imagePipeline = Fresco.getImagePipeline();
            DataSource<CloseableReference<CloseableImage>>
                    dataSource = imagePipeline.fetchDecodedImage(imageRequest, RadioDetailActivity.this);

            dataSource.subscribe(new BaseBitmapDataSubscriber() {

                                     @Override
                                     public void onNewResultImpl(@Nullable Bitmap bitmap) {
                                         // You can use the bitmap in only limited ways
                                         // No need to do any cleanup.
                                         if (bitmap != null) {
                                             new setBlurredAlbumArt().execute(bitmap);
                                         }
                                         ;
                                     }

                                     @Override
                                     public void onFailureImpl(DataSource dataSource) {
                                         // No cleanup required here.

                                     }
                                 },
                    CallerThreadExecutor.getInstance());


            //drawable = Drawable.createFromStream( new URL(albumPath).openStream(),"src");


        } catch (Exception e) {

        }

    }


    private class setBlurredAlbumArt extends AsyncTask<Bitmap, Void, Drawable> {

        @Override
        protected Drawable doInBackground(Bitmap... loadedImage) {
            Drawable drawable = null;

            try {
                drawable = ImageUtils.createBlurredImageFromBitmap(loadedImage[0], RadioDetailActivity.this, 20);
//                drawable = ImageUtils.createBlurredImageFromBitmap(ImageUtils.getBitmapFromDrawable(Drawable.createFromStream(new URL(albumPath).openStream(), "src")),
//                        NetPlaylistDetailActivity.this, 30);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return drawable;
        }

        @Override
        protected void onPostExecute(Drawable result) {
            if (result != null) {
                if (albumArt.getDrawable() != null) {
                    final TransitionDrawable td =
                            new TransitionDrawable(new Drawable[]{
                                    albumArt.getDrawable(),
                                    result
                            });
                    albumArt.setImageDrawable(td);
                    td.startTransition(200);

                } else {
                    albumArt.setImageDrawable(result);
                }
            }
        }
    }


    @Override
    public void onScrollChanged(int scrollY, boolean firstScroll, boolean dragging) {

        updateViews(scrollY, false);

        if (scrollY > 0 && scrollY < mFlexibleSpaceImageHeight - mActionBarSize - mStatusSize) {
            toolbar.setTitle(albumName);
            toolbar.setSubtitle(albumDes);
            actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.toolbar_background));
        }
        if (scrollY == 0) {
            toolbar.setTitle("歌单");
            actionBar.setBackgroundDrawable(null);
        }
        if (scrollY > mFlexibleSpaceImageHeight - mActionBarSize - mStatusSize) {

//            if(mBlurDrawable != null){
//                mBlurDrawable.setColorFilter(Color.parseColor("#79000000"), PorterDuff.Mode.SRC_OVER);
//                actionBar.setBackgroundDrawable(mBlurDrawable);
//            }
        }

        float a = (float) scrollY / (mFlexibleSpaceImageHeight - mActionBarSize - mStatusSize);
        headerDetail.setAlpha(1f - a);
        Log.e("alpha", " " + a);
    }

    @Override
    public void onDownMotionEvent() {

    }

    @Override
    public void onUpOrCancelMotionEvent(ScrollState scrollState) {

    }


    class PlaylistDetailAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        final static int FIRST_ITEM = 0;
        final static int ITEM = 1;
        private ArrayList<MusicInfo> arraylist;
        private Activity mContext;

        public PlaylistDetailAdapter(Activity context, ArrayList<MusicInfo> mList) {
            this.arraylist = mList;
            this.mContext = context;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            if (viewType == FIRST_ITEM) {
                return new CommonItemViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.header_common_item, viewGroup, false));
            } else {
                return new ItemViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.fragment_playlist_detail_item, viewGroup, false));
            }
        }

        //判断布局类型
        @Override
        public int getItemViewType(int position) {
            return position == FIRST_ITEM ? FIRST_ITEM : ITEM;

        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder itemHolder, final int i) {
            if (itemHolder instanceof ItemViewHolder) {
                final MusicInfo localItem = arraylist.get(i - 1);
                ((ItemViewHolder) itemHolder).trackNumber.setText(i + "");
                ((ItemViewHolder) itemHolder).title.setText(localItem.musicName);
                ((ItemViewHolder) itemHolder).artist.setText(artistName);
                ((ItemViewHolder) itemHolder).menu.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        Log.e("re", "get");
                        HttpUtil.getResposeJsonObject("http://tingapi.ting.baidu.com/v1/restserver/ting?from=android&version=5.8.1.0&channel=ppzs&operator=3&method=baidu.ting.artist.item&format=json&tinguid=1035&artistid=14");

                        if (localItem.islocal) {
                            MoreFragment morefragment = MoreFragment.newInstance(arraylist.get(i - 1),
                                    IConstants.MUSICOVERFLOW);
                            morefragment.show(((AppCompatActivity) mContext).getSupportFragmentManager(), "music");
                        } else {
                            NetMoreFragment morefragment = NetMoreFragment.newInstance(arraylist.get(i - 1),
                                    IConstants.MUSICOVERFLOW);
                            morefragment.show(((AppCompatActivity) mContext).getSupportFragmentManager(), "music");
                        }
//                        new AlertDialog.Builder(mContext).setTitle("要下载音乐吗").
//                                setPositiveButton(mContext.getString(R.string.sure), new DialogInterface.OnClickListener() {
//
//                                    @Override
//                                    public void onClick(DialogInterface dialog, int which) {
//
//                                        Down.downMusic(MainApplication.context, localItem.songId + "", localItem.musicName);
//                                        dialog.dismiss();
//                                    }
//                                }).
//                                setNegativeButton(mContext.getString(R.string.cancel), new DialogInterface.OnClickListener() {
//                                    @Override
//                                    public void onClick(DialogInterface dialog, int which) {
//                                        dialog.dismiss();
//                                    }
//                                }).show();
                    }
                });

            } else if (itemHolder instanceof CommonItemViewHolder) {

                ((CommonItemViewHolder) itemHolder).textView.setText("(共" + arraylist.size() + "首)");

                ((CommonItemViewHolder) itemHolder).select.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });

            }

        }

        @Override
        public int getItemCount() {
            return arraylist == null ? 0 : arraylist.size() + 1;
        }

        public void updateDataSet(ArrayList<MusicInfo> arraylist) {
            this.arraylist = arraylist;
            this.notifyDataSetChanged();
        }

        public class CommonItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            TextView textView;
            ImageView select;
            RelativeLayout layout;

            CommonItemViewHolder(View view) {
                super(view);
                this.textView = (TextView) view.findViewById(R.id.play_all_number);
                this.select = (ImageView) view.findViewById(R.id.select);
                this.layout = (RelativeLayout) view.findViewById(R.id.play_all_layout);
                layout.setOnClickListener(this);
            }

            public void onClick(View v) {
                //// TODO: 2016/1/20
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        HashMap<Long, MusicInfo> infos = new HashMap<Long, MusicInfo>();
                        int len = arraylist.size();
                        long[] list = new long[len];
                        for (int i = 0; i < len; i++) {
                            MusicInfo info = arraylist.get(i);
                            list[i] = info.songId;
                            infos.put(list[i], info);
                        }
                        MusicPlayer.playAll(infos, list, 0, false);
                    }
                }).start();

            }

        }

        public class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            protected TextView title, artist, trackNumber;
            protected ImageView menu;

            public ItemViewHolder(View view) {
                super(view);
                this.title = (TextView) view.findViewById(R.id.song_title);
                this.artist = (TextView) view.findViewById(R.id.song_artist);
                this.trackNumber = (TextView) view.findViewById(R.id.trackNumber);
                this.menu = (ImageView) view.findViewById(R.id.popup_menu);
                view.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        HashMap<Long, MusicInfo> infos = new HashMap<Long, MusicInfo>();
                        int len = arraylist.size();
                        long[] list = new long[len];
                        for (int i = 0; i < len; i++) {
                            MusicInfo info = arraylist.get(i);
                            list[i] = info.songId;
                            infos.put(list[i], info);
                        }

//                            long[] list = new long[arraylist.size()];
//                            HashMap<Long,MusicInfo> infos = new HashMap<Long,MusicInfo>();
//                            for (int i = 0; i < arraylist.size(); i++) {
//                                list[i] = Long.parseLong(arraylist.get(i).getSong_id());
//                                MusicInfo musicInfo = new MusicInfo();
//                                musicInfo.songId = Integer.parseInt(arraylist.get(i).getSong_id());
//                                musicInfo.musicName = arraylist.get(i).getTitle();
//                                musicInfo.artist = sparseArray.get(i).getArtist_name();
//                                musicInfo.islocal = false;
//                                musicInfo.albumName = sparseArray.get(i).getAlbum_title();
//                                musicInfo.albumId = Integer.parseInt(arraylist.get(i).getAlbum_id());
//                                musicInfo.artistId = Integer.parseInt(sparseArray.get(i).getArtist_id());
//                                musicInfo.lrc = sparseArray.get(i).getLrclink();
//                                musicInfo.albumData = sparseArray.get(i).getPic_radio();
//                                infos.put(list[i] , musicInfo);
//                            }
                        if(getAdapterPosition() > 0)
                        MusicPlayer.playAll(infos, list, getAdapterPosition() - 1, false);
                    }
                }).start();

            }

        }
    }
}
