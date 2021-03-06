package com.example.catchy.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.example.catchy.R;
import com.example.catchy.adapters.FindFriendsAdapter;
import com.example.catchy.databinding.ActivityFollowersBinding;
import com.example.catchy.misc.BitmapCache;
import com.example.catchy.models.User;
import com.parse.ParseUser;

import java.util.List;

import co.revely.gradient.RevelyGradient;

public class FollowersActivity extends AppCompatActivity {

    private List<ParseUser> followers;
    boolean currentUser;
    ParseUser user;
    private FindFriendsAdapter adapter;
    androidx.coordinatorlayout.widget.CoordinatorLayout layout;
    RecyclerView rvFollowers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        ActivityFollowersBinding binding = ActivityFollowersBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        BitmapCache.InitBitmapCache(false); // for adapter, for users
        BitmapCache.clearUserCache();

        Intent intent = getIntent();
        followers = intent.getExtras().getParcelableArrayList("followers");
        user = intent.getExtras().getParcelable("user");
        currentUser = intent.getBooleanExtra("currentUser", false);

        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);

        rvFollowers = binding.rvFollowers;
        adapter = new FindFriendsAdapter(followers, FollowersActivity.this);

        layout = binding.layout;

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvFollowers.setLayoutManager(layoutManager);
        rvFollowers.setAdapter(adapter);

        adapter.notifyDataSetChanged();

        setBackgroundColor();
    }

    private void setBackgroundColor() {
        Bitmap bitmap;
        if (currentUser) {
            bitmap = User.profileBitmap;
        } else
            bitmap = User.otherUserBitmaps.get(user.getObjectId());

        if (bitmap != null && !bitmap.isRecycled()) {
            Palette palette = Palette.from(bitmap).generate();
            Palette.Swatch swatch = palette.getLightVibrantSwatch();
            if (swatch == null) {
                swatch = palette.getDominantSwatch();
            }

            if (swatch != null) {
                int color = swatch.getRgb();
                RevelyGradient
                        .linear()
                        .colors(new int[]{Color.parseColor("#000000"), color}).angle(90f).alpha(0.46f)
                        .onBackgroundOf(layout);
            }


        } else {
            RevelyGradient
                    .linear()
                    .colors(new int[]{Color.parseColor("#000000"), Color.parseColor("#00EDFF")}).angle(90f).alpha(0.46f)
                    .onBackgroundOf(layout);
        }


    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.d("FollowersActivity", "Back here");
        if (User.otherUserPos != -1) {
            adapter.notifyItemChanged(User.otherUserPos);
            User.otherUserPos = -1;
        }
    }
}