package com.ghkjgod.lightnovel.framelayout;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.ghkjgod.lightnovel.adapter.NovelItemAdapter;
import com.ghkjgod.lightnovel.global.GlobalConfig;
import com.ghkjgod.lightnovel.global.api.NovelItemInfo;
import com.ghkjgod.lightnovel.global.api.NovelItemList;
import com.ghkjgod.lightnovel.global.api.NovelListWithInfoParser;
import com.ghkjgod.lightnovel.global.api.Wenku8API;
import com.ghkjgod.lightnovel.lightnovel.MainActivity;
import com.ghkjgod.lightnovel.lightnovel.NovelInfoActivity;
import com.ghkjgod.lightnovel.lightnovel.R;
import com.ghkjgod.lightnovel.listener.MyItemClickListener;
import com.ghkjgod.lightnovel.listener.MyItemLongClickListener;
import com.ghkjgod.lightnovel.util.LightNetwork;
import com.ghkjgod.lightnovel.util.SingletonThreadPool;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class LatestFragment extends Fragment implements MyItemClickListener, MyItemLongClickListener,SwipeRefreshLayout.OnRefreshListener {

    static private final String TAG = "LatestFragment";

    // components
    private MainActivity mainActivity = null;
    private StaggeredGridLayoutManager mLayoutManager;
    private RecyclerView mRecyclerView;
    private TextView mTextView;
    SwipeRefreshLayout mSwipeRefreshWidget;

    // Novel Item info
    private NovelItemList novelItemList;
    private List<NovelItemInfo> listNovelItemInfo;
    private NovelItemAdapter mAdapter;
    private int currentPage=1, totalPage=1;

    // switcher
    private boolean isLoading = false;

    private int pastVisiblesItems, visibleItemCount, totalItemCount;

    public static final int PARSER_ERR = -1,NET_ERR = -2,NET_OK = 0;

    private Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(!LatestFragment.this.isAdded())
                return;
            switch (msg.what) {
                case PARSER_ERR:
                   // Toast.makeText(mainActivity, getResources().getString(R.string.system_parse_failed), Toast.LENGTH_SHORT).show();
                    mTextView.setText(getResources().getString(R.string.system_parse_failed));
                    showRetryButton();
                    isLoading = false;
                    break;
                case NET_ERR:
                   // Toast.makeText(mainActivity, getResources().getString(R.string.system_network_error), Toast.LENGTH_SHORT).show();
                    mTextView.setText(getResources().getString(R.string.system_network_error));
                    showRetryButton();
                    isLoading = false;
                    break;
                case NET_OK:
                    if (mAdapter == null) {
                        mAdapter = new NovelItemAdapter(listNovelItemInfo);
                        mAdapter.setOnItemClickListener(LatestFragment.this);
                        mAdapter.setOnItemLongClickListener(LatestFragment.this);
                        mRecyclerView.setAdapter(mAdapter);
                    }
                    if (mainActivity.findViewById(R.id.list_loading) != null)
                        mainActivity.findViewById(R.id.list_loading).setVisibility(View.GONE);
                    mAdapter.notifyDataSetChanged();
                    currentPage++; // add when loaded
                    isLoading = false;
                    mSwipeRefreshWidget.setRefreshing(false);
                    break;
            }
            super.handleMessage(msg);
        }
    };
    public LatestFragment() {

    }

    public static LatestFragment newInstance() {

        return new LatestFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        while (mainActivity == null)
            mainActivity = (MainActivity) getActivity();

    }

    public NovelItemAdapter getNovelItemAdapter() {

        return mAdapter;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_latest, container, false);
        mSwipeRefreshWidget = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_refresh_widget);
        mSwipeRefreshWidget.setRefreshing(true);
        mSwipeRefreshWidget.setOnRefreshListener(this);

        // set click event
        rootView.findViewById(R.id.btn_loading).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isLoading) {
                    isLoading = false; // set this false as a terminator signal

                } else {
                    // need to reload novel list all
                    currentPage = 1;
                    totalPage = 1;
                    isLoading = false;
                    loadNovelList(currentPage);
                }

            }
        });
        loadNovelList(currentPage);
        // get views
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.novel_item_list);
        mTextView = (TextView) rootView.findViewById(R.id.list_loading_status);

        mRecyclerView.setHasFixedSize(true);
        // use a linear layout manager
        // 2 column
        int column = 2;
        mLayoutManager = new StaggeredGridLayoutManager(column,StaggeredGridLayoutManager.VERTICAL);

        mRecyclerView.setLayoutManager(mLayoutManager);
        // Listener
        mRecyclerView.addOnScrollListener(new MyOnScrollListener());

        return rootView;
    }

    private void loadNovelList(int page) {
        // In fact, I don't need to know what it really is.
        // I just need to get the NOVELSORTBY
        isLoading = true; // set loading states
        hideRetryButton();
        // use ThreadPool replace AsyTask
        ExecutorService threadPool = SingletonThreadPool.getInstance();
        threadPool.execute(new AsyNetLoad(Wenku8API.getNovelListWithInfo(Wenku8API.NOVELSORTBY.lastUpdate, page, GlobalConfig.getCurrentLang())));


    }
    @Override
    public void onRefresh() {
        currentPage = 1;
        totalPage = 1;
        isLoading = false;
        loadNovelList(currentPage);
    }

    @Override
    public void onItemClick(View view, final int position) {

        if (position < 0 || position >= listNovelItemInfo.size()) {
            // ArrayIndexOutOfBoundsException
            Toast.makeText(getActivity(), "ArrayIndexOutOfBoundsException: " + position + " in size " + listNovelItemInfo.size(), Toast.LENGTH_SHORT).show();
            return;
        }

        // go to detail activity
        Intent intent = new Intent(mainActivity, NovelInfoActivity.class);
        intent.putExtra("aid", listNovelItemInfo.get(position).getAid());
        intent.putExtra("from", "latest");
        intent.putExtra("title", listNovelItemInfo.get(position).getTitle());

        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(mainActivity,
                Pair.create(view.findViewById(R.id.novel_cover), "novel_cover"),
                Pair.create(view.findViewById(R.id.novel_title), "novel_title"));
        ActivityCompat.startActivity(mainActivity, intent, options.toBundle());


    }

    @Override
    public void onItemLongClick(View view, int postion) {
        // empty
        onItemClick(view, postion);
    }


    @Override
    public void onDetach() {
        isLoading = false;
        super.onDetach();
    }



    private class MyOnScrollListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView,
                                         int newState) {

            visibleItemCount = mLayoutManager.getChildCount();
            totalItemCount = mLayoutManager.getItemCount();
            int[] firstVisibleItemPositions = new int[2];
            pastVisiblesItems = mLayoutManager.findFirstVisibleItemPositions(firstVisibleItemPositions)[0];

            if (!isLoading) {
                // 滚动到一半的时候加载，即：剩余5个元素的时候就加载
                if (visibleItemCount + pastVisiblesItems + 5 >= totalItemCount) {
                    isLoading = true;
                    // load more toast
                    Snackbar.make(mRecyclerView, getResources().getString(R.string.list_loading)
                                    + "(" + Integer.toString(currentPage) + "/" + totalPage + ")",
                            Snackbar.LENGTH_SHORT).show();

                    // load more thread
                    if (currentPage <= totalPage) {
                        loadNovelList(currentPage);
                    } else {
                        Snackbar.make(mRecyclerView, "Every page is loaded!",
                                Snackbar.LENGTH_SHORT).show();
                    }
                }
            }


        }
            @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);


        }
    }

    private  class AsyNetLoad implements Runnable{
        private ContentValues params;
        public AsyNetLoad(ContentValues params){
           this.params = params;
        }
        @Override
        public void run() {
            try {
                byte[] tempXml = LightNetwork.okHttpPostConnection(Wenku8API.getBaseURL(), params);
                if (tempXml == null) {
                    Message message = new Message();
                    message.what = PARSER_ERR;
                    myHandler.sendMessage(message);
                    return;
                }
                String xml = new String(tempXml, "UTF-8");
                totalPage = NovelListWithInfoParser.getNovelListWithInfoPageNum(xml);
                List<NovelListWithInfoParser.NovelListWithInfo> l = NovelListWithInfoParser.getNovelListWithInfo(xml);
                if (l == null) {
                    Message message = new Message();
                    message.what = NET_ERR;
                    myHandler.sendMessage(message);
                    return; // network error
                }
                if (listNovelItemInfo == null)
                    listNovelItemInfo = new ArrayList<>();
                if(currentPage==1){

                   listNovelItemInfo.clear();

                }
                for (int i = 0; i < l.size(); i++) {
                    NovelListWithInfoParser.NovelListWithInfo nlwi = l.get(i);
                    NovelItemInfo ni = new NovelItemInfo();
                    ni.setAid(nlwi.aid);
                    ni.setTitle(nlwi.name);
                    ni.setAuthor(nlwi.hit + ""); // hit
                    ni.setUpdate(nlwi.push + ""); // push
                    ni.setIntro_short(nlwi.fav + ""); // fav

                    listNovelItemInfo.add(ni);
                }

                if (!isAdded())
                    return; // detached
                Message message = new Message();
                message.what = NET_OK;
                myHandler.sendMessage(message);


            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        GlobalConfig.LeaveLatest();
    }
    @Override
    public void onResume() {
        super.onResume();
        GlobalConfig.EnterLatest();
    }
    /**
     * After button pressed, should hide the "retry" button
     */
    private void hideRetryButton() {
        if (mainActivity.findViewById(R.id.btn_loading) == null)
            return;

        mainActivity.findViewById(R.id.btn_loading).setVisibility(TextView.GONE);
    }
    private void showRetryButton() {
        if (mainActivity.findViewById(R.id.btn_loading) == null || !isAdded())
            return;
        ((TextView) mainActivity.findViewById(R.id.btn_loading)).setText(getResources().getString(R.string.task_retry));
        mainActivity.findViewById(R.id.btn_loading).setVisibility(TextView.VISIBLE);
    }

}
