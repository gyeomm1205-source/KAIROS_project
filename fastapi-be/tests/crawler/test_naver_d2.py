import json
import urllib.request
from urllib.error import URLError, HTTPError

def main():
    api_url = "https://d2.naver.com/api/v1/contents?categoryId=2&page=0&size=5"
    print(f"Fetching API: {api_url}")
    
    req = urllib.request.Request(
        api_url, 
        headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'}
    )
    
    try:
        with urllib.request.urlopen(req) as response:
            status_code = response.getcode()
            print(f"Status: {status_code}")
            
            data = json.loads(response.read().decode('utf-8'))
            print("\n--- JSON Data Structure ---")
            print("Root keys:", data.keys())
            
            if 'content' in data:
                contents = data['content']
                print(f"Total items in 'content' array: {len(contents)}")
                if contents:
                    first_item = contents[0]
                    print("\nFirst item keys:", first_item.keys())
                    print("Sample URL:", first_item.get('url', 'N/A'))
                    print("Sample Title:", first_item.get('postTitle', first_item.get('title', 'N/A')))
                    print("Sample Post Published At:", first_item.get('postPublishedAt', 'N/A'))
    except HTTPError as e:
        print(f"HTTP Error: {e.code}")
    except URLError as e:
        print(f"URL Error: {e.reason}")

if __name__ == "__main__":
    main()
