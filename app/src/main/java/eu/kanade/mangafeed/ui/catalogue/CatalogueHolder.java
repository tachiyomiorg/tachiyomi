package eu.kanade.mangafeed.ui.catalogue;

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
public class CatalogueHolder extends ItemViewHolder<Manga> {

    @ViewId(R.id.title)
    TextView title;

    @ViewId(R.id.author)
    TextView author;

    @ViewId(R.id.thumbnail)
    ImageView thumbnail;

    public CatalogueHolder(View view) {
        super(view);
    }

    @Override
    public void onSetValues(Manga manga, PositionInfo positionInfo) {
        title.setText(manga.title);
        author.setText(manga.author);

        if (manga.thumbnail_url != null) {
            Glide.with(getContext())
                    .load(manga.thumbnail_url)
                    .diskCacheStrategy(DiskCacheStrategy.RESULT)
                    .centerCrop()
                    .into(thumbnail);
        } else {
            thumbnail.setImageResource(android.R.color.transparent);
        }
    }
}
