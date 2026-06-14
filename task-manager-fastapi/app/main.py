from fastapi import FastAPI
from .database import init_db
from .router import router

app = FastAPI(title="Task Manager API", version="1.0.0")

@app.on_event("startup")
def on_startup():
    init_db()

app.include_router(router)


@app.get("/hello")
def hello():
    return {"message": "Hello Agent World"}