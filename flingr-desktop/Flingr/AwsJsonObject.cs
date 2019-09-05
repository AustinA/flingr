using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Flingr
{
    public class LanPort
    {
        public string S { get; set; }
    }

    public class WanPort
    {
        public string S { get; set; }
    }

    public class IpAddr
    {
        public string S { get; set; }
    }

    public class LanAddr
    {
        public string S { get; set; }
    }

    public class Timestamp
    {
        public string S { get; set; }
    }

    public class Id
    {
        public string S { get; set; }
    }

    public class Item
    {
        public LanPort lanPort { get; set; }
        public WanPort wanPort { get; set; }
        public IpAddr ipAddr { get; set; }
        public LanAddr lanAddr { get; set; }
        public Timestamp timestamp { get; set; }
        public Id id { get; set; }
    }

    public class HTTPHeaders
    {
        [JsonProperty("x-amzn-requestid")]
        public string xAmznRequestid { get; set; }
        [JsonProperty("content-length")]
        public string contentLength { get; set; }
        public string server { get; set; }
        public string connection { get; set; }
        [JsonProperty("x-amz-crc32")]
        public string xAmzCrc32 { get; set; }
        public string date { get; set; }
        [JsonProperty("content-type")]
        public string contentType { get; set; }
    }

    public class ResponseMetadata
    {
        public int RetryAttempts { get; set; }
        public int HTTPStatusCode { get; set; }
        public string RequestId { get; set; }
        public HTTPHeaders HTTPHeaders { get; set; }
    }

    public class AwsJsonObject
    {
        public Item Item { get; set; }
        public ResponseMetadata ResponseMetadata { get; set; }
    }
}