from google.cloud import firestore
from datetime import datetime
import mcp
import cloudinary
import cloudinary.uploader


from utils.context_utils import extract_text_from_image, extract_text_from_pdf
from utils.model_loader import get_model_response

db = firestore.Client()

@mcp.tool()
async def add_file_metadata(uid: str, conversation_id: str, file_url: str, file_type: str, file_name: str, public_id: str) -> str:
    """
    Add file metadata to Firestore for a conversation.
    """
    try:
        doc_ref = db.collection("files").document()
        doc_ref.set({
            "uid": uid,
            "conversation_id": conversation_id,
            "file_url": file_url,
            "file_type": file_type,
            "file_name": file_name,
            "public_id": public_id,
            "uploaded_at": datetime.utcnow()
        })
        return "File metadata added successfully"
    except Exception as e:
        return f"Error adding file metadata: {e}"

@mcp.tool()
async def get_files_for_conversation(conversation_id: str) -> list:
    """
    Fetch all file metadata for a conversation.
    """
    try:
        files_ref = db.collection("files").where("conversation_id", "==", conversation_id)
        docs = files_ref.stream()
        files = [doc.to_dict() for doc in docs]
        return files
    except Exception as e:
        return []

cloudinary.config(
  cloud_name = "dkkyiygll",
  api_key = "713672969564931",
  api_secret = "4A9T1zfrrI5rad0eidhr6DOTsTk"
)

@mcp.tool()
async def delete_file(public_id: str, conversation_id: str) -> str:
    """
    Delete a file from Cloudinary and its metadata from Firestore.
    """
    try:
        # Delete from Cloudinary
        cloudinary.uploader.destroy(public_id, invalidate=True)
        # Delete from Firestore
        files_ref = db.collection("files").where("public_id", "==", public_id).where("conversation_id", "==", conversation_id)
        docs = files_ref.stream()
        for doc in docs:
            doc.reference.delete()
        return "File deleted successfully"
    except Exception as e:
        return f"Error deleting file: {e}"

@mcp.tool()
async def delete_files_for_conversation(conversation_id: str) -> str:
    """
    Delete all files for a conversation from Cloudinary and Firestore.
    """
    try:
        files_ref = db.collection("files").where("conversation_id", "==", conversation_id)
        docs = files_ref.stream()
        for doc in docs:
            data = doc.to_dict()
            public_id = data.get("public_id")
            if public_id:
                cloudinary.uploader.destroy(public_id, invalidate=True)
            doc.reference.delete()
        return "All files deleted for conversation"
    except Exception as e:
        return f"Error deleting files: {e}"
    
@mcp.tool()
async def chat_with_context(conversation_id: str, prompt: str, model: str) -> str:
    """
    Fetch all files for the conversation, extract context, and call the AI model.
    """
    try:
        files = await get_files_for_conversation(conversation_id)
        context = ""
        for file in files:
            if file['file_type'] == 'pdf':
                context += extract_text_from_pdf(file['file_url'])
            elif file['file_type'] in ['jpg', 'png']:
                context += extract_text_from_image(file['file_url'])
        full_prompt = f"Context:\n{context}\n\nUser: {prompt}"
        ai_response = get_model_response(model, [{"role": "user", "content": full_prompt}])        
        return ai_response
    except Exception as e:
        return f"Error in chat_with_context: {e}"
    
