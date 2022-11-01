package com.mapswithme.maps;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.mapswithme.util.Counters;

public class BoardingScreenActivity extends AppCompatActivity {

    private ViewPager viewPager;
    private MyViewPagerAdapter myViewPagerAdapter;
    private LinearLayout dotsLayout;
    private TextView[]dots;
    private int[] layouts;
    private Button btnSkip, btnNext;

    private static final String EXTRA_ACTIVITY_TO_START = "extra_activity_to_start";
    public static final String EXTRA_INITIAL_INTENT = "extra_initial_intent";
    private static final int REQUEST_PERMISSIONS = 1;
    private static final int REQ_CODE_API_RESULT = 10;
 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_boarding_screen);

        initiateVariables();
        addBottomDots(0);
        changeStatusBarColor();

        myViewPagerAdapter = new MyViewPagerAdapter();
        viewPager.setAdapter(myViewPagerAdapter);
        viewPager.addOnPageChangeListener(viewPagerPageChangeListener);

        btnSkip.setOnClickListener(v -> launchHomeScreen());

        btnNext.setOnClickListener(v -> {
            // if last page home screen will be launched
            int current = getItem(+1);
            if (current < layouts.length) {
                // move to next screen
                viewPager.setCurrentItem(current);
            } else {
                launchHomeScreen();
            }
        });
    }

    ViewPager.OnPageChangeListener viewPagerPageChangeListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageSelected(int position) {
            addBottomDots(position);

            // adjusting buttons depending on slide
            if (position == layouts.length - 1) {
                btnNext.setText(getString(R.string.start));
                btnSkip.setVisibility(View.GONE);
            } else {
                btnNext.setText(getString(R.string.next));
                btnSkip.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
        }

        @Override
        public void onPageScrollStateChanged(int arg0) {
        }
    };

    private void initiateVariables(){
        viewPager = findViewById(R.id.view_pager);
        dotsLayout = findViewById(R.id.bs_layoutDots);
        btnSkip = findViewById(R.id.bs_btn_skip);
        btnNext = findViewById(R.id.bs_btn_next);


        // layouts of all welcome sliders
        layouts = new int[]{
                R.layout.bs_screen1,
                R.layout.bs_screen2,
                R.layout.bs_screen3};
    }

    private int getItem(int i){
        return viewPager.getCurrentItem() + i;
    }

    private void launchHomeScreen(){
        //startActivity(new Intent(BoardingScreenActivity.this, MainActivity.class));
        Counters.setFirstStartDialogSeen(this);
        processNavigation();
        finish();
    }

    private void processNavigation()
    {
        Intent input = getIntent();
        Intent result = new Intent(this, DownloadResourcesLegacyActivity.class);
        if (input != null)
        {
            if (input.hasExtra(EXTRA_ACTIVITY_TO_START))
            {
                result = new Intent(this,
                        (Class<? extends Activity>) input.getSerializableExtra(EXTRA_ACTIVITY_TO_START));
            }

            Intent initialIntent = input.hasExtra(EXTRA_INITIAL_INTENT) ?
                    input.getParcelableExtra(EXTRA_INITIAL_INTENT) :
                    input;
            result.putExtra(EXTRA_INITIAL_INTENT, initialIntent);
            if (!initialIntent.hasCategory(Intent.CATEGORY_LAUNCHER))
            {
                // Wait for the result from MwmActivity for API callers.
                startActivityForResult(result, REQ_CODE_API_RESULT);
                return;
            }
        }
        Counters.setFirstStartDialogSeen(this);
        startActivity(result);
        finish();
    }

    // making notification bar transparent
    private void changeStatusBarColor(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void addBottomDots(int currentPage){
        dots = new TextView[layouts.length];

        int[] colorActive = getResources().getIntArray(R.array.bs_array_dot_active);
        int[] colorInactive = getResources().getIntArray(R.array.bs_array_dot_inactive);

        dotsLayout.removeAllViews();
        for (int i = 0; i < dots.length; i++){
            dots[i] = new TextView(this);
            //android >= 24
            if(Build.VERSION.SDK_INT >= 24){
                dots[i].setText(Html.fromHtml("&#8226", Html.FROM_HTML_MODE_COMPACT));
            }
            else{
                dots[i].setText(Html.fromHtml("&#8226"));
            }
            dots[i].setTextSize(35);
            dots[i].setTextColor(colorInactive[currentPage]);
            dotsLayout.addView(dots[i]);
        }
        if(dots.length > 0){
            dots[currentPage].setTextColor(colorActive[currentPage]);
        }
    }

    public class MyViewPagerAdapter extends PagerAdapter{

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            LayoutInflater layoutInflater = getLayoutInflater();
            View view = layoutInflater.inflate(layouts[position], container, false);
            container.addView(view);
            return view;
        }

        @Override
        public int getCount() {
            return layouts.length;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            View view = (View) object;
            container.removeView(view);
        }
    }
}
