package flingr.app.ui;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import flingr.app.R;
import flingr.app.activities.NewConnectionActivity;
import flingr.app.entities.Connection;
import flingr.app.managers.ConnectionManager;
import flingr.app.utilities.Serializer;

/**
 * Adapts {@link Connection}(s) to view items to be placed in a
 * {@link android.widget.ListView}.
 */
public class ConnectionAdapter extends ArrayAdapter<Connection>
{
    public ConnectionAdapter(Activity context)
    {
        super(context, R.layout.previous_server_list_item,
                ConnectionManager.getInstance().getConnections());
    }


    @Override
    @NonNull
    public View getView(int position, View convertView, @NonNull  ViewGroup parent)
    {
        if (convertView == null)
        {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.previous_server_list_item, parent, false);
        }

        Connection connection = getItem(position);
        if (connection != null)
        {
            TextView headerLabel = convertView.findViewById(R.id.header_text_label);
            TextView subLabel = convertView.findViewById(R.id.secondary_text_label);
            TextView thirdLabel = convertView.findViewById(R.id.third_text_label);

            ImageButton imageButton = convertView.findViewById(R.id.server_menu_button);

            headerLabel.setText(connection.getColloquialName());
            subLabel.setText(connection.getInfoSummary());
            thirdLabel.setText(connection.getUserNameSummary());

            imageButton.setOnClickListener((clickedView) -> createPopupMenus(imageButton, position));
        }
        return convertView;
    }

    private void createPopupMenus(ImageButton imageButton, int position)
    {
        final PopupMenu popupMenu = new PopupMenu(getContext(), imageButton);

        popupMenu.getMenuInflater().inflate(R.menu.previous_connection_item_menu,
                popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener((menuItem) ->
        {
            int menu_item_id = menuItem.getItemId();
            if (menu_item_id == R.id.delete_menu_item)
            {
                ConnectionManager.getInstance().removeConnection(position);
                notifyDataSetChanged();
                return true;
            }
            else if (menu_item_id == R.id.edit_menu_item)
            {
                Connection selectedConnection = getItem(position);
                if (selectedConnection != null)
                {
                    byte[] bytes = Serializer.serialize(selectedConnection);
                    if (bytes != null)
                    {
                        Intent intent = new Intent(NewConnectionActivity.EDIT_CONNECTION_ACTION);
                        intent.putExtra(NewConnectionActivity.SERIALIZED_CONNECTION, bytes);
                        intent.putExtra(NewConnectionActivity.SELECTED_CONNECTION_POS, position);
                        ((Activity) getContext()).startActivityForResult(intent,
                                NewConnectionActivity.REQUEST_CODE);
                    }
                }
                return true;
            }
            return false;
        });

        popupMenu.show();
    }
}
