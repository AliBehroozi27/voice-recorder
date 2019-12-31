package com.example.voicerecorder_mvp;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
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

    private List<VoiceMessage> voiceMessages;
    private Context context;
    private ItemClickListener mClickListener;

    ChatRvAdapter(Context context, List<VoiceMessage> data) {
        this.context = context;
        this.voiceMessages = data;
    }

    @NonNull
    @Override
    public ChatRvAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.voice_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatRvAdapter.ViewHolder holder, int position) {
        VoiceMessage voiceMessage = voiceMessages.get(position);
        holder.timer.setText(String.valueOf(voiceMessage.getDuration()));
        holder.seekBar.setProgress(voiceMessage.getLastProgress());
        holder.seekBar.setMax(voiceMessage.getDuration());
        holder.dateModified.setText(voiceMessage.getDateModified());

        holder.playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (presenter.getVoiceMessage().getPath() == voiceMessage.getPath()) {
                    if (voiceMessage.isPlaying()) {
                        holder.playButton.setImageDrawable(pauseIcon);
                        //TODO : presenter resume playing
                    } else {
                        holder.playButton.setImageDrawable(playIcon);
                        //TODO : presenter pause playing
                    }
                }else {
                    if (voiceMessage.isPlaying()) {
                        holder.playButton.setImageDrawable(pauseIcon);
                        //TODO : previous voice icon change to play
                        //TODO : presenter stop playing previous voice and start current one
                    } else {
                        holder.playButton.setImageDrawable(playIcon);
                        //TODO : previous voice icon change to play
                        //TODO : presenter stop playing previous voice and start current one
                    }
                }
            }
        });

        holder.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (presenter.getMediaPlayer() != null && presenter.getVoiceMessage().getPath() == voiceMessage.getPath() && fromUser) {
                    presenter.isUserSeeking = true;
                    presenter.seek(progress);
                    presenter.setLastProgress(progress);
                } else if (fromUser) {
                    voiceMessage.setLastProgress(progress);
                }

                int seconds = progress / 1000;
                int minutes = seconds / 60;
                seconds = seconds - (minutes * 60);

                holder.timer.setText(minutes + ":" + checkDigit(seconds));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


    }


    @Override
    public int getItemCount() {
        return voiceMessages.size();
    }

    private String checkDigit(int number) {
        return number <= 9 ? "0" + number : String.valueOf(number);
    }


    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @BindView(R.id.play_button)
        ImageView playButton;
        @BindView(R.id.seek_bar)
        SeekBar seekBar;
        @BindView(R.id.date)
        TextView dateModified;
        @BindView(R.id.timer)
        TextView timer;


        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
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
