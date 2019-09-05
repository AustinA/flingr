using System;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Media;

namespace Flingr
{
    public partial class MainWindow : Window
    {
        public bool ClosingForreal { get; internal set; }
        public FolderManager FolderManager { get; set; }
        public MainWindowDataBinding DataBinding { get; set; }

        private const string DEFAULT_OPENSSH_LOCATION = "C:\\WINDOWS\\System32\\OpenSSH\\sshd.exe";
        private bool AutoconfigureIpSettings = true;
        private bool connected = false;
        private string currentId = "";
        private OpenSshController controller;
        private Thread registerWithAwsThread = null;

        public MainWindow(ref OpenSshController controller, ref FolderManager FolderManager)
        {
            this.controller = controller;
            this.FolderManager = FolderManager;
            DataBinding = new MainWindowDataBinding();
            registerWithAwsThread = new Thread(RegisterWithAws);

            InitializeComponent();

            FindOrInstallOpenSshLocation();

            // This can be read in from the settings file.
            // TODO: create persistent settings file.
            this.FolderManager.DirectoryPath = Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments);

            DataBinding.LanIp = controller.GetLocalIp();
            DataBinding.LanPort = controller.GetLocalPort().ToString();
            DataBinding.WanIp = controller.GetRemoteIp();
            DataBinding.WanPort = controller.GetRemotePort().ToString();


            WanIpTextBox.IsReadOnly = true;
            WanPortTextBox.IsReadOnly = true;
            LanIpTextBox.IsReadOnly = true;
            LanPortTextBox.IsReadOnly = true;
        }

        private void FindOrInstallOpenSshLocation()
        {
            ConnectButton.IsEnabled = false;
            LogLine("Looking for OpenSSH Installation...");

            Task.Run(() =>
            {
                string sshLocation = OpenSshController.LocateSshdExecutable();

                Application.Current.Dispatcher.Invoke((Action)(() =>
                {
                    if (sshLocation == null)
                    {
                        OpenSshExecutableLocationTextBox.IsReadOnly = true;
                        DataBinding.OpenSshInstallLocation = DEFAULT_OPENSSH_LOCATION;
                        LogLine("OpenSSH is not currently installed. Flingr will install it in the following location: " + DEFAULT_OPENSSH_LOCATION);
                    }
                    else
                    {
                        OpenSshExecutableLocationTextBox.IsReadOnly = false;
                        DataBinding.OpenSshInstallLocation = sshLocation;
                        LogLine("OpenSSH is currently installed. This is where Flingr thinks it lives on the device: " + sshLocation);
                    }
                }));

                if (System.Net.NetworkInformation.NetworkInterface.GetIsNetworkAvailable())
                {
                    bool installationSuccess = OpenSshController.InstallOpenSshd();
                    LogLine("OpenSSH Installed: " + installationSuccess);
                    if (installationSuccess)
                    {
                        Application.Current.Dispatcher.Invoke((Action)(() =>
                        {
                            ConnectButton.IsEnabled = true;
                        }));
                    }
                }
                else
                {
                    LogLine("Could not install OpenSSH because network is not available");
                }
            });
        }

        ~MainWindow()
        {
            if (registerWithAwsThread != null && registerWithAwsThread.ThreadState != ThreadState.Stopped)
            {
                registerWithAwsThread.Abort();
            }
        }

        private void LogLine(string text)
        {
            Application.Current.Dispatcher.Invoke((Action)(() =>
            {
                LoggingTextBlock.Text += (text + "\r\n");
            }));
        }

        private void CancelAwsThread()
        {
            if (registerWithAwsThread != null && registerWithAwsThread.ThreadState != ThreadState.Stopped)
            {
                registerWithAwsThread.Abort();
            }
        }

        private void StartAwsThread()
        {
            if (!registerWithAwsThread.IsAlive)
            {
                CancelAwsThread();
                registerWithAwsThread = new Thread(RegisterWithAws);
                registerWithAwsThread.Start();
            }
        }

        private void LocateOpenSshFile(object sender, RoutedEventArgs e)
        {
            // Create OpenFileDialog 
            Microsoft.Win32.OpenFileDialog dlg = new Microsoft.Win32.OpenFileDialog();
            dlg.DefaultExt = ".exe";
            //dlg.Filter = "Executable Files (*.exe)";
            if (!string.IsNullOrEmpty(DataBinding.OpenSshInstallLocation))
            {
                dlg.InitialDirectory = DataBinding.OpenSshInstallLocation;
            }

            // Display OpenFileDialog by calling ShowDialog method 
            Nullable<bool> result = dlg.ShowDialog();

            // Get the selected file name and display in a TextBox 
            if (result == true)
            {
                // Open document 
                DataBinding.OpenSshInstallLocation = dlg.FileName;
            }
        }

        private void LocateLocalDirectory(object sender, RoutedEventArgs e)
        {
            using (var fbd = new System.Windows.Forms.FolderBrowserDialog())
            {
                fbd.SelectedPath = FolderManager.DirectoryPath;
                fbd.ShowNewFolderButton = true;
                System.Windows.Forms.DialogResult retVal = fbd.ShowDialog();

                if (retVal == System.Windows.Forms.DialogResult.OK && !string.IsNullOrWhiteSpace(fbd.SelectedPath))
                {
                    FolderManager.DirectoryPath = fbd.SelectedPath;
                }
            }
        }

        private void Connect(object sender, RoutedEventArgs e)
        {
            LogLine("Connecting...");

            // Before we actually do anything, all of the fields need to be uneditable at this point
            WanIpTextBox.IsReadOnly = true;
            WanPortTextBox.IsReadOnly = true;
            LanIpTextBox.IsReadOnly = true;
            LanPortTextBox.IsReadOnly = true;
            RegisterWithAwsCheckBox.IsEnabled = false;
            AutoConfigureCheckBox.IsEnabled = false;
            OpenSshExecutableLocationTextBox.IsReadOnly = true;
            LocalStorageDirectoryTextBox.IsReadOnly = true;
            LocateOpenSshFileButton.IsEnabled = false;
            LocateLocalDirectoryButton.IsEnabled = false;

            if (System.Net.NetworkInformation.NetworkInterface.GetIsNetworkAvailable())
            {
                LogLine("Network is available.");
                if (!AutoconfigureIpSettings)
                {
                    LogLine("Setting OpenSSH settings from user configured settings.");
                    controller.SetLocalIp(DataBinding.LanIp);

                    int wanPort;
                    if (Int32.TryParse(DataBinding.WanPort, out wanPort))
                    {
                        controller.SetRemotePort(wanPort);
                    }

                    int lanPort;
                    if (Int32.TryParse(DataBinding.LanPort, out lanPort))
                    {
                        controller.SetLocalPort(lanPort);
                    }
                }
                else
                {
                    controller.SetRemotePort(OpenSshController.GetRandomPortValue());
                }

                bool portRedirected = controller.AddPortRedirection();
                LogLine("Redirecting LAN IP/Port combination for WAN availability...");
                LogLine("Server WAN access available: " + portRedirected);

                bool sshdStarted = false;
                if (controller.SetSftpDirectory(FolderManager.DirectoryPath))
                {
                    LogLine("Creating OpenSSH sshd_config and starting OpenSSH service...");
                    controller.CreateSshdConfig();
                    controller.UpdateSshdConfig(DataBinding.OpenSshInstallLocation);
                    sshdStarted = controller.StartSshd();
                    LogLine("OpenSSH service started: " + sshdStarted);

                    // Fill in the WAN info for the UI
                    DataBinding.WanIp = controller.GetRemoteIp();
                    DataBinding.WanPort = controller.GetRemotePort().ToString();
                }

                if (!portRedirected && !sshdStarted)
                {
                    SetConnectionStatusLabelToDisconnected();
                    LogLine("COULD NOT CONNECT! UNABLE TO REDIRECT PORT, UNABLE TO START OPENSSH.");
                }
                if (!portRedirected && sshdStarted)
                {
                    var converter = new System.Windows.Media.BrushConverter();
                    ConnectionStatusLabel.Background = (Brush)converter.ConvertFromString("#F7f937");
                    LogLine("COULD NOT CONNECT! UNABLE TO REDIRECT PORT.");
                }

                if (portRedirected && sshdStarted)
                {
                    LogLine("Connected. Starting Registration...");
                    var converter = new System.Windows.Media.BrushConverter();
                    ConnectionStatusLabel.Background = (Brush)converter.ConvertFromString("#41F618");

                    if (RegisterWithAwsCheckBox.IsChecked.GetValueOrDefault())
                    {
                        connected = true;
                        StartAwsThread();
                    }
                }
            }
            else
            {
                LogLine("Network was not available. Please connect to a network before starting Flingr.");
            }
        }

        private void SetConnectionStatusLabelToDisconnected()
        {
            var converter = new System.Windows.Media.BrushConverter();
            ConnectionStatusLabel.Background = (Brush)converter.ConvertFromString("#FFFF0000");
            ConnectionStatusLabel.Content = "";
        }

        /*
         * This ONLY goes inside of a thread.
         */
        private void RegisterWithAws()
        {
            while(true)
            {
                string keyword = AwsRequest.RegisterWithAws(
                    controller.GetRemoteIp(),
                    controller.GetRemotePort().ToString(),
                    controller.GetLocalIp(),
                    controller.GetLocalPort().ToString());

                if (!string.IsNullOrEmpty(keyword))
                {
                    // Present the ID to the user in the GUI
                    Dispatcher.Invoke(new Action(() => {
                        currentId = keyword;
                        ConnectionStatusLabel.Content = keyword;
                    }));
                }

                Thread.Sleep(180100);
            }
        }

        private void Disconnect(object sender, RoutedEventArgs e)
        {
            LogLine("Disconnecting...");
            SetConnectionStatusLabelToDisconnected();

            Task.Run(() => DisconnectServices());

            if (AutoConfigureCheckBox.IsChecked.GetValueOrDefault())
            {
                WanPortTextBox.IsReadOnly = true;
                LanIpTextBox.IsReadOnly = true;
                LanPortTextBox.IsReadOnly = true;
            }

            RegisterWithAwsCheckBox.IsEnabled = true;
            AutoConfigureCheckBox.IsEnabled = true;
            OpenSshExecutableLocationTextBox.IsReadOnly = false;
            LocalStorageDirectoryTextBox.IsReadOnly = false;
            LocateOpenSshFileButton.IsEnabled = true;
            LocateLocalDirectoryButton.IsEnabled = true;
        }

        private void DisconnectServices()
        {
            connected = false;
            CancelAwsThread();

            if (controller != null)
            {
                controller.StopSshd();
                bool portRedirected = controller.RemovePortRedirection();
                LogLine("Removed Port Redirection: " + portRedirected);
                LogLine("Disconnected.");
            }
        }

        private void AutoConfigureCheckBoxChanged(object sender, RoutedEventArgs e)
        {
            AutoconfigureIpSettings = AutoConfigureCheckBox.IsChecked.GetValueOrDefault();
            if (AutoconfigureIpSettings)
            {
                //WanIpTextBox.IsReadOnly = true;
                if (WanPortTextBox != null)
                {
                    WanPortTextBox.IsReadOnly = true;
                }
                if (LanIpTextBox != null)
                {
                    LanIpTextBox.IsReadOnly = true;
                }
                if (LanPortTextBox != null)
                {
                    LanPortTextBox.IsReadOnly = true;
                }
            }
            else
            {
                //WanIpTextBox.IsReadOnly = false;
                if (WanPortTextBox != null)
                {
                    WanPortTextBox.IsReadOnly = false;
                }
                if (LanIpTextBox != null)
                {
                    LanIpTextBox.IsReadOnly = false;
                }
                if (LanPortTextBox != null)
                {
                    // For now SSH is forced on local port 22. I dont wanna go crazy on opening tons of ports on peoples computers.
                    //LanPortTextBox.IsReadOnly = false;
                }
            }
        }

        protected override void OnContentRendered(EventArgs e)
        {
            if (connected)
            {
                StartAwsThread();
            }
        }

        protected override void OnClosing(System.ComponentModel.CancelEventArgs e)
        {
            CancelAwsThread();

            if (!ClosingForreal)
            {
                e.Cancel = true;
                this.Hide();
            }
        }
    }
}
