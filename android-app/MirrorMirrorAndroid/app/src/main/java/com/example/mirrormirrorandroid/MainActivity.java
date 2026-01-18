package com.example.mirrormirrorandroid;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private final String[] tabTitles =
            new String[]{"Status", "To-Do List", "Maps", "Comps", "Health"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(new CardPagerAdapter(this));
        viewPager.setClipToPadding(false);
        viewPager.setClipChildren(false);

        int pageMarginPx = getResources().getDimensionPixelOffset(R.dimen.pageMargin);
        viewPager.setPageTransformer(new MarginPageTransformer(pageMarginPx));

        tabLayout = findViewById(R.id.tabLayout);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(tabTitles[position])
        ).attach();

    }
}
