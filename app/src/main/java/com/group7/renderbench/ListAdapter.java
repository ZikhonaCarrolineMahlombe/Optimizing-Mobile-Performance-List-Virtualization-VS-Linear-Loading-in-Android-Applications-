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
 * RecyclerView adapter for the virtualisation engine (FR3).
 * Only the rows currently on screen are inflated into view holders.
 * When a row scrolls off-screen, its view holder is recycled and
 * rebound which new data instead of being inflated again.
 * It shares R.layout.item_row with the ScrollView engine so both
 * engines render identical rows and the rendering approach stays
 * the only variable
 */
public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ItemViewHolder> {
    private final List<Item> items;

    /**
     * Creates an adapter that displays the given items, one per row.
     *
     * @param items the list of items to display, in display order
     */
    public ListAdapter(List<Item> items) {
        this.items = items;
    }

    /**
     * Inflates R.layout.item_row and wraps it in a new view holder.
     * The RecyclerView calls this only when it needs a new holder,
     * not once for every row.
     *
     * @param parent the RecyclerView the new row will be attached to
     * @param viewType the type of the new row, unused because all rows
     *                 share one layout
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
     * Binds the item at the given position to a view holder by
     * setting its title and description text.
     * The holder may be a recycled one that previously showed a
     * different item.
     *
     * @param holder the biew holder to update
     * @param position the position of the item in the dataset.
     */
    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Item item = items.get(position);
        holder.title.setText(item.getTitle());
        holder.description.setText(item.getDescription());
    }

    /**
     * Returns the number of items in the dataset.
     * Rows that are currently off-screen are counted as well.
     *
     * @return the total number of rows
     */
    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * Caches the row's views.
     * RecyclerView keeps a small pool of these holders and rebinds
     * them while scrolling instead of re-inflating layout files.
     */
    static class ItemViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView description;

        /**
         * Creates a holder for the given row and looks up its title
         * and description views once.
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
