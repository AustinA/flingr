package flingr.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

import flingr.app.R;
import flingr.app.entities.Connection;
import flingr.app.fragments.MainContentFragment;
import flingr.app.managers.ConnectionManager;
import flingr.app.utilities.FileSharingIntentParser;
import flingr.app.utilities.Serializer;

public class ShareActivity extends AppCompatActivity
{
    private static final String MAIN_FRAG_TAG = "MainContentFragment";
    private FileSharingIntentParser.SharedFileInfo sharedFileInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        FileSharingIntentParser fileSharingIntentParser = new FileSharingIntentParser(this);

        // Initialize and configure the UI
        initializeUI();

        // TODO  Implement some sort of temporary file cleanup
        Intent intent = getIntent();
        if (intent != null)
        {
            sharedFileInfo = fileSharingIntentParser.parseIntent(intent);
        }

        ConnectionManager.getInstance().readFromPreferences(this);

        // If savedInstanceState is not null, the device was rotated.
        if (savedInstanceState == null)
        {
            MainContentFragment newFragment = MainContentFragment.newInstance(sharedFileInfo);


            getSupportFragmentManager().beginTransaction().add(R.id.fragment_content,
                    newFragment, MAIN_FRAG_TAG).commit();
        }
        else
        {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(MAIN_FRAG_TAG);
            if (fragment instanceof MainContentFragment)
            {
                // TODO This is disgusting. Make the Connections Parcelable to use bundles in the Fragment
                ((MainContentFragment) fragment).setSharedFileInfo(sharedFileInfo);
            }
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        ConnectionManager.getInstance().saveToPreferences(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == NewConnectionActivity.REQUEST_CODE)
        {
            if (resultCode == NewConnectionActivity.NEW_CONNECTION_RESULT)
            {
                if (data != null)
                {
                    byte[] serializedConnection = data.getByteArrayExtra(NewConnectionActivity.SERIALIZED_CONNECTION);
                    if (serializedConnection != null)
                    {
                        Connection connection = Serializer.deserialize(serializedConnection);
                        if (connection != null)
                        {
                            ConnectionManager.getInstance().addConnection(connection);
                            // Easy approach? Yes. Ugly? Absolutely.
                            Fragment fragment = getSupportFragmentManager().findFragmentByTag(MAIN_FRAG_TAG);
                            if (fragment instanceof MainContentFragment)
                            {
                                ((MainContentFragment) fragment).notifyDataSetChanged();
                            }
                        }
                    }
                }
            }
            else if (resultCode == NewConnectionActivity.EDIT_CONNECTION_RESULT)
            {

                if (data != null)
                {
                    byte[] serializedConnection = data.getByteArrayExtra(NewConnectionActivity.SERIALIZED_CONNECTION);
                    if (serializedConnection != null)
                    {
                        Connection connection = Serializer.deserialize(serializedConnection);
                        if (connection != null)
                        {
                            int itemEdited = data.getIntExtra(NewConnectionActivity.SELECTED_CONNECTION_POS, -1);
                            if (itemEdited != -1)
                            {
                                ConnectionManager.getInstance().removeConnection(itemEdited);
                                ConnectionManager.getInstance().addConnection(connection);
                                // Easy approach? Yes. Ugly? Absolutely.
                                Fragment fragment = getSupportFragmentManager().findFragmentByTag(MAIN_FRAG_TAG);
                                if (fragment instanceof MainContentFragment)
                                {
                                    ((MainContentFragment) fragment).notifyDataSetChanged();
                                }
                            }

                        }
                    }
                }
            }
        }
    }


    /**
     * Initialize UI for application.
     */
    private void initializeUI()
    {
        // Hide the action bar
        android.support.v7.app.ActionBar theActionBar = getSupportActionBar();
        if (theActionBar != null)
        {
            theActionBar.hide();
        }
        setContentView(R.layout.activity_share);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener((view) -> launchNewConnectionActivity());
    }

    private void launchNewConnectionActivity()
    {
        Intent intent = new Intent(NewConnectionActivity.NEW_CONNECTION_ACTION);
        startActivityForResult(intent, NewConnectionActivity.REQUEST_CODE);

    }
}
