package eu.kanade.tachiyomi.ui.catalogue;

import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import eu.kanade.tachiyomi.R;
import eu.kanade.tachiyomi.data.database.models.Manga;

public class CatalogueListHolder extends CatalogueHolder {

    @Bind(R.id.title) TextView title;

    private final int favoriteColor;
    private final int unfavoriteColor;

    public CatalogueListHolder(View view, CatalogueAdapter adapter, OnListItemClickListener listener) {
        super(view, adapter, listener);
        ButterKnife.bind(this, view);

        favoriteColor = ContextCompat.getColor(view.getContext(), R.color.hint_text);
        unfavoriteColor = ContextCompat.getColor(view.getContext(), R.color.primary_text);
    }

    @Override
    public void onSetValues(Manga manga, CataloguePresenter presenter) {
        title.setText(manga.title);
        title.setTextColor(manga.favorite ? favoriteColor : unfavoriteColor);
    }
}
