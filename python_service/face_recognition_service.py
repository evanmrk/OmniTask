#!/usr/bin/env python3
"""
Face Recognition Service for OmniTask
This service handles biometric verification using face recognition.
It serves as a microservice called by the Java backend via ProcessBuilder.
"""

import sys
import os
import face_recognition
import numpy as np
import shutil
from pathlib import Path

# Directory to store employee face encodings
# Adjust this path if necessary relative to where the script is run
STORED_FACES_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "stored_faces")

def load_known_face(employee_id):
    """
    Load the stored face encoding for an employee.
    """
    face_path = os.path.join(STORED_FACES_DIR, f"{employee_id}.jpg")

    if not os.path.exists(face_path):
        print(f"ERROR: No stored face found for employee {employee_id}")
        return None

    try:
        image = face_recognition.load_image_file(face_path)
        encodings = face_recognition.face_encodings(image)

        if len(encodings) == 0:
            print(f"ERROR: No face found in stored image for {employee_id}")
            return None

        return encodings[0]
    except Exception as e:
        print(f"ERROR: Failed to load face encoding: {str(e)}")
        return None

def verify_face(captured_image_path, employee_id):
    """
    Verify if the captured face matches the stored face.
    """
    # 1. Load known face encoding
    known_encoding = load_known_face(employee_id)
    if known_encoding is None:
        return False

    # 2. Validate captured image existence
    if not os.path.exists(captured_image_path):
        print(f"ERROR: Captured image not found: {captured_image_path}")
        return False

    try:
        # 3. Load and encode captured image
        captured_image = face_recognition.load_image_file(captured_image_path)
        captured_encodings = face_recognition.face_encodings(captured_image)

        if len(captured_encodings) == 0:
            print("ERROR: No face detected in captured image")
            return False

        # 4. Compare faces
        # compare_faces returns a list of True/False values
        captured_encoding = captured_encodings[0]
        matches = face_recognition.compare_faces([known_encoding], captured_encoding, tolerance=0.6)

        # 5. Calculate face distance (lower is better match)
        face_distance = face_recognition.face_distance([known_encoding], captured_encoding)

        print(f"DEBUG: Face distance: {face_distance[0]:.4f}")

        # 6. Final Decision
        # A match is confirmed if compare_faces is True AND distance is low enough
        if matches[0] and face_distance[0] < 0.6:
            print("MATCH: Face verification successful")
            return True
        else:
            print("NO_MATCH: Face verification failed")
            return False

    except Exception as e:
        print(f"ERROR: Face verification failed: {str(e)}")
        return False

def register_face(image_path, employee_id):
    """
    Register a new face for an employee.
    This copies the validated image to the stored_faces directory.
    """
    if not os.path.exists(image_path):
        print(f"ERROR: Image not found: {image_path}")
        return False

    try:
        # Create directory if it doesn't exist
        os.makedirs(STORED_FACES_DIR, exist_ok=True)

        # Load and verify image contains a face before saving
        image = face_recognition.load_image_file(image_path)
        encodings = face_recognition.face_encodings(image)

        if len(encodings) == 0:
            print("ERROR: No face detected in image. Cannot register.")
            return False

        # Save image to stored_faces directory
        destination = os.path.join(STORED_FACES_DIR, f"{employee_id}.jpg")
        shutil.copy(image_path, destination)

        print(f"SUCCESS: Face registered for employee {employee_id}")
        return True

    except Exception as e:
        print(f"ERROR: Face registration failed: {str(e)}")
        return False

def main():
    """
    Main entry point. Java calls this script with arguments.
    Usage:
      1. Verification: python script.py <captured_image_path> <employee_id>
      2. Registration: python script.py --register <image_path> <employee_id>
    """
    if len(sys.argv) < 3:
        print("Usage:")
        print("  Verify:   python face_recognition_service.py <captured_image_path> <employee_id>")
        print("  Register: python face_recognition_service.py --register <image_path> <employee_id>")
        sys.exit(1)

    # Handle Registration Mode
    if sys.argv[1] == "--register":
        if len(sys.argv) != 4:
            print("Error: Missing arguments for registration.")
            sys.exit(1)

        image_path = sys.argv[2]
        employee_id = sys.argv[3]

        success = register_face(image_path, employee_id)
        # Exit with 0 if success, 1 if failed (Java checks this exit code)
        sys.exit(0 if success else 1)

    # Handle Verification Mode (Default)
    else:
        captured_image_path = sys.argv[1]
        employee_id = sys.argv[2]

        success = verify_face(captured_image_path, employee_id)
        sys.exit(0 if success else 1)

if __name__ == "__main__":
    main()