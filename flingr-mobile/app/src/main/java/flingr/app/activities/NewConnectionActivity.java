package flingr.app.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import flingr.app.R;
import flingr.app.entities.Connection;
import flingr.app.remote.QueryServerCallback;
import flingr.app.remote.QueryServerTask;
import flingr.app.utilities.Serializer;
import timber.log.Timber;

/**
 * A wizard-like Activity that edits, creates, and queries the Flingr servers for Connection
 * information.
 */
public class NewConnectionActivity extends AppCompatActivity implements QueryServerCallback
{
    public static final String NEW_CONNECTION_ACTION = "app.flingr.NewConnectionAction";
    public static final String EDIT_CONNECTION_ACTION = "app.flingr.EditConnectionAction";

    private static final String IP_SAVED = "IPSaved";
    private static final String PORT_SAVED = "PortSaved";

    private static final String WAN_IP_SAVED = "WANIPSaved";
    private static final String WAN_PORT_SAVED = "WANPortSaved";

    private static final String ACTIVATION_CODE_SAVED = "NumericSaved";
    private static final String NAME_SAVED = "NameSaved";
    private static final String USERNAME_SAVED = "USERNAMESAVED";
    private static final String PASSWORD_SAVED = "PASSWORDSAVED";
    private static final String CURRENT_UI_STATE = "UISTATE";

    public static final int REQUEST_CODE = 9031;
    public static final int CANCEL_RESULT = 9032;
    public static final int NEW_CONNECTION_RESULT = 9033;
    public static final int EDIT_CONNECTION_RESULT = 9034;
    public static final String SELECTED_CONNECTION_POS = "SelectedConnectionAdapter";
    public static final String SERIALIZED_CONNECTION = "NewConnectionBlob";

    private LinearLayout activationCodeLayout;
    private EditText activationCodeEditText;

    private LinearLayout progressBarContentLayout;
    private LinearLayout couldNotConnectLayout;

    private LinearLayout additionalInfoLayout;
    private LinearLayout editIpContainer;
    private Button editIPButton;

    private TextView summaryTextView;
    private TextView summaryIPTextView;
    private EditText nameOfDevice;
    private EditText userName;
    private EditText password;

    private Button nextButton;
    private Button backButton;

    private EditText ipEditText;
    private EditText portEditText;

    private EditText ipWANEditText;
    private EditText portWANEditText;

    private VisualStates currentVisualState;

    private QueryServerTask serverTask;

    private boolean isEditingConnection = false;
    private int editedItemPos = -1;

    @Override
    public void onQueryCompleted(Connection response)
    {
        if (Looper.getMainLooper().isCurrentThread())
        {
            processServerResults(response);
        }
        else
        {
            runOnUiThread(()->processServerResults(response));
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        initializeUI();

        // Handle Connection being edited
        Intent intent = getIntent();
        if (intent != null)
        {
            if (intent.hasExtra(NewConnectionActivity.SERIALIZED_CONNECTION)
                && intent.hasExtra(NewConnectionActivity.SELECTED_CONNECTION_POS))
            {
                byte[] serializedConnection = intent.getByteArrayExtra(SERIALIZED_CONNECTION);
                if (serializedConnection != null)
                {
                    Connection connection = Serializer.deserialize(serializedConnection);
                    if (connection != null)
                    {
                        ipEditText.setText(connection.getLocalAddress());
                        portEditText.setText(String.valueOf(connection.getLocalPort()));

                        ipWANEditText.setText(connection.getWanAddress());
                        portWANEditText.setText(String.valueOf(connection.getWanPort()));

                        setIPPortSummaryLabel();

                        summaryTextView.setText(connection.getActivationCode());
                        activationCodeEditText.setText(connection.getActivationCode());

                        userName.setText(connection.getUserName());
                        password.setText(connection.getUserPassword());

                        nameOfDevice.setText(connection.getColloquialName());

                        isEditingConnection = true;
                    }
                }
                editedItemPos = intent.getIntExtra(SELECTED_CONNECTION_POS, -1);

            }
        }
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState)
    {
        super.onSaveInstanceState(outState);

        outState.putString(IP_SAVED, ipEditText.getText().toString());
        outState.putString(PORT_SAVED, portEditText.getText().toString());
        outState.putString(WAN_IP_SAVED, ipWANEditText.getText().toString());
        outState.putString(WAN_PORT_SAVED, portWANEditText.getText().toString());
        outState.putString(ACTIVATION_CODE_SAVED, activationCodeEditText.getText().toString());
        outState.putString(USERNAME_SAVED, userName.getText().toString());
        outState.putString(PASSWORD_SAVED, password.getText().toString());
        outState.putString(NAME_SAVED, nameOfDevice.getText().toString());

        outState.putInt(CURRENT_UI_STATE, currentVisualState.toIndex());

    }

    @Override
    protected void onRestoreInstanceState(final Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);

        String ip = savedInstanceState.getString(IP_SAVED, "");
        String port = savedInstanceState.getString(PORT_SAVED, "");

        String wanIp = savedInstanceState.getString(WAN_IP_SAVED, "");
        String wanPort = savedInstanceState.getString(WAN_PORT_SAVED, "");

        ipEditText.setText(ip);
        portEditText.setText(port);

        ipWANEditText.setText(wanIp);
        portWANEditText.setText(wanPort);

        setIPPortSummaryLabel();

        String activationCode = savedInstanceState.getString(ACTIVATION_CODE_SAVED, "");
        summaryTextView.setText(activationCode);
        activationCodeEditText.setText(activationCode);
        userName.setText(savedInstanceState.getString(USERNAME_SAVED, ""));
        password.setText(savedInstanceState.getString(PASSWORD_SAVED, ""));

        VisualStates currentVisState = VisualStates.getState(
                savedInstanceState.getInt(CURRENT_UI_STATE, 0));

        // TODO Verify activation code format
        if (currentVisState == VisualStates.VERIFYING_CODE &&
            !activationCode.isEmpty())
        {
            setToVerifyingCredentials();
        }
        else
        {
            setVisualState(currentVisState);
        }

    }

    /**
     * Initializes the UI.
     */
    private void initializeUI()
    {
        android.support.v7.app.ActionBar theActionBar = getSupportActionBar();
        if (theActionBar != null)
        {
            theActionBar.hide();
        }

        setContentView(R.layout.activity_new_connection);

        nextButton = findViewById(R.id.nextButton);
        nextButton.setOnClickListener((v) -> nextPressed());
        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener((v) -> backPressed());

        activationCodeLayout = findViewById(R.id.activationCodeLayout);
        activationCodeEditText = findViewById(R.id.activationCodeEditText);
        progressBarContentLayout = findViewById(R.id.progressBarContent);
        couldNotConnectLayout = findViewById(R.id.couldNotContactLinearLayout);
        additionalInfoLayout = findViewById(R.id.additionalInfoLinearLayout);
        editIPButton = findViewById(R.id.editIPButton);
        summaryTextView = findViewById(R.id.summaryTextView);
        summaryIPTextView = findViewById(R.id.ipPortSummaryTextView);
        nameOfDevice = findViewById(R.id.nameEditText);
        userName = findViewById(R.id.userNameEditText);
        password = findViewById(R.id.passwordEditText);

        ipEditText = findViewById(R.id.ipEditText);
        portEditText = findViewById(R.id.portEditText);

        editIpContainer = findViewById(R.id.ipEditContainer);

        ipWANEditText = findViewById(R.id.ipWANEditText);
        portWANEditText = findViewById(R.id.portWANEditText);

        editIPButton.setOnClickListener((v) -> toggleEditIP());


        setVisualState(VisualStates.PROVIDING_CODE);

    }

    /**
     * Visual state control for toggling between the IP and port summary and EditTexts to edit them
     */
    @SuppressLint("SetTextI18n")
    private void toggleEditIP()
    {
        if (summaryIPTextView.getVisibility() == View.VISIBLE)
        {
            summaryIPTextView.setVisibility(View.GONE);

            editIpContainer.setVisibility(View.VISIBLE);

            editIPButton.setText("Done");
        }
        else
        {
            summaryIPTextView.setVisibility(View.VISIBLE);
            editIPButton.setText("Edit");

            editIpContainer.setVisibility(View.GONE);

            summaryIPTextView.setSelected(true);

            setIPPortSummaryLabel();
        }
    }

    /**
     * Signal for the user experience state machine. This is always executed on the main thread.
     *
     * @param newVisualState The new visual state to set the UI to.
     */
    private void setVisualState(VisualStates newVisualState)
    {
        if (Looper.getMainLooper().isCurrentThread())
        {
            setState(newVisualState);
        }
        else
        {
            runOnUiThread(()->setState(newVisualState));
        }
}

    /**
     * Method to set the user experience. Should only be called from the scope of setVisualState().
     *
     * @param newVisualState The new visual state to set the UI to.
     */
    @SuppressLint("SetTextI18n")
    private void setState(VisualStates newVisualState)
    {
        currentVisualState = newVisualState;
        if(newVisualState == VisualStates.PROVIDING_CODE)
        {
            additionalInfoLayout.setVisibility(View.GONE);

            progressBarContentLayout.setVisibility(View.GONE);
            couldNotConnectLayout.setVisibility(View.GONE);

            activationCodeLayout.setVisibility(View.VISIBLE);

            backButton.setText("Cancel");
            nextButton.setEnabled(true);
            nextButton.setText("Next");

        }
        else if(newVisualState == VisualStates.VERIFYING_CODE)
        {
            additionalInfoLayout.setVisibility(View.GONE);

            progressBarContentLayout.setVisibility(View.VISIBLE);
            couldNotConnectLayout.setVisibility(View.GONE);

            activationCodeLayout.setVisibility(View.VISIBLE);

            backButton.setText("Back");
            nextButton.setText("Next");

            nextButton.setEnabled(false);


        }
        else if(newVisualState == VisualStates.CONNECTION_ISSUE)
        {
            additionalInfoLayout.setVisibility(View.GONE);

            progressBarContentLayout.setVisibility(View.GONE);
            couldNotConnectLayout.setVisibility(View.VISIBLE);

            activationCodeLayout.setVisibility(View.VISIBLE);

            backButton.setText("Back");
            nextButton.setText("Next");

            nextButton.setEnabled(true);

        }
        else if(newVisualState == VisualStates.PROVIDING_CREDENTIALS)
        {
            additionalInfoLayout.setVisibility(View.VISIBLE);

            progressBarContentLayout.setVisibility(View.GONE);
            couldNotConnectLayout.setVisibility(View.GONE);

            activationCodeLayout.setVisibility(View.GONE);

            summaryTextView.setText(activationCodeEditText.getText().toString());

            backButton.setText("Back");
            nextButton.setEnabled(true);
            nextButton.setText("Save");

            summaryIPTextView.setVisibility(View.VISIBLE);
            summaryIPTextView.setSelected(true);
            editIPButton.setText("Edit");

            editIpContainer.setVisibility(View.GONE);

            setIPPortSummaryLabel();

        }
        else
        {
            Timber.e("UI state not recognized!");
        }

    }

    /**
     * Generates and sets the IP/port summary text label.
     */
    private void setIPPortSummaryLabel()
    {
        String displayText;
        if (!ipEditText.getText().toString().isEmpty()
                && !portEditText.getText().toString().isEmpty())
        {
            displayText = "WAN:  " + ipWANEditText.getText().toString()
                    + ":" + portWANEditText.getText().toString();

            displayText += "  |  LAN:  " + ipEditText.getText().toString()
                    + ":" + portEditText.getText().toString();
        }
        else
        {
            displayText = "IP and port required";
        }
        summaryIPTextView.setText(displayText);
    }

    /**
     * Called when the next button is pressed. Moves the wizard to the next "page".
     */
    private void nextPressed()
    {
        closeKeyboard();
        if (currentVisualState == VisualStates.PROVIDING_CODE)
        {
            if (!activationCodeEditText.getText().toString().isEmpty())
            {
                setToVerifyingCredentials();
            }
            else
            {
                Toast.makeText(this, "An activation code must be provided",
                        Toast.LENGTH_LONG).show();
            }
        }
        else if (currentVisualState == VisualStates.CONNECTION_ISSUE)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Connection Issue");
            builder.setMessage("Connection could not be established." +
                    "  Would you like to continue with the setup?");
            builder.setPositiveButton("Yes", (dialog, which) -> setVisualState(VisualStates.PROVIDING_CREDENTIALS));
            builder.setNeutralButton("Retry", (dialog, which) -> setToVerifyingCredentials());
            builder.setNegativeButton("Cancel", null);

            builder.create().show();
        }
        else if (currentVisualState == VisualStates.PROVIDING_CREDENTIALS)
        {
            Connection connection = verifyInput();
            if (connection != null)
            {
                byte[] serialized = Serializer.serialize(connection);
                if (serialized != null)
                {
                    Intent intent = new Intent();
                    intent.putExtra(SERIALIZED_CONNECTION, serialized);

                    if (editedItemPos != -1)
                    {
                        intent.putExtra(SELECTED_CONNECTION_POS, editedItemPos);
                    }

                    setResult((isEditingConnection)
                            ? EDIT_CONNECTION_RESULT : NEW_CONNECTION_RESULT, intent);
                    finish();
                }
                else
                {
                    Toast.makeText(this, "An unknown error occurred",
                            Toast.LENGTH_LONG).show();
                }
            }
            else
            {
                Toast.makeText(this, "Could not save connection. Check parameters",
                        Toast.LENGTH_LONG).show();
            }
        }
        else
        {
            Timber.e("UI state not recognized!");
        }
    }

    /**
     * Called when the back button is pressed.  Moves the wizard to the last "page".
     */
    private void backPressed()
    {
        closeKeyboard();
        if (currentVisualState == VisualStates.PROVIDING_CODE)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Confirm Exit");
            builder.setMessage("Are you sure you want to exit?");
            builder.setPositiveButton("Yes", (dialog, which) ->
            {
                setResult(CANCEL_RESULT);
                finish();
            });
            builder.setNegativeButton("Cancel", null);

            builder.create().show();
        }
        else if (currentVisualState == VisualStates.VERIFYING_CODE)
        {
            if (serverTask != null)
            {
                serverTask.cancel(true);
            }
            setVisualState(VisualStates.PROVIDING_CODE);
        }
        else if (currentVisualState == VisualStates.CONNECTION_ISSUE)
        {
            setVisualState(VisualStates.PROVIDING_CODE);
        }
        else if (currentVisualState == VisualStates.PROVIDING_CREDENTIALS)
        {
            setVisualState(VisualStates.PROVIDING_CODE);
        }
        else
        {
            Timber.e("UI state not recognized!");
        }
    }

    /**
     * Verifies ability of the program to generate a Connection from the user input.
     *
     * @return The generated object if successful, null if unsuccessful.
     */
    private Connection verifyInput()
    {
        String colloquialName = nameOfDevice.getText().toString();
        String activationCode = activationCodeEditText.getText().toString();
        String un = userName.getText().toString();
        String pw = password.getText().toString();

        String port = portEditText.getText().toString();
        String ip = ipEditText.getText().toString();

        String wanPort = portWANEditText.getText().toString();
        String wanIp = ipWANEditText.getText().toString();

        if (!activationCode.isEmpty())
        {
            if (!colloquialName.isEmpty())
            {
                Integer convertedPort;
                try
                {
                    convertedPort = Integer.valueOf(port);
                }
                catch (NumberFormatException e)
                {
                    convertedPort = null;
                }

                if (convertedPort != null)
                {
                    if (!ip.isEmpty())
                    {
                        Integer convertedWANPort;
                        boolean wanVerified = false;
                        try
                        {
                            convertedWANPort = Integer.valueOf(wanPort);
                        }
                        catch (NumberFormatException e)
                        {
                            convertedWANPort = null;
                        }

                        if (convertedWANPort != null)
                        {
                            if (!wanIp.isEmpty())
                            {
                                if (Patterns.IP_ADDRESS.matcher(wanIp).matches())
                                {
                                    wanVerified = true;
                                }
                            }
                        }

                        if (Patterns.IP_ADDRESS.matcher(ip).matches() && wanVerified)
                        {
                            if (!un.isEmpty() && !pw.isEmpty())
                            {
                                Connection connection = new Connection(activationCode, colloquialName,
                                        wanIp, convertedWANPort, un, pw);

                                connection.setLocalPort(convertedPort);
                                connection.setLocalAddress(ip);

                                return connection;
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Processes HTTP query results from the Flingr lambda service.
     *
     * @param response The response from the Flingr lambda service.
     */
    private void processServerResults(Connection response)
    {
        if (response != null && response.isValid())
        {
            if (response.getLocalAddress() != null && response.getLocalPort() != null)
            {
                ipEditText.setText(response.getLocalAddress());
                portEditText.setText(String.valueOf(response.getWanPort()));
            }

            if (response.getWanAddress() != null && response.getWanPort() != null)
            {
                ipWANEditText.setText(response.getWanAddress());
                portWANEditText.setText(String.valueOf(response.getWanPort()));
            }

            setIPPortSummaryLabel();

            setVisualState(VisualStates.PROVIDING_CREDENTIALS);
        }
        else
        {
            setVisualState(VisualStates.CONNECTION_ISSUE);
        }
    }


    /**
     * Controls query asynchronous task, and sets the visual state for querying the lambda service.
     */
    private void setToVerifyingCredentials()
    {
        setVisualState(VisualStates.VERIFYING_CODE);
        if (serverTask != null)
        {
            serverTask.cancel(true);
        }
        serverTask = new QueryServerTask(this);
        serverTask.execute(activationCodeEditText.getText().toString());
    }

    /**
     * Possible visual states of this activity.
     */
    private enum VisualStates
    {

        PROVIDING_CODE(0),
        VERIFYING_CODE(1),
        CONNECTION_ISSUE(2),
        PROVIDING_CREDENTIALS(3);

        VisualStates(int i)
        {
            index = i;
        }

        protected int toIndex()
        {
            return index;
        }

        protected static VisualStates getState(int i)
        {
            for (VisualStates state : values())
            {
                if (state.index == i)
                {
                    return state;
                }
            }
            return null;
        }

        private int index;
    }

    private void closeKeyboard()
    {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm.isActive())
        {
            imm.hideSoftInputFromWindow(backButton.getWindowToken(), 0); // hide
        }
    }
}
