#!/bin/bash

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
BUILD_DIR="$PROJECT_DIR/target/llama-cpp"

echo "Building llama.cpp native library..."

# Create build directory
mkdir -p "$BUILD_DIR"
cd "$BUILD_DIR"

# Clone llama.cpp if not exists
if [ ! -d "llama.cpp" ]; then
echo "Cloning llama.cpp repository..."
git clone https://github.com/ggerganov/llama.cpp.git
cd llama.cpp
git checkout b3561 # Use stable version
else
cd llama.cpp
git pull
fi

# Check for CUDA
if command -v nvcc &> /dev/null; then
echo "CUDA detected, building with GPU support..."
make LLAMA_CUBLAS=1 -j$(nproc)

# Copy CUDA library
mkdir -p "$BUILD_DIR/lib/cuda"
cp libllama.so "$BUILD_DIR/lib/cuda/"
else
echo "Building CPU-only version..."
make -j$(nproc)
fi

# Copy CPU library
mkdir -p "$BUILD_DIR/lib/cpu"
cp libllama.so "$BUILD_DIR/lib/cpu/"

# Copy headers
mkdir -p "$BUILD_DIR/include"
cp llama.h "$BUILD_DIR/include/"

echo "llama.cpp build completed successfully!"
