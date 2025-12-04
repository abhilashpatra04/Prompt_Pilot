# # mcp_server/utils/firebase_utils.py

from fastapi import APIRouter, HTTPException, Query, FastAPI
from pydantic import BaseModel
from google.cloud import firestore
from datetime import datetime
import os
import requests

db = firestore.Client()

class ChatRequest(BaseModel):
    uid: str
    prompt: str
    model: str
    chat_id: str = None 
    title: str = "Untitled"

class ChatResponse(BaseModel):
    reply: str
    chat_id: str

def create_new_chat(uid: str, title: str) -> str:
    doc_ref = db.collection("conversations").document()
    doc_ref.set({
        "id": doc_ref.id,
        "title": title,
        "createdAt": firestore.SERVER_TIMESTAMP,
        "uid": uid
    })
    return doc_ref.id

def get_chat_threads(uid: str) -> list:
    threads_ref = db.collection("conversations").where("uid", "==", uid)
    threads = threads_ref.order_by("createdAt", direction=firestore.Query.DESCENDING).stream()
    return [doc.to_dict() for doc in threads]

def store_message(uid: str, chat_id: str, user_msg: str, ai_msg: str) -> bool:
    db.collection("messages").add({
        "id": str(datetime.utcnow().timestamp()),
        "conversationId": chat_id,
        "question": user_msg,
        "answer": ai_msg,
        "createdAt": firestore.SERVER_TIMESTAMP,
        "uid": uid
    })
    return True

def get_chat_messages(uid: str, chat_id: str) -> list:
    messages_ref = db.collection("messages").where("conversationId", "==", chat_id).order_by("createdAt")
    messages = messages_ref.stream()
    return [doc.to_dict() for doc in messages]
