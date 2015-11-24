package eu.kanade.mangafeed.ui.manga.chapter;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.Bind;
import butterknife.ButterKnife;
import eu.kanade.mangafeed.R;
import eu.kanade.mangafeed.data.database.models.Chapter;

public class ChaptersHolder extends RecyclerView.ViewHolder implements
        View.OnClickListener, View.OnLongClickListener {

    private ChaptersAdapter adapter;

    @Bind(R.id.chapter_title) TextView title;
    @Bind(R.id.chapter_download_image) ImageView download_icon;
    @Bind(R.id.chapter_pages) TextView pages;
    @Bind(R.id.chapter_date) TextView date;

    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

    public ChaptersHolder(View view) {
        super(view);
        ButterKnife.bind(this, view);
    }

    public ChaptersHolder(View view, final ChaptersAdapter adapter) {
        this(view);

        this.adapter = adapter;
        itemView.setOnClickListener(this);
        itemView.setOnLongClickListener(this);
    }

    public void onSetValues(Context context, Chapter chapter) {
        title.setText(chapter.name);

        if (chapter.read) {
            title.setTextColor(ContextCompat.getColor(context, R.color.hint_text));
        } else {
            title.setTextColor(ContextCompat.getColor(context, R.color.primary_text));
        }

        if (chapter.last_page_read > 0 && !chapter.read) {
            pages.setText(context.getString(R.string.chapter_progress, chapter.last_page_read + 1));
        } else {
            pages.setText("");
        }

        if (chapter.downloaded == Chapter.UNKNOWN) {
            adapter.getMangaChaptersFragment().getPresenter().checkIsChapterDownloaded(chapter);
        }
        if (chapter.downloaded == Chapter.DOWNLOADED)
            download_icon.setImageResource(R.drawable.ic_action_delete_36dp);
        else if (chapter.downloaded == Chapter.NOT_DOWNLOADED)
            download_icon.setImageResource(R.drawable.ic_file_download_black_36dp);

        date.setText(sdf.format(new Date(chapter.date_fetch)));
        toggleActivation();
    }

    private void toggleActivation() {
        itemView.setActivated(adapter.isSelected(getAdapterPosition()));
    }

    @Override
    public void onClick(View v) {
        if (adapter.clickListener.onListItemClick(getAdapterPosition()))
            toggleActivation();
    }

    @Override
    public boolean onLongClick(View v) {
        adapter.clickListener.onListItemLongClick(getAdapterPosition());
        toggleActivation();
        return true;
    }
}
