package org.jetbrains.kannotator.annotationsInference.engine;

import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;
import org.objectweb.asm.tree.analysis.Value;

public class InferenceFrame<V extends Value> extends Frame<V> {
    private V lostValue;

    public InferenceFrame(int nLocals, int nStack) {
        super(nLocals, nStack);
    }

    public InferenceFrame(Frame<? extends V> src) {
        super(src);
    }

    public V getLostValue() {
        return lostValue;
    }

    public void setLostValue(V lostValue) {
        this.lostValue = lostValue;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Frame<V> init(Frame<? extends V> src) {
        if (!(src instanceof InferenceFrame)) {
            throw new IllegalArgumentException("InferenceFrame expected");
        }
        InferenceFrame<V> inferenceFrame = (InferenceFrame<V>)src;

        super.init(inferenceFrame);

        this.lostValue = inferenceFrame.lostValue;

        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean merge(Frame<? extends V> frame, Interpreter<V> interpreter) throws AnalyzerException {
        if (!(frame instanceof InferenceFrame)) {
            throw new IllegalArgumentException("InferenceFrame expected");
        }

        InferenceFrame<V> inferenceFrame = (InferenceFrame<V>)frame;

        boolean changed = super.merge(frame, interpreter);

        if (lostValue == null) {
            lostValue = inferenceFrame.lostValue;
            changed |= lostValue != null;
        } else if (inferenceFrame.lostValue != null) {
            V newLostValue = interpreter.merge(lostValue, inferenceFrame.lostValue);
            changed |= lostValue != newLostValue;
            lostValue = newLostValue;
        }

        return changed;
    }
}
