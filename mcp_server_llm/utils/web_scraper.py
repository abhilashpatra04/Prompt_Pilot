from charset_normalizer import detect
import requests
from bs4 import BeautifulSoup

from config import OPENROUTER_API_KEY

SERPAPI_KEY = "05d01ba12f2017637397d9519b0b8fbe01d22c752aaf185e1de65e4217d0f4af"

def fetch_links_serpapi(query, count=3):
    try:
        response = requests.get("https://serpapi.com/search", params={
            "engine": "google",
            "q": query,
            "api_key": SERPAPI_KEY
        }, timeout=10)

        data = response.json()
        organic_results = data.get("organic_results", [])

        links = []
        for item in organic_results:
            link = item.get("link")
            if link and link.startswith("http"):
                links.append(link)
            if len(links) >= count:
                break

        return links or ["❌ No valid links found."]
    except Exception as e:
        return [f"Error fetching links: {str(e)}"]


def scrape_and_summarize_from_topic(topic):
    urls = fetch_links_serpapi(topic)
    combined_text = ""

    for url in urls:
        try:
            page = requests.get(url, timeout=5)
            soup = BeautifulSoup(page.content, 'html.parser')
            text = soup.get_text(separator=' ', strip=True)

            if detect(text) == "en":
                combined_text += f"Content from {url}:\n{text[:2000]}\n\n"
            else:
                combined_text += f"[Skipped non-English content from {url}]\n\n"

        except Exception as e:
            combined_text += f"[Error scraping {url}: {str(e)}]\n\n"

    if not combined_text.strip():
        return {
            "summary": "❌ Could not extract usable content to summarize.",
            "sources": urls
        }

    try:
        response = requests.post(
            "https://openrouter.ai/api/v1/chat/completions",
            headers={
                "Authorization": f"Bearer {OPENROUTER_API_KEY}",
                "Content-Type": "application/json"
            },
            json={
                "model": "meta-llama/llama-4-maverick:free",
                "messages": [
                    {"role": "system", "content": "You are a helpful assistant. Answer the user’s question followed by summary based only on the following web content."},
                    {"role": "user", "content": f"{combined_text}"}
]

            }
        )
        result = response.json()
        summary = result["choices"][0]["message"]["content"]
        return {
            "summary": summary,
            "sources": urls
        }

    except Exception as e:
        return {
            "summary": f"Error during summarization: {str(e)}",
            "sources": urls
        }
