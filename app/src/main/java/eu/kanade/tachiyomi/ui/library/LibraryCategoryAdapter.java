package eu.kanade.tachiyomi.ui.library;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import java.util.ArrayList;
import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.kanade.tachiyomi.R;
import eu.kanade.tachiyomi.data.database.models.Manga;
import rx.Observable;

public class LibraryCategoryAdapter extends FlexibleAdapter<LibraryHolder, Manga>
        implements Filterable {

    List<Manga> mangas;
    Filter filter;
    private LibraryCategoryFragment fragment;

    public LibraryCategoryAdapter(LibraryCategoryFragment fragment) {
        this.fragment = fragment;
        mItems = new ArrayList<>();
        filter = new LibraryFilter();
        setHasStableIds(true);
    }

    public void setItems(List<Manga> list) {
        mItems = list;
        notifyDataSetChanged();

        // TODO needed for filtering?
        mangas = list;
    }

    public void clear() {
        mItems.clear();
    }

    @Override
    public long getItemId(int position) {
        return mItems.get(position).id;
    }

    @Override
    public void updateDataSet(String param) {

    }

    @Override
    public LibraryHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(fragment.getActivity()).inflate(R.layout.item_catalogue_grid, parent, false);
        return new LibraryHolder(v, this, fragment);
    }

    @Override
    public void onBindViewHolder(LibraryHolder holder, int position) {
        final LibraryPresenter presenter = ((LibraryFragment) fragment.getParentFragment()).getPresenter();
        final Manga manga = getItem(position);
        holder.onSetValues(manga, presenter);

        //When user scrolls this bind the correct selection status
        holder.itemView.setActivated(isSelected(position));
    }

    public int getCoverHeight() {
        return fragment.recycler.getItemWidth() / 3 * 4;
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    private class LibraryFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            FilterResults results = new FilterResults();
            String query = charSequence.toString().toLowerCase();

            if (query.length() == 0) {
                results.values = mangas;
                results.count = mangas.size();
            } else {
                List<Manga> filteredMangas = Observable.from(mangas)
                        .filter(x ->
                                (x.title != null && x.title.toLowerCase().contains(query)) ||
                                (x.author != null && x.author.toLowerCase().contains(query)) ||
                                (x.artist != null && x.artist.toLowerCase().contains(query)))
                        .toList()
                        .toBlocking()
                        .single();
                results.values = filteredMangas;
                results.count = filteredMangas.size();
            }

            return results;
        }

        @Override
        public void publishResults(CharSequence constraint, FilterResults results) {
            setItems((List<Manga>) results.values);
        }
    }

}
