# ONNX Model Setup Instructions

## Overview
The CropRecommendation feature uses an ONNX Runtime model to analyze soil data and recommend the best crops based on nutrient levels, pH, temperature, and moisture.

## Model Requirements
The model file `crop_recommendation.onnx` should be placed in the `app/src/main/assets/` directory.

### Model Input
The model expects a single input tensor with 6 features (normalized 0-1 range):
1. **Nitrogen** - Normalized value (0 to 100 mg/kg) / 100
2. **Phosphorus** - Normalized value (0 to 100 mg/kg) / 100
3. **Potassium** - Normalized value (0 to 100 mg/kg) / 100
4. **pH Level** - Normalized value (0 to 14 pH) / 14
5. **Temperature** - Normalized value (-40 to 50°C) / 50
6. **Moisture** - Normalized value (0 to 100%) / 100

### Model Output
The model should output confidence scores (0-1) for each crop recommendation. The output should be a 1D tensor where each index corresponds to a crop:

Index mapping (default crops):
- 0: Rice
- 1: Maize
- 2: Wheat
- 3: Barley
- 4: Pulses
- 5: Sugarcane
- 6: Cotton
- 7: Groundnut
- 8: Coconut
- 9: Arecanut
- 10: Banana
- 11: Citrus
- 12: Mango
- 13: Cardamom
- 14: Black Pepper

## Creating Your ONNX Model

You can create the model using:
1. **Python with ONNX** - Train a classification model and export to ONNX format
2. **TensorFlow/PyTorch** - Train your model and convert to ONNX
3. **ML.NET** - Microsoft's machine learning framework

### Example Python Setup:
```python
import onnx
import onnxruntime
from sklearn.ensemble import RandomForestClassifier
import numpy as np

# Train your model
X_train = np.array(...)  # 6 features: N, P, K, pH, Temp, Moisture
y_train = np.array(...)  # 15 crop classes

model = RandomForestClassifier(n_estimators=100)
model.fit(X_train, y_train)

# Export to ONNX
# You'll need skl2onnx for this
from skl2onnx import convert_sklearn
from skl2onnx.common.data_types import FloatTensorType

initial_type = [('float_input', FloatTensorType([None, 6]))]
onnx_model = convert_sklearn(model, initial_type=initial_type)

with open("crop_recommendation.onnx", "wb") as f:
    f.write(onnx_model.SerializeToString())
```

## Fallback Behavior
If the ONNX model is not found or fails to load, the app will provide default crop recommendations based on common agricultural practices:
- Rice
- Wheat
- Maize
- Sugarcane
- Cotton

## Integration Details
The CropRecommendation screen:
1. Loads all soil data samples collected by the user
2. Calculates average values across all locations
3. Normalizes the data according to the ranges above
4. Runs ONNX inference
5. Displays top 10 crop recommendations with confidence percentages
6. Shows reasoning for each recommendation based on confidence level

## Testing
You can test the integration without a real model by:
1. Temporarily commenting out the model loading in `CropRecommendation.kt`
2. The fallback recommendations will be displayed
3. Verify the UI works correctly with the returned data

