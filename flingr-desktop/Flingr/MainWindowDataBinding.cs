using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Linq;
using System.Runtime.CompilerServices;
using System.Text;
using System.Threading.Tasks;

namespace Flingr
{
    public class MainWindowDataBinding : INotifyPropertyChanged
    {
        private string _lanIP = string.Empty;
        private string _lanPort = string.Empty;
        private string _wanIp = string.Empty;
        private string _wanPort = string.Empty;
        private string _openSshInstallLocation = string.Empty;

        public MainWindowDataBinding()
        {
        }

        public event PropertyChangedEventHandler PropertyChanged;

        public string LanIp
        {
            get
            {
                return this._lanIP;
            }

            set
            {
                if (value != this._lanIP)
                {
                    this._lanIP = value;
                    NotifyPropertyChanged();
                }
            }
        }

        public string LanPort
        {
            get
            {
                return this._lanPort;
            }

            set
            {
                if (value != this._lanPort)
                {
                    this._lanPort = value;
                    NotifyPropertyChanged();
                }
            }
        }

        public string WanIp
        {
            get
            {
                return this._wanIp;
            }

            set
            {
                if (value != this._wanIp)
                {
                    this._wanIp = value;
                    NotifyPropertyChanged();
                }
            }
        }

        public string WanPort
        {
            get
            {
                return this._wanPort;
            }

            set
            {
                if (value != this._wanPort)
                {
                    this._wanPort = value;
                    NotifyPropertyChanged();
                }
            }
        }

        public string OpenSshInstallLocation
        {
            get
            {
                return this._openSshInstallLocation;
            }

            set
            {
                if (value != this._openSshInstallLocation)
                {
                    this._openSshInstallLocation = value;
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
    }
}
