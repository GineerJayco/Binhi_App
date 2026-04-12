!pip install skl2onnx onnxmltools

import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier
from sklearn.tree import DecisionTreeClassifier
from xgboost import XGBClassifier
from sklearn.metrics import accuracy_score

# ONNX Libraries
from skl2onnx import to_onnx
from skl2onnx.common.data_types import FloatTensorType as SklearnFloatType
import onnxmltools
from onnxmltools.convert.common.data_types import FloatTensorType as XGBoostFloatType

# 1. LOAD DATA
print("Loading data...")
df = pd.read_csv('TINAGO_5000.csv')
print(f"Dataset shape: {df.shape}")

# Extract features and labels
X = df.drop('Crop', axis=1).values.astype(np.float32)
y_raw = df['Crop'].str.strip().str.lower()

# 2. CROP MAPPING (0-4 index order)
# 0: Banana, 1: Cassava, 2: Corn, 3: Coconut, 4: Sweet Potato
mapping = {
    'banana': 0,
    'cassava': 1,
    'corn': 2,
    'coconut': 3,
    'sweet potato': 4
}
y_encoded = y_raw.map(mapping)

# Remove NaN values (unmapped crops)
valid_idx = ~y_encoded.isna()
X = X[valid_idx]
y_encoded = y_encoded[valid_idx].values.astype(int)

print(f"Data after cleaning: {X.shape}")
print(f"Label distribution: {np.bincount(y_encoded)}")

# Crop names (must match indices 0-4)
class_names = ['Banana', 'Cassava', 'Corn', 'Coconut', 'Sweet Potato']

# 3. SPLIT DATA (NO SCALING - RAW DATA ONLY)
X_train, X_test, y_train, y_test = train_test_split(X, y_encoded, test_size=0.2, random_state=42)
print(f"Training set: {X_train.shape}, Test set: {X_test.shape}")

# 4. TRAIN MODELS (3 models: Decision Tree, Random Forest, XGBoost)
# Training on RAW data (NO scaling)
models = {
    "decision_tree": DecisionTreeClassifier(random_state=42),
    "random_forest": RandomForestClassifier(n_estimators=100, random_state=42),
    "xgboost": XGBClassifier(eval_metric='mlogloss', verbosity=0)
}

print("\n--- TRAINING MODELS (RAW DATA - NO SCALING) ---")
for name, model in models.items():
    model.fit(X_train, y_train)
    acc = accuracy_score(y_test, model.predict(X_test))
    print(f"{name.upper():<20} Accuracy: {acc*100:.2f}%")

# 5. EXPORT TO ONNX
print("\n--- EXPORTING ONNX FILES ---")
for name, model in models.items():
    if name == "xgboost":
        initial_type = [('float_input', XGBoostFloatType([None, 6]))]
        onx = onnxmltools.convert_xgboost(model, initial_types=initial_type)
    else:
        initial_type = [('float_input', SklearnFloatType([None, 6]))]
        onx = to_onnx(model, initial_types=initial_type)

    file_name = f"{name}.onnx"
    with open(file_name, "wb") as f:
        f.write(onx.SerializeToString())
    print(f"✅ Saved: {file_name}")

print("\n✅ SUCCESS: Models trained on RAW data and exported to ONNX!")
print("Models are ready for Android deployment.\n")

# 6. MULTI MODEL TESTER
def test_models_final_loop():
    while True:
        print("\n" + "="*65)
        print("   BINHI MULTI-MODEL COMPARISON (RAW DATA)")
        print("="*65)
        print("Enter: N, P, K, pH, Temp, Moisture")
        print("Type 'q' to quit.")

        user_input = input("\nInput Values: ").strip().lower()

        if user_input in ['q', 'exit', 'quit']:
            print("\n✅ Testing session closed.")
            break

        try:
            # Parse raw values (NO scaling)
            vals = [float(x.strip()) for x in user_input.replace(',', ' ').split()]
            if len(vals) != 6:
                print(f"\n❌ Error: Expected 6 values, you entered {len(vals)}.")
                continue

            # Use raw values directly (no scaler)
            raw_vals = np.array([vals])

            # 2. SUMMARY TABLE (The Winners)
            print("\n" + "-"*65)
            print(f"{'MODEL':<20} | {'TOP RECOMMENDATION':<20} | {'CONFIDENCE':<10}")
            print("-"*65)

            for name, model in models.items():
                probs = model.predict_proba(raw_vals)[0]
                top_idx = np.argmax(probs)
                confidence = probs[top_idx] * 100
                print(f"{name.upper():<20} | {class_names[top_idx]:<20} | {confidence:>6.1f}%")

            # 3. FULL BREAKDOWN (All crops for each model)
            print("\n" + "-"*65)
            print("FULL PROBABILITY BREAKDOWN (ALL CROPS)")
            print("-"*65)

            for name, model in models.items():
                print(f"\n[{name.upper()}]")
                probs = model.predict_proba(raw_vals)[0]

                # Create and sort crop results
                crop_results = sorted(
                    [(class_names[i], probs[i] * 100) for i in range(len(class_names))],
                    key=lambda x: x[1], reverse=True
                )

                # Print each crop as a clean row
                for crop, pct in crop_results:
                    print(f"   {crop:<20}: {pct:>6.1f}%")

        except ValueError:
            print("\n❌ Error: Invalid input. Use numbers only.")
        except Exception as e:
            print(f"\n❌ Error: {e}")

# Run testing loop automatically
test_models_final_loop()
