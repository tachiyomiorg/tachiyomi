package eu.kanade.mangafeed.ui.library;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.Objects;

import eu.kanade.mangafeed.R;
import eu.kanade.mangafeed.data.database.models.Manga;
import uk.co.ribot.easyadapter.ItemViewHolder;
import uk.co.ribot.easyadapter.PositionInfo;
import uk.co.ribot.easyadapter.annotations.LayoutId;
import uk.co.ribot.easyadapter.annotations.ViewId;


@LayoutId(R.layout.item_catalogue)
public class LibraryHolder extends ItemViewHolder<Manga> {

    @ViewId(R.id.thumbnail)
    ImageView thumbnail;

    @ViewId(R.id.title)
    TextView title;

    @ViewId(R.id.author)
    TextView author;

    @ViewId(R.id.unreadText)
    TextView unreadText;

    public LibraryHolder(View view) {
        super(view);
    }

    public void onSetValues(Manga manga, PositionInfo positionInfo) {
        title.setText(manga.title);
        author.setText(manga.author);

        if (manga.unread > 0) {
            unreadText.setVisibility(View.VISIBLE);
            unreadText.setText(Integer.toString(manga.unread));
        }
        else {
            unreadText.setVisibility(View.GONE);
        }

        String thumbnail;
        if (manga.thumbnail_url != null)
            thumbnail = manga.thumbnail_url;
        else
            thumbnail = "http://img1.wikia.nocookie.net/__cb20090524204255/starwars/images/thumb/1/1a/R2d2.jpg/400px-R2d2.jpg";

        Glide.with(getContext())
                .load(thumbnail)
                .diskCacheStrategy(DiskCacheStrategy.RESULT)
                .centerCrop()
                .into(this.thumbnail);
    }

}
