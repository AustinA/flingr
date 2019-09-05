using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Runtime.CompilerServices;
using System.Security.Permissions;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Media.Imaging;
using System.Windows.Threading;
using Windows.Data.Xml.Dom;
using Windows.UI.Notifications;

namespace Flingr
{
    // Responsible for listing the files in the specified directory
    public class FolderManager : INotifyPropertyChanged
    {
        private ObservableCollection<FlingrResource> flingrResources;
        private FileSystemWatcher watcher;
        private string directoryPath = string.Empty;

        public event PropertyChangedEventHandler PropertyChanged;

        public string DirectoryPath
        {
            get
            {
                return this.directoryPath;
            }

            set
            {
                if (value != this.directoryPath)
                {
                    this.directoryPath = value;
                    getFilesInDirectory();
                    StartFileWatcher(directoryPath);
                    NotifyPropertyChanged();
                }
            }
        }

        // This method is called by the Set accessor of each property.
        // The CallerMemberName attribute that is applied to the optional propertyName
        // parameter causes the property name of the caller to be substituted as an argument.
        private void NotifyPropertyChanged([CallerMemberName] String propertyName = "")
        {
            if (PropertyChanged != null)
            {
                PropertyChanged(this, new PropertyChangedEventArgs(propertyName));
            }
        }

        [PermissionSet(SecurityAction.Demand, Name = "FullTrust")]
        public FolderManager()
        {
            flingrResources = new ObservableCollection<FlingrResource>();
        }

        private void StartFileWatcher(string directoryPath)
        {
            watcher = new FileSystemWatcher(directoryPath);
            this.directoryPath = directoryPath;

            // Watch for changes in LastAccess and LastWrite times, and
            // the renaming of files or directories.
            watcher.NotifyFilter = NotifyFilters.LastWrite
                                 | NotifyFilters.FileName;

            // Only watch text files.
            watcher.Filter = "";

            // Add event handlers.
            watcher.Created += OnCreated;
            watcher.Deleted += OnRemoved;
            // Begin watching.
            watcher.EnableRaisingEvents = true;
        }

        private void OnCreated(object sender, FileSystemEventArgs e)
        {
            // TODO: Get the whole toast notification system working.
            //ToastNotificationManager.CreateToastNotifier().Show(toast);

            Application.Current.Dispatcher.Invoke((Action)(() =>
            {
                // TODO: When a file is created in the directory, pop up a Win10 notification.
                AddFlingrResource(new FileInfo(e.FullPath));
            }));
        }

        private void ToastActivated(ToastNotification sender, object args)
        {
            throw new NotImplementedException();
        }

        private void OnRemoved(object sender, FileSystemEventArgs e)
        {
            Application.Current.Dispatcher.Invoke((Action)(() =>
            {
                RemoveFlingrResource(new FileInfo(e.FullPath));
            }));
        }

        public ref ObservableCollection<FlingrResource> getFilesInDirectory()
        {
            flingrResources.Clear();
            FileInfo[] fileList = {};
            DirectoryInfo dir = new DirectoryInfo(directoryPath);
            if (dir != null && dir.Exists)
            {
                fileList = dir.GetFiles();
                fileList.OrderBy(f => f.CreationTimeUtc);
            }

            List <FlingrResource> resources = new List<FlingrResource>();
            foreach(FileInfo fileInfo in fileList)
            {
                AddFlingrResource(fileInfo);
            }

            return ref flingrResources;
        }

        private void AddFlingrResource(FileInfo fileInfo)
        {
            FlingrResource resource = new FlingrResource(fileInfo);

            using (System.Drawing.Icon sysicon = System.Drawing.Icon.ExtractAssociatedIcon(fileInfo.FullName))
            {
                resource.Icon = sysicon.ToBitmap();
            }

            flingrResources.Add(resource);
        }

        private void RemoveFlingrResource(FileInfo fileInfo)
        {
            FlingrResource resource = new FlingrResource(fileInfo);

            flingrResources.Remove(resource);
        }

        public static void OpenFile(FileInfo fileInfo)
        {
            System.Diagnostics.Process.Start(fileInfo.FullName);
        }
    }
}
