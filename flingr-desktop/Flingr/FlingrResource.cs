using System;
using System.Collections.Generic;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Flingr
{
    public class FlingrResource
    {
        public FileInfo FileInfo { get; set; }
        public string Name { get; set; }
        public Bitmap Icon { get; set; }
        public string Data { get; set; }

        public FlingrResource(FileInfo fileInfo)
        {
            if (fileInfo != null)
            {
                this.FileInfo = fileInfo;
                this.Name = this.FileInfo.Name;
                this.Data = "";
            }
        }

        public override bool Equals(object obj)
        {
            // STEP 1: Check for null
            if (obj == null)
            {
                return false;
            }

            // STEP 3: equivalent data types
            if (this.GetType() != obj.GetType())
            {
                return false;
            }
            return Equals((FlingrResource)obj);
        }

        public bool Equals(FlingrResource obj)
        {
            // STEP 1: Check for null if nullable (e.g., a reference type)
            if (obj == null)
            {
                return false;
            }
            // STEP 2: Check for ReferenceEquals if this is a reference type
            if (ReferenceEquals(this, obj))
            {
                return true;
            }
            // STEP 4: Possibly check for equivalent hash codes
            if (this.GetHashCode() != obj.GetHashCode())
            {
                return false;
            }
            // STEP 5: Check base.Equals if base overrides Equals()
            System.Diagnostics.Debug.Assert(
                base.GetType() != typeof(object));

            if (!base.Equals(obj))
            {
                return false;
            }

            // STEP 6: Compare identifying fields for equality.
            return ((this.FileInfo.FullName.Equals(obj.FileInfo.FullName)) && 
                    (this.Name.Equals(obj.Name)) &&
                    (this.Data.Equals(obj.Data)));
        }

        public override int GetHashCode()
        {
            return ((FileInfo.FullName.GetHashCode() ^ Name.GetHashCode()) ^ Data.GetHashCode());
        }
    }
}
