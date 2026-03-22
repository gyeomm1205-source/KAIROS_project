import asyncio
from curl_cffi.requests import AsyncSession
from bs4 import BeautifulSoup

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
}

async def main():
    url = "https://techblog.woowahan.com/2684/"
    async with AsyncSession(impersonate="chrome120") as session:
        resp = await session.get(url, headers=HEADERS)
        soup = BeautifulSoup(resp.text, "lxml")
        
        title_tag = soup.find("title")
        print("TITLE TAG:", title_tag.get_text() if title_tag else "None")
        
        h1 = soup.find("h1")
        print("H1 TAG:", h1.get_text() if h1 else "None")
        
        h2 = soup.find("h2")
        print("H2 TAG:", h2.get_text() if h2 else "None")
        
if __name__ == "__main__":
    asyncio.run(main())
