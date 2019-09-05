using System;
using System.Diagnostics;
using System.IO;
using System.Linq;

namespace Flingr
{
    public class OpenSshController
    {
        private const string UPNPC_EXECUTABLE_NAME = "upnpc-static.exe";
        private string sftpDirectory = null;
        private string localIp = null;
        private int localPort = 22;
        private string remoteIp = null;
        private int remotePort = GetRandomPortValue();
        private byte[] sshdConfigByteArray = Flingr.Properties.Resources.sshd_config;
        private string baseDir = AppDomain.CurrentDomain.BaseDirectory;
        private string sshd_config_filename = "sshd_config_flingr";

        public OpenSshController()
        {
            SetLocalIp(OpenSshController.GetLocalIPAddress());
        }

        public static int GetRandomPortValue()
        {
            Random r = new Random();
            return r.Next(1, 65535);
        }

        public static string GetLocalIPAddress()
        {
            string upnpcTempPath = Path.Combine(Path.GetTempPath(), UPNPC_EXECUTABLE_NAME);
            File.WriteAllBytes(upnpcTempPath, Flingr.Properties.Resources.upnpc_static);
            string command = upnpcTempPath + " -P";
            string output = ExecuteCmd(command);

            foreach (string line in output.Split(new[] { Environment.NewLine }, System.StringSplitOptions.RemoveEmptyEntries))
            {
                Console.WriteLine(line);
                if (line.Contains("Local LAN ip address"))
                {
                    string[] lineContents = line.Split(new[] { ' ' }, System.StringSplitOptions.RemoveEmptyEntries);
                    if (lineContents.Length == 6 && ValidateIPv4(lineContents[5]))
                    {
                        return lineContents[5];
                    }
                }
            }

            return null;
        }

        public static string LocateSshdExecutable()
        {
            string retval = null;
            string output = ExecuteCmd("sc qc sshd");
            foreach (string line in output.Split(new[] { Environment.NewLine }, System.StringSplitOptions.RemoveEmptyEntries))
            {
                if (line.Contains("BINARY_PATH_NAME"))
                {
                    foreach (string word in line.Split(new[] { ' ' }, System.StringSplitOptions.RemoveEmptyEntries))
                    {
                        if (word.Contains("OpenSSH\\sshd.exe"))
                        {
                            retval = word;
                        }
                    }
                }
            }

            return retval;
        }

        public static bool InstallOpenSshd()
        {
            bool retval = false;

            string opensshServerPackageName = null;
            const string testIfOpenSshInstalledCommand = "powershell -command \"Get-WindowsCapability -Online | ? Name -like 'OpenSSH.Server*'\"";
            string output = ExecuteCmd(testIfOpenSshInstalledCommand);

            bool installed = true;
            foreach (string line in output.Split(new[] { Environment.NewLine }, System.StringSplitOptions.RemoveEmptyEntries))
            {
                if (line.Contains("OpenSSH.Server"))
                {
                    string[] lineContents = line.Split(new[] { ' ' }, System.StringSplitOptions.RemoveEmptyEntries);
                    foreach (string word in lineContents)
                    {
                        if (word.Contains("OpenSSH.Server"))
                        {
                            opensshServerPackageName = word;
                            Console.WriteLine("OpenSSH Server Version:" + word);
                        }
                    }
                }

                // Quick test to see if already installed.
                if (line.Contains("NotPresent"))
                {
                    installed = false;
                }
            }

            if (opensshServerPackageName != null && !installed)
            {
                string installOpenSshServerCommand = "powershell -command \"Add-WindowsCapability -Online -Name " + opensshServerPackageName + "\"";
                string outputInstalledServer = ExecuteCmd(installOpenSshServerCommand);
            }

            retval = IsInstalled();

            return retval;
        }

        private static bool IsInstalled()
        {
            const string testIfOpenSshInstalledCommand = "powershell -command \"Get-WindowsCapability -Online | ? Name -like 'OpenSSH.Server*'\"";
            string output = ExecuteCmd(testIfOpenSshInstalledCommand);
            bool installed = false;
            foreach (string line in output.Split(new[] { Environment.NewLine }, System.StringSplitOptions.RemoveEmptyEntries))
            {
                if (line.Contains("State : Installed"))
                {
                    installed = true;
                }
            }

            return installed;
        }

        public bool SetLocalIp(string ip)
        {
            if (ValidateIPv4(ip))
            {
                this.localIp = ip;
                return true;
            }

            return false;
        }

        public bool SetLocalPort(int port)
        {
            if (port > 0 && port <= 65535)
            {
                this.localPort = port;
                return true;
            }

            return false;
        }

        public bool SetRemotePort(int port)
        {
            if (port > 0 && port <= 65535)
            {
                this.remotePort = port;
                return true;
            }

            return false;
        }

        public bool SetSftpDirectory(String dir)
        {
            if (Directory.Exists(dir))
            {
                sftpDirectory = dir;
                return true;
            }

            return false;
        }

        public string GetLocalIp()
        {
            return localIp;
        }

        public string GetRemoteIp()
        {
            return remoteIp;
        }

        public int GetLocalPort()
        {
            return localPort;
        }

        public int GetRemotePort()
        {
            return remotePort;
        }

        // TODO: Fix the retval situation, if we actually care...
        public bool AddPortRedirection()
        {
            bool retval = false;
            if (!string.IsNullOrEmpty(localIp) && localPort > 0 && remotePort > 0)
            {
                string upnpcTempPath = Path.Combine(Path.GetTempPath(), UPNPC_EXECUTABLE_NAME);
                File.WriteAllBytes(upnpcTempPath, Flingr.Properties.Resources.upnpc_static);
                string[] protocols = { "TCP", "UDP" };
                foreach (string protocol in protocols)
                {
                    string command = upnpcTempPath + " -a " + localIp + " " + localPort + " " + remotePort + " " + protocol;
                    string output = ExecuteCmd(command);

                    foreach (string line in output.Split(new[] { Environment.NewLine }, System.StringSplitOptions.RemoveEmptyEntries))
                    {
                        Console.WriteLine(line);
                        if (line.Contains("ExternalIPAddress"))
                        {
                            string[] lineContents = line.Split(new[] { ' ' }, System.StringSplitOptions.RemoveEmptyEntries);
                            if (lineContents.Length == 3 && ValidateIPv4(lineContents[2]))
                            {
                                remoteIp = lineContents[2];
                            }
                        }

                        if (line.Contains("external") && line.Contains(protocol + " is redirected to internal"))
                        {
                            string[] lineContents = line.Split(new[] { ' ' }, System.StringSplitOptions.RemoveEmptyEntries);
                            if (lineContents.Length == 9)
                            {
                                string[] remoteIpAndPort = lineContents[1].Split(new[] { ':' }, System.StringSplitOptions.RemoveEmptyEntries);
                                if (ValidateIPv4(remoteIpAndPort[0]) &&
                                    remoteIpAndPort[0] == remoteIp &&
                                    remoteIpAndPort[1] == remotePort.ToString())
                                {
                                    retval = true;
                                }
                                else
                                {
                                    retval = false;
                                }
                            }
                        }
                    }
                }
            }

            return retval;
        }

        public bool RemovePortRedirection()
        {
            bool retval = false;
            if (remotePort > 0)
            {
                string[] protocols = { "TCP", "UDP" };
                foreach (string protocol in protocols)
                {
                    string upnpcTempPath = Path.Combine(Path.GetTempPath(), UPNPC_EXECUTABLE_NAME);
                    File.WriteAllBytes(upnpcTempPath, Flingr.Properties.Resources.upnpc_static);
                    string output = ExecuteCmd(upnpcTempPath + " -d " + remotePort + " " + protocol);

                    if (output.Contains("UPNP_DeletePortMapping() returned : 0"))
                    {
                        retval = true;
                    }
                    else
                    {
                        retval = false;
                    }
                }
            }

            return retval;
        }

        public bool StartSshd()
        {
            string output = ExecuteCmd("net start sshd");

            return output.Contains("started successfully");
        }

        public bool StopSshd()
        {
            string output = ExecuteCmd("net stop sshd");

            return output.Contains("stopped successfully");
        }

        public void CreateSshdConfig()
        {
            bool foundListenAddress = false;
            bool foundChrootDirectory = false;
            bool foundSftpCommand = false;
            string sshd_config_fullpath = Path.Combine(baseDir, sshd_config_filename);

            try
            {
                using (StreamWriter writer = new StreamWriter(sshd_config_fullpath))
                {
                    using (StreamReader reader = new StreamReader(new MemoryStream(sshdConfigByteArray)))
                    {
                        String line;
                        while (reader.Peek() >= 0)
                        {
                            line = reader.ReadLine();

                            if (line.Contains("ListenAddress"))
                            {
                                foundListenAddress = true;
                                if (!string.IsNullOrEmpty(localIp))
                                {
                                    line = "ListenAddress " + localIp;
                                }
                            }

                            if (line.Contains("ChrootDirectory"))
                            {
                                foundChrootDirectory = true;
                                if (!string.IsNullOrEmpty(sftpDirectory))
                                {
                                    line = "ChrootDirectory " + sftpDirectory;
                                }
                            }

                            if (line.Contains("ForceCommand") && line.Contains("internal-sftp"))
                            {
                                foundSftpCommand = true;
                                if (!string.IsNullOrEmpty(sftpDirectory))
                                {
                                    line = "ForceCommand internal-sftp -d " + sftpDirectory;
                                }
                            }

                            writer.WriteLine(line);
                        }

                        // If we could not find the parameter in existing cfg, add it at the end
                        if (!foundListenAddress && !string.IsNullOrEmpty(localIp))
                        {
                            line = "ListenAddress " + localIp;
                            writer.WriteLine(line);
                        }

                        // if we could not find the parameter in existing cfg, add it at the end
                        if (!foundChrootDirectory && !string.IsNullOrEmpty(sftpDirectory))
                        {
                            line = "ChrootDirectory " + sftpDirectory;
                            writer.WriteLine(line);
                        }

                        // if we could not find the parameter in existing cfg, add it at the end
                        if (!foundSftpCommand && !string.IsNullOrEmpty(sftpDirectory))
                        {
                            line = "ForceCommand internal-sftp -d " + sftpDirectory;
                            writer.WriteLine(line);
                        }
                    }
                }
            }
            catch (Exception e)
            {
                Console.WriteLine("Could not read from sshd_config byte array: {0}", e.ToString());
            }
        }

        public void UpdateSshdConfig(string sshdExecutable)
        {
            const string changeService = "sc config sshd binpath= ";
            string sshd_config_fullpath = Path.Combine(baseDir, sshd_config_filename);
            string SSHD_CONFIG_LOCATION = " -f \\\"" + sshd_config_fullpath + "\\\"\"";

            
            if (string.IsNullOrEmpty(sshdExecutable))
            {
                sshdExecutable = OpenSshController.LocateSshdExecutable();
            }

            if (!string.IsNullOrEmpty(sshdExecutable))
            {
                string output = ExecuteCmd(changeService + sshdExecutable + SSHD_CONFIG_LOCATION);
                // TODO: check the output to make sure the command is successful
            }
        }

        private static string ExecuteCmd(string command)
        {
            Console.WriteLine("CMD.exe running command:" + command);
            ProcessStartInfo procStartInfo = new ProcessStartInfo("cmd", "/c " + command)
            {
                RedirectStandardOutput = true,
                UseShellExecute = false,
                CreateNoWindow = true
            };

            // wrap IDisposable into using (in order to release hProcess) 
            string result;

            using (Process process = new Process())
            {
                process.StartInfo = procStartInfo;
                process.Start();

                // wait until process does its work
                process.WaitForExit();

                // and only then read the result
                result = process.StandardOutput.ReadToEnd();
                Console.WriteLine(result);
            }

            return result;
        }

        private static bool ValidateIPv4(string ipString)
        {
            if (String.IsNullOrWhiteSpace(ipString))
            {
                return false;
            }

            string[] splitValues = ipString.Split('.');
            if (splitValues.Length != 4)
            {
                return false;
            }

            byte tempForParsing;

            return splitValues.All(r => byte.TryParse(r, out tempForParsing));
        }
    }
}
