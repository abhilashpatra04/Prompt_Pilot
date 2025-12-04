# ===== Fixed FastAPI Backend with CORS Support =====

import os
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from chains.base_chat import router
from dotenv import load_dotenv

load_dotenv()
print(os.getenv("OPENROUTER_API_KEY"))

# ------------------ Main FastAPI App ------------------
app = FastAPI(title="PromptPilot API", version="1.0.0")

# Add CORS middleware - THIS IS CRUCIAL
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # In production, replace with your specific origins
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "DELETE", "OPTIONS"],
    allow_headers=["*"],
    expose_headers=["*"]
)

# Add router
app.include_router(router)

# Add a root endpoint for health check
@app.get("/")
async def root():
    return {"message": "PromptPilot API is running", "status": "healthy"}

# Add health check endpoint
@app.get("/health")
async def health_check():
    return {"status": "healthy", "message": "API is running successfully"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)



# To run the server:
# 1. Set environment variable: set GOOGLE_APPLICATION_CREDENTIALS=path\to\your\firebase-service-account.json
# 2. Run: uvicorn main:app --host 0.0.0.0 --port 8000 --reload# # ===== FastAPI Backend for Flat Firestore Structure (Minimal, Only Required Logic) =====

# import os
# from fastapi import FastAPI
# from chains.base_chat import router
# from dotenv import load_dotenv
# load_dotenv()
# print (os.getenv("OPENROUTER_API_KEY"))

# # ------------------ Main FastAPI App ------------------
# app = FastAPI()
# app.include_router(router)

# #set GOOGLE_APPLICATION_CREDENTIALS=C:\Users\abhilahpatra\service-keys\chatbot-53ecb-firebase-adminsdk-fbsvc-f92323769c.json
# uvicorn main:app --host 0.0.0.0 --port 8000 --reload
