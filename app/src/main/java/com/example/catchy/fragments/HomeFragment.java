package com.example.catchy.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager2.widget.ViewPager2;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.catchy.HomeFragmentAdapter;
import com.example.catchy.MainActivity;
import com.example.catchy.R;
import com.example.catchy.SpotifyAppRemoteSingleton;
import com.example.catchy.models.Song;
import com.example.catchy.service.SpotifyBroadcastReceiver;
import com.example.catchy.service.SpotifyService;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.List;

import static com.parse.Parse.getApplicationContext;


public class HomeFragment extends Fragment{
    public static final String TAG = "HomeFragment";
    HomeFragmentAdapter homeFragmentAdapter;
    ViewPager2 mViewPager;
    List<Song> arr;
    private SpotifyBroadcastReceiver spotifyBroadcastReceiver;
    Context context;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        arr = new ArrayList<Song>();
        spotifyBroadcastReceiver = ((MainActivity)getContext()).getReceiver();
        // spotifyBroadcastReceiver.playNew(getContext(), "spotify:track:6KfoDhO4XUWSbnyKjNp9c4");
        homeFragmentAdapter = new HomeFragmentAdapter(this, arr, getContext(), spotifyBroadcastReceiver);
        context = getContext();

        try {
            populatePlaylist();
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewPager = view.findViewById(R.id.viewpager);
        mViewPager.setAdapter(homeFragmentAdapter);
        mViewPager.setOrientation(ViewPager2.ORIENTATION_VERTICAL);
        homeFragmentAdapter.notifyDataSetChanged();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    private void populatePlaylist() throws ParseException {
        ParseQuery<Song> query = new ParseQuery<>(Song.class);
        query.addAscendingOrder("createdAt"); // oldest songs first
        // query.whereEqualTo("seen", false);
        query.setLimit(10);
        query.findInBackground(new FindCallback<Song>() {
            @Override
            public void done(List<Song> objects, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Issue with getting songs", e);
                    return;
                }
                for (Song song : objects) {
                    Log.i(TAG, "Song: " + song.getTitle());
                    arr.add(song);
                }

                homeFragmentAdapter.notifyDataSetChanged();

            }
        });
    }


    @Override
    public void onPause() {
        super.onPause();
        spotifyBroadcastReceiver.enqueueService(getContext(), SpotifyBroadcastReceiver.ACTION_PLAY_PAUSE);
        LocalBroadcastManager.getInstance(context).unregisterReceiver(spotifyBroadcastReceiver);
        Log.d("HomeFragment", "Paused");
    }


    @Override
    public void onResume() {
        super.onResume();
        // Register for the particular broadcast based on ACTION string
        IntentFilter filter = new IntentFilter(SpotifyService.ACTION);
        spotifyBroadcastReceiver.enqueueService(getContext(), SpotifyBroadcastReceiver.ACTION_PLAY_PAUSE);
        LocalBroadcastManager.getInstance(context).registerReceiver(spotifyBroadcastReceiver, filter);
        // or `registerReceiver(testReceiver, filter)` for a normal broadcast
    }
}