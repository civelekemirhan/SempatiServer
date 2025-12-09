import os
import io
import cv2
import tempfile
import numpy as np
from PIL import Image, UnidentifiedImageError
from fastapi import FastAPI, UploadFile, File, Form
from ultralytics import YOLO

app = FastAPI()

# Modeli yükle
model = YOLO("yolov8n.pt")

# AYAR: Güven oranı %60'ın altındaysa kabul etme (Eskisi %40 idi)
MIN_CONFIDENCE = 0.60

def analyze_frame(img_pil):
    try:
        results = model(img_pil, verbose=False) 
        
        detections = []
        found_pet = False
        
        if not results:
            return False, []

        for result in results:
            if not result.boxes:
                continue

            for box in result.boxes:
                if box.cls is None or len(box.cls) == 0:
                    continue

                class_id = int(box.cls[0])
                class_name = model.names[class_id]
                confidence = float(box.conf[0])
                
                # Sadece Kedi/Köpek ve Güven Oranı Yüksekse
                if class_name in ["cat", "dog"]:
                    print(f"--> Nesne: {class_name}, Oran: {confidence:.2f}") # Terminale ne bulduğunu yaz
                    
                    if confidence > MIN_CONFIDENCE:
                        found_pet = True
                        detections.append({
                            "type": class_name,
                            "confidence": round(confidence, 2),
                            "bbox": box.xyxy[0].tolist()
                        })
        
        return found_pet, detections

    except Exception as e:
        print(f"HATA (analyze_frame): {str(e)}")
        return False, []

@app.post("/detect")
async def detect_objects(
    file: UploadFile = File(...), 
    file_type: str = Form("image")
):
    print(f"--- Yeni İstek: Tür={file_type}, Dosya={file.filename} ---")
    try:
        content = await file.read()
        if not content:
            return {"success": False, "error": "Dosya boş."}

        # --- FOTOĞRAF KONTROLÜ ---
        if file_type == "image":
            try:
                img = Image.open(io.BytesIO(content))
            except UnidentifiedImageError:
                return {"success": False, "error": "Geçersiz resim dosyası."}
            
            found, detections = analyze_frame(img)
            return {
                "success": True, 
                "type": "image", 
                "contains_animal": found, 
                "detections": detections
            }

        # --- VİDEO KONTROLÜ ---
        elif file_type == "video":
            with tempfile.NamedTemporaryFile(delete=False, suffix=".mp4") as temp_video:
                temp_video.write(content)
                temp_video_path = temp_video.name
            
            cap = cv2.VideoCapture(temp_video_path)
            found_any = False
            detected_label = None
            max_conf_found = 0.0
            
            frame_count = 0
            skip_frames = 15 # Her 15 karede bir bak
            
            while cap.isOpened():
                ret, frame = cap.read()
                if not ret:
                    break
                
                if frame_count % skip_frames == 0:
                    frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                    pil_img = Image.fromarray(frame_rgb)
                    
                    found, detections = analyze_frame(pil_img)
                    if found:
                        found_any = True
                        detected_label = detections[0]['type']
                        max_conf_found = detections[0]['confidence']
                        print(f"!!! VİDEODA {detected_label} BULUNDU (Güven: {max_conf_found}) !!!")
                        break # Hayvanı bulduk, çıkabiliriz
                
                frame_count += 1
            
            cap.release()
            try:
                os.remove(temp_video_path)
            except:
                pass
            
            return {
                "success": True,
                "type": "video",
                "contains_animal": found_any,
                "message": f"{detected_label} tespit edildi." if found_any else "Bulunamadı."
            }
        
        else:
            return {"success": False, "error": "Geçersiz file_type"}

    except Exception as e:
        print(f"GENEL HATA: {str(e)}")
        return {"success": False, "error": str(e)}