package flingr.app.fragments;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import flingr.app.R;
import flingr.app.entities.Connection;
import flingr.app.services.FileUploadIntentService;
import flingr.app.ui.ConnectionAdapter;
import flingr.app.utilities.FileSharingIntentParser;


/**
 * Splash page for the entire app.  Displays a list of previously connected servers.
 */
public class MainContentFragment extends Fragment
{
    private FileSharingIntentParser.SharedFileInfo sharedFileInfo;

    private ConnectionAdapter adapter;

    public MainContentFragment()
    {
    }


    public static MainContentFragment newInstance(FileSharingIntentParser.SharedFileInfo sharedFileInfo)
    {
        MainContentFragment fragment = new MainContentFragment();
        fragment.setSharedFileInfo(sharedFileInfo);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_main_content, container, false);

        ListView previousConnectionsListView = view.findViewById(R.id.recent_servers_list_view);
        previousConnectionsListView.setNestedScrollingEnabled(true);

        adapter = new ConnectionAdapter(getActivity());

        previousConnectionsListView.setAdapter(adapter);

        TextView fileLabel = view.findViewById(R.id.file_name_label);

        String fileNameLabel;
        if (sharedFileInfo != null && !sharedFileInfo.getFileName().isEmpty())
        {
            fileNameLabel = "Sharing " + sharedFileInfo.getFileName();
        }
        else
        {
            fileNameLabel = "No file selected";
        }
        fileLabel.setText(fileNameLabel);

        previousConnectionsListView.setOnItemClickListener((parentAV, clickedView, position, id) ->
        {
            Connection selectedConnection = adapter.getItem(position);
            if (selectedConnection != null)
            {
                if (sharedFileInfo != null)
                {
                    FileUploadIntentService.startService(getActivity(), selectedConnection, sharedFileInfo.getFileUri(),
                            sharedFileInfo.getFileName());
                }
                else
                {
                    Toast.makeText(getActivity(), "No file to send", Toast.LENGTH_LONG).show();
                }
            }
        });

        return view;
    }


    public void setSharedFileInfo(FileSharingIntentParser.SharedFileInfo sharedFileInfo)
    {
        this.sharedFileInfo = sharedFileInfo;
    }

    public void notifyDataSetChanged()
    {
        if (adapter != null)
        {
            adapter.notifyDataSetChanged();
        }
    }
}
