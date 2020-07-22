package com.example.catchy.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.catchy.DetailTransition;
import com.example.catchy.EndlessRecyclerViewScrollListener;
import com.example.catchy.activities.MainActivity;
import com.example.catchy.R;
import com.example.catchy.activities.SettingsActivity;
import com.example.catchy.adapters.UserAdapter;
import com.example.catchy.models.Like;
import com.example.catchy.service.SpotifyBroadcastReceiver;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class UserFragment extends Fragment {

    public static final String TAG="ProfileFragment";
    private RecyclerView rvLikes;
    protected UserAdapter adapter;
    private GridLayoutManager gridLayoutManager;
    private ParseUser currentUser;
    private TextView tvUsername;
    private TextView tvBio;
    private EditText etBio;
    private Button btnUpdate;
    private ImageView ivProfileImage;
    private ImageView ivMore;


    private EndlessRecyclerViewScrollListener scrollListener;
    protected List<Like> userLikes;
    boolean infScroll = false;
    SpotifyBroadcastReceiver spotifyBroadcastReceiver;

    public UserFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        spotifyBroadcastReceiver = new SpotifyBroadcastReceiver();
        spotifyBroadcastReceiver.enqueueService(getContext(), SpotifyBroadcastReceiver.ACTION_PAUSE);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(spotifyBroadcastReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user, container, false);
        // Inflate the layout for this fragment
        ivMore = view.findViewById(R.id.ivMore);
        currentUser = ParseUser.getCurrentUser();
        tvUsername = view.findViewById(R.id.tvUsername);
        ivProfileImage = view.findViewById(R.id.ivProfileImage);
        tvBio = view.findViewById(R.id.tvBio);
        etBio = view.findViewById(R.id.etBio);
        btnUpdate = view.findViewById(R.id.btnUpdate);
        ivMore = view.findViewById(R.id.ivMore);


        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvUsername.setText(currentUser.getUsername());
        String bio = currentUser.getString("bio");

        if (bio != null)
            tvBio.setText(bio);
        else
            tvBio.setText("[update bio here]");


        ivMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(intent);
            }
        });

        tvBio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tvBio.setVisibility(View.GONE);
                etBio.setVisibility(View.VISIBLE);
                btnUpdate.setVisibility(View.VISIBLE);
            }
        });

        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String newBio = etBio.getText().toString();

                ParseUser currentUser = ParseUser.getCurrentUser();
                currentUser.put("bio", newBio);
                currentUser.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e == null) {
                            Log.d(TAG, "Updated bio successfully");
                        }
                    }
                });
                tvBio.setText(newBio);
                tvBio.setVisibility(View.VISIBLE);
                etBio.setVisibility(View.GONE);
                btnUpdate.setVisibility(View.GONE);

            }
        });

        ParseFile profileImage = currentUser.getParseFile("profilePic");
        if (profileImage != null) {
            Glide.with(this).load(profileImage.getUrl()).transform(new CircleCrop()).into(ivProfileImage);
        }

        rvLikes = view.findViewById(R.id.rvLikes);
        userLikes = new ArrayList<>();
        queryUserLikes();
        adapter = new UserAdapter(getContext(), userLikes);
        gridLayoutManager = new GridLayoutManager(getContext(), 3);
        rvLikes.setAdapter(adapter);
        rvLikes.setLayoutManager(gridLayoutManager);



        scrollListener = new EndlessRecyclerViewScrollListener(gridLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                infScroll = true;
                Log.i(TAG, "Asking for inf scroll posts");
                queryUserLikes();
            }
        };
        rvLikes.addOnScrollListener(scrollListener);

    }

    private void queryUserLikes() {
        ParseQuery<Like> query = ParseQuery.getQuery(Like.class);
        query.include(Like.KEY_LIKED_BY);

        if (infScroll && userLikes.size() > 0) {
            Date oldest = userLikes.get(userLikes.size() - 1).getCreatedAt();
            Log.i(TAG, "Getting inf scroll posts");
            query.whereLessThan("createdAt", oldest);
            infScroll = false;
        }

        query.setLimit(24);
        query.addDescendingOrder("createdAt");
        Log.e(TAG, "Id: " + currentUser.getObjectId());
        query.whereEqualTo("likedBy", currentUser);
        query.findInBackground(new FindCallback<Like>() {
            @Override
            public void done(List<Like> likes, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Issue with getting posts", e);
                    return;
                }
                for (Like like : likes) {
                    Log.i(TAG, "Liked: " + like.getTitle());
                }
                userLikes.addAll(likes);
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // reset profile pic
        ParseFile profileImage = currentUser.getParseFile("profilePic");
        if (profileImage != null) {
             Glide.with(this).load(profileImage.getUrl()).transform(new CircleCrop()).into(ivProfileImage);
            // Glide.with(this).load("/storage/emulated/0/Android/data/com.example.catchy/files/Pictures/SettingsActivity/photo.jpg").transform(new CircleCrop()).into(ivProfileImage);
        }

        if (!DetailTransition.liked) {
            adapter.notifyItemRemoved(DetailTransition.pos);
        }

        View decorView = ((MainActivity)getActivity()).getWindow().getDecorView();
        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

    }
}