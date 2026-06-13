from fastapi import FastAPI

app = FastAPI(title="Task Manager", version="1.0.0")


@app.get("/hello")
def hello():
    return {"message": "Hello Agent World"}


@app.get("/hello/{name}")
def hello_name(name: str):
    return {"message": f"Hello {name}"}