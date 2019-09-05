using System;
using System.ComponentModel;
using System.Windows;

namespace Flingr
{
    public partial class App : Application
    {
        private System.Windows.Forms.NotifyIcon NotifyIcon;
        private OpenSshController controller;
        private FolderManager folderManager;
        private FileListing FileListingWindow;
        new MainWindow MainWindow;

        protected override void OnStartup(StartupEventArgs e)
        {
            base.OnStartup(e);

            controller = new OpenSshController();
            folderManager = new FolderManager();
            MainWindow = new MainWindow(ref controller, ref folderManager);
            ShowMainWindow();

            FileListingWindow = new FileListing(ref folderManager, new Action(() => ShowMainWindow()));

            NotifyIcon = new System.Windows.Forms.NotifyIcon();
            NotifyIcon.DoubleClick += (s, args) => ShowFileListingWindow();
            NotifyIcon.Icon = Flingr.Properties.Resources.FlingrIcon;
            NotifyIcon.Visible = true;

            CreateContextMenu();
        }

        private void CreateContextMenu()
        {
            NotifyIcon.ContextMenuStrip = new System.Windows.Forms.ContextMenuStrip();
            NotifyIcon.ContextMenuStrip.Items.Add("Open File Listing").Click += (s, e) => ShowFileListingWindow();
            NotifyIcon.ContextMenuStrip.Items.Add("Open Connection Window").Click += (s, e) => ShowMainWindow();
            NotifyIcon.ContextMenuStrip.Items.Add("Exit").Click += (s, e) => ExitApplication();
        }

        private void ExitApplication()
        {
            NotifyIcon.Visible = false;
            NotifyIcon.Dispose();
            FileListingWindow.ClosingForreal = true;
            FileListingWindow.Close();
            MainWindow.ClosingForreal = true;
            MainWindow.Close();

            if (controller != null)
            {
                controller.StopSshd();
                bool portRedirected = controller.RemovePortRedirection();
            }
        }

        private void ShowMainWindow()
        {
            if (MainWindow.IsVisible)
            {
                if (MainWindow.WindowState == WindowState.Minimized)
                {
                    MainWindow.WindowState = WindowState.Normal;
                }
                MainWindow.Activate();
            }
            else if (MainWindow != null)
            {
                MainWindow.Show();
            }
        }

        private void ShowFileListingWindow()
        {
            if (FileListingWindow.IsVisible)
            {
                if (FileListingWindow.WindowState == WindowState.Minimized)
                {
                    FileListingWindow.WindowState = WindowState.Normal;
                }
                FileListingWindow.Activate();
            }
            else
            {
                FileListingWindow.Show();
            }
        }      
    }
}
