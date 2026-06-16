import urllib.request
import urllib.parse
import json

url = "http://localhost:8080/api/auth/login"
data = json.dumps({"email": "owner04@billmygym.local", "password": "password"}).encode("utf-8")
headers = {"Content-Type": "application/json"}
req = urllib.request.Request(url, data=data, headers=headers, method="POST")

try:
    response = urllib.request.urlopen(req)
    cookies = response.headers.get("Set-Cookie")
    
    url_status = "http://localhost:8080/api/subscription/status"
    req_status = urllib.request.Request(url_status, headers={"Cookie": cookies})
    try:
        res_status = urllib.request.urlopen(req_status)
        print("Status Code:", res_status.getcode())
        print("Response:", res_status.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        print("Status Error Code:", e.code)
        print("Status Error Response:", e.read().decode("utf-8"))

except urllib.error.HTTPError as e:
    print("Login Error Code:", e.code)
    print("Login Error Response:", e.read().decode("utf-8"))
except Exception as e:
    print("Exception:", e)
