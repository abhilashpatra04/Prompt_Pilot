import mimetypes
import requests
import base64
import os
import json
import asyncio
from typing import AsyncGenerator

from google import genai
from utils.context_utils import extract_text_from_image, extract_text_from_pdf

# Load .env if present (optional, but recommended)
from dotenv import load_dotenv
load_dotenv()

GEMINI_IMAGE_MODELS = [
    "gemini-2.5-pro",
    "gemini-2.5-flash",
    "gemini-2.5-flash-lite-preview-06-17",
    "gemini-2.0-flash",
    "gemini-2.0-flash-lite",
    "gemini-1.5-flash"
]
Groq_MODELS = [
    "groq",
    "qwen/qwen3-32b",
]

OPENROUTER_API_KEY = os.getenv("OPENROUTER_API_KEY", "")
GROQ_API_KEY = os.getenv("GROQ_API_KEY", "")
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")

if not GEMINI_API_KEY or GEMINI_API_KEY == "YOUR_GEMINI_API_KEY":
    raise ValueError("GEMINI_API_KEY environment variable is not set or is invalid. Please set it in your environment or .env file.")

client = genai.Client(api_key=GEMINI_API_KEY)

# Default free model for users without API keys
# This uses your server's Gemini API key but may be slower due to rate limits
DEFAULT_FREE_MODEL = "gemini-1.5-flash"  # Fast and free model
DEFAULT_FREE_PROVIDER = "gemini"

def get_api_key_for_model(model: str, user_api_keys: dict = None) -> str:
    """
    Returns the appropriate API key for the given model.
    Uses user's API key if provided, otherwise falls back to server's key.
    
    Args:
        model: The model name (e.g., "gemini-1.5-pro", "groq", "anthropic/claude")
        user_api_keys: Optional dict with user's API keys {"gemini": "key", "groq": "key", "openrouter": "key"}
    
    Returns:
        The API key to use for this model
    """
    model_lower = model.lower()
    
    # Determine provider based on model name
    if model_lower.startswith("gemini") or model_lower in ["gemini-2.5-pro", "gemini-2.5-flash", "gemini-1.5-flash", "gemini-1.5-pro", "gemini-2.0-flash"]:
        provider = "gemini"
        server_key = GEMINI_API_KEY
    elif model_lower in ["groq", "qwen/qwen3-32b"] or model_lower.startswith(("llama", "mixtral", "gemma")):
        provider = "groq"
        server_key = GROQ_API_KEY
    else:
        # Default to OpenRouter for other models
        provider = "openrouter"
        server_key = OPENROUTER_API_KEY
    
    # Use user's key if provided, otherwise use server's key
    if user_api_keys and provider in user_api_keys and user_api_keys[provider]:
        return user_api_keys[provider]
    
    return server_key


def get_openrouter_response(model: str, messages: list, api_key: str = None) -> str:
    """Get response from OpenRouter API with optional user API key"""
    key = api_key or OPENROUTER_API_KEY
    url = "https://openrouter.ai/api/v1/chat/completions"
    headers = {
        "Authorization": f"Bearer {key}",
        "Content-Type": "application/json"
    }
    data = {
        "model": model,
        "messages": messages
    }
    res = requests.post(url, json=data, headers=headers)
    if not res.ok:
        raise Exception(f"OpenRouter Error: {res.text}")
    return res.json()["choices"][0]["message"]["content"]

async def get_openrouter_streaming_response(model: str, messages: list, api_key: str = None) -> AsyncGenerator[str, None]:
    """Get streaming response from OpenRouter API with optional user API key"""
    key = api_key or OPENROUTER_API_KEY
    url = "https://openrouter.ai/api/v1/chat/completions"
    headers = {
        "Authorization": f"Bearer {key}",
        "Content-Type": "application/json"
    }
    data = {
        "model": model,
        "messages": messages,
        "stream": True
    }
    
    try:
        with requests.post(url, json=data, headers=headers, stream=True) as response:
            response.raise_for_status()
            for line in response.iter_lines():
                if line:
                    line = line.decode('utf-8')
                    if line.startswith('data: '):
                        json_str = line[6:]  # Remove 'data: ' prefix
                        if json_str.strip() == '[DONE]':
                            break
                        try:
                            data = json.loads(json_str)
                            if 'choices' in data and len(data['choices']) > 0:
                                delta = data['choices'][0].get('delta', {})
                                content = delta.get('content', '')
                                if content:
                                    yield content
                        except json.JSONDecodeError:
                            continue
    except Exception as e:
        yield f"Error in streaming: {str(e)}"

def call_groq_api(messages, model, api_key: str = None):
    """Call Groq API with optional user API key"""
    key = api_key or GROQ_API_KEY
    url = "https://api.groq.com/openai/v1/chat/completions"
    headers = {
        "Authorization": f"Bearer {key}",
        "Content-Type": "application/json"
    }
    data = {
        "model": model,
        "messages": messages
    }
    response = requests.post(url, json=data, headers=headers)
    response.raise_for_status()
    return response.json()["choices"][0]["message"]["content"]

async def call_groq_streaming_api(messages, model, api_key: str = None) -> AsyncGenerator[str, None]:
    """Get streaming response from Groq API with optional user API key"""
    key = api_key or GROQ_API_KEY
    url = "https://api.groq.com/openai/v1/chat/completions"
    headers = {
        "Authorization": f"Bearer {key}",
        "Content-Type": "application/json"
    }
    data = {
        "model": model,
        "messages": messages,
        "stream": True
    }
    
    try:
        with requests.post(url, json=data, headers=headers, stream=True) as response:
            response.raise_for_status()
            for line in response.iter_lines():
                if line:
                    line = line.decode('utf-8')
                    if line.startswith('data: '):
                        json_str = line[6:]  # Remove 'data: ' prefix
                        if json_str.strip() == '[DONE]':
                            break
                        try:
                            data = json.loads(json_str)
                            if 'choices' in data and len(data['choices']) > 0:
                                delta = data['choices'][0].get('delta', {})
                                content = delta.get('content', '')
                                if content:
                                    yield content
                        except json.JSONDecodeError:
                            continue
    except Exception as e:
        yield f"Error in streaming: {str(e)}"

import os
from PyPDF2 import PdfReader

def upload_file_to_gemini(file_url):
    response = requests.get(file_url)
    filename = file_url.split("/")[-1]
    with open(filename, "wb") as f:
        f.write(response.content)
    # Check file size
    print("Downloaded file size:", os.path.getsize(filename))
    # Check PDF page count
    try:
        reader = PdfReader(filename)
        print("PDF page count:", len(reader.pages))
    except Exception as e:
        print("PDF integrity check failed:", e)
    # Upload to Gemini
    file_obj = client.files.upload(file=filename, config={'display_name': filename})
    os.remove(filename)
    print("Uploaded file to Gemini:", file_obj)
    return file_obj

async def get_gemini_streaming_response(model: str, messages: list, image_urls=None, api_key: str = None) -> AsyncGenerator[str, None]:
    """Get streaming response from Gemini with optional user API key"""
    try:
        key = api_key or GEMINI_API_KEY
        gemini_client = genai.Client(api_key=key)
        prompt = messages[-1]["content"]
        file_objs = []
        
        if image_urls:
            for url in image_urls:
                if url.lower().endswith(('.jpg', '.jpeg', '.png', '.pdf')):
                    try:
                        file_objs.append(upload_file_to_gemini(url))
                    except Exception as e:
                        print(f"Error uploading file {url} to Gemini: {e}")
        
        contents = file_objs + [prompt]
        
        # Use generate_content_stream() method
        response = gemini_client.models.generate_content_stream(
            model=model,
            contents=contents
        )
        
        # Stream the chunks
        for chunk in response:
            if hasattr(chunk, 'text') and chunk.text:
                yield chunk.text
                
    except Exception as e:
        yield f"Error in Gemini streaming: {str(e)}"

def get_model_response(model, messages, image_urls=None, api_key: str = None):
    """Get model response with optional user API key"""
    model_lower = model.lower()
    if model_lower in ["gemini-2.5-pro", "gemini-2.5-flash"]:
        key = api_key or GEMINI_API_KEY
        gemini_client = genai.Client(api_key=key)
        prompt = messages[-1]["content"]
        file_objs = []
        if image_urls:
            for url in image_urls:
                if url.lower().endswith(('.jpg', '.jpeg', '.png', '.pdf')):
                    try:
                        file_objs.append(upload_file_to_gemini(url))
                    except Exception as e:
                        print(f"Error uploading file {url} to Gemini: {e}")
        contents = file_objs + [prompt]
        response = gemini_client.models.generate_content(
            model=model,
            contents=contents
        )
        return response.text
    elif model_lower in Groq_MODELS:
        return call_groq_api(messages, model, api_key=api_key)
    else:
        # Fallback: extract text from PDFs/images and prepend to prompt
        context = ""
        if image_urls:
            for url in image_urls:
                if url.lower().endswith(".pdf"):
                    context += extract_text_from_pdf(url)
                elif url.lower().endswith((".jpg", ".jpeg", ".png")):
                    context += extract_text_from_image(url)
        prompt = messages[-1]["content"]
        full_prompt = f"Context:\n{context}\n\nUser: {prompt}" if context else prompt
        messages[-1]["content"] = full_prompt
        return get_openrouter_response(model, messages, api_key=api_key)

async def get_streaming_response(model, messages, image_urls=None, api_key: str = None) -> AsyncGenerator[str, None]:
    """Get streaming response based on model type with optional user API key"""
    model_lower = model.lower()
    
    if model_lower in ["gemini-2.5-pro", "gemini-2.5-flash"]:
        async for chunk in get_gemini_streaming_response(model, messages, image_urls, api_key=api_key):
            yield chunk
    elif model_lower in Groq_MODELS:
        async for chunk in call_groq_streaming_api(messages, model, api_key=api_key):
            yield chunk
    else:
        # For other models, use OpenRouter streaming
        context = ""
        if image_urls:
            for url in image_urls:
                if url.lower().endswith(".pdf"):
                    context += extract_text_from_pdf(url)
                elif url.lower().endswith((".jpg", ".jpeg", ".png")):
                    context += extract_text_from_image(url)
        
        prompt = messages[-1]["content"]
        full_prompt = f"Context:\n{context}\n\nUser: {prompt}" if context else prompt
        messages[-1]["content"] = full_prompt
        
        async for chunk in get_openrouter_streaming_response(model, messages, api_key=api_key):
            yield chunk