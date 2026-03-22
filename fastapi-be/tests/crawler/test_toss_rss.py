import asyncio
from curl_cffi.requests import AsyncSession
from bs4 import BeautifulSoup

async def main():
    async with AsyncSession(impersonate="chrome120") as session:
        print("Fetching RSS XML...")
        resp = await session.get("https://toss.tech/rss.xml")
        print("Status:", resp.status_code)
        
        if resp.status_code == 200:
            soup = BeautifulSoup(resp.content, 'xml')
            items = soup.find_all('item')
            print(f"Total items in RSS: {len(items)}")
            if items:
                urls = [item.find('link').text for item in items if item.find('link')]
                print("--- 3 Sample URLs ---")
                for u in urls[:3]:
                    print(u)
        else:
            print("Failed to fetch RSS.")

if __name__ == "__main__":
    asyncio.run(main())
