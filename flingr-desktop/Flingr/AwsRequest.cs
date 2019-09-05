using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net;
using System.Security.Cryptography;
using System.Text;
using System.Web.Script.Serialization;

namespace Flingr
{
    class AwsRequest
    {
        private const string RegionName = "us-east-1"; //This is the regionName
        private const string ServiceName = "execute-api";
        private const string Algorithm = "AWS4-HMAC-SHA256";
        private const string ContentType = "application/json";
        private const string Host = "20d3ektd5h.execute-api.us-east-1.amazonaws.com/test"; // Link to the AWS API Gateway
        private const string SignedHeaders = "content-type;host;x-amz-date";
        private const string xApiKey = "sorry, private"; // api key (Can we store this somewhere safer? XD)
        private const string accessKey = "sorry, private"; // (Can we store this somewhere safer? XD)
        private const string secretKey = "sorry, private"; // (Can we store this somewhere safer? XD)

        public static string RegisterWithAws(string remoteIp, string remotePort, string localIp, string localPort)
        {
            string jsonRequest = new JavaScriptSerializer().Serialize(new
            {
                Item = new
                {
                    ipAddr = new { S = remoteIp },
                    wanPort = new { S = remotePort },
                    lanAddr = new { S = localIp },
                    lanPort = new { S = localPort }
                },
                TableName = "flingrMap"
            });

            WebRequest request = AwsRequest.RequestPut("/FlingrRegistration", "", jsonRequest);

            string responseJson = null;
            try
            {
                using (WebResponse response = request.GetResponse())
                {
                    StreamReader responseReader = new StreamReader(response.GetResponseStream());
                    responseJson = responseReader.ReadToEnd();
                }
            }
            catch (WebException error)
            {
                Console.WriteLine(error.Message);
                StreamReader responseReader = new StreamReader(error.Response.GetResponseStream());
                responseJson = responseReader.ReadToEnd();
            }

            AwsJsonObject jsonObj = JsonConvert.DeserializeObject<AwsJsonObject>(responseJson);

            if (jsonObj != null && jsonObj.Item != null && jsonObj.Item.id != null && jsonObj.Item.id.S != null)
            {
                return jsonObj.Item.id.S;
            }

            return null;
        }

        public static void UnregisterWithAws(string id)
        {
            string jsonRequest = new JavaScriptSerializer().Serialize(new
            {
                Key = new
                {
                    id = new { S = id }
                },
                TableName = "flingrMap"
            });

            AwsRequest.RequestPut("/FlingrRegistration", "", jsonRequest);
        }

        public static WebRequest RequestGet(string canonicalUri, string canonicalQueriString, string jsonString)
        {
            string hashedRequestPayload = CreateRequestPayload("");

            string authorization = Sign(hashedRequestPayload, "GET", canonicalUri, canonicalQueriString);
            string requestDate = DateTime.UtcNow.ToString("yyyyMMddTHHmmss") + "Z";

            WebRequest webRequest = WebRequest.Create("https://" + Host + canonicalUri + "?" + canonicalQueriString);

            webRequest.Method = "GET";
            webRequest.ContentType = ContentType;
            webRequest.Headers.Add("X-Amz-Date", requestDate);
            webRequest.Headers.Add("Authorization", authorization);
            webRequest.Headers.Add("x-amz-content-sha256", hashedRequestPayload);

            return webRequest;
        }

        public static WebRequest RequestPut(string canonicalUri, string canonicalQueryString, string jsonString)
        {
            string hashedRequestPayload = CreateRequestPayload(jsonString);

            string authorization = Sign(hashedRequestPayload, "PUT", canonicalUri, canonicalQueryString);
            string requestDate = DateTime.UtcNow.ToString("yyyyMMddTHHmmss") + "Z";

            WebRequest webRequest = WebRequest.Create("https://" + Host + canonicalUri);

            webRequest.Method = "PUT";
            webRequest.ContentType = ContentType;
            webRequest.Headers.Add("X-Amz-date", requestDate);
            webRequest.Headers.Add("Authorization", authorization);
            webRequest.Headers.Add("x-amz-content-sha256", hashedRequestPayload);
            webRequest.ContentLength = jsonString.Length;

            ASCIIEncoding encoding = new ASCIIEncoding();
            byte[] data = encoding.GetBytes(jsonString);

            Stream newStream = webRequest.GetRequestStream();
            newStream.Write(data, 0, data.Length);

            return webRequest;
        }

        public static WebRequest RequestDelete(string canonicalUri, string canonicalQueryString, string jsonString)
        {
            string hashedRequestPayload = CreateRequestPayload(jsonString);

            string authorization = Sign(hashedRequestPayload, "DELETE", canonicalUri, canonicalQueryString);
            string requestDate = DateTime.UtcNow.ToString("yyyyMMddTHHmmss") + "Z";

            WebRequest webRequest = WebRequest.Create("https://" + Host + canonicalUri);

            webRequest.Method = "DELETE";
            webRequest.ContentType = ContentType;
            webRequest.Headers.Add("X-Amz-date", requestDate);
            webRequest.Headers.Add("Authorization", authorization);
            webRequest.Headers.Add("x-amz-content-sha256", hashedRequestPayload);
            webRequest.ContentLength = jsonString.Length;

            ASCIIEncoding encoding = new ASCIIEncoding();
            byte[] data = encoding.GetBytes(jsonString);

            Stream newStream = webRequest.GetRequestStream();
            newStream.Write(data, 0, data.Length);

            return webRequest;
        }

        private static string CreateRequestPayload(string jsonString)
        {
            //Here should be JSON object of the model we are sending with POST request
            //var jsonToSerialize = new { Data = String.Empty };

            //We parse empty string to the serializer if we are makeing GET request
            //string requestPayload = new JavaScriptSerializer().Serialize(jsonToSerialize);
            string hashedRequestPayload = HexEncode(Hash(ToBytes(jsonString)));

            return hashedRequestPayload;
        }

        private static string Sign(string hashedRequestPayload, string requestMethod, string canonicalUri, string canonicalQueryString)
        {
            DateTime currentDateTime = DateTime.UtcNow;
            string dateStamp = currentDateTime.ToString("yyyyMMdd");
            string requestDate = currentDateTime.ToString("yyyyMMddTHHmmss") + "Z";
            string credentialScope = string.Format("{0}/{1}/{2}/aws4_request", dateStamp, RegionName, ServiceName);

            var headers = new SortedDictionary<string, string> {
                { "Content-Type", ContentType },
                { "Host", Host  },
                { "X-Amz-Date", requestDate }//,
                //{"x-api-key", xApiKey} // This was fucking shit up for me so I bailed. 
            };

            string canonicalHeaders = string.Join("\n", headers.Select(x => x.Key.ToLowerInvariant() + ":" + x.Value.Trim())) + "\n";

            // Task 1: Create a Canonical Request For Signature Version 4
            string canonicalRequest = requestMethod + "\n" + canonicalUri + "\n" + canonicalQueryString + "\n" + canonicalHeaders + "\n" + SignedHeaders + "\n" + hashedRequestPayload;
            string hashedCanonicalRequest = HexEncode(Hash(ToBytes(canonicalRequest)));

            // Task 2: Create a String to Sign for Signature Version 4
            string stringToSign = Algorithm + "\n" + requestDate + "\n" + credentialScope + "\n" + hashedCanonicalRequest;

            // Task 3: Calculate the AWS Signature Version 4
            byte[] signingKey = GetSignatureKey(secretKey, dateStamp, RegionName, ServiceName);
            string signature = HexEncode(HmacSha256(stringToSign, signingKey));

            // Task 4: Prepare a signed request
            // Authorization: algorithm Credential=access key ID/credential scope, SignedHeadaers=SignedHeaders, Signature=signature

            string authorization = string.Format("{0} Credential={1}/{2}/{3}/{4}/aws4_request, SignedHeaders={5}, Signature={6}",
            Algorithm, accessKey, dateStamp, RegionName, ServiceName, SignedHeaders, signature);

            return authorization;
        }

        private static byte[] GetSignatureKey(string key, string dateStamp, string regionName, string serviceName)
        {
            byte[] kDate = HmacSha256(dateStamp, ToBytes("AWS4" + key));
            byte[] kRegion = HmacSha256(regionName, kDate);
            byte[] kService = HmacSha256(serviceName, kRegion);
            return HmacSha256("aws4_request", kService);
        }

        private static byte[] ToBytes(string str)
        {
            return Encoding.UTF8.GetBytes(str.ToCharArray());
        }

        private static string HexEncode(byte[] bytes)
        {
            return BitConverter.ToString(bytes).Replace("-", string.Empty).ToLowerInvariant();
        }

        private static byte[] Hash(byte[] bytes)
        {
            return SHA256.Create().ComputeHash(bytes);
        }

        private static byte[] HmacSha256(string data, byte[] key)
        {
            return new HMACSHA256(key).ComputeHash(ToBytes(data));
        }
    }
}
