package com.example.catchy.fragments;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.catchy.activities.FindFriendsActivity;
import com.example.catchy.databinding.FragmentAboutBinding;
import com.example.catchy.databinding.FragmentSearchBinding;
import com.example.catchy.misc.BitmapCache;
import com.example.catchy.misc.EndlessRecyclerViewScrollListener;
import com.example.catchy.R;
import com.example.catchy.adapters.SearchAdapter;
import com.example.catchy.models.Song;
import com.example.catchy.models.User;
import com.example.catchy.service.SpotifyBroadcastReceiver;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import co.revely.gradient.RevelyGradient;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.ArtistSimple;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.TracksPager;
import retrofit.client.Response;

public class SearchFragment extends Fragment {
    public static final String TAG = "SearchFragment";
    SpotifyService spotify;
    SearchAdapter searchAdapter;
    RecyclerView rvResults;
    EditText etSearch;
    ImageView ivSearch;
    List<Song> results;
    String query;
    SpotifyBroadcastReceiver spotifyBroadcastReceiver;
    RelativeLayout layout;
    FragmentSearchBinding binding;

    private EndlessRecyclerViewScrollListener scrollListener;

    public SearchFragment() {
        // Required empty public constructor
    }

    public static SearchFragment newInstance() {
        SearchFragment fragment = new SearchFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        spotifyBroadcastReceiver = new SpotifyBroadcastReceiver();
        spotifyBroadcastReceiver.enqueueService(getContext(), SpotifyBroadcastReceiver.ACTION_PAUSE);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(spotifyBroadcastReceiver);
        Log.d("SearchFragment", "Paused");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        View view = binding.getRoot();


        // Get Spotify service
        SpotifyApi spotifyApi = new SpotifyApi();
        spotifyApi.setAccessToken(ParseUser.getCurrentUser().getString("token"));
        spotify = spotifyApi.getService();

        getActivity().setTheme(R.style.CustomDialogTheme);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvResults = binding.rvResults;
        etSearch = binding.etSearch;
        ivSearch = binding.ivSearch;

        results = new ArrayList<>();
        searchAdapter = new SearchAdapter(results, getContext());

        BitmapCache.InitBitmapCache(true); // for search adapter; for songs

        rvResults.setAdapter(searchAdapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        rvResults.setLayoutManager(linearLayoutManager);

        scrollListener = new EndlessRecyclerViewScrollListener(linearLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                fetchSongs(query, page);
            }
        };
        rvResults.addOnScrollListener(scrollListener);

        ivSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                query = etSearch.getText().toString();
                if (query.isEmpty()) {
                    Toast toast = Toast.makeText(getActivity(), "Search cannot be empty", Toast.LENGTH_SHORT);
                    View toastView = toast.getView();

                    TextView text = (TextView) toastView.findViewById(android.R.id.message);
                    text.setTextColor(getResources().getColor(R.color.white));
                    toast.show();
                    return;
                }

                // clear search list
                results.clear();
                BitmapCache.clearSongCache();
                fetchSongs(query, 0);
                scrollListener.resetState();
            }
        });

        etSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                boolean handled = false;
                if (i == EditorInfo.IME_ACTION_SEARCH) {
                    query = etSearch.getText().toString();
                    if (query.isEmpty()) {
                        Toast toast = Toast.makeText(getActivity(), "Search cannot be empty", Toast.LENGTH_SHORT);
                        View toastView = toast.getView();
                        TextView text = (TextView) toastView.findViewById(android.R.id.message);
                        text.setTextColor(getResources().getColor(R.color.white));
                        toast.show();
                    }

                    // clear search list
                    results.clear();
                    BitmapCache.clearSongCache();
                    fetchSongs(query, 0);
                    scrollListener.resetState();
                    handled = true;
                }
                return handled;
            }
        });

        layout = binding.layout;
        setBackgroundColor();


    }

    private void setBackgroundColor() {
        if (User.profileBitmap != null && !User.profileBitmap.isRecycled()) {
            Palette palette = Palette.from(User.profileBitmap).generate();
            Palette.Swatch swatch = palette.getLightVibrantSwatch();
            // int color = palette.getDarkMutedColor(0);
            if (swatch == null) {
                swatch = palette.getDominantSwatch();
            }

            // swatch.getRgb()
            if (swatch != null) {
                int color = swatch.getRgb();
                RevelyGradient
                        .linear()
                        .colors(new int[]{Color.parseColor("#000000"), color}).angle(270f).alpha(0.76f)
                        .onBackgroundOf(layout);
            }
        } else {
            RevelyGradient
                    .linear()
                    .colors(new int[]{Color.parseColor("#000000"), Color.parseColor("#00EDFF")}).angle(270f).alpha(0.76f)
                    .onBackgroundOf(layout);
        }
    }

    private void fetchSongs(String query, int offset) {
        Map<String, Object> options = new HashMap<>();
        options.put(SpotifyService.OFFSET, 20 * offset);
        options.put(SpotifyService.LIMIT, 20);

        spotify.searchTracks(query, options, new SpotifyCallback<TracksPager>() {
            @Override
            public void failure(SpotifyError spotifyError) {
                Log.e(TAG, "Search tracks failed!", spotifyError);
            }

            @Override
            public void success(TracksPager tracksPager, Response response) {
                Log.d(TAG, "Search tracks success!");
                List<Track> tracks = tracksPager.tracks.items;
                for (int i = 0; i < tracks.size(); i++) {
                    Track track = tracks.get(i);
                    Song song = new Song();
                    song.setURI(track.uri);
                    song.setImageUrl(track.album.images.get(0).url);
                    song.setTitle(track.name);
                    song.setDuration(track.duration_ms);
                    String artists = "";
                    List<ArtistSimple> artistList = track.artists;
                    for (int j = 0; j < artistList.size(); j++) {
                        artists += artistList.get(j).name + ", ";
                    }
                    song.setArtist(artists.substring(0, artists.length() - 2));
                    results.add(song);
                }
                searchAdapter.notifyDataSetChanged();
                rvResults.smoothScrollToPosition(0);
            }
        });
    }


}