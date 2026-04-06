# ============================================================================
# MANGO CROP RECOMMENDATION - REAL-TIME TESTING WITH ONNX MODEL
# Google Colab Ready - Tests and Validates Real-Time Inference
# ============================================================================

# STEP 1: Install Required Libraries
print("=" * 80)
print("STEP 1: Installing required libraries...")
print("=" * 80)

import subprocess
import sys

packages = ['numpy', 'pandas', 'onnx', 'onnxruntime', 'matplotlib', 'seaborn']
for package in packages:
    subprocess.check_call([sys.executable, '-m', 'pip', 'install', '-q', package])

print("✓ All libraries installed successfully!\n")

# STEP 2: Import Libraries
print("=" * 80)
print("STEP 2: Importing libraries...")
print("=" * 80)

import numpy as np
import pandas as pd
import onnx
import onnxruntime as rt
from onnxruntime import InferenceSession
import os
import json
import warnings
warnings.filterwarnings('ignore')

import matplotlib.pyplot as plt
import seaborn as sns

print("✓ Libraries imported successfully!\n")

# STEP 3: Load ONNX Model
print("=" * 80)
print("STEP 3: Loading ONNX Model...")
print("=" * 80)

MODEL_PATH = 'crop_recommendation_model.onnx'

try:
    # Load ONNX model for verification
    onnx_model = onnx.load(MODEL_PATH)
    onnx.checker.check_model(onnx_model)
    print(f"✓ ONNX model loaded and validated: {MODEL_PATH}")
except Exception as e:
    print(f"✗ Error loading ONNX model: {e}")
    print("Make sure crop_recommendation_model.onnx is in the current directory")
    exit(1)

# Load ONNX Runtime Session
sess = rt.InferenceSession(MODEL_PATH)
print(f"✓ ONNX Runtime session created successfully\n")

# STEP 4: Extract Model Information
print("=" * 80)
print("STEP 4: Extracting Model Information...")
print("=" * 80)

# Get input and output names
input_name = sess.get_inputs()[0].name
input_shape = sess.get_inputs()[0].shape
output_names = [output.name for output in sess.get_outputs()]

print(f"Input Name: {input_name}")
print(f"Input Shape: {input_shape}")
print(f"Output Names: {output_names}")

# Crop mapping (must match mango_mock_data.py LabelEncoder order)
CROP_NAMES = ["Banana", "Cassava", "Corn", "Mango", "Sweet Potato"]
CROP_MAPPING = {i: crop for i, crop in enumerate(CROP_NAMES)}

print(f"\nCrop Mapping:")
for idx, crop in CROP_MAPPING.items():
    print(f"  {idx}: {crop}")
print()

# STEP 5: Define Real-Time Test Cases
print("=" * 80)
print("STEP 5: Defining Real-Time Test Cases...")
print("=" * 80)

test_cases = {
    "Mango (User Data)": {
        "N": 12.0,
        "P": 7.0,
        "K": 9.0,
        "pH": 6.5,
        "Moisture": 62.0,
        "Temperature": 29.4,
        "description": "User-provided mango soil data"
    },
    "Banana (Typical)": {
        "N": 125.0,
        "P": 37.0,
        "K": 125.0,
        "pH": 6.2,
        "Moisture": 70.0,
        "Temperature": 27.0,
        "description": "Typical banana growing conditions"
    },
    "Cassava (Typical)": {
        "N": 100.0,
        "P": 30.0,
        "K": 100.0,
        "pH": 5.5,
        "Moisture": 60.0,
        "Temperature": 24.0,
        "description": "Typical cassava growing conditions"
    },
    "Corn (Typical)": {
        "N": 150.0,
        "P": 55.0,
        "K": 150.0,
        "pH": 6.7,
        "Moisture": 70.0,
        "Temperature": 24.0,
        "description": "Typical corn growing conditions"
    },
    "Sweet Potato (Typical)": {
        "N": 90.0,
        "P": 40.0,
        "K": 110.0,
        "pH": 6.3,
        "Moisture": 65.0,
        "Temperature": 26.0,
        "description": "Typical sweet potato growing conditions"
    }
}

print(f"Total test cases: {len(test_cases)}\n")

# STEP 6: Real-Time Inference Function
print("=" * 80)
print("STEP 6: Running Real-Time Inference Tests...")
print("=" * 80)

def run_inference(N, P, K, pH, Moisture, Temperature):
    """
    Run inference on a single soil sample

    Args:
        N: Nitrogen level (mg/kg)
        P: Phosphorus level (mg/kg)
        K: Potassium level (mg/kg)
        pH: pH level (3.0-9.0)
        Moisture: Moisture percentage (0-100%)
        Temperature: Temperature in Celsius (15-35°C)

    Returns:
        Dictionary with predictions and confidence scores
    """
    try:
        # Prepare input data
        input_data = np.array([[N, P, K, pH, Moisture, Temperature]], dtype=np.float32)

        # Run inference
        output = sess.run(None, {input_name: input_data})

        # Handle output
        # output[0] is the predicted label
        # output[1] is the probability dictionary
        predicted_label = output[0][0] if isinstance(output[0], np.ndarray) else output[0]
        probabilities = output[1]

        # Extract probabilities for all crops
        if isinstance(probabilities, dict):
            scores = {CROP_MAPPING[i]: float(probabilities.get(i, 0.0)) for i in range(len(CROP_NAMES))}
        else:
            scores = {CROP_MAPPING[i]: float(probabilities[0][i]) if len(probabilities[0]) > i else 0.0
                     for i in range(len(CROP_NAMES))}

        # Sort by confidence
        sorted_crops = sorted(scores.items(), key=lambda x: x[1], reverse=True)

        return {
            "predicted_crop": CROP_MAPPING.get(int(predicted_label), "Unknown"),
            "confidence": scores[CROP_MAPPING.get(int(predicted_label), "Unknown")],
            "all_scores": scores,
            "ranked_crops": sorted_crops
        }

    except Exception as e:
        print(f"✗ Error during inference: {e}")
        return None

# STEP 7: Execute Real-Time Tests
print("\nExecuting inference on all test cases:\n")

all_results = []

for test_name, params in test_cases.items():
    print(f"{'─' * 80}")
    print(f"Test: {test_name}")
    print(f"Description: {params['description']}")
    print(f"Parameters:")
    print(f"  N (Nitrogen): {params['N']} mg/kg")
    print(f"  P (Phosphorus): {params['P']} mg/kg")
    print(f"  K (Potassium): {params['K']} mg/kg")
    print(f"  pH Level: {params['pH']}")
    print(f"  Moisture: {params['Moisture']}%")
    print(f"  Temperature: {params['Temperature']}°C")

    # Run inference
    result = run_inference(
        params['N'], params['P'], params['K'],
        params['pH'], params['Moisture'], params['Temperature']
    )

    if result:
        print(f"\nResults:")
        print(f"  ✓ Predicted Crop: {result['predicted_crop']}")
        print(f"  ✓ Confidence: {result['confidence']:.4f} ({result['confidence']*100:.2f}%)")
        print(f"\n  All Crop Scores:")
        for crop, score in result['ranked_crops']:
            percentage = score * 100
            bar = "█" * int(percentage / 2) + "░" * (50 - int(percentage / 2))
            print(f"    {crop:15} | {bar} {percentage:.2f}%")

        all_results.append({
            "test_name": test_name,
            "predicted_crop": result['predicted_crop'],
            "confidence": result['confidence'],
            "all_scores": result['all_scores'],
            "input_params": {
                "N": params['N'],
                "P": params['P'],
                "K": params['K'],
                "pH": params['pH'],
                "Moisture": params['Moisture'],
                "Temperature": params['Temperature']
            }
        })
    else:
        print(f"  ✗ Inference failed for this test case")

    print()

# STEP 8: Real-Time Single Prediction Function (For Android Integration)
print("=" * 80)
print("STEP 8: Real-Time Single Prediction Function")
print("=" * 80)

def predict_crop_real_time(N, P, K, pH, Moisture, Temperature):
    """
    Real-time crop recommendation function for Android integration

    This function is designed to be called continuously with live soil sensor data
    and returns immediate crop recommendations.

    Args:
        N, P, K: Soil nutrients (mg/kg)
        pH: Soil pH level (3.0-9.0)
        Moisture: Soil moisture percentage (0-100%)
        Temperature: Ambient temperature (°C)

    Returns:
        dict: {
            "top_crop": str,
            "confidence": float,
            "top_5_recommendations": list[(crop, score), ...],
            "recommendation_text": str
        }
    """
    result = run_inference(N, P, K, pH, Moisture, Temperature)

    if result:
        top_5 = result['ranked_crops'][:5]
        confidence = result['confidence']
        top_crop = result['predicted_crop']

        # Generate recommendation text
        if confidence >= 0.8:
            rec_text = f"Highly recommended: {top_crop} (Excellent match)"
        elif confidence >= 0.6:
            rec_text = f"Good match: {top_crop} (Well-suited for current conditions)"
        elif confidence >= 0.4:
            rec_text = f"Moderate match: {top_crop} (May require adjustments)"
        elif confidence >= 0.2:
            rec_text = f"Fair match: {top_crop} (Consider alternatives)"
        else:
            rec_text = f"Low compatibility: {top_crop} (Not recommended)"

        return {
            "top_crop": top_crop,
            "confidence": float(confidence),
            "top_5_recommendations": [(crop, float(score)) for crop, score in top_5],
            "recommendation_text": rec_text
        }

    return None

# Test the real-time function with Mango data
print("\nTesting Real-Time Prediction Function with Mango Data:")
print("─" * 80)

mango_pred = predict_crop_real_time(12.0, 7.0, 9.0, 6.5, 62.0, 29.4)
if mango_pred:
    print(f"Top Crop: {mango_pred['top_crop']}")
    print(f"Confidence: {mango_pred['confidence']:.4f} ({mango_pred['confidence']*100:.2f}%)")
    print(f"Recommendation: {mango_pred['recommendation_text']}")
    print(f"\nTop 5 Recommendations:")
    for i, (crop, score) in enumerate(mango_pred['top_5_recommendations'], 1):
        print(f"  {i}. {crop}: {score:.4f} ({score*100:.2f}%)")

print()

# STEP 9: Batch Processing (Simulating continuous sensor data)
print("=" * 80)
print("STEP 9: Batch Processing - Simulating Real-Time Sensor Data")
print("=" * 80)

print("\nSimulating 10 real-time sensor readings for Mango field:\n")

# Generate simulated sensor readings (variation around mango optimal values)
np.random.seed(42)
sensor_data = []

for i in range(10):
    # Add realistic variation to mango parameters
    N = 12.0 + np.random.normal(0, 1)
    P = 7.0 + np.random.normal(0, 0.5)
    K = 9.0 + np.random.normal(0, 1)
    pH = 6.5 + np.random.normal(0, 0.1)
    Moisture = 62.0 + np.random.normal(0, 3)
    Temperature = 29.4 + np.random.normal(0, 1)

    # Ensure values stay within valid ranges
    N = max(0, N)
    P = max(0, P)
    K = max(0, K)
    pH = max(3.0, min(9.0, pH))
    Moisture = max(0, min(100, Moisture))
    Temperature = max(15, min(35, Temperature))

    pred = predict_crop_real_time(N, P, K, pH, Moisture, Temperature)

    sensor_data.append({
        "reading_id": i + 1,
        "N": N,
        "P": P,
        "K": K,
        "pH": pH,
        "Moisture": Moisture,
        "Temperature": Temperature,
        "predicted_crop": pred['top_crop'],
        "confidence": pred['confidence']
    })

    print(f"Reading {i+1:2d} | N:{N:6.2f} P:{P:5.2f} K:{K:5.2f} | " +
          f"pH:{pH:4.2f} | Moisture:{Moisture:5.1f}% | Temp:{Temperature:5.1f}°C | " +
          f"Predicted: {pred['top_crop']:15} | Confidence: {pred['confidence']:.4f}")

print()

# STEP 10: Statistics and Analysis
print("=" * 80)
print("STEP 10: Real-Time Analysis Statistics")
print("=" * 80)

sensor_df = pd.DataFrame(sensor_data)

print("\nStatistics from 10 Real-Time Readings:")
print(f"  Most frequent prediction: {sensor_df['predicted_crop'].mode().values[0]}")
print(f"  Average confidence: {sensor_df['confidence'].mean():.4f}")
print(f"  Min confidence: {sensor_df['confidence'].min():.4f}")
print(f"  Max confidence: {sensor_df['confidence'].max():.4f}")

print(f"\nPrediction Distribution:")
print(sensor_df['predicted_crop'].value_counts())

print()

# STEP 11: Export Results for Android Integration
print("=" * 80)
print("STEP 11: Exporting Results for Android Integration")
print("=" * 80)

# Create JSON output for Android app
android_integration = {
    "model_info": {
        "type": "RandomForestClassifier",
        "crops": CROP_NAMES,
        "features": ["N", "P", "K", "pH", "Moisture", "Temperature"],
        "feature_ranges": {
            "N": {"min": 0, "max": 200, "unit": "mg/kg"},
            "P": {"min": 0, "max": 100, "unit": "mg/kg"},
            "K": {"min": 0, "max": 200, "unit": "mg/kg"},
            "pH": {"min": 3.0, "max": 9.0, "unit": ""},
            "Moisture": {"min": 0, "max": 100, "unit": "%"},
            "Temperature": {"min": 15, "max": 35, "unit": "°C"}
        }
    },
    "real_time_test_results": all_results,
    "batch_processing_results": sensor_data,
    "prediction_function": "predict_crop_real_time(N, P, K, pH, Moisture, Temperature)"
}

# Save as JSON
output_file = "mango_realtime_predictions.json"
with open(output_file, 'w') as f:
    json.dump(android_integration, f, indent=2)

print(f"✓ Results exported to {output_file}")

# Save as CSV for easy viewing
csv_file = "mango_realtime_predictions.csv"
sensor_df.to_csv(csv_file, index=False)
print(f"✓ Real-time readings exported to {csv_file}")

print()

# STEP 12: Visualization
print("=" * 80)
print("STEP 12: Creating Visualizations")
print("=" * 80)

fig, axes = plt.subplots(2, 2, figsize=(14, 10))

# Plot 1: Confidence Scores for All Test Cases
test_names = [r['test_name'] for r in all_results]
confidences = [r['confidence'] for r in all_results]
colors = ['#FF6347' if 'Mango' in name else '#4CAF50' for name in test_names]

axes[0, 0].barh(test_names, confidences, color=colors)
axes[0, 0].set_xlabel('Confidence Score')
axes[0, 0].set_title('Real-Time Inference Confidence Scores', fontweight='bold')
axes[0, 0].set_xlim(0, 1)

# Plot 2: Mango Field Readings - Confidence Over Time
readings = sensor_df['reading_id'].values
conf = sensor_df['confidence'].values

axes[0, 1].plot(readings, conf, marker='o', linestyle='-', color='#FF6347', linewidth=2, markersize=8)
axes[0, 1].fill_between(readings, conf, alpha=0.3, color='#FF6347')
axes[0, 1].set_xlabel('Reading Number')
axes[0, 1].set_ylabel('Confidence Score')
axes[0, 1].set_title('Real-Time Confidence - Mango Field Simulation', fontweight='bold')
axes[0, 1].grid(True, alpha=0.3)
axes[0, 1].set_ylim(0, 1)

# Plot 3: Sensor Parameters Distribution
param_names = ['N (mg/kg)', 'P (mg/kg)', 'K (mg/kg)', 'pH', 'Moisture (%)', 'Temperature (°C)']
param_values = [
    sensor_df['N'].mean(),
    sensor_df['P'].mean(),
    sensor_df['K'].mean(),
    sensor_df['pH'].mean(),
    sensor_df['Moisture'].mean(),
    sensor_df['Temperature'].mean()
]

# Normalize for visualization
normalized_values = [
    sensor_df['N'].mean() / 20,  # Normalize N
    sensor_df['P'].mean() / 1.5,  # Normalize P
    sensor_df['K'].mean() / 1.5,  # Normalize K
    sensor_df['pH'].mean() / 10,   # Normalize pH
    sensor_df['Moisture'].mean() / 100,  # Normalize Moisture
    (sensor_df['Temperature'].mean() - 15) / 20  # Normalize Temperature
]

axes[1, 0].bar(param_names, normalized_values, color='#2196F3', alpha=0.7)
axes[1, 0].set_ylabel('Normalized Value')
axes[1, 0].set_title('Average Sensor Parameters - Normalized', fontweight='bold')
axes[1, 0].tick_params(axis='x', rotation=45)

# Plot 4: Crop Prediction Distribution
crop_counts = sensor_df['predicted_crop'].value_counts()

axes[1, 1].pie(crop_counts.values, labels=crop_counts.index, autopct='%1.1f%%',
               colors=['#FF6347', '#4CAF50', '#FFD700', '#87CEEB', '#FFA500'])
axes[1, 1].set_title('Crop Prediction Distribution - 10 Readings', fontweight='bold')

plt.tight_layout()
plt.savefig('mango_realtime_analysis.png', dpi=150, bbox_inches='tight')
print("✓ Visualization saved as 'mango_realtime_analysis.png'")
plt.show()

print()

# STEP 13: Real-Time Integration Guide
print("=" * 80)
print("STEP 13: Android Real-Time Integration Guide")
print("=" * 80)