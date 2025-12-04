"""
Enhanced Web Search Tool with content extraction and summarization
Performs web searches, extracts content, and provides contextual information
"""
from duckduckgo_search import DDGS
from typing import List, Dict
import requests
from bs4 import BeautifulSoup
import re
from urllib.parse import urljoin, urlparse
import logging

logger = logging.getLogger(__name__)

class WebSearchTool:
    def __init__(self, max_results: int = 5, max_content_length: int = 3000):
        self.max_results = max_results
        self.max_content_length = max_content_length

    async def search_and_extract(self, query: str) -> Dict:
        """
        Perform web search and extract content from top results
        
        Parameters:
        query (str): Search query from user
        
        Returns:
        Dict with search results and extracted content
        """
        try:
            # Perform search using DuckDuckGo
            search_results = await self._perform_search(query)
            
            if not search_results:
                return {
                    "query": query,
                    "results": [],
                    "extracted_content": "",
                    "sources": [],
                    "error": "No search results found"
                }
            
            # Extract content from top results
            extracted_content = ""
            valid_sources = []
            
            for result in search_results[:3]:  # Extract from top 3 results
                content = await self._extract_content(result.get("href", ""))
                if content:
                    extracted_content += f"\n\n--- From {result.get('title', 'Unknown')} ({result.get('href', '')}) ---\n"
                    extracted_content += content[:self.max_content_length]
                    valid_sources.append({
                        "title": result.get("title", ""),
                        "url": result.get("href", ""),
                        "snippet": result.get("body", "")
                    })
            
            return {
                "query": query,
                "results": search_results,
                "extracted_content": extracted_content,
                "sources": valid_sources,
                "summary": f"Found {len(search_results)} results for '{query}'"
            }
            
        except Exception as e:
            logger.error(f"Error in search_and_extract: {str(e)}")
            return {
                "query": query,
                "results": [],
                "extracted_content": "",
                "sources": [],
                "error": f"Search failed: {str(e)}"
            }

    async def _perform_search(self, query: str) -> List[Dict]:
        """Perform the actual web search"""
        try:
            with DDGS() as ddgs:
                results = [r for r in ddgs.text(query, max_results=self.max_results)]
                return results
        except Exception as e:
            logger.error(f"Search error: {str(e)}")
            return []

    async def _extract_content(self, url: str) -> str:
        """Extract readable content from a webpage"""
        try:
            headers = {
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
            }
            
            response = requests.get(url, headers=headers, timeout=10)
            response.raise_for_status()
            
            soup = BeautifulSoup(response.content, 'html.parser')
            
            # Remove script and style elements
            for script in soup(["script", "style", "nav", "footer", "header", "aside"]):
                script.decompose()
            
            # Get text content
            text = soup.get_text()
            
            # Clean up text
            lines = (line.strip() for line in text.splitlines())
            chunks = (phrase.strip() for line in lines for phrase in line.split("  "))
            text = ' '.join(chunk for chunk in chunks if chunk)
            
            # Remove extra whitespace
            text = re.sub(r'\s+', ' ', text).strip()
            
            return text[:self.max_content_length]
            
        except Exception as e:
            logger.error(f"Content extraction error for {url}: {str(e)}")
            return ""

# Updated web_search function for backward compatibility
async def web_search(query: str) -> List[Dict]:
    """
    Enhanced web search function with content extraction
    
    Parameters:
    query (str): Search query from user
    
    Returns:
    List of dicts with enhanced search results
    """
    search_tool = WebSearchTool()
    result = await search_tool.search_and_extract(query)
    
    if result.get("error"):
        return [{"error": result["error"]}]
    
    # Format results for backward compatibility while adding enhanced content
    enhanced_results = []
    for i, source in enumerate(result["sources"]):
        enhanced_results.append({
            "title": source["title"],
            "href": source["url"],
            "body": source["snippet"],
            "extracted_content": result["extracted_content"] if i == 0 else "",  # Only include full content once
        })
    
    return enhanced_results[:3]  # Return top 3 results

