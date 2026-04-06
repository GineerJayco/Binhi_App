# ============================================================================
# CROP RECOMMENDATION MODEL - RANDOM FOREST WITH ONNX CONVERSION
# Google Colab Ready - Just Copy and Paste!
# ============================================================================

# STEP 1: Install Required Libraries
print("=" * 70)
print("STEP 1: Installing required libraries...")
print("=" * 70)

import subprocess
import sys

packages = ['scikit-learn', 'numpy', 'pandas', 'matplotlib', 'seaborn', 'skl2onnx', 'onnx', 'onnxruntime']
for package in packages:
    subprocess.check_call([sys.executable, '-m', 'pip', 'install', '-q', package])

print("✓ All libraries installed successfully!\n")

# STEP 2: Import Libraries
print("=" * 70)
print("STEP 2: Importing libraries...")
print("=" * 70)

import numpy as np
import pandas as pd
import os
import warnings
warnings.filterwarnings('ignore')

from sklearn.ensemble import RandomForestClassifier
from sklearn.preprocessing import LabelEncoder
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix

import matplotlib.pyplot as plt
import seaborn as sns

from skl2onnx import convert_sklearn
from skl2onnx.common.data_types import FloatTensorType
import onnx
import onnxruntime as rt

print("✓ Libraries imported successfully!\n")

# STEP 3: Generate Mock Data for 5 Crops
print("=" * 70)
print("STEP 3: Generating mock training data...")
print("=" * 70)

np.random.seed(42)

# Define optimal soil parameters for each crop
crop_data = {
    'Banana': {
        'N': (100, 150),
        'P': (25, 50),
        'K': (100, 150),
        'pH': (5.5, 7.0),
        'Moisture': (60, 80),
        'Temperature': (25, 30)
    },
    'Cassava': {
        'N': (80, 120),
        'P': (20, 40),
        'K': (80, 120),
        'pH': (5.0, 6.5),
        'Moisture': (50, 70),
        'Temperature': (20, 28)
    },
    'Sweet Potato': {
        'N': (70, 110),
        'P': (30, 50),
        'K': (90, 130),
        'pH': (5.8, 6.8),
        'Moisture': (55, 75),
        'Temperature': (24, 29)
    },
    'Corn': {
        'N': (120, 180),
        'P': (40, 70),
        'K': (120, 180),
        'pH': (6.0, 7.5),
        'Moisture': (60, 80),
        'Temperature': (20, 28)
    },
    'Mango': {
        'N': (90, 140),
        'P': (35, 60),
        'K': (110, 160),
        'pH': (5.5, 7.5),
        'Moisture': (40, 60),
        'Temperature': (25, 35)
    }
}

# Generate 150 samples per crop (750 total samples)
datasets = []
samples_per_crop = 150

for crop, params in crop_data.items():
    for _ in range(samples_per_crop):
        n_val = np.random.uniform(params['N'][0], params['N'][1]) + np.random.normal(0, 5)
        p_val = np.random.uniform(params['P'][0], params['P'][1]) + np.random.normal(0, 2)
        k_val = np.random.uniform(params['K'][0], params['K'][1]) + np.random.normal(0, 5)
        ph_val = np.random.uniform(params['pH'][0], params['pH'][1]) + np.random.normal(0, 0.2)
        moisture_val = np.random.uniform(params['Moisture'][0], params['Moisture'][1]) + np.random.normal(0, 3)
        temp_val = np.random.uniform(params['Temperature'][0], params['Temperature'][1]) + np.random.normal(0, 1)

        datasets.append({
            'N': max(0, n_val),
            'P': max(0, p_val),
            'K': max(0, k_val),
            'pH': max(3.0, min(9.0, ph_val)),
            'Moisture': max(0, min(100, moisture_val)),
            'Temperature': max(15, min(35, temp_val)),
            'Crop': crop
        })

df = pd.DataFrame(datasets)

print(f"✓ Dataset created with shape: {df.shape}")
print(f"\nFirst 5 rows:")
print(df.head())
print(f"\nCrop distribution:")
print(df['Crop'].value_counts())
print()

# STEP 4: Prepare Data for Training
print("=" * 70)
print("STEP 4: Preparing data for training...")
print("=" * 70)

X = df[['N', 'P', 'K', 'pH', 'Moisture', 'Temperature']]
y = df['Crop']

# Encode crop labels
label_encoder = LabelEncoder()
y_encoded = label_encoder.fit_transform(y)

crop_mapping = dict(zip(label_encoder.classes_, label_encoder.transform(label_encoder.classes_)))
print("Crop to Class Mapping:")
for crop, code in crop_mapping.items():
    print(f"  {code}: {crop}")

# Split data: 80% training, 20% testing
X_train, X_test, y_train, y_test = train_test_split(
    X, y_encoded, test_size=0.2, random_state=42, stratify=y_encoded
)

print(f"\n✓ Training set size: {X_train.shape[0]}")
print(f"✓ Testing set size: {X_test.shape[0]}\n")

# STEP 5: Train Random Forest Model
print("=" * 70)
print("STEP 5: Training Random Forest model...")
print("=" * 70)

rf_model = RandomForestClassifier(
    n_estimators=100,
    max_depth=15,
    min_samples_split=5,
    min_samples_leaf=2,
    random_state=42,
    n_jobs=-1
)

rf_model.fit(X_train, y_train)
print("✓ Model training completed!")

# Make predictions
y_pred = rf_model.predict(X_test)
accuracy = accuracy_score(y_test, y_pred)
print(f"✓ Model Accuracy: {accuracy:.4f} ({accuracy*100:.2f}%)\n")

# STEP 6: Model Evaluation
print("=" * 70)
print("STEP 6: Model Evaluation")
print("=" * 70)

print("\nClassification Report:")
print(classification_report(y_test, y_pred, target_names=label_encoder.classes_))

feature_importance = pd.DataFrame({
    'Feature': X.columns,
    'Importance': rf_model.feature_importances_
}).sort_values('Importance', ascending=False)

print("\nFeature Importance:")
print(feature_importance)
print()

# STEP 7: Visualize Results
print("=" * 70)
print("STEP 7: Creating visualizations...")
print("=" * 70)

fig, axes = plt.subplots(2, 2, figsize=(14, 10))

# Confusion Matrix
cm = confusion_matrix(y_test, y_pred)
sns.heatmap(cm, annot=True, fmt='d', cmap='Blues', xticklabels=label_encoder.classes_,
            yticklabels=label_encoder.classes_, ax=axes[0, 0])
axes[0, 0].set_title('Confusion Matrix', fontsize=12, fontweight='bold')
axes[0, 0].set_ylabel('True Label')
axes[0, 0].set_xlabel('Predicted Label')

# Feature Importance
axes[0, 1].barh(feature_importance['Feature'], feature_importance['Importance'], color='steelblue')
axes[0, 1].set_xlabel('Importance')
axes[0, 1].set_title('Feature Importance', fontsize=12, fontweight='bold')
axes[0, 1].invert_yaxis()

# Crop Distribution
crop_counts = df['Crop'].value_counts()
axes[1, 0].bar(crop_counts.index, crop_counts.values, color='skyblue')
axes[1, 0].set_title('Crop Distribution in Dataset', fontsize=12, fontweight='bold')
axes[1, 0].set_ylabel('Number of Samples')
axes[1, 0].tick_params(axis='x', rotation=45)

# Accuracy by Crop
accuracies = []
crops = []
for crop_idx in range(len(label_encoder.classes_)):
    mask = y_test == crop_idx
    if mask.sum() > 0:
        acc = accuracy_score(y_test[mask], y_pred[mask])
        accuracies.append(acc)
        crops.append(label_encoder.classes_[crop_idx])

axes[1, 1].bar(crops, accuracies, color='lightgreen')
axes[1, 1].set_title('Accuracy by Crop', fontsize=12, fontweight='bold')
axes[1, 1].set_ylabel('Accuracy')
axes[1, 1].set_ylim(0, 1)
axes[1, 1].tick_params(axis='x', rotation=45)
for i, v in enumerate(accuracies):
    axes[1, 1].text(i, v + 0.02, f'{v:.2%}', ha='center', fontsize=10)

plt.tight_layout()
plt.savefig('crop_recommendation_analysis.png', dpi=150, bbox_inches='tight')
plt.show()

print("✓ Visualization saved as 'crop_recommendation_analysis.png'\n")

# STEP 8: Convert Model to ONNX Format
print("=" * 70)
print("STEP 8: Converting model to ONNX format...")
print("=" * 70)

initial_type = [('float_input', FloatTensorType([None, 6]))]

onnx_model = convert_sklearn(rf_model, initial_types=initial_type, target_opset=12)

onnx_filename = 'crop_recommendation_model.onnx'
with open(onnx_filename, 'wb') as f:
    f.write(onnx_model.SerializeToString())

file_size = os.path.getsize(onnx_filename) / 1024
print(f"✓ ONNX model saved as '{onnx_filename}'")
print(f"✓ File size: {file_size:.2f} KB\n")

# STEP 9: Verify ONNX Model
print("=" * 70)
print("STEP 9: Verifying ONNX model...")
print("=" * 70)

onnx_loaded = onnx.load(onnx_filename)

try:
    onnx.checker.check_model(onnx_loaded)
    print("✓ ONNX model is valid!")
except onnx.checker.ValidationError as e:
    print(f"✗ Model validation error: {e}")

print("\nONNX Model Information:")
print(f"  IR Version: {onnx_loaded.ir_version}")
print(f"  Producer Name: {onnx_loaded.producer_name}")

print(f"\nInput:")
for input_node in onnx_loaded.graph.input:
    print(f"  Name: {input_node.name}")
    print(f"  Shape: {[dim.dim_value for dim in input_node.type.tensor_type.shape.dim]}")

print(f"\nOutput:")
for output_node in onnx_loaded.graph.output:
    print(f"  Name: {output_node.name}")
    print(f"  Shape: {[dim.dim_value for dim in output_node.type.tensor_type.shape.dim]}")

print()

# STEP 10: Test ONNX Model with Sample Data
print("=" * 70)
print("STEP 10: Testing ONNX Model with Sample Soil Parameters")
print("=" * 70)

sess = rt.InferenceSession(onnx_filename)

input_name = sess.get_inputs()[0].name
output_name = sess.get_outputs()[0].name
label_name = sess.get_outputs()[1].name

print(f"Input name: {input_name}")
print(f"Output name: {output_name}")
print(f"Label name: {label_name}\n")

# Test samples for each crop
test_samples = {
    'Banana': np.array([[125, 37, 125, 6.2, 70, 27]], dtype=np.float32),
    'Cassava': np.array([[100, 30, 100, 5.5, 60, 24]], dtype=np.float32),
    'Sweet Potato': np.array([[90, 40, 110, 6.3, 65, 26]], dtype=np.float32),
    'Corn': np.array([[150, 55, 150, 6.7, 70, 24]], dtype=np.float32),
    'Mango': np.array([[115, 47, 135, 6.5, 50, 30]], dtype=np.float32)
}

for crop_name, sample_data in test_samples.items():
    pred_probs_dict = sess.run([output_name], {input_name: sample_data})[0]

    # Handle both dictionary and array outputs from ONNX
    if isinstance(pred_probs_dict, dict):
        # Extract probabilities from dictionary
        pred_probs = [pred_probs_dict.get(i, 0.0) for i in range(len(label_encoder.classes_))]
        pred_class_idx = max(pred_probs_dict, key=pred_probs_dict.get)
    else:
        # If it's an array, use it directly; otherwise wrap scalar in array
        if hasattr(pred_probs_dict, '__len__'):
            pred_probs = pred_probs_dict
        else:
            pred_probs = np.array([pred_probs_dict])
        pred_class_idx = np.argmax(pred_probs)

    predicted_crop = label_encoder.classes_[pred_class_idx]

    print(f"{crop_name} Soil Parameters:")
    print(f"  N: {sample_data[0,0]:.1f} ppm, P: {sample_data[0,1]:.1f} ppm, K: {sample_data[0,2]:.1f} ppm")
    print(f"  pH: {sample_data[0,3]:.1f}, Moisture: {sample_data[0,4]:.1f}%, Temperature: {sample_data[0,5]:.1f}°C")
    print(f"  → Predicted Crop: {predicted_crop}")
    print(f"  → Confidence Scores:")
    for idx, crop in enumerate(label_encoder.classes_):
        if isinstance(pred_probs_dict, dict):
            score = pred_probs_dict.get(idx, 0.0)
        else:
            score = float(pred_probs[idx]) if idx < len(pred_probs) else 0.0
        print(f"     {crop}: {score:.4f}")
    print()

# STEP 11: Download ONNX Model (for Google Colab)
print("=" * 70)
print("STEP 11: Downloading ONNX Model")
print("=" * 70)

try:
    from google.colab import files
    print("Google Colab detected. Downloading crop_recommendation_model.onnx...")
    files.download(onnx_filename)
    print("✓ File downloaded successfully!")
except ImportError:
    print("Not running in Google Colab")
    print(f"ONNX model saved at: {os.path.abspath(onnx_filename)}")

print()

# STEP 12: Final Summary
print("=" * 70)
print("CROP RECOMMENDATION MODEL - FINAL SUMMARY")
print("=" * 70)

print(f"\n📊 MODEL INFORMATION:")
print(f"  Model Type: Random Forest Classifier")
print(f"  Number of Trees: {rf_model.n_estimators}")
print(f"  Max Depth: {rf_model.max_depth}")
print(f"  Total Training Samples: {len(X_train)}")
print(f"  Total Testing Samples: {len(X_test)}")
print(f"  Model Accuracy: {accuracy:.4f} ({accuracy*100:.2f}%)")

print(f"\n🌾 SUPPORTED CROPS:")
for i, crop in enumerate(label_encoder.classes_):
    print(f"  {i}: {crop}")

print(f"\n📝 INPUT FEATURES (IN ORDER):")
print(f"  1. N (Nitrogen): ppm")
print(f"  2. P (Phosphorus): ppm")
print(f"  3. K (Potassium): ppm")
print(f"  4. pH: 3.0-9.0")
print(f"  5. Moisture: 0-100 %")
print(f"  6. Temperature: 15-35 °C")

print(f"\n🚀 HOW TO USE THE ONNX MODEL:")
print(f"  1. Load the model with ONNX Runtime")
print(f"  2. Prepare input data as float32 array: [[N, P, K, pH, Moisture, Temperature]]")
print(f"  3. Run inference to get predictions")
print(f"  4. Map the output class to crop name using label_encoder")

print(f"\n✅ OUTPUT FILES:")
print(f"  • crop_recommendation_model.onnx (Model file)")
print(f"  • crop_recommendation_analysis.png (Visualization)")

print("\n" + "=" * 70)
print("TRAINING AND CONVERSION COMPLETE! ✓")
print("=" * 70)
