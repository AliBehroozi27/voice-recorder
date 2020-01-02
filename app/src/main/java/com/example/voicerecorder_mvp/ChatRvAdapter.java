package com.example.voicerecorder_mvp;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.widget.RecyclerView;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.List;

import butterknife.BindDrawable;
import butterknife.BindView;
import butterknife.ButterKnife;


public class ChatRvAdapter extends RecyclerView.Adapter<ChatRvAdapter.ViewHolder> {
    @BindDrawable(R.drawable.ic_play)
    Drawable playIcon;
    @BindDrawable(R.drawable.ic_pause)
    Drawable pauseIcon;

    private final MainPresenter presenter;
    private List<VoiceMessage> voiceMessages;
    private Context context;
    private ItemClickListener mClickListener;
    private boolean isPlaying;
    private int last_index = -1;
    private ViewHolder viewHolder;


    public ViewHolder getViewHolder() {
        return viewHolder;
    }

    public void setViewHolder(ViewHolder viewHolder) {
        this.viewHolder = viewHolder;
    }

    ChatRvAdapter(Context context, List<VoiceMessage> data, MainPresenter presenter) {
        this.context = context;
        this.voiceMessages = data;
        this.presenter = presenter;
    }

    @NonNull
    @Override
    public ChatRvAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.voice_item, parent, false);
        this.viewHolder = new ViewHolder(view);
        return viewHolder;
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onBindViewHolder(@NonNull ChatRvAdapter.ViewHolder holder, int position) {
        Log.e("AAAAA", "--------------- start binding :: " + position);
        VoiceMessage voiceMessage = voiceMessages.get(position);
        Log.e("AAAAA on Bind ", voiceMessage.getString());

        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(context, Uri.parse(voiceMessage.getPath()));
        calculateTime(Integer.parseInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)), holder);
        holder.seekBar.setMax(Integer.parseInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)));
        holder.seekBar.setProgress(voiceMessage.getLastProgress());

        if (voiceMessage.isPlaying()) {
            holder.playButton.setImageResource(R.drawable.ic_pause);
            holder.seekUpdation(holder);
        } else {
            holder.playButton.setImageResource(R.drawable.ic_play);
        }

        Log.e("AAAAA", "--------------- end binding :: " + position);

        holder.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (presenter.getMediaPlayer() != null && presenter.getVoiceMessage().getPath().equals(voiceMessage.getPath()) && fromUser) {
                    presenter.isUserSeeking = true;
                    presenter.seek(progress);
                    presenter.setLastProgress(progress);
                    voiceMessage.setLastProgress(progress);
                    calculateTime(progress, holder);

                } else if (position == presenter.getPosition()) {
                    voiceMessage.setLastProgress(progress);
                    presenter.setLastProgress(progress);
                    voiceMessage.setLastProgress(progress);
                    Log.e("aaa onProgressChange", "pos : " + position + voiceMessage.getString());
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void calculateTime(Integer duration, ViewHolder holder) {
        int seconds = duration / 1000;
        int minutes = seconds / 60;
        seconds = seconds - (minutes * 60);

        holder.timer.setText(minutes + ":" + checkDigit(seconds));
    }


    @Override
    public int getItemCount() {
        return voiceMessages.size();
    }

    private String checkDigit(int number) {
        return number <= 9 ? "0" + number : String.valueOf(number);
    }


    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, MainContract.Adapter {
        @BindView(R.id.play_button)
        ImageView playButton;
        @BindView(R.id.seek_bar)
        SeekBar seekBar;
        @BindView(R.id.date)
        TextView dateModified;
        @BindView(R.id.timer)
        TextView timer;

        private int position;
        private ViewHolder holder;
        private Handler mHandler = new Handler();


        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
            bindEvents();
        }

        private void bindEvents() {
            playButton.setOnClickListener(new View.OnClickListener() {
                @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                @Override
                public void onClick(View view) {
                    position = getAdapterPosition();
                    VoiceMessage voiceMessage = voiceMessages.get(position);

                    Log.e("AAAA onClick", last_index + " " + position + " " + voiceMessage.getString());

                    if (voiceMessage.isPlaying()) {
                        //presenter.stopPlay();
                        if (position == last_index) {
                            voiceMessage.setPlaying(false);
                            voiceMessages.set(position, voiceMessage);
                            presenter.stopPlay();
                            Log.e("AAAA", "STOP CURRENT " + position + "  " + voiceMessage.getString());
                            //notifyItemChanged(position);
                            notifyDataSetChanged();
                        } else {
                            markAllPaused();
                            presenter.stopPlay();
                            presenter.play(position);
                            voiceMessage.setPlaying(true);
                            //notifyItemChanged(position);
                            notifyDataSetChanged();

                            Log.e("AAAA", "PLAY NEW " + position + "  " + voiceMessage.getString());
                            last_index = position;
                        }

                    } else {
                        if (position == last_index) {
                            markAllPaused();
                            presenter.play(position);
                            voiceMessage.setPlaying(true);
                            Log.e("AAAA", "PLAY CURRENT " + position + "  " + voiceMessage.getString());
                            //notifyItemChanged(position);
                            notifyDataSetChanged();

                            last_index = position;
                        } else {
                            markAllPaused();
                            presenter.stopPlay();
                            presenter.play(position);
                            voiceMessage.setPlaying(true);
                            //notifyItemChanged(position);
                            notifyDataSetChanged();
                            Log.e("AAAA", "PLAY NEW " + position + "  " + voiceMessage.getString());
                            last_index = position;
                        }

                    }
                }
            });
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                viewHolder.seekUpdation(holder);
            }
        };





        private void seekUpdation(ViewHolder holder) {
            this.holder = holder;
            if (presenter.getMediaPlayer() != null) {
                int mCurrentPosition = presenter.getMediaPlayer().getCurrentPosition();
                this.holder.seekBar.setMax(presenter.getMediaPlayer().getDuration());
                this.holder.seekBar.setProgress(mCurrentPosition);
                Log.e("AA update", presenter.getVoiceMessage().getString());
                presenter.getVoiceMessage().setLastProgress(mCurrentPosition);

            }
            mHandler.postDelayed(runnable, 100);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }

        @Override
        public void startPlaying() {
            //seekUpdation(holder);
            seekBar.setProgress(presenter.getLastProgress());
            presenter.seek(presenter.getLastProgress());
        }

        @Override
        public void setSeekBarProgress(Integer progress) {
            seekBar.setProgress(progress);
            Log.e("AAA set seek bar", "adapter progress : " + progress);
            //notifyItemChanged(position);
        }


        @Override
        public void stopPlaying() {
            mHandler.removeCallbacks(runnable);
        }

    }

    private void markAllPaused() {
        for (int i = 0; i < voiceMessages.size(); i++) {
            voiceMessages.get(i).setPlaying(false);
            voiceMessages.set(i, voiceMessages.get(i));
        }
        notifyDataSetChanged();
    }

    VoiceMessage getItem(int id) {
        return voiceMessages.get(id);
    }

    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}
