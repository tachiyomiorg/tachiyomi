package eu.kanade.tachiyomi.ui.catalogue;

import android.view.View;

import eu.kanade.tachiyomi.data.database.models.Manga;
import eu.kanade.tachiyomi.ui.base.adapter.FlexibleViewHolder;

public abstract class CatalogueHolder extends FlexibleViewHolder {

    public CatalogueHolder(View view, CatalogueAdapter adapter, OnListItemClickListener listener) {
        super(view, adapter, listener);
    }

    abstract void onSetValues(Manga manga, CataloguePresenter presenter);
}
