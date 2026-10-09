import requests

url = "http://127.0.0.1:8000/api_root/Post/"

token = "YOUR_TOKEN_HERE"

headers = {
    "Authorization": f"Token {token}"
}

data = {
    "author": 1,
    "title": "python requests test",
    "text": "created by Python requests",
    "created_date": "2026-09-22T19:40:00+09:00",
    "published_date": "2026-09-22T19:40:00+09:00",
}

response = requests.post(
    url,
    headers=headers,
    data=data
)

print("status code:", response.status_code)
print("response:", response.text)