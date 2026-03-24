package com.example.wantuch.ml;

import android.content.Context;
import android.util.Log;
import org.tensorflow.lite.Interpreter;
import java.nio.ByteBuffer;

/**
 * Simple, direct TensorFlow Lite wrapper using the standalone interpreter.
 * No reflection, no GMS dependency — works immediately on any Android device.
 */
public class TfLiteWrapper {
    private Interpreter interpreter;
    private final String TAG = "TfLiteWrapper";

    public void initialize(Context context) {
        // No async initialization needed for standalone TFLite
        Log.d(TAG, "Standalone TFLite — no pre-initialization needed.");
    }

    public synchronized void loadModel(ByteBuffer modelBuffer) {
        try {
            close();
            modelBuffer.rewind();
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(4);
            interpreter = new Interpreter(modelBuffer, options);
            Log.d(TAG, "Interpreter loaded successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to load TFLite Interpreter: " + e.getMessage());
            interpreter = null;
        }
    }

    public synchronized void runInference(ByteBuffer input, float[][] output) throws Exception {
        if (interpreter == null) {
            throw new IllegalStateException("runInference called but interpreter is null!");
        }
        input.rewind();
        interpreter.run(input, output);
    }

    public int getInputSize() {
        if (interpreter != null && interpreter.getInputTensorCount() > 0) {
            return interpreter.getInputTensor(0).shape()[1];
        }
        return 160;
    }

    public int getOutputSize() {
        if (interpreter != null && interpreter.getOutputTensorCount() > 0) {
            return interpreter.getOutputTensor(0).shape()[1];
        }
        return 512;
    }

    public boolean isModelNCHW() {
        if (interpreter != null && interpreter.getInputTensorCount() > 0) {
            int[] shape = interpreter.getInputTensor(0).shape();
            // NCHW format usually looks like [1, 3, 112, 112]
            return shape.length == 4 && shape[1] == 3;
        }
        return false;
    }

    public void close() {
        if (interpreter != null) {
            try {
                interpreter.close();
            } catch (Exception ignored) {}
            interpreter = null;
        }
    }

    public boolean isReady() {
        return interpreter != null;
    }
}
