package eu.kanade.tachiyomi.ui.manga.chapter;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.Bind;
import butterknife.ButterKnife;
import eu.kanade.tachiyomi.R;
import eu.kanade.tachiyomi.data.database.models.Chapter;
import eu.kanade.tachiyomi.data.database.models.Manga;
import eu.kanade.tachiyomi.data.download.model.Download;
import eu.kanade.tachiyomi.ui.base.adapter.FlexibleViewHolder;
import rx.Observable;

public class ChaptersHolder extends FlexibleViewHolder {

    @Bind(R.id.chapter_title) TextView title;
    @Bind(R.id.download_text) TextView downloadText;
    @Bind(R.id.chapter_menu) RelativeLayout chapterMenu;
    @Bind(R.id.chapter_pages) TextView pages;
    @Bind(R.id.chapter_date) TextView date;

    private Context context;

    private final ChaptersAdapter adapter;
    private Chapter item;

    private final int readColor;
    private final int unreadColor;

    private final DecimalFormat decimalFormat;
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

    public ChaptersHolder(View view, ChaptersAdapter adapter, OnListItemClickListener listener) {
        super(view, adapter, listener);
        this.adapter = adapter;
        context = view.getContext();
        ButterKnife.bind(this, view);

        readColor = ContextCompat.getColor(view.getContext(), R.color.hint_text);
        unreadColor = ContextCompat.getColor(view.getContext(), R.color.primary_text);

        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        decimalFormat = new DecimalFormat("#.###", symbols);

        chapterMenu.setOnClickListener(v -> v.post(() -> showPopupMenu(v)));
    }

    public void onSetValues(Chapter chapter, Manga manga) {
        this.item = chapter;
        String name;
        switch (manga.getDisplayMode()) {
            case Manga.DISPLAY_NAME:
            default:
                name = chapter.name;
                break;
            case Manga.DISPLAY_NUMBER:
                String formattedNumber = decimalFormat.format(chapter.chapter_number);
                name = context.getString(R.string.display_mode_chapter, formattedNumber);
                break;
        }
        title.setText(name);
        title.setTextColor(chapter.read ? readColor : unreadColor);

        if (!chapter.read && chapter.last_page_read > 0) {
            pages.setText(context.getString(R.string.chapter_progress, chapter.last_page_read + 1));
        } else {
            pages.setText("");
        }

        onStatusChange(chapter.status);
        date.setText(sdf.format(new Date(chapter.date_upload)));
    }

    public void onStatusChange(int status) {
        switch (status) {
            case Download.QUEUE:
                downloadText.setText(R.string.chapter_queued); break;
            case Download.DOWNLOADING:
                downloadText.setText(R.string.chapter_downloading); break;
            case Download.DOWNLOADED:
                downloadText.setText(R.string.chapter_downloaded); break;
            case Download.ERROR:
                downloadText.setText(R.string.chapter_error); break;
            default:
                downloadText.setText(""); break;
        }
    }

    public void onProgressChange(Context context, int downloaded, int total) {
        downloadText.setText(context.getString(
                R.string.chapter_downloading_progress, downloaded, total));
    }

    private void showPopupMenu(View view) {
        // Create a PopupMenu, giving it the clicked view for an anchor
        PopupMenu popup = new PopupMenu(adapter.getFragment().getActivity(), view);

        // Inflate our menu resource into the PopupMenu's Menu
        popup.getMenuInflater().inflate(R.menu.chapter_single, popup.getMenu());

        // Set a listener so we are notified if a menu item is clicked
        popup.setOnMenuItemClickListener(menuItem -> {
            Observable<Chapter> chapter = Observable.just(item);

            switch (menuItem.getItemId()) {
                case R.id.action_mark_as_read:
                    return adapter.getFragment().onMarkAsRead(chapter);
                case R.id.action_mark_as_unread:
                    return adapter.getFragment().onMarkAsUnread(chapter);
                case R.id.action_download:
                    return adapter.getFragment().onDownload(chapter);
                case R.id.action_delete:
                    return adapter.getFragment().onDelete(chapter);
                case R.id.action_mark_previous_as_read:
                    return adapter.getFragment().onMarkPreviousAsRead(item);
            }
            return false;
        });

        // Finally show the PopupMenu
        popup.show();
    }

}
