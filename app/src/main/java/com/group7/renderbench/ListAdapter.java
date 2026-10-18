package com.group7.renderbench;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.group7.renderbench.entities.Item;

import java.util.List;

/**
 *  RecyclerView adapter for the list virtualization engine (FR3).
 *  <p>
 *      Only the rows currently on screen are inflated into view holders. When a row scrolls
 *      off-screen its view holder is recycled and rebound with new data instead of being
 *      inflated again.
 *  <p>
 *  Shares {@code R.layout.item_row} with the ScrollView engine so both engines render
 *  identical rows and the rendering approach stays the only variable.
 *
 * @see RecyclerViewFragment
 * @see Item
 */
public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ItemViewHolder> {
    /** The dataset backing this adapter, one {@link Item} per row. */
    private final List<Item> items;

    /**
     * Creates an adapter for the given dataset.
     *
     * @param items the items to display, in display order; must not be {@code null}
     */
    public ListAdapter(List<Item> items) {
        this.items = items;
    }

    /**
     * Inflates {@code R.layout.item_row} and wraps it in a new {@link ItemViewHolder}.
     * Called by the RecyclerView only when it needs a new holder, not for every row.
     *
     * @param parent The RecyclerView the new row will be attached to
     * @param viewType The view type of the new row (unused, as all rows share one layout)
     *
     * @return a new view holder for one row
     */
    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_row, parent, false);
        return new ItemViewHolder(row);
    }

    /**
     * Binds the item at the given position to a (possibly recycled) view holder by setting
     * its title and description text.
     *
     * @param holder The ViewHolder to update
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Item item = items.get(position);
        holder.title.setText(item.getTitle());
        holder.description.setText(item.getDescription());
    }

    /**
     * Returns the number of rows in the dataset.
     *
     * @return the size of the item list
     */
    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     *  Caches the row's views. RecyclerView keeps a small pool of these
     *  holders and rebinds them while scrolling instead of re-inflating layout files.
     */
    static class ItemViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView description;

        /**
         *
         * @param itemView the inflated row layout
         */
        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.item_row_title);
            description = itemView.findViewById(R.id.item_row_description);
        }
    }
}
