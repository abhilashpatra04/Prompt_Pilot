import requests
import pdfplumber
import tempfile
import os

import pytesseract
from PIL import Image
import io

def extract_text_from_pdf(path_or_url: str) -> str:
    # If it's a URL, download to temp file; if it's a local file, use directly
    if path_or_url.startswith("http://") or path_or_url.startswith("https://"):
        response = requests.get(path_or_url)
        with tempfile.NamedTemporaryFile(suffix=".pdf") as tmp:
            tmp.write(response.content)
            tmp.flush()
            text = ""
            with pdfplumber.open(tmp.name) as pdf:
                for page in pdf.pages:
                    text += page.extract_text() or ""
            return text
    elif os.path.exists(path_or_url):
        text = ""
        with pdfplumber.open(path_or_url) as pdf:
            for page in pdf.pages:
                text += page.extract_text() or ""
        return text
    else:
        raise ValueError(f"Invalid path or URL: {path_or_url}")


def extract_text_from_image(url: str) -> str:
    response = requests.get(url)
    img = Image.open(io.BytesIO(response.content))
    text = pytesseract.image_to_string(img)
    return text