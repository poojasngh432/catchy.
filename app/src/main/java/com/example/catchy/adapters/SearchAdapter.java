package com.example.catchy.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.catchy.databinding.ItemResultBinding;
import com.example.catchy.misc.BitmapCache;
import com.example.catchy.misc.DetailTransition;
import com.example.catchy.misc.ImageLoaderTask;
import com.example.catchy.R;
import com.example.catchy.activities.SongDetailsActivity;
import com.example.catchy.models.Song;

import java.util.List;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {
    private List<Song> results;
    private Context context;
    private ItemResultBinding binding;


    public SearchAdapter(List<Song> results, Context context) {
        this.results = results;
        this.context = context;
    }

    @NonNull
    @Override
    public SearchAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        binding = ItemResultBinding.inflate(inflater, parent, false);
        View view = binding.getRoot();
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchAdapter.ViewHolder holder, int position) {
        Song song = results.get(position);
        holder.bind(song);


    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    // Clean all elements of the recycler
    public void clear() {
        results.clear();
        notifyDataSetChanged();
    }

    // Add a list of items
    public void addAll(List<Song> list) {
        results.addAll(list);
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivAlbumImage;
        private TextView tvTitle;
        private TextView tvArtist;
        private View itemView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            ivAlbumImage = binding.ivAlbumImage;
            tvTitle = binding.tvTitle;
            tvArtist = binding.tvArtist;

        }


        public void bind(Song song) {
            tvTitle.setText(song.getTitle());
            tvArtist.setText(song.getArtist());
            Glide.with(context).load(song.getImageUrl()).into(ivAlbumImage);


            int position = getAdapterPosition();
            Bitmap bitmap = null;

            bitmap = BitmapCache.getBitmapFromMemCache(position, true); // searching songs
            if (bitmap != null) {
                song.bitmap = bitmap; // save bitmap to song
            } else {
                new ImageLoaderTask(position, song.getImageUrl(), true).executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR, (Integer[]) null);
            }

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    DetailTransition.bitmap = BitmapCache.getBitmapFromMemCache(position, true);
                    Intent intent = new Intent(context, SongDetailsActivity.class);
                    // pack something
                    intent.putExtra("song", song);
                    intent.putExtra("liked", false);
                    intent.putExtra("from", "search");
                    context.startActivity(intent);
                }
            });

        }
    }
}
