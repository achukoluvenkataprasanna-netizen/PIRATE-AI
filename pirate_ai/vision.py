"""Small, model-free image detection helpers."""
from __future__ import annotations
import cv2
import numpy as np

def find_color_regions(frame, lower_hsv, upper_hsv, min_area: int = 150):
    """Return bounding boxes for HSV regions as (x, y, w, h)."""
    if frame is None or frame.size == 0:
        raise ValueError("frame must be a non-empty image")
    hsv = cv2.cvtColor(frame, cv2.COLOR_BGR2HSV)
    mask = cv2.inRange(hsv, np.array(lower_hsv, dtype=np.uint8), np.array(upper_hsv, dtype=np.uint8))
    contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    boxes = []
    for contour in contours:
        if cv2.contourArea(contour) >= min_area:
            x, y, w, h = cv2.boundingRect(contour)
            boxes.append((x, y, w, h))
    return boxes

def match_template(frame, template, threshold: float = 0.85):
    """Return best (x, y, w, h, score), or None if below threshold."""
    if frame is None or template is None or frame.size == 0 or template.size == 0:
        raise ValueError("frame and template must be non-empty images")
    if template.shape[0] > frame.shape[0] or template.shape[1] > frame.shape[1]:
        return None
    result = cv2.matchTemplate(frame, template, cv2.TM_CCOEFF_NORMED)
    _, score, _, location = cv2.minMaxLoc(result)
    if score < threshold:
        return None
    h, w = template.shape[:2]
    return (location[0], location[1], w, h, float(score))
