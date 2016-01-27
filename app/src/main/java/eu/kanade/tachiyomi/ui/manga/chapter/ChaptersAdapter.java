package eu.kanade.tachiyomi.ui.manga.chapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.kanade.tachiyomi.R;
import eu.kanade.tachiyomi.data.database.models.Chapter;
import eu.kanade.tachiyomi.data.database.models.Manga;

public class ChaptersAdapter extends FlexibleAdapter<ChaptersHolder, Chapter> {

    private ChaptersFragment fragment;

    public ChaptersAdapter(ChaptersFragment fragment) {
        this.fragment = fragment;
        mItems = new ArrayList<>();
        setHasStableIds(true);
    }

    @Override
    public void updateDataSet(String param) {}

    @Override
    public ChaptersHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(fragment.getActivity()).inflate(R.layout.item_chapter, parent, false);
        return new ChaptersHolder(v, this, fragment);
    }

    @Override
    public void onBindViewHolder(ChaptersHolder holder, int position) {
        final Chapter chapter = getItem(position);
        final Manga manga = fragment.getPresenter().getManga();
        holder.onSetValues(chapter, manga);

        //When user scrolls this bind the correct selection status
        holder.itemView.setActivated(isSelected(position));
    }

    @Override
    public long getItemId(int position) {
        return mItems.get(position).id;
    }

    public void setItems(List<Chapter> chapters) {
        mItems = chapters;
        notifyDataSetChanged();
    }

    public ChaptersFragment getFragment() {
        return fragment;
    }
}
