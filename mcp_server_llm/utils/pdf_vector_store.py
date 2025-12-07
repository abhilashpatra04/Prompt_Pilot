import os
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_google_genai import GoogleGenerativeAIEmbeddings
from langchain_community.vectorstores import FAISS  # Updated import path
from utils.context_utils import extract_text_from_pdf

VECTOR_DIR = "vectorstores"

def ensure_vector_dir():
    if not os.path.exists(VECTOR_DIR):
        os.makedirs(VECTOR_DIR)

def process_and_store_pdfs(pdf_urls, chat_id):
    
    ensure_vector_dir()
    text = ""
    for url in pdf_urls:
        text += extract_text_from_pdf(url)
    if not text.strip():
        return
    text_splitter = RecursiveCharacterTextSplitter(chunk_size=10000, chunk_overlap=1000)
    chunks = text_splitter.split_text(text)
    embeddings = GoogleGenerativeAIEmbeddings(model="models/embedding-001")
    vector_store = FAISS.from_texts(chunks, embedding=embeddings)
    vector_store.save_local(os.path.join(VECTOR_DIR, f"{chat_id}"))
    print("Extracted text:", text)
    print("Number of chunks:", len(chunks))

def search_pdf_context(chat_id, query):
    faiss_path = os.path.join(VECTOR_DIR, f"{chat_id}")
    if not os.path.exists(faiss_path):
        return ""
    embeddings = GoogleGenerativeAIEmbeddings(model="models/embedding-001")
    db = FAISS.load_local(faiss_path, embeddings, allow_dangerous_deserialization=True)
    docs = db.similarity_search(query)
    print("Vector search results:", [doc.page_content for doc in docs])
    return "\n".join([doc.page_content for doc in docs])