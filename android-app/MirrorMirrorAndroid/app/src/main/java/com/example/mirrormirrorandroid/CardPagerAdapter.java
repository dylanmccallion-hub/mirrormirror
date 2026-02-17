package com.example.mirrormirrorandroid;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class CardPagerAdapter extends FragmentStateAdapter {

    public CardPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new MirrorStatusFragment();
            case 1: return new ToDoListFragment();
            case 2: return new MirrorMapsFragment();
            case 3: return new ComplimentsFragment();
            case 4: return new HealthFragment();
            case 5: return new SmartCommuteFragment();
            default: return new MirrorStatusFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 6; // number of cards/fragments
    }
}
