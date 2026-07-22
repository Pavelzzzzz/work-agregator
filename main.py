from fastapi import FastAPI
import uvicorn
import os

app = FastAPI(title="Work Aggregator Service")

@app.get("/health")
def read_root():
    return {"status": "ok", "service": "work-aggregator", "message": "Service is running"}

@app.get("/aggregate")
def aggregate_data(source: str, target: str):
    """
    Симулирует агрегацию данных между двумя источниками.
    """
    if not source or not target:
        return {"error": "Both source and target must be provided."}
    
    # Здесь будет реальная логика вызова API или базы данных
    result = f"Successfully aggregated data from {source} to {target}."
    return {"status": "success", "data": result}

if __name__ == "__main__":
    # Запуск сервера на порту 8000
    uvicorn.run(app, host="0.0.0.0", port=8000)