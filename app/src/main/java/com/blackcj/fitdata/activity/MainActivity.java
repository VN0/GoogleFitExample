package com.blackcj.fitdata.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import com.blackcj.fitdata.Utilities;
import com.blackcj.fitdata.adapter.TabPagerAdapter;
import com.blackcj.fitdata.database.CacheManager;
import com.blackcj.fitdata.database.CupboardSQLiteOpenHelper;
import com.blackcj.fitdata.R;
import com.blackcj.fitdata.database.DataManager;
import com.blackcj.fitdata.fragment.AddEntryFragment;
import com.blackcj.fitdata.fragment.PageFragment;
import com.blackcj.fitdata.model.Workout;
import com.blackcj.fitdata.model.WorkoutReport;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.result.DataReadResult;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * This sample demonstrates how to use the History API of the Google Fit platform to insert data,
 * query against existing data, and remove data. It also demonstrates how to authenticate
 * a user with Google Play Services and how to properly represent data in a {@link DataSet}.
 */
public class MainActivity extends BaseActivity implements SearchView.OnQueryTextListener,
        FragmentManager.OnBackStackChangedListener, AppBarLayout.OnOffsetChangedListener,
        IMainActivityCallback, DataManager.IDataManager, FloatingActionsMenu.OnFloatingActionsMenuUpdateListener {

    public static final String TAG = "MainActivity";
    public static boolean active = false;

    private WorkoutReport report = new WorkoutReport();
    private CacheManager mCacheManager;
    private DataManager mDataManager;
    private TabPagerAdapter mAdapter;

    @InjectView(R.id.coordinatorLayout) CoordinatorLayout coordinatorLayout;
    @InjectView(R.id.appBarLayout) AppBarLayout appBarLayout;
    @InjectView(R.id.viewPager) ViewPager mViewPager;
    @InjectView(R.id.main_overlay) View overlay;
    @InjectView(R.id.floatingActionMenu) FloatingActionsMenu floatingActionMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActionBarIcon(R.drawable.barchart_icon);
        ButterKnife.inject(this);

        CupboardSQLiteOpenHelper dbHelper = new CupboardSQLiteOpenHelper(this);
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        mCacheManager = new CacheManager(db);
        mDataManager = new DataManager(db, this);
        mAdapter = new TabPagerAdapter(this.getSupportFragmentManager());

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        tabLayout.setTabsFromPagerAdapter(mAdapter);
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mViewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        mViewPager = (ViewPager) findViewById(R.id.viewPager);
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        mViewPager.setAdapter(mAdapter);

        overlay.setVisibility(View.GONE);

        overlay.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                floatingActionMenu.collapse();
                return true;
            }
        });

        floatingActionMenu.setOnFloatingActionsMenuUpdateListener(this);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DataManager.REQUEST_OAUTH) {
            mDataManager.authInProgress = false;
            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                mDataManager.connect();
            }
        }
    }

    int index = 0;

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
        index = i;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                PageFragment pageFragment = mAdapter.getFragment(mViewPager.getCurrentItem());
                if (pageFragment != null) {
                    if (index == 0) {
                        pageFragment.setSwipeToRefreshEnabled(true);
                    } else {
                        pageFragment.setSwipeToRefreshEnabled(false);
                    }
                }
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onResume() {
        super.onResume();
        appBarLayout.addOnOffsetChangedListener(this);
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        Log.d("MainActivity", "OnResume");
    }


    @Override
    protected void onPause() {
        appBarLayout.removeOnOffsetChangedListener(this);
        getSupportFragmentManager().removeOnBackStackChangedListener(this);
        Log.d("MainActivity", "OnPause");
        super.onPause();
    }

    public void onBackStackChanged()
    {
        FragmentManager manager = getSupportFragmentManager();
        if (manager != null && manager.getBackStackEntryCount() == 0 && overlay.getVisibility() == View.VISIBLE) {
            floatingActionMenu.collapse();
            /*
            getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
            ValueAnimator fadeAnim = ObjectAnimator.ofFloat(overlay, "alpha", 1f, 0f);
            fadeAnim.setDuration(250);
            fadeAnim.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    overlay.setVisibility(View.GONE);
                }
            });
            fadeAnim.start();
            */
        }
    }

    Menu mOptionsMenu;
    MenuItem mSearchItem;
    SearchView mSearchView;

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        mOptionsMenu = menu;
        if (mOptionsMenu != null) {
            mSearchItem = menu.findItem(R.id.search_participants);
            mSearchView = (SearchView) MenuItemCompat.getActionView(mSearchItem);
            mSearchView.setOnQueryTextListener(this);
            mSearchView.setQueryHint("Search");
            MenuItemCompat.setOnActionExpandListener(mSearchItem, new MenuItemCompat.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    return true;
                }
            });
        }
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
        active = true;
        mDataManager.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        active = false;
        mDataManager.disconnect();
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_main;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        //mAdapter.filter(newText);
        Log.d("MainActivity", "Query test: " + newText);
        return false;
    }

    @Override
    public void launch(View transitionView, Workout workout) {
        DetailActivity.launch(MainActivity.this, transitionView, workout);
    }

    @Override
    public List<Workout> getData(Utilities.TimeFrame timeFrame) {
        return mCacheManager.getReport(timeFrame);
    }

    private void writeActivityDataToWorkout(DataReadResult dataReadResult) {
        for (Bucket bucket : dataReadResult.getBuckets()) {
            for (DataSet dataSet : bucket.getDataSets()) {
                parseDataSet(dataSet);
            }
        }
    }

    /**
     * Count step data for a bucket of step count deltas.
     *
     * @param dataReadResult Read result from the step count estimate Google Fit call.
     * @return Step count for data read.
     */
    private int countStepData(DataReadResult dataReadResult) {
        int stepCount = 0;
        for (Bucket bucket : dataReadResult.getBuckets()) {
            for (DataSet dataSet : bucket.getDataSets()) {
                stepCount += parseDataSet(dataSet);
            }
        }
        return stepCount;
    }

    @OnClick({R.id.step_button, R.id.bike_button, R.id.run_button, R.id.other_button})
    void showAddEntryFragment() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_anim, R.anim.exit_anim, R.anim.enter_anim, R.anim.exit_anim);
        transaction.add(R.id.top_container, new AddEntryFragment(), AddEntryFragment.TAG);
        transaction.addToBackStack("add_entry");
        transaction.commit();
    }

    /**
     * Walk through all fields in a step_count dataset and return the sum of steps. Used to
     * calculate step counts.
     *
     * @param dataSet
     */
    private int parseDataSet(DataSet dataSet) {
        int dataSteps = 0;
        for (DataPoint dp : dataSet.getDataPoints()) {
            // Accumulate step count for estimate

            if(dp.getDataType().getName().equals("com.google.step_count.delta")) {
                for (Field field : dp.getDataType().getFields()) {
                    if (dp.getValue(field).asInt() > 0) {
                        dataSteps += dp.getValue(field).asInt();
                    }
                }
            }else {
                Workout workout = new Workout();
                workout.start = 0;
                workout.stepCount = 0;
                for (Field field : dp.getDataType().getFields()) {

                    String fieldName = field.getName();
                    if(fieldName.equals("activity")) {
                        workout.type = dp.getValue(field).asInt();
                    }else if(fieldName.equals("duration")) {
                        workout.duration = dp.getValue(field).asInt();
                    }
                }
                report.addWorkoutData(workout);
            }
        }
        return dataSteps;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh_data) {
            mDataManager.refreshData();
            return true;
        } else if (id == R.id.action_delete_steps) {
            mDataManager.deleteData();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void insertData(Workout workout) {
        mDataManager.insertData(workout);
    }

    @Override
    public void onMenuExpanded() {
        overlay.clearAnimation();
        float viewAlpha = overlay.getAlpha();
        overlay.setVisibility(View.VISIBLE);

        ValueAnimator fadeAnim = ObjectAnimator.ofFloat(overlay, "alpha", viewAlpha, 1f);
        fadeAnim.setDuration(200L);

        fadeAnim.start();
    }

    @Override
    public void onMenuCollapsed() {
        overlay.clearAnimation();
        float viewAlpha = overlay.getAlpha();
        ValueAnimator fadeAnim = ObjectAnimator.ofFloat(overlay, "alpha", viewAlpha, 0f);
        fadeAnim.setDuration(200L);
        fadeAnim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                overlay.setVisibility(View.GONE);
            }
        });

        fadeAnim.start();
    }
}
