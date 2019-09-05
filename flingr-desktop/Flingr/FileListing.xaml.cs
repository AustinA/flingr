using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Shapes;
using Windows.UI.Notifications;

namespace Flingr
{
    public partial class FileListing : Window
    {
        public bool ClosingForreal { get; internal set; }

        private FolderManager FolderManager;
        private Action showMainWindow;

        public FileListing(ref FolderManager folderManager, Action showMainWindow)
        {
            this.FolderManager = folderManager;
            this.showMainWindow = showMainWindow;
            InitializeComponent();

            // Create a new list view, add content, 
            fileListView.ItemsSource = folderManager.getFilesInDirectory();
        }

        private void FlingrResource_PreviewMouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            ListViewItem item = sender as ListViewItem;
            if (item != null && item.IsSelected)
            {
                FlingrResource resource = fileListView.SelectedItem as FlingrResource;
                if (resource != null)
                {
                    FolderManager.OpenFile(resource.FileInfo);
                }
            }
        }

        protected override void OnActivated(EventArgs e)
        {
            // If we just want it on the primary screen, use this: 
            //var desktopWorkingArea = System.Windows.Forms.Screen.PrimaryScreen.WorkingArea;
            var desktopWorkingArea = System.Windows.SystemParameters.WorkArea;
            this.WindowStartupLocation = WindowStartupLocation.Manual;
            this.Left = desktopWorkingArea.Right - this.Width;
            this.Top = desktopWorkingArea.Bottom - this.Height;
        }

        protected override void OnClosing(System.ComponentModel.CancelEventArgs e)
        {
            if (!ClosingForreal)
            {
                e.Cancel = true;
                this.Hide();
            }
        }

        private void OpenConnectionWindow(object sender, RoutedEventArgs e)
        {
            if (showMainWindow != null)
            {
                showMainWindow.Invoke();
            }
        }
    }

    
}
